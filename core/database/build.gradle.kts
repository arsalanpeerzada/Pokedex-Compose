plugins {
    alias(libs.plugins.pokedex.android.library)
    alias(libs.plugins.pokedex.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "dev.pokedex.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(libs.room.runtime)
    ksp(libs.room.compiler)
}
