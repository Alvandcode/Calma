pluginManagement {
    repositories {
        // gradlePluginPortal() first for non-Android plugins.
        gradlePluginPortal()
        // Aliyun Google mirror first (dl.google.com blocked on this network).
        maven("https://maven.aliyun.com/repository/google") {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Google Maven MUST be present for AGP plugin markers (.gradle.plugin files).
        google()
        // Explicit URL instead of mavenCentral(): on some networks the
        // repo.maven.apache.org hostname fails the TLS handshake, while the identical
        // repo1.maven.org host works. Both serve the same content.
        maven("https://repo1.maven.org/maven2")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Aliyun mirror first for restricted networks (dl.google.com blocked).
        maven("https://maven.aliyun.com/repository/google")
        // Canonical Google Maven as fallback.
        google()
        maven("https://repo1.maven.org/maven2")
    }
}

rootProject.name = "Calma"
include(":app")