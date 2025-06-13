plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.raival.fileexplorer"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
    }

    packagingOptions.resources.excludes.add("license/*")

    buildFeatures.buildConfig = true

    viewBinding {
        enable = true
    }

    signingConfigs {
        named("debug") {
            storeFile = file("../testkey.keystore")
            storePassword = "testkey"
            keyAlias = "testkey"
            keyPassword = "testkey"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0-alpha14")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("commons-io:commons-io:2.16.1")
    implementation("com.jsibbold:zoomage:1.3.1")
    implementation("io.noties.markwon:core:4.6.2")

    implementation("com.github.bumptech.glide:glide:5.0.0-rc01")
    ksp("com.github.bumptech.glide:ksp:5.0.0-rc01")
    implementation("com.caverock:androidsvg:1.4")

    implementation("com.pixplicity.easyprefs:EasyPrefs:1.10.0")
    implementation("net.lingala.zip4j:zip4j:2.11.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")

    implementation(platform("io.github.Rosemoe.sora-editor:bom:0.23.4-43e1706-SNAPSHOT"))
    implementation("io.github.Rosemoe.sora-editor:editor")
    implementation("io.github.Rosemoe.sora-editor:language-textmate")
    implementation("io.github.Rosemoe.sora-editor:language-java")
}
