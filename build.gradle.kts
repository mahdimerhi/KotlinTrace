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
            baseName = "KotlinTrace"
            isStatic = true
        }

        // Exposes the Kotlin/Native runtime's source-info dispatcher
        // (Kotlin_getSourceInfo_Function) and the bundled libbacktrace
        // backend so the library can redirect the former to the latter
        // on the iOS simulator (where CoreSymbolication is broken).
        compilations.getByName("main").cinterops.create("sourceinfohook") {
            defFile(project.file("src/nativeInterop/cinterop/sourceinfohook.def"))
            compilerOpts("-I${project.file("src/nativeInterop/cinterop").absolutePath}")
        }
    }

    sourceSets {
        commonMain.dependencies {
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
