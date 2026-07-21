plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinParcelize)
}

android {
    namespace = "org.idpass.smartscanner.lib"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables.useSupportLibrary = true

        // Sentry Default DSN
        manifestPlaceholders["dsn"] = "https://90ebf03b06534e01a21f82c1b2e86ae2@sentry.newlogic.dev/4"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    testOptions {
        // MrzParser (bundled parser) calls android.util.Log; let JVM unit tests use the stub.
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(files("libs/jj2000_imageutil.jar"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ML Kit dependencies
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // MRZ
    implementation(libs.slf4j.android)
    // Gson
    implementation(libs.gson)
    // Glide
    implementation(libs.glide)
    implementation(libs.glide.transformations)
    annotationProcessor(libs.glide.compiler)

    // ID PASS lite
    implementation(libs.protobuf.lite)
    implementation("org.idpass:idpass-lite-java-android:0.1@aar")

    // SmartScanner Projects
    implementation(project(":smartscanner-mrz-parser"))
    implementation(project(":smartscanner-android-api"))

    // JSON
    implementation(libs.json.path)
    implementation(libs.json.flattener)

    // NFC
    implementation(libs.jmrtd)
    implementation(libs.spongycastle.prov)
    implementation(libs.scuba.sc.android)
    implementation(libs.cert.cvc)

    // WSQ
    implementation(libs.jnbis)
    // DatatypeConverter
    implementation(libs.commons.codec)

    // RX
    implementation(libs.rxandroid)
    implementation(libs.rxjava)

    // Sentry
    implementation(libs.sentry.android)

    // WorkManager
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.runtime.ktx)

    // JWT
    api(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.orgjson) {
        exclude(group = "org.json", module = "json")
    }
    implementation(libs.jjwt.gson)

    // Barcode
    implementation(libs.zxing.android.embedded)
    implementation(libs.zxing.core)
}
