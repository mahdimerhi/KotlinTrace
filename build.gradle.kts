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
// The staging repository accumulates across builds, so wipe it before any
// publication re-populates it, then zip the result into the bundle.
val cleanCentralStaging = tasks.register<Delete>("cleanCentralStaging") {
    delete(layout.buildDirectory.dir("central-staging"))
}

tasks.register<Zip>("assembleCentralBundle") {
    group = "publishing"
    description = "Assemble the Maven Central deployment bundle for the Central Portal."
    val published = subprojects.filter { it.name.startsWith("kotlintrace") }
    dependsOn(cleanCentralStaging)
    dependsOn(":publishAllPublicationsToCentralStagingRepository")
    published.forEach { dependsOn("${it.path}:publishAllPublicationsToCentralStagingRepository") }
    from(layout.buildDirectory.dir("central-staging"))
    archiveFileName.set("kotlintrace-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central"))
}

// Ensure the wipe runs before any module re-publishes into the staging repo
// (wired after the module is evaluated, when its publish tasks exist).
subprojects.filter { it.name.startsWith("kotlintrace") }.forEach { p ->
    p.afterEvaluate {
        p.tasks.named("publishAllPublicationsToCentralStagingRepository").configure {
            dependsOn(cleanCentralStaging)
        }
    }
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
