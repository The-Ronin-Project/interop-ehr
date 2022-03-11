plugins {
    id("com.projectronin.interop.gradle.jackson")
    id("com.projectronin.interop.gradle.spring")

    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.mockk")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop:interop-common-jackson:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop.fhir:interop-fhir:${project.property("interopFhirVersion")}")
    implementation(project(":interop-tenant"))
    implementation(project(":interop-ehr"))
}
