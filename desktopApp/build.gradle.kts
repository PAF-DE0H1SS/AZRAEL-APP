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
        }
    }
}