import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    id("composedestinations.convention.publish")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }
    jvm()
//    macosX64()
//    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        jvmAndAndroid()
    }

    sourceSets {

        commonMain.dependencies {
            api("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha02") // TODO RACOSTA move to catalog
        }

        androidMain {
            dependsOn(commonMain.get())
//            api(libs.compose.navigation)
            dependencies {
//                api(libs.compose.navigation)
                api("org.jetbrains.androidx.core:core-bundle:1.0.0") // TODO RACOSTA why do I need this?
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=com.ramcosta.composedestinations.annotation.internal.InternalDestinationsApi"
        )
    }
}

android {

    namespace = "com.ramcosta.composedestinations"
    compileSdk = libs.versions.compileSdk.get().toIntOrNull()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toIntOrNull()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles.add(File("consumer-rules.pro"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint {
        disable.add("EmptyNavDeepLink")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinHierarchyBuilder.Root.jvmAndAndroid() {
    common {
        group("jvmAndAndroid") {
            withJvm()
            withAndroidTarget()
        }
    }
}