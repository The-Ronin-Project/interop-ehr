rootProject.name = "interop-ehr-build"

include("interop-ehr-liquibase")
include("interop-tenant")
include("interop-fhir")
include("interop-ehr")
include("interop-transform")
include("interop-ehr-factory")
include("interop-ehr-auth")

include("interop-ehr-epic")

for (project in rootProject.children) {
    project.buildFileName = "${project.name}.gradle.kts"
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.github.com/projectronin/package-repo")
            credentials {
                username = System.getenv("PACKAGE_USER")
                password = System.getenv("PACKAGE_TOKEN")
            }
        }
        mavenLocal()
    }
}
