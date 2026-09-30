package xyz.azraellab.shared.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.azraellab.shared.t
import xyz.azraellab.shared.ui.theme.AzraelCornerMd

/**
 * Цвета поля — чистые функции (см. комментарий в [AzraelButton]): `colors()` в
 * Material3 `@Composable`, поэтому контракт проверяется по отдельным функциям,
 * которые composable лишь склеивает в `TextFieldColors`.
 */

/** Заливка поля. Фокус и потеря фокуса различаются, но обе — почти прозрачные. */
internal fun fieldContainerColor(scheme: ColorScheme, focused: Boolean): Color =
    scheme.onSurface.copy(alpha = if (focused) 0.07f else 0.04f)

/**
 * Рамка поля — единственное место в приложении, где граница поля смысловая: по ней
 * видно, что это поле ввода, и по ней же читается фокус. Поэтому 3:1, а `outline`
 * и акцент без альфы: прежние 10% и 70% давали 1.23 и 2.86 (светлая).
 */
internal fun fieldBorderColor(scheme: ColorScheme, accent: Color, focused: Boolean, isError: Boolean): Color =
    when {
        isError -> scheme.error
        focused -> accent
        else -> scheme.outline
    }

/**
 * Подпись поля — текст, значит 4.5:1, а не 3:1. Было `onSurface@42%` (3.67/2.83) —
 * на светлой теме подпись была нечитаема.
 */
internal fun fieldLabelColor(scheme: ColorScheme, accent: Color, focused: Boolean): Color =
    if (focused) accent else scheme.onSurfaceVariant

/**
 * Подсказка под полем и иконки внутри него — тоже носители информации (глаз =
 * показать пароль), значит 4.5:1 для текста и 3:1 для иконок. Было `@35%`:
 * 2.92/2.31.
 */
internal fun fieldSupportColor(scheme: ColorScheme): Color = scheme.onSurfaceVariant

/**
 * Обобщение бывшего `AuthField`: то же самое поле, но на токенах темы и с явным
 * переключателем тем для выбранной роли/акцента.
 *
 * @param accent цвет акцента поля (роль пользователя, валюта ввода) вместо фиксированного.
 * @param spaced разрядка символов — для provision-ключа и пароля (бывший `spaced`).
 */
@Composable
fun AzraelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    leading: ImageVector? = null,
    supporting: String? = null,
    isPassword: Boolean = false,
    visible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
    spaced: Boolean = false,
    maxLength: Int? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val scheme = MaterialTheme.colorScheme
    val onSurface = scheme.onSurface
    // Иконка «глаз» активна, когда пароль виден: тогда она подсвечивается акцентом.
    val showPassword = isPassword && visible
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(if (maxLength == null) raw else raw.take(maxLength)) },
        label = { Text(label) },
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        shape = RoundedCornerShape(AzraelCornerMd),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !visible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        leadingIcon = if (leading == null) null else {
            {
                Icon(
                    leading,
                    contentDescription = null,
                    tint = fieldSupportColor(scheme),
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        trailingIcon = if (onToggleVisibility == null) {
            null
        } else {
            {
                IconButton(
                    onClick = onToggleVisibility,
                    enabled = enabled,
                    modifier = Modifier.size(38.dp)
                ) {
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = t["login.password.show"],
                    tint = if (showPassword) accent else fieldSupportColor(scheme),
                    modifier = Modifier.size(18.dp)
                )
                }
            }
        },
        supportingText = if (supporting == null) null else {
            { Text(supporting) }
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(letterSpacing = if (spaced) 2.sp else 0.4.sp),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = fieldContainerColor(scheme, focused = true),
            unfocusedContainerColor = fieldContainerColor(scheme, focused = false),
            disabledContainerColor = onSurface.copy(alpha = 0.02f),
            focusedBorderColor = fieldBorderColor(scheme, accent, focused = true, isError),
            unfocusedBorderColor = fieldBorderColor(scheme, accent, focused = false, isError),
            disabledBorderColor = scheme.outline.copy(alpha = 0.38f),
            focusedLabelColor = fieldLabelColor(scheme, accent, focused = true),
            unfocusedLabelColor = fieldLabelColor(scheme, accent, focused = false),
            disabledLabelColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            focusedTextColor = onSurface,
            unfocusedTextColor = onSurface.copy(alpha = 0.9f),
            disabledTextColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            cursorColor = accent,
            focusedLeadingIconColor = accent,
            unfocusedLeadingIconColor = fieldSupportColor(scheme),
            focusedSupportingTextColor = fieldSupportColor(scheme),
            unfocusedSupportingTextColor = fieldSupportColor(scheme)
        )
    )
}
