package net.rentalhost.plugins.hammer.extensions.psi

import com.intellij.psi.PsiFile

fun PsiFile.getBasename(): String =
    name.substringBefore(".")

fun PsiFile.isBlade(): Boolean =
    name.endsWith(".blade.php")
