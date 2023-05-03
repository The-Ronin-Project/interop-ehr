plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    implementation(libs.dd.trace.api)

    implementation(libs.interop.publishers.aidbox)
    implementation(libs.interop.common)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.interop.publishers.datalake)
    implementation(project(":interop-tenant"))
    implementation(project(":interop-ehr"))
    implementation(project(":interop-fhir-ronin"))
    implementation("org.springframework:spring-context")
    implementation(libs.opentracing.api)
    implementation(libs.opentracing.util)

    testImplementation(libs.mockk)

    // Using MockWebservice to ensure we can verify the headers set by the ktor engine
    testImplementation(libs.mockwebserver)
}
