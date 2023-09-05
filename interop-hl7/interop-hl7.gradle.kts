plugins {
    alias(libs.plugins.interop.gradle.spring)
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":interop-tenant"))

    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.hl7v2)

    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")
}
