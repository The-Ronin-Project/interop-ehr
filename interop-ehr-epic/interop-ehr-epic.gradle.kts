plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.jackson")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(project(":interop-tenant"))
    implementation(project(":interop-ehr"))
    implementation(project(":interop-transform"))
    implementation("org.springframework:spring-context")
    implementation(libs.swagger.annotations)

    // Using MockWebservice to ensure we can verify the headers set by the ktor engine
    testImplementation(libs.mockwebserver)
}
