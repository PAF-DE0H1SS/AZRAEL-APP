import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "xyz.azraellab.shared"
        compileSdk = 37
        minSdk = 33

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }
    }

    jvm("desktop")

    sourceSets {
        // Раньше androidLibrary и jvm("desktop") пришлось соединять отдельным
        // jvmCommonMain ради Ed25519 на BigInteger. Теперь крипто — чистый
        // Kotlin в commonMain (core/crypto/{Ed25519,Fe25519,Sha512}.kt), и
        // посредник не нужен.
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        val desktopTest by getting
        desktopTest.dependencies {
            implementation(kotlin("test"))
        }
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
        }
    }
}