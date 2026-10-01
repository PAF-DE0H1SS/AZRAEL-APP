import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val azraelVersion: String by project

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":composeApp"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "xyz.azraellab.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "xyz.azraellab.app"
            // Та же версия, что в APK: раньше число было продублировано вручную
            // и разъехалось (в артефактах 1.1.0 при versionName 1.2.1 в APK).
            packageVersion = azraelVersion
            description = "AZRAEL-APP - client for azrael-lab.xyz"
            vendor = "AZRAEL Lab"
            copyright = "Copyright (C) 2026 AZRAEL Lab"
            // Без этих полей jpackage писал в .desktop «Categories=Unknown»,
            // в control — «Maintainer: AZRAEL Lab <Unknown>», и в меню приложений
            // AZRAEL-APP попадал в случайную/ни одну секцию.
            linux {
                appCategory = "Network"
                menuGroup = "Network"
                // Compose подставляет сюда vendor сам, поэтому здесь только
                // e-mail: со словом «AZRAEL Lab» получалось
                // «Maintainer: AZRAEL Lab <AZRAEL Lab <support@…>>».
                debMaintainer = "support@azrael-lab.xyz"
            }
        }
    }
}