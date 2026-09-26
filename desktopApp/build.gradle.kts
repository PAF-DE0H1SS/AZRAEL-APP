import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
            packageVersion = "1.1.0"
            description = "AZRAEL-APP - client for azrael-lab.xyz"
            vendor = "AZRAEL Lab"
        }
    }
}