pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sora-editor"
include(
    ":bom",
    ":editor",
    ":app",
    ":language-java",
    ":language-textmate",
    ":editor-lsp",
    ":language-treesitter"
)
