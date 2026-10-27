// Builds against the components in ../kiln-components, so a component change and its use here are
// built together. The conventions come from its kiln-build.
pluginManagement {
    includeBuild("../kiln-components/kiln-build")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kiln-oss-service"

includeBuild("../kiln-components")
