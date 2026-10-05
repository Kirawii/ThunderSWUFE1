plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Keep generated artifacts in one root-level directory. This also avoids stale
// module-local lint caches being picked up by IDE and antivirus indexers.
layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("app"))

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
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


dependencies {

    // AndroidX 核心依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose 相关依赖 - 只包含必要组件
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // 网络请求 - 只包含必要组件
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson) {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // 图表库 - 只包含必要组件
    implementation(libs.vico.compose) {
        exclude(group = "androidx.compose.foundation")
        exclude(group = "androidx.compose.animation")
    }
    implementation(libs.vico.compose.m3) {
        exclude(group = "androidx.compose.material3")
    }

    // 数据持久化
    implementation(libs.androidx.datastore.preferences)

    // Room 数据库
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // 协程 - 只包含 Android 版本
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime.ktx)

    // 调试依赖
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
