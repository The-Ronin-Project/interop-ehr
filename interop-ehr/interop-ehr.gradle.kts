plugins {
    alias(libs.plugins.interop.gradle.spring)
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    implementation(project(":interop-tenant"))
    implementation(libs.interop.common)
    implementation(libs.ehrda.models)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.interop.publishers.datalake)
    implementation("org.springframework:spring-context")

    implementation(libs.dd.trace.api)

    testImplementation(libs.mockk)
    // Using MockWebservice to ensure we can verify the headers set by the ktor engine
    testImplementation(libs.mockwebserver)
    testImplementation("org.springframework:spring-test")
}
