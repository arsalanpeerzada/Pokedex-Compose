plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

// Formatting with ktlint, on demand: `gradlew spotlessCheck` reports, `gradlew spotlessApply` fixes.
// Not yet part of CI: the existing code hasn't had its one-off reformat. The same rules are in
// .editorconfig for the IDE; they're passed here directly so the check never depends on finding it.
val ktlintRules = mapOf(
    "ktlint_code_style" to "android_studio",
    "max_line_length" to "160",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    "ij_kotlin_allow_trailing_comma" to "true",
    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    "ktlint_standard_blank-line-between-when-conditions" to "disabled",
)

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
}
