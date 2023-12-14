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
    implementation(project(":interop-hl7"))
    implementation("org.springframework:spring-context")
    implementation(libs.ktor.server.auth.jwt) {
        exclude(group = "junit")
    }
    implementation(libs.swagger.annotations)
    implementation(libs.opentracing.api)
    implementation(libs.opentracing.util)
    implementation(libs.bundles.hl7v2)
    implementation(libs.interop.queue.api)

    testImplementation(libs.mockk)
    testImplementation(libs.interop.fhir.generators)
    testImplementation(libs.ronin.test.data.generator)

    // Using MockWebservice to ensure we can verify the headers set by the ktor engine
    testImplementation(libs.mockwebserver)
    testImplementation("org.springframework:spring-test")
}
