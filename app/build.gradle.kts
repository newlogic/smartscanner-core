import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

val buildTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())

android {
    namespace = "org.newlogic.smartscanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.newlogic.smartscanner"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")

        // Demo app always uses the full variant of core-lib
        missingDimensionStrategy("variant", "full")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // signingConfigs {
    //     create("release") {
    //         // Manually configure if needed
    //     }
    // }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            // signingConfig = signingConfigs.getByName("release")
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

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.multidex)
    implementation("androidx.activity:activity-ktx:1.8.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.gson)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.glide)
    implementation(libs.timber)
    implementation(libs.zoomage)
    annotationProcessor(libs.glide.compiler)

    // ID PASS Smart Scanner
    implementation(project(":core-lib"))
    
    // ID PASS lite
    implementation(libs.protobuf.lite)
    implementation("org.idpass:idpass-lite-java-android:0.1@aar")
    
    // SmartScanner API
    implementation(project(":smartscanner-android-api"))
}
