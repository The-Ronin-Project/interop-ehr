plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.jackson")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop:interop-common-jackson:${project.property("interopCommonVersion")}")
    implementation(project(":interop-tenant"))
    implementation(project(":interop-ehr"))
    implementation(project(":interop-ehr-factory"))
    implementation(project(":interop-ehr-auth"))
    implementation(project(":interop-fhir"))
    implementation(project(":interop-transform"))
    implementation("org.springframework:spring-context")
    implementation("com.beust:klaxon:5.5")

    // Using MockWebservice to ensure we can verify the headers set by the ktor engine
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.2")
}
