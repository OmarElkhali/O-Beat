@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.dd3boh.outertune"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.obeat.ocompany"
        minSdk = 24
        targetSdk = 36
        versionCode = 67
        versionName = "0.9.3.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // === Forcer le nom affich� de l�app ===

    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("ot_release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                (keystoreProperties["keyAlias"] as? String)?.let { keyAlias = it }
                (keystoreProperties["keyPassword"] as? String)?.let { keyPassword = it }
                (keystoreProperties["storePassword"] as? String)?.let { storePassword = it }
            }
        } else {
            create("ot_release") { }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("ot_release")
            buildConfigField("boolean", "SHOW_DEBUG_WATERMARK", "false")
        }
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "SHOW_DEBUG_WATERMARK", "false")
        }
        // userdebug = release sans minification
        create("userdebug") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("boolean", "SHOW_DEBUG_WATERMARK", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // build variants and stuff
    splits {
        abi {
            isEnable = true
            reset()
            include("x86_64", "x86", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    flavorDimensions.add("abi")

    productFlavors {
        // main version
        create("core") {
            isDefault = true
            dimension = "abi"
        }
        // fully featured version, large file size
        create("full") {
            dimension = "abi"
        }
    }

    // === Renommage des APK (compatible avec ton AGP actuel) ===
    applicationVariants.all {
        val variant = this
        variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { output ->
                    val outputFileName =
                            "O-Beat-${variant.versionName}-${output.baseName}-${output.versionCode}.apk"
                    output.outputFileName = outputFileName
                }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    tasks.withType<KotlinCompile> {
        if (!name.substringAfter("compile").lowercase().startsWith("full")) {
            exclude("**/*FFmpegScanner.kt")
        } else {
            exclude("**/*FFmpegScannerDud.kt")
        }
    }

    aboutLibraries {
        offlineMode = true

        collect {
            fetchRemoteLicense = false
            fetchRemoteFunding = false
            filterVariants.addAll("release")
        }

        export {
            // Remove the "generated" timestamp to allow for reproducible builds
            excludeFields = listOf("generated")
        }

        license {
            // Strict mode
            strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL
            allowedLicenses.addAll(
                    "Apache-2.0",
                    "BSD-3-Clause",
                    "GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1",
                    "EPL-2.0",
                    "MIT",
                    "MPL-2.0",
                    "Public Domain"
            )
            // Textes de licences additionnels
            additionalLicenses.addAll("apache_2_0", "gpl_2_1") // taglib, ffMpeg in ffMetadataEx
        }

        library {
            duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
            duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
        }
    }

    // for RB
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    lint {
        lintConfig = file("lint.xml")
    }

    androidResources {
        generateLocaleConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    // compose
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)
    implementation(libs.compose.icons.extended)

    // ui
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.lazycolumnscrollbar)
    implementation(libs.shimmer)

    // material
    implementation(libs.adaptive)
    implementation(libs.material3)
    implementation(libs.palette)

    // viewmodel
    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.media3)
    implementation(libs.media3.okhttp)
    implementation(libs.media3.session)
    implementation(libs.media3.workmanager)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.json)

    // modules
    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":material-color-utilities"))

    // misc
    implementation(libs.aboutlibraries.compose.m3)

    // sdk24 support
    implementation("androidx.webkit:webkit:1.14.0")
    implementation(project(":taglib"))
}

afterEvaluate {
    dependencies {
        add("fullImplementation", project(":ffMetadataEx"))
    }
}