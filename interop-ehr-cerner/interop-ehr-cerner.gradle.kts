plugins {
    alias(libs.plugins.interop.gradle.spring)
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    implementation(libs.dd.trace.api)
    implementation(libs.ehrda.client)
    implementation(libs.ehrda.models)
    implementation(libs.interop.common)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.interop.publishers.datalake)
    implementation(project(":interop-tenant"))
    implementation(project(":interop-ehr"))
    implementation("org.springframework:spring-context")
    implementation(libs.opentracing.api)
    implementation(libs.opentracing.util)

    testImplementation(libs.mockk)

    // Using MockWebservice to ensure we can verify the headers set by the ktor engine
    testImplementation(libs.mockwebserver)
    testImplementation("org.springframework:spring-test")
}
