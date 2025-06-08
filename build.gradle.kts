// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

buildscript {
    repositories {
        google()  // Google's Maven repository
        mavenCentral()  // Maven Central repository
    }
    
    dependencies {
        // Add the Google services Gradle plugin
        classpath("com.google.gms:google-services:4.4.0")
    }
}

// Configure all projects in the root project
allprojects {
    repositories {
        google()  // Google's Maven repository
        mavenCentral()  // Maven Central repository
        maven { url = java.net.URI("https://jitpack.io") }  // JitPack repository for GitHub packages
        maven { url = java.net.URI("https://dl.bintray.com/razorpay/maven") }  // Razorpay repository
    }
}