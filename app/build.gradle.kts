plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Xray core (gomobile AAR, built by 2dust/AndroidLibXrayLite). Pinned on purpose:
// the Kotlin wrapper in core/XrayCore.kt was written against this exact API.
val xrayCoreVersion = "v26.10.29"
val xrayAar = layout.projectDirectory.file("libs/libv2ray.aar").asFile

// Downloaded once at configuration time so that IDE sync and CLI builds both find it.
if (!xrayAar.exists()) {
    xrayAar.parentFile.mkdirs()
    val url = uri("https://github.com/2dust/AndroidLibXrayLite/releases/download/$xrayCoreVersion/libv2ray.aar").toURL()
    logger.lifecycle("Downloading Xray core $xrayCoreVersion (~65 MB) ...")
    val tmp = File(xrayAar.parentFile, "libv2ray.aar.part")
    url.openStream().use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
    tmp.renameTo(xrayAar)
}

android {
    namespace = "net.nexus.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.nexus.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 44
        versionName = "3.6.1-beta"
    }

    splits {
        abi {
            isEnable = false
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystoreFile = keystorePath?.let { file(it) }
            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                // Fallback to debug configuration only when release keystore is not supplied in environment
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            // The gomobile core uses JNI/reflection heavily; keep shrinking off unless tested with proguard-rules.pro.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
