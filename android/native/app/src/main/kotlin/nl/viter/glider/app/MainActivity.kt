package nl.viter.glider.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.viter.glider.R
import nl.viter.glider.shared.data.*

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject lateinit var store: AppStore

    @javax.inject.Inject lateinit var fonts: FontStore

    @javax.inject.Inject lateinit var repository: HnRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val ready by store.ready.collectAsStateWithLifecycle()
            val prefs by store.state.collectAsStateWithLifecycle()
            val dark = when (prefs.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            val seed = Color(prefs.seed)
            val customColors = com.materialkolor.rememberDynamicColorScheme(
                seedColor = seed,
                isDark = dark,
                style = when (prefs.variant) {
                    "Monochrome" -> com.materialkolor.PaletteStyle.Monochrome
                    "Content" -> com.materialkolor.PaletteStyle.Content
                    "Fidelity" -> com.materialkolor.PaletteStyle.Fidelity
                    "Rainbow" -> com.materialkolor.PaletteStyle.Rainbow
                    "Fruit salad" -> com.materialkolor.PaletteStyle.FruitSalad
                    "Vibrant" -> com.materialkolor.PaletteStyle.Vibrant
                    "Expressive" -> com.materialkolor.PaletteStyle.Expressive
                    "Neutral" -> com.materialkolor.PaletteStyle.Neutral
                    else -> com.materialkolor.PaletteStyle.TonalSpot
                }
            )
            var colors = if (prefs.dynamic && android.os.Build.VERSION.SDK_INT >= 31) {
                if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            } else {
                customColors
            }
            if (prefs.pure) {
                colors =
                    colors.copy(
                        surface = if (dark) Color.Black else Color.White,
                        background = if (dark) Color.Black else Color.White
                    )
            }
            var downloaded by remember { mutableStateOf<FontFamily?>(null) }
            LaunchedEffect(prefs.font) {
                downloaded = null
                if (prefs.font !in listOf("Noto Sans", "Monospace", "Sans serif")) {
                    try {
                        downloaded = fonts.load(prefs.font)
                    } catch (
                        e: kotlinx.coroutines.CancellationException
                    ) {
                        throw e
                    } catch (
                        _: Exception
                    ) { /* bundled Noto Sans remains available offline */ }
                }
            }
            val family = downloaded ?: when (prefs.font) {
                "Monospace" -> FontFamily(Font(R.font.noto_sans_mono))
                "Sans serif" -> FontFamily.SansSerif
                else -> FontFamily(Font(R.font.noto_sans))
            }
            val base = Typography()
            val typography = Typography(
                displayLarge = base.displayLarge.copy(
                    fontFamily = family
                ),
                displayMedium = base.displayMedium.copy(
                    fontFamily = family
                ),
                displaySmall = base.displaySmall.copy(fontFamily = family),
                headlineLarge = base.headlineLarge.copy(
                    fontFamily = family
                ),
                headlineMedium = base.headlineMedium.copy(
                    fontFamily = family
                ),
                headlineSmall = base.headlineSmall.copy(fontFamily = family),
                titleLarge = base.titleLarge.copy(
                    fontFamily = family
                ),
                titleMedium = base.titleMedium.copy(
                    fontFamily = family
                ),
                titleSmall = base.titleSmall.copy(fontFamily = family),
                bodyLarge = base.bodyLarge.copy(
                    fontFamily = family
                ),
                bodyMedium = base.bodyMedium.copy(
                    fontFamily = family
                ),
                bodySmall = base.bodySmall.copy(fontFamily = family),
                labelLarge = base.labelLarge.copy(
                    fontFamily = family
                ),
                labelMedium = base.labelMedium.copy(
                    fontFamily = family
                ),
                labelSmall = base.labelSmall.copy(fontFamily = family)
            )
            MaterialTheme(colorScheme = colors, typography = typography) {
                if (ready) {
                    GliderApp(
                        store,
                        repository,
                        intent?.data?.toString()
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
