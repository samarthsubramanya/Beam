import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.components.resources)
            }
        }
    }

    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.beam.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Beam"
            // CI passes -Pbeam.version=1.0.<run number>; jpackage only accepts numeric MAJOR.MINOR.BUILD.
            packageVersion = (findProperty("beam.version") as String?) ?: "1.0.0"
            // The jlinked runtime must include java.prefs / java.sql etc. or the packaged app crashes at launch.
            includeAllModules = true
            macOS { iconFile.set(project.file("icons/icon.icns")) }
            windows { iconFile.set(project.file("icons/icon.ico")) }
            linux { iconFile.set(project.file("icons/icon.png")) }
        }
    }
}
