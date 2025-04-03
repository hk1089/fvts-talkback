plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.hk1089.mettax"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi", "armeabi-v7a",
                "arm64-v8a")
        }
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs(setOf("src/main/jniLibs"))
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

afterEvaluate {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.hk1089"
                artifactId = "fvts-talkback"
                version = "1.0.4"
            }
        }
    }
}


dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

   // implementation(libs.okhttp)
    // implementation("com.alibaba:fastjson:1.1.72.android")
    //implementation(libs.fastjson)
   // implementation("com.alibaba:fastjson:1.2.83") { exclude(group = "com.alibaba", module = "fastjson-support-jaxrs") }
    // For Permission.
    implementation(libs.permissionx)

    implementation(files("libs/commons-lang-2.3.jar"))
    implementation(files("libs/commons-logging-api-1.0.4.jar"))
    implementation(files("libs/httpcore-4.4.11.jar"))
    implementation(files("libs/microlog4android-1.1.jar"))
    implementation(files("libs/pinyin4j-2.5.0.jar"))
}