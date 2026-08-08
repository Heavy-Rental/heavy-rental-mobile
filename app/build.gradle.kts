import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/** Load api.properties, then overlay root local.properties for local overrides. */
fun loadApiServerTarget(): String {
    val props = Properties()
    val apiFile = file("api.properties")
    if (apiFile.exists()) {
        apiFile.inputStream().use { props.load(it) }
    }
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        val local = Properties()
        localFile.inputStream().use { local.load(it) }
        local.getProperty("api.server.target")?.let { props.setProperty("api.server.target", it) }
    }
    val raw = props.getProperty("api.server.target", "MOCKOON").trim().uppercase()
    val allowed = setOf("MOCKOON", "SPRING_BOOT")
    require(raw in allowed) {
        "api.server.target must be one of $allowed (was \"$raw\"). " +
            "Set it in app/api.properties or root local.properties."
    }
    return raw
}

val apiServerTarget = loadApiServerTarget()

android {
    namespace = "com.heavyrental"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.heavyrental"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_SERVER_TARGET", "\"$apiServerTarget\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
}
