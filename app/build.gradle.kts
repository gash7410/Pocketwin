plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.pocketwin.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pocketwin.launcher"
        // proot relies on ptrace(2) behavior that is unreliable on very old kernels;
        // 26 (Android 8) is the practical floor for this class of app.
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-dev"

        ndk {
            // Only 64-bit ARM is supported for now. box64/wine64 need arm64-v8a;
            // 32-bit (armeabi-v7a + box86) can be added later once that path is tested.
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Engine binaries (proot/box86/box64/wine loader) are shipped as jniLibs so the
    // package manager installs them into nativeLibraryDir, which is the one writable-ish
    // location Android still permits execution from post-API 29 W^X enforcement.
    // See README.md "Native engine binaries" for what goes here and why.
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

    packaging {
        // Engine binaries are placed under jniLibs deliberately; don't let the build
        // strip or compress them out of the APK.
        jniLibs.useLegacyPackaging = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // The standalone org.jetbrains.kotlin.plugin.compose Gradle plugin only exists from
    // Kotlin 2.0 onward; on 1.9.24 the Compose compiler is wired in this way instead.
    // Version pinned per the Kotlin↔Compose-Compiler compatibility map for Kotlin 1.9.24.
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.documentfile:documentfile:1.0.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // tar.xz/tar.gz extraction for rootfs and Wine build payloads (java.util has no tar support).
    implementation("org.apache.commons:commons-compress:1.26.2")
    implementation("org.tukaani:xz:1.9")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
