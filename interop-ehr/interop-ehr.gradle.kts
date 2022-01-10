plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.mockk")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation(project(":interop-fhir"))
    implementation(project(":interop-tenant"))
    implementation("com.beust:klaxon:5.5")
}
