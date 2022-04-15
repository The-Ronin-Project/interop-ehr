plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.ktorm")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.jackson")
}

dependencies {
    implementation(libs.interop.common)

    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation(libs.commons.text)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    testImplementation(project(":interop-ehr-liquibase"))
    testImplementation(libs.interop.commonTestDb)
}
