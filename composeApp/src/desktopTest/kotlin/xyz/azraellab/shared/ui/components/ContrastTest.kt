package xyz.azraellab.shared.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertTrue
import xyz.azraellab.shared.ui.starfieldPalette
import xyz.azraellab.shared.ui.theme.AzraelDarkScheme
import xyz.azraellab.shared.ui.theme.AzraelLightScheme
import xyz.azraellab.shared.ui.theme.NON_TEXT_CONTRAST_MIN
import xyz.azraellab.shared.ui.theme.TEXT_CONTRAST_MIN
import xyz.azraellab.shared.ui.theme.composite
import xyz.azraellab.shared.ui.theme.contrast
import xyz.azraellab.shared.ui.theme.contrastOver

/**
 * Регрессия по контрасту компонентов.
 *
 * Почему тест, а не скрипт: `/tmp/opencode/contrast.py` и `audit3/5.py` проверяли
 * числа, продублированные из исходников. Как только палитра или компонент менялись,
 * скрипт продолжал считать старые значения и показывал «всё хорошо», хотя на
 * экране было иначе. Здесь считается контраст тех цветов, которые компонент
 * возвращает по-настоящему (`buttonBorderColor` и т. п.), поэтому правка
 * палитры или компонента без правки контракта валит тест.
 *
 * Правила WCAG AA, которые тут применяются:
 *  - 1.4.3 — текст 4.5:1;
 *  - 1.4.11 — границы элементов управления и значимые иконки 3:1.
 */
class ContrastTest {

    private val schemes = listOf(
        "тёмная" to AzraelDarkScheme,
        "светлая" to AzraelLightScheme
    )

    /**
     * Подложка, на которой реально окажется компонент: фон схемы сам полупрозрачен
     * (`surface` — это 5% белого), поэтому «сравнить цвет с `background` схемы»
     * нельзя, сначала надо положить фон на страницу.
     */
    private fun ColorScheme.page(): Color =
        if (background.alpha < 1f) composite(background, Color.Black) else background

    /** Заливка компонента поверх страницы — то, что видит глаз под полупрозрачным слоем. */
    private fun ColorScheme.overPage(color: Color): Color = composite(color, page())

    private fun assertText(c: Double, what: String, scheme: String) = assertTrue(
        c >= TEXT_CONTRAST_MIN,
        "$scheme: $what = ${"%.2f".format(c)} < $TEXT_CONTRAST_MIN (WCAG 1.4.3)"
    )

    private fun assertNonText(c: Double, what: String, scheme: String) = assertTrue(
        c >= NON_TEXT_CONTRAST_MIN,
        "$scheme: $what = ${"%.2f".format(c)} < $NON_TEXT_CONTRAST_MIN (WCAG 1.4.11)"
    )

    @Test
    fun buttonLabelsPassTextContrastOnTheirOwnContainer() {
        schemes.forEach { (name, s) ->
            AzraelButtonTone.entries.forEach { tone ->
                listOf(false, true).forEach { busy ->
                    // `busy` гасит подпись Glass — это состояние работы, а не ошибка,
                    // но подпись всё равно обязана оставаться читаемой.
                    val container = s.overPage(buttonContainerColor(tone, s, busy))
                    val content = buttonContentColor(tone, s, busy)
                    assertText(
                        contrast(composite(content, container), container),
                        "подпись кнопки ${tone.name}${if (busy) " (busy)" else ""}",
                        name
                    )
                }
            }
        }
    }

    @Test
    fun buttonBordersPassNonTextContrastAgainstPage() {
        schemes.forEach { (name, s) ->
            AzraelButtonTone.entries.forEach { tone ->
                assertNonText(
                    contrastOver(buttonBorderColor(tone, s), s.page()),
                    "рамка кнопки ${tone.name}",
                    name
                )
            }
        }
    }

    @Test
    fun dangerButtonStaysSeparatedFromItsBackdrop() {
        // Заливка Danger почти совпадает с фоном страницы, поэтому граница — единственное,
        // что отличает кнопку «отменить» от подложки. Проверяем обе роли вместе.
        schemes.forEach { (name, s) ->
            val border = contrastOver(buttonBorderColor(AzraelButtonTone.Danger, s), s.page())
            assertNonText(border, "рамка Danger против страницы", name)
            assertText(
                contrast(
                    composite(
                        buttonContentColor(AzraelButtonTone.Danger, s, false),
                        s.overPage(buttonContainerColor(AzraelButtonTone.Danger, s, false))
                    ),
                    s.overPage(buttonContainerColor(AzraelButtonTone.Danger, s, false))
                ),
                "подпись Danger",
                name
            )
        }
    }

