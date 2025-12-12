import org.gradle.kotlin.dsl.implementation
import java.util.Properties // 👈 1. 新增：导入 Properties 类

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.stay_healthy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.stay_healthy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 👇👇👇 2. 新增：读取 local.properties 的 Kotlin 写法 👇👇👇
        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }

        // 获取 Key，如果没有则为空字符串
        val apiKey = properties.getProperty("GEMINI_API_KEY") ?: ""

        // 生成 BuildConfig 字段
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
        // 👆👆👆 新增结束 👆👆👆
    }

    // 👇👇👇 3. 新增：开启 BuildConfig 功能 👇👇👇
    buildFeatures {
        buildConfig = true
        viewBinding = true // 如果你之前用到ViewBinding，也建议开启
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Google Play Services
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-firestore:24.10.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database:20.3.1")

    // AndroidX Libraries from libs.versions.toml
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.transition:transition:1.4.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
}