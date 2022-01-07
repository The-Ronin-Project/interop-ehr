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
            dependencySet("com.projectronin.interop:${ext["interopCommonVersion"]}") {
                entry("interop-common")
                entry("interop-common-jackson")
                entry("interop-common-test-db")
            }
        }
    }
}
