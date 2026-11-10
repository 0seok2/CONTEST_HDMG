plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pedalguardian"
    compileSdk = 34
    // AAOS Car API(CarPropertyManager)를 쓰기 위한 시스템 라이브러리
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.pedalguardian"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }
    buildTypes {
        release {
            // 공모전 심사용: 별도 키 없이 바로 설치되도록 디버그 키로 서명한다.
            // ponytail: App Market 배포 때는 전용 키스토어로 바꿔야 한다.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    // Pleos Vehicle SDK (Brake API)
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    testImplementation("junit:junit:4.13.2")
}
