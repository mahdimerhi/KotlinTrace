// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "SampleShared"
            isStatic = true
        }
    }

    // KT-75992: the simulator's CoreSymbolication backend is broken, so the
    // KotlinTrace library redirects the runtime's source-info dispatcher to
    // the bundled libbacktrace backend and resolves file:line from the dSYM
    // embedded in the app bundle (see dev.kotlintrace.SourceInfoHook).
    sourceSets {
        commonMain.dependencies {
            implementation(project(":"))
            implementation(project(":kotlintrace-crashlytics"))
            implementation(project(":kotlintrace-sentry"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

listOf("iosSimulatorArm64Test", "iosX64Test").forEach {
    tasks.named(it) {
        enabled = false
    }
}
