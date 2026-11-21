plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.voxyn.halcyon.hiddenapi"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }
}
