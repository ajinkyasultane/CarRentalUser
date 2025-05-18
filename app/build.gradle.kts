plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")}

android {
    namespace = "com.example.carrentaluser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.carrentaluser"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.messaging)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-auth:23.2.0")
    implementation ("com.google.firebase:firebase-firestore:25.1.4")
    implementation ("com.google.firebase:firebase-storage:21.0.1")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.google.android.gms:play-services-auth:21.3.0") // or the specific service you're using
    implementation ("com.google.android.gms:play-services-maps:19.2.0")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")
    
    // Lottie Animation
    implementation ("com.airbnb.android:lottie:6.3.0")
    
    // Location and directions
    implementation ("com.google.android.gms:play-services-location:21.2.0")
    implementation ("com.google.maps.android:android-maps-utils:3.4.0")
    
    // OkHttp for network requests
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
}