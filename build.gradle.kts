plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.dokka)
}

// Publish conventions shared by all library modules (see gradle/publish.gradle).
apply(from = rootProject.file("gradle/publish.gradle"))

// The demo module is not published; its public surface is not part of the
// library ABI contract.
apiValidation {
    ignoredProjects += listOf("sample", "shared")
}

// Assembles the Maven Central deployment bundle: one zip containing the Maven
// repository layout (group/module/version/{pom,module,jars,sources,javadoc,asc})
// that the Central Portal accepts via its Publisher API.
tasks.register<Zip>("assembleCentralBundle") {
    group = "publishing"
    description = "Assemble the Maven Central deployment bundle for the Central Portal."
    val published = subprojects.filter { it.name.startsWith("kotlintrace") }
    dependsOn(":publishAllPublicationsToCentralStagingRepository")
    published.forEach { dependsOn("${it.path}:publishAllPublicationsToCentralStagingRepository") }
    from(layout.buildDirectory.dir("central-staging"))
    archiveFileName.set("kotlintrace-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central"))
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
