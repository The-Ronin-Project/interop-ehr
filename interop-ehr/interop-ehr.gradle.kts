plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.jackson")
    id("com.projectronin.interop.gradle.mockk")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(project(":interop-tenant"))

    // Spring
    implementation("org.springframework:spring-context")
}
