package net.rentalhost.plugins.hammer.services

import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message
import java.awt.Component

abstract class ExceptionService(private val projectService: ProjectService): ErrorReportSubmitter() {
    private val initializeSentry: Unit by lazy {
        Sentry.init { options: SentryOptions -> options.dsn = projectService.sentryDsn }
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        initializeSentry

        for (event in events) {
            if (event !is IdeaReportingEvent) {
                continue
            }

            val message = Message()
            message.message = event.getThrowableText().lines().firstOrNull() ?: event.getMessage()

            val sentryEvent = SentryEvent(event.data.throwable)
            sentryEvent.message = message
            sentryEvent.level = SentryLevel.ERROR

            with(event.plugin as IdeaPluginDescriptor) {
                sentryEvent.release = version
                sentryEvent.environment = "production"
            }

            sentryEvent.setTag("IDE", ApplicationInfo.getInstance().build.asString())
            sentryEvent.setTag("OS", SystemInfo.getOsNameAndVersion())

            if (additionalInfo != null) {
                sentryEvent.setExtra("Additional info", additionalInfo)
            }

            Sentry.captureEvent(sentryEvent)
        }

        consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))

        return true
    }

    override fun getReportActionText(): String =
        "Report to ${projectService.name} plugin"
}
