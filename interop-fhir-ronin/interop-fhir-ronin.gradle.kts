plugins {
    alias(libs.plugins.interop.gradle.spring)
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":interop-tenant"))
    implementation(project(":interop-ehr"))

    implementation(libs.caffeine)
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.publishers.datalake)
    implementation(libs.interop.fhir)
    implementation(libs.interop.validation.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.event.interop.resource.internal)

    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")
    testImplementation(libs.interop.commonHttp)
    testImplementation(libs.jackson.module.kotlin)
}
