plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp") version libs.versions.ksp.get()
}

android {

    namespace = "com.ramcosta.playground.featurex"
    compileSdk = libs.versions.compileSdk.get().toIntOrNull()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toIntOrNull()
        targetSdk = libs.versions.targetSdk.get().toIntOrNull()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles.add(File("consumer-rules.pro"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {

    implementation(project(mapOf("path" to ":compose-destinations")))
    ksp(project(":compose-destinations-ksp"))

    implementation(libs.androidMaterial)

    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.viewModel)

    implementation(libs.androidx.lifecycleRuntimeKtx)
    implementation(libs.androidx.activityCompose)

    implementation(libs.ktxSerializationJson)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)

    testImplementation(project(":compose-destinations-ksp"))
    testImplementation(libs.test.kotlinCompile)
    testImplementation(libs.test.kotlinCompileKsp)
}