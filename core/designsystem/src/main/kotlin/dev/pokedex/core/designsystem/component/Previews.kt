package dev.pokedex.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Every screen is previewed in light and dark, at a common phone size (412 x 917 dp, as in Figma). */
@Preview(name = "Light", widthDp = 412, heightDp = 917, showBackground = true)
@Preview(name = "Dark", widthDp = 412, heightDp = 917, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class DexPreviews
