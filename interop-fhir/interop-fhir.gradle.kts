plugins {
    id("com.projectronin.interop.gradle.jackson")
    id("com.projectronin.interop.gradle.mockk")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop:interop-common-jackson:${project.property("interopCommonVersion")}")
    implementation("ca.uhn.hapi.fhir:org.hl7.fhir.validation:5.4.11")

    // Used for some tests utilizing classpath inspection to verify configuration
    testImplementation("io.github.classgraph:classgraph:4.8.138")
}

tasks.withType<Test> {
    minHeapSize = "2048m"
    maxHeapSize = "4096m"
}

// For now, we will need to manually invoke the updateFHIRImplGuide gradle task.
val updateFHIRImplGuide = task<Exec>("updateFHIRImplGuide") {
    workingDir = projectDir
    commandLine("./update_FHIR_impl_guide.sh")
}
