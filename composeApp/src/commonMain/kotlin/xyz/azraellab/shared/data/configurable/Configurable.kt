package xyz.azraellab.shared.data.configurable

/**
 * Декларативное описание одной строки настроек.
 *
 * Экран собирает список [ConfigSpec] и отдаёт его в [ConfigurableList]; рендерер
 * рисует строку по типу. Новая настройка — это ещё один элемент списка, без правки
 * разметки экрана.
 *
 * Значения выборов намеренно строковые (без дженериков): состояние сервера приходит
 * и уходит JSON-строками, а контракт `onSet: (String) -> Unit` не заставляет каждый
 * экран думать о сериализации.
 */
sealed interface ConfigSpec {
    /** Стабильный идентификатор строки — ключ состояния, тестов и семантики. */
    val key: String

    val title: String

    val desc: String?
}

/** Переключатель — состояние `true`/`false`. */
data class ToggleSpec(
    override val key: String,
    override val title: String,
    override val desc: String? = null,
    val value: Boolean,
    val onSet: (Boolean) -> Unit
) : ConfigSpec

/** Один вариант строкового выбора. */
data class ChoiceOption(
    val value: String,
    val label: String
)

/** Выбор одного из нескольких вариантов; значение — строка. */
data class ChoiceSpec(
    override val key: String,
    override val title: String,
    override val desc: String? = null,
    val options: List<ChoiceOption>,
    val value: String,
    val onSet: (String) -> Unit
) : ConfigSpec

/** Текстовое поле (ввод значения). */
data class FieldSpec(
    override val key: String,
    override val title: String,
    override val desc: String? = null,
    val label: String,
    val value: String,
    val onSet: (String) -> Unit,
    val secret: Boolean = false
) : ConfigSpec

/** Кнопка действия. */
data class ActionSpec(
    override val key: String,
    override val title: String,
    override val desc: String? = null,
    val label: String,
    val busy: Boolean = false,
    val busyLabel: String? = null,
    val onClick: () -> Unit
) : ConfigSpec