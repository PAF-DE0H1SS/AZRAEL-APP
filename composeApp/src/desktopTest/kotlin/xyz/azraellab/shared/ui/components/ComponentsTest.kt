package xyz.azraellab.shared.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import xyz.azraellab.shared.ui.theme.AzraelCornerGlass
import xyz.azraellab.shared.ui.theme.AzraelCornerMd
import xyz.azraellab.shared.ui.theme.AzraelCornerSm
import xyz.azraellab.shared.ui.theme.AzraelDarkScheme
import xyz.azraellab.shared.ui.theme.AzraelLightScheme
import xyz.azraellab.shared.ui.theme.AzraelPrimary
import xyz.azraellab.shared.ui.theme.AzraelSpace
import xyz.azraellab.shared.ui.theme.NON_TEXT_CONTRAST_MIN
import xyz.azraellab.shared.ui.theme.contrast
import xyz.azraellab.shared.ui.theme.isDark

/**
 * Рендер-тесты Compose недоступны офлайн (`compose.ui-test` не в кэше Gradle), поэтому
 * проверяем то, что реально ломает экран: согласованность тон↔цвет, отсутствие «пустых»
 * наборов и то, что компоненты ссылаются на токены, а не на случайные числа.
 */
class ComponentsTest {

    @Test
    fun buttonTonesCoverEveryCallSite() {
        assertEquals(4, AzraelButtonTone.entries.size)
        assertEquals(
            setOf("Accent", "Glass", "Ghost", "Danger"),
            AzraelButtonTone.entries.map { it.name }.toSet()
        )
    }

    @Test
    fun bannerTonesPairEachAccentWithItsIcon() {
        val tones = AzraelBannerTone.entries
        assertEquals(4, tones.size)
        tones.forEach { tone ->
            assertTrue(tone.icon.defaultWidth.value > 0f, "у тона ${tone.name} пустая иконка")
        }

        // Цвет тона берётся из схемы, а не лежит в enum'е: иначе светлая тема
        // получала бы тёмную палитру. Проверяем обе схемы и то, что тона не
        // делят цвет ни в одной из них.
        listOf(AzraelDarkScheme, AzraelLightScheme).forEach { scheme ->
            val accents = tones.map { it.accent(scheme) }
            assertEquals(accents.size, accents.toSet().size, "тона баннера не должны делить цвет (${scheme.isDark()})")
        }
        assertEquals(AzraelDarkScheme.secondary, AzraelBannerTone.Info.accent(AzraelDarkScheme))
        assertEquals(AzraelDarkScheme.primary, AzraelBannerTone.Success.accent(AzraelDarkScheme))
        assertEquals(AzraelDarkScheme.tertiary, AzraelBannerTone.Warning.accent(AzraelDarkScheme))
        assertEquals(AzraelDarkScheme.error, AzraelBannerTone.Error.accent(AzraelDarkScheme))
    }

    @Test
    fun bannerAccentFollowsThemeInsteadOfDarkPalette() {
        // Смысл всей правки: один и тот же тон в тёмной и светлой теме обязан
        // давать разные цвета. Если это перестанет быть так — снова появится
        // «тёмное приложение со светлыми словами».
        val dark = listOf(
            AzraelBannerTone.Info, AzraelBannerTone.Success,
            AzraelBannerTone.Warning, AzraelBannerTone.Error
        ).map { it.accent(AzraelDarkScheme) }
        val light = listOf(
            AzraelBannerTone.Info, AzraelBannerTone.Success,
            AzraelBannerTone.Warning, AzraelBannerTone.Error
        ).map { it.accent(AzraelLightScheme) }
        assertTrue(
            dark.zip(light).all { (d, l) -> d != l },
            "акцент тона не должен совпадать в тёмной и светлой темах"
        )
        // В светлой теме текст/иконка тона обязаны читаться на фоне.
        AzraelBannerTone.entries.forEach { tone ->
            val accent = tone.accent(AzraelLightScheme)
            assertTrue(
                contrast(accent, AzraelLightScheme.surface) >= NON_TEXT_CONTRAST_MIN,
                "тон ${tone.name} на светлой теме: ${contrast(accent, AzraelLightScheme.surface)}"
            )
        }
    }

    @Test
    fun accentTonesMatchDesignTokens() {
        val accent = Color(0xFF4ADE80)
        assertEquals(AzraelPrimary, accent)
        assertEquals(AzraelCornerMd, 14.dp)
        assertEquals(AzraelCornerSm, 10.dp)
    }

    @Test
    fun spacingScaleIsMonotonic() {
        val scale = listOf(
            AzraelSpace.xxs, AzraelSpace.xs, AzraelSpace.sm, AzraelSpace.md,
            AzraelSpace.lg, AzraelSpace.xl, AzraelSpace.xxl
        )
        scale.zipWithNext().forEach { (smaller, bigger) ->
            assertTrue(smaller < bigger, "шкала отступов должна расти: $smaller !< $bigger")
        }
        assertEquals(AzraelSpace.touchTarget, 48.dp)
        assertEquals(AzraelSpace.controlHeight, 44.dp)
    }

    @Test
    fun navItemDefaultsAreSane() {
        val item = AzraelNavItem(id = "chats", label = "Чаты", icon = AzraelBannerTone.Info.icon)
        assertEquals(0, item.badge)
        assertTrue(item.id.isNotBlank() && item.label.isNotBlank())
    }
}
