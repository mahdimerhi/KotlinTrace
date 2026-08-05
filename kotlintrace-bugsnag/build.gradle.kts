// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
}

apply(from = rootProject.file("gradle/publish.gradle"))

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
        compilations.getByName("main").cinterops.create("bugsnag")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":"))
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
