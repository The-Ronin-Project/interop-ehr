plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    api(libs.ktorm.core)

    implementation(libs.interop.common)

    implementation(libs.commons.text)
    implementation(libs.jackson.datatype.jsr310)

    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    testImplementation(project(":interop-ehr-liquibase"))
    testImplementation(libs.interop.commonTestDb)

    testImplementation(libs.mockk)
    testImplementation(libs.rider.core)

    testRuntimeOnly(libs.bundles.test.mysql)
}
