plugins {
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.fhir)
    implementation(project(":interop-tenant"))
    implementation(project(":interop-ehr"))

    testImplementation(libs.mockk)
}
