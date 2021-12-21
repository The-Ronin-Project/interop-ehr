plugins {
    id("com.projectronin.interop.gradle.base") version "1.0.0-SNAPSHOT"
    id("com.projectronin.interop.gradle.ktorm") version "1.0.0-SNAPSHOT" apply false
    id("com.projectronin.interop.gradle.mockk") version "1.0.0-SNAPSHOT" apply false
    id("com.projectronin.interop.gradle.publish") version "1.0.0-SNAPSHOT" apply false
    id("com.projectronin.interop.gradle.spring") version "1.0.0-SNAPSHOT" apply false
}

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.publish")

    dependencyManagement {
        dependencies {
            // This all works, but IntelliJ does not properly recognize the actual dependencies method being called and thus cannot find the refenced methods below.
            dependencySet("com.projectronin.interop:${ext["interopCommonVersion"]}") {
                entry("interop-common")
                entry("interop-common-test-db")
            }
        }
    }
}
