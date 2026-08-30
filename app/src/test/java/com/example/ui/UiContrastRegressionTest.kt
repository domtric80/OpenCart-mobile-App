package com.example.ui

import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiContrastRegressionTest {
    @Test
    fun darkThemeScreensUseThemeAwareForegrounds() {
        val auditSource = File("src/main/java/com/example/ui/screens/AuditScreen.kt").readText()
        val sectionSheetSource = File(
            "src/main/java/com/example/ui/components/OrdersSubSectionSheet.kt"
        ).readText()

        assertFalse(auditSource.contains("color = ThemeOnSurface"))
        assertFalse(auditSource.contains("color = ThemeOnSurfaceVariant"))
        assertTrue(
            sectionSheetSource.contains(
                "containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer"
            )
        )
        assertTrue(sectionSheetSource.contains("MaterialTheme.colorScheme.onPrimaryContainer"))
    }

    @Test
    fun configFirstStoreHelpAndVersionBadgeUsePairedThemeColors() {
        val configSource = File("src/main/java/com/example/ui/screens/ConfigScreen.kt").readText()
        val firstStoreHelp = configSource
            .substringAfter("if (currentStore == null)")
            .substringBefore("OutlinedTextField(")

        assertTrue(
            firstStoreHelp.contains(
                ".background(MaterialTheme.colorScheme.primaryContainer)"
            )
        )
        assertTrue(
            firstStoreHelp.contains(
                "color = MaterialTheme.colorScheme.onPrimaryContainer"
            )
        )
        assertTrue(
            firstStoreHelp.contains(
                "color = MaterialTheme.colorScheme.onSurfaceVariant"
            )
        )
        assertTrue(configSource.contains("?: \"Configura il primo negozio\""))
        assertFalse(configSource.contains("\${currentStore?.url} • \${currentStore?.version}"))
    }

    @Test
    fun fixedDiagnosticPalettesMeetNormalTextContrast() {
        assertTrue(contrastRatio(0xFF3730A3, 0xFFFFFFFF) >= 4.5)
        assertTrue(contrastRatio(0xFFDCFCE7, 0xFF15803D) >= 4.5)
        assertTrue(contrastRatio(0xFFFEE2E2, 0xFFB91C1C) >= 4.5)
    }

    private fun contrastRatio(background: Long, foreground: Long): Double {
        val first = luminance(background)
        val second = luminance(foreground)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val srgb = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (srgb <= 0.04045) srgb / 12.92 else ((srgb + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
