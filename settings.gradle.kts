pluginManagement {
    repositories {
        // gradlePluginPortal() first for non-Android plugins.
        gradlePluginPortal()
        // Google Maven MUST be present for AGP plugin markers (.gradle.plugin files).
        // dl.google.com is blocked for PowerShell but Gradle's Java HTTP client works.
        google()
        // Aliyun Google mirror as fallback for artifacts (no plugin markers).
        maven("https://maven.aliyun.com/repository/google") {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Explicit URL instead of mavenCentral(): on some networks the
        // repo.maven.apache.org hostname fails the TLS handshake, while the identical
        // repo1.maven.org host works. Both serve the same content.
        maven("https://repo1.maven.org/maven2")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Aliyun mirror for Google Maven (Android artifacts) — dl.google.com blocked.
        maven("https://maven.aliyun.com/repository/google")
        maven("https://repo1.maven.org/maven2")
    }
}

rootProject.name = "Calma"
include(":app")

// ---------------------------------------------------------------------------------------
// Unhealthy mirror filtering
//
// A machine-wide Gradle init script (outside this repository) may prepend third-party
// mirrors to plugin, dependency and buildscript repositories. When one of those mirrors is
// unreachable, Gradle aborts resolution with a 5xx even though the canonical repositories
// are healthy, which makes Calma impossible to build.
//
// The block below drops such repositories after settings evaluation, without touching any
// file outside this repository. It only removes hosts listed in the
// `calma.unreachableMirrors` Gradle property (comma-separated, default: maven.aliyun.com),
// so on a healthy machine nothing changes at all.
//
// Set `calma.unreachableMirrors=` (empty) in gradle.properties to disable the filter.
// ---------------------------------------------------------------------------------------
val unreachableMirrors: Set<String> = (
    providers.gradleProperty("calma.unreachableMirrors").orNull ?: "maven.aliyun.com"
    ).split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

fun RepositoryHandler.dropUnreachableMirrors() {
    if (unreachableMirrors.isEmpty()) return
    val doomed = filterIsInstance<MavenArtifactRepository>().filter { repo ->
        val host = repo.url.host ?: return@filter false
        unreachableMirrors.any { host == it || host.endsWith(".$it") }
    }
    doomed.forEach { remove(it) }
}

gradle.settingsEvaluated {
    pluginManagement.repositories.dropUnreachableMirrors()
    @Suppress("UnstableApiUsage")
    dependencyResolutionManagement.repositories.dropUnreachableMirrors()
}

// The same machine-wide init script also injects mirrors into each project's buildscript.
gradle.beforeProject {
    buildscript.repositories.dropUnreachableMirrors()
}