    @Test
    fun chipBordersPassNonTextContrast() {
        schemes.forEach { (name, s) ->
            listOf(false, true).forEach { selected ->
                val container = s.overPage(chipContainerColor(s, s.primary, selected))
                assertNonText(
                    contrastOver(chipBorderColor(s, s.primary, selected), container),
                    "рамка чипа (${if (selected) "выбранный" else "невыбранный"})",
                    name
                )
            }
        }
    }

    @Test
    fun chipLabelsPassTextContrastOnTheirOwnContainer() {
        schemes.forEach { (name, s) ->
            listOf(false, true).forEach { selected ->
                val container = s.overPage(chipContainerColor(s, s.primary, selected))
                assertText(
                    contrast(composite(chipLabelColor(s, selected), container), container),
                    "подпись чипа (${if (selected) "выбранный" else "невыбранный"})",
                    name
                )
            }
        }
    }

    @Test
    fun fieldBordersPassNonTextContrastInEveryState() {
        schemes.forEach { (name, s) ->
            listOf(false, true).forEach { focused ->
                listOf(false, true).forEach { isError ->
                    val container = s.overPage(fieldContainerColor(s, focused))
                    assertNonText(
                        contrastOver(fieldBorderColor(s, s.primary, focused, isError), container),
                        "рамка поля (фокус=$focused, ошибка=$isError)",
                        name
                    )
                }
            }
        }
    }

    @Test
    fun fieldLabelsAndHintsPassTextContrast() {
        schemes.forEach { (name, s) ->
            listOf(false, true).forEach { focused ->
                // В фокусе подпись уезжает на заливку поля (7% onSurface), не в фокусе —
                // на обычную (4%). Подложки разные, поэтому проверяем обе.
                val container = s.overPage(fieldContainerColor(s, focused))
                assertText(
                    contrast(composite(fieldLabelColor(s, s.primary, focused), container), container),
                    "подпись поля (фокус=$focused)",
                    name
                )
            }
            val container = s.overPage(fieldContainerColor(s, focused = false))
            assertText(
                contrast(composite(fieldSupportColor(s), container), container),
                "подсказка поля",
                name
            )
            // Иконки внутри поля — значимая графика (глаз = показать пароль), 3:1.
            assertNonText(
                contrast(composite(fieldSupportColor(s), container), container),
                "иконка внутри поля",
                name
            )
        }
    }

    @Test
    fun accentAsFocusedLabelIsReadableInLightTheme() {
        // Регрессия: светлый `primary` #15803D давал 4.14 на заливке поля в фокусе —
        // чуть ниже 4.5. Подпись в фокусе красится акцентом, значит падение
        // контраста акцента автоматически ломает подпись поля.
        val light = AzraelLightScheme
        val container = light.overPage(fieldContainerColor(light, focused = true))
        val c = contrast(composite(light.primary, container), container)
        assertText(c, "светлый primary как подпись поля в фокусе", "светлая")
    }

    @Test
    fun starfieldIsVisibleOnBothPageBackgrounds() {
        // Фон — украшение, WCAG 1.4.11 к нему не относится. Но если звёзды не видно
        // совсем, фон не выполняет своей задачи: раньше палитра была одна на обе темы,
        // и на `#FAFAFA` белые звёзды просто исчезали. Порог здесь 3:1 — не требование
        // нормы, а проверка «фон существует».
        schemes.forEach { (name, s) ->
            val page = s.page()
            starfieldPalette(dark = name == "тёмная").stars.forEachIndexed { i, (color, _) ->
                val c = contrastOver(color, page)
                assertTrue(
                    c >= 3.0,
                    "$name: звезда #$i (${color.value}) к странице = ${"%.2f".format(c)}"
                )
            }
        }
    }

    @Test
    fun starfieldCometAndFlashAreVisibleOnBothPageBackgrounds() {
        schemes.forEach { (name, s) ->
            val page = s.page()
            val p = starfieldPalette(dark = name == "тёмная")
            listOf("вспышка" to p.flashColor, "шлейф" to p.trailTo, "хвост" to p.trailFrom)
                .forEach { (what, color) ->
                    val c = contrastOver(color, page)
                    assertTrue(c >= 3.0, "$name: $what кометы = ${"%.2f".format(c)}")
                }
        }
    }

    @Test
    fun outlineTokenIsUsableAsARealBorder() {
        // Смысл разделения токенов: `outline` — смысловая граница (3:1), а
        // `outlineVariant` — декоративная и 3:1 не обязана достигать. Если бы в
        // `outline` попал слабый цвет, подставили бы его и рамку поля, и рамку
        // кнопки, и чипа — молча, без падения теста. Этот тест ловит именно
        // подмену токена, а не конкретный компонент.
        schemes.forEach { (name, s) ->
            assertNonText(contrastOver(s.outline, s.page()), "токен outline", name)
        }
    }
}
