plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.kirawii.thunderswufe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kirawii.thunderswufe"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"https://rhfw.swufe.edu.cn/\"")
        ndk {
            // 只保留必要的 ABI
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://rhfw.swufe.edu.cn/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://rhfw.swufe.edu.cn/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        noCompress += "tflite"
    }
}


dependencies {
    // AndroidX 核心依赖
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose 相关依赖 - 只包含必要组件
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // 网络请求 - 只包含必要组件
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 图表库 - 只包含必要组件
    implementation("com.patrykandpatrick.vico:compose:1.13.1") {
        exclude(group = "androidx.compose.foundation")
        exclude(group = "androidx.compose.animation")
    }
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1") {
        exclude(group = "androidx.compose.material3")
    }

    // 数据持久化
    implementation("androidx.datastore:datastore-preferences:1.0.0") {
        exclude(group = "androidx.lifecycle")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }

    // Room数据库 - 只包含必要组件
    implementation("androidx.room:room-runtime:2.6.1") {
        exclude(group = "androidx.sqlite")
    }
    implementation("androidx.room:room-ktx:2.6.1") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }
    ksp("androidx.room:room-compiler:2.6.1")
    
    // 协程 - 只包含 Android 版本
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager - 只包含必要组件
    implementation("androidx.work:work-runtime-ktx:2.9.0") {
        exclude(group = "androidx.lifecycle")
        exclude(group = "androidx.startup")
    }

    // TensorFlow Lite - 必要的核心组件
    implementation("org.tensorflow:tensorflow-lite:2.14.0") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-gpu")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-select-tf-ops")
    }
    implementation("org.tensorflow:tensorflow-lite-api:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-metadata")
        exclude(group = "com.google.android.gms", module = "play-services-tasks")
    }

    // 调试依赖
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}