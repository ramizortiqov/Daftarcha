pluginManagement {
    repositories {
        // 1. УБЕРИТЕ БЛОК 'content { ... }' ОТСЮДА
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Daftarcha"
include(":app")