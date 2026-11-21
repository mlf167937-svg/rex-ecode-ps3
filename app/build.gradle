// app/build.gradle
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.rexofficial.rextestbenchmark'
    compileSdk 34

    defaultConfig {
        applicationId "com.rexofficial.rextestbenchmark"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding true
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    implementation 'org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'org.geckoview:geckoview:119.0.20231204160245'
    implementation 'com.google.code.gson:gson:2.11.0'
}