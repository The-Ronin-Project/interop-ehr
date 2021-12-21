plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.mockk")
}

dependencies {
    implementation("com.projectronin.interop:interop-common")
    implementation(project(":interop-ehr"))
    implementation(project(":interop-ehr-factory"))
    implementation(project(":interop-tenant"))

    // Spring
    implementation("org.springframework:spring-context")
}
