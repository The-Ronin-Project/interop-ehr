plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.ktorm")
    id("com.projectronin.interop.gradle.mockk")
}

dependencies {
    implementation("com.projectronin.interop:interop-common")

    // Spring
    implementation("org.springframework:spring-context")

    testImplementation(project(":interop-ehr-liquibase"))
    testImplementation("com.projectronin.interop:interop-common-test-db")
}
