// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.apollo) apply false
    id("org.sonarqube") version "4.4.1.3373"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("com.github.ben-manes.versions") version "0.50.0"
    id("org.owasp.dependencycheck") version "9.0.7"
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:2.2.20")
        }
    }
}

// SonarQube configuration
sonar {
    properties {
        property("sonar.projectKey", "earthmax-android")
        property("sonar.organization", "earthmax")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

// Detekt configuration
detekt {
    toolVersion = "1.23.4"
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = false
    
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
    }
}

// Ktlint configuration
ktlint {
    version.set("1.0.1")
    debug.set(false)
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
}

// Dependency updates configuration
dependencyUpdates {
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

// OWASP Dependency Check configuration
dependencyCheck {
    outputDirectory = "build/reports"
    format = "ALL"
    suppressionFile = "config/dependency-check-suppressions.xml"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}