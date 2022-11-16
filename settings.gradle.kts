rootProject.name = "interop-ehr-build"

include("interop-ehr-liquibase")
include("interop-tenant")
include("interop-ehr")
include("interop-fhir-ronin")

include("interop-ehr-epic")

for (project in rootProject.children) {
    project.buildFileName = "${project.name}.gradle.kts"
}

pluginManagement {
    plugins {
        id("com.projectronin.interop.gradle.base") version "2.2.0"
        id("com.projectronin.interop.gradle.publish") version "2.2.0"
        id("com.projectronin.interop.gradle.spring") version "2.2.0"
        id("com.projectronin.interop.gradle.version") version "2.2.0"
    }

    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
            mavenContent {
                releasesOnly()
            }
        }
        mavenLocal()
        gradlePluginPortal()
    }
}
