// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.0")
        classpath("com.google.gms:google-services:4.3.10")
        classpath(kotlin("gradle-plugin", version = "1.6.10"))
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.1")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
        maven(url = "https://github.com/wada811/Android-Material-Design-Colors/raw/master/repository/")
        maven (url = "https://oss.sonatype.org/content/repositories/snapshots")

    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}