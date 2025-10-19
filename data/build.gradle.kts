plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.apollo)
}

android {
    namespace = "com.earthmax.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

apollo {
    service("service") {
        packageName.set("com.earthmax.data.graphql")
        schemaFile.set(file("src/main/graphql/schema.json"))
        generateKotlinModels.set(true)
        generateApolloMetadata.set(true)
        mapScalar("BigFloat", "kotlin.String")
        mapScalar("BigInt", "kotlin.Long")
        mapScalar("Date", "kotlinx.datetime.LocalDate")
        mapScalar("Datetime", "kotlinx.datetime.Instant")
        mapScalar("Time", "kotlinx.datetime.LocalTime")
        mapScalar("UUID", "kotlin.String")
        mapScalar("JSON", "kotlin.String")
        mapScalar("Cursor", "kotlin.String")
        mapScalar("Opaque", "kotlin.String")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core:core-network"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    
    // Paging
    implementation(libs.androidx.paging.runtime)
    
    // Supabase
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // JSON Serialization
    implementation(libs.gson)
    
    // Networking
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.cache)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}