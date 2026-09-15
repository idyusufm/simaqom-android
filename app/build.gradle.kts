import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.idyusufm.simaqom"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.idyusufm.simaqom"
        minSdk = 24
        targetSdk = 34
        versionCode = (project.findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 3
        versionName = (project.findProperty("VERSION_NAME") as String?) ?: "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("MYAPP_RELEASE_STORE_FILE") as String? ?: "simaqom-release-key.jks"
            val keystorePassword = project.findProperty("MYAPP_RELEASE_STORE_PASSWORD") as String? ?: "android123"
            val keyAliasName = project.findProperty("MYAPP_RELEASE_KEY_ALIAS") as String? ?: "simaqom"
            val keyPasswordValue = project.findProperty("MYAPP_RELEASE_KEY_PASSWORD") as String? ?: "android123"

            val keystoreFile = rootProject.file(keystorePath).takeIf { it.exists() }
                ?: file(keystorePath).takeIf { it.exists() }

            if (keystoreFile != null) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as BaseVariantOutputImpl
            output.outputFileName = "simaqom-${versionName}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.webkit)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
