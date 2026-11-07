package dev.tomasmed.jlight.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * High-contrast OSL (Fluorescent Light-Spill) palette tailored for circular AMOLED displays (#000000 true black)
 * matching upstream JlightCalendar ADR 0016.
 */
object JlightOslColors {
    val TrueBlack = Color(0xFF000000)
    val ElevatedObsidian = Color(0xFF141821)
    val FluorescentCyan = Color(0xFF38BDF8)
    val FluorescentEmerald = Color(0xFF34D399)
    val FluorescentAmber = Color(0xFFFBBF24)
    val FluorescentRose = Color(0xFFFB7185)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
}

private val JlightWearColorScheme = ColorScheme(
    primary = JlightOslColors.FluorescentCyan,
    onPrimary = JlightOslColors.TrueBlack,
    secondary = JlightOslColors.FluorescentEmerald,
    onSecondary = JlightOslColors.TrueBlack,
    tertiary = JlightOslColors.FluorescentAmber,
    onTertiary = JlightOslColors.TrueBlack,
    background = JlightOslColors.TrueBlack,
    onBackground = JlightOslColors.TextPrimary,
    surfaceContainer = JlightOslColors.ElevatedObsidian,
    onSurface = JlightOslColors.TextPrimary,
    onSurfaceVariant = JlightOslColors.TextSecondary,
)

@Composable
fun JlightWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JlightWearColorScheme,
        content = content,
    )
}
