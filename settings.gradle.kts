pluginManagement {
    repositories {
        // Mirrors first: the GitHub runner cannot always reach Maven Central or the
        // Gradle plugin portal directly, and buildscript plugins must resolve.
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        // Android publications of the local compose-miuix-ui checkout (main ac0af467).
        // Each module publishes into its own build/repository/local.
        maven { url = uri("../miuix/miuix-core/build/repository/local") }
        maven { url = uri("../miuix/miuix-material/build/repository/local") }
        maven { url = uri("../miuix/miuix-squircle/build/repository/local") }
        maven { url = uri("../miuix/miuix-blur/build/repository/local") }
        maven { url = uri("../miuix/miuix-ui/build/repository/local") }
        maven { url = uri("../miuix/miuix-icons/build/repository/local") }
        maven { url = uri("../miuix/miuix-preference/build/repository/local") }
        maven { url = uri("../miuix/miuix-glass/build/repository/local") }
        maven { url = uri("../miuix/miuix-glass-icons/build/repository/local") }
        maven { url = uri("../miuix/miuix-nav/build/repository/local") }
        maven { url = uri("../miuix/miuix-window/build/repository/local") }
        // FFmpegKit's archived Android artifacts are mirrored here after their original
        // distribution repository was retired.
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
        // HONOR Audio Kit publishes the optional HD audio playback SDK here.
        maven { url = uri("https://developer.honor.com/repo") }
    }
}

rootProject.name = "Halcyon"
include(":app")
include(":ffmpeg-decoder")
include(":lyrico-audiotag")
include(":hidden-api")
