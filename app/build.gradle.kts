plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.terramaster"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.terramaster"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation("androidx.core:core:1.13.0")
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.firebase:firebase-functions-ktx:21.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx:24.8.0")
    implementation ("com.google.firebase:firebase-auth:22.1.0")
    implementation ("com.google.firebase:firebase-messaging:24.1.0")
    implementation ("com.google.firebase:firebase-storage-ktx:20.2.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("com.google.firebase:firebase-storage-ktx:20.0.0")
    implementation ("org.osmdroid:osmdroid-android:6.1.14")
    implementation ("androidx.preference:preference-ktx:1.2.1")
    //scanner
    implementation ("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation ("com.google.android.material:material:1.10.0")
    implementation("com.stripe:stripe-android:21.0.1")
    implementation("com.stripe:stripe-java:28.0.0")
    implementation ("androidx.multidex:multidex:2.0.1")
    implementation ("com.github.gcacace:signature-pad:1.3.1")
}
