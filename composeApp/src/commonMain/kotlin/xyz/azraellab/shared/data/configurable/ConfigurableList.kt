package xyz.azraellab.shared.data.configurable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import xyz.azraellab.shared.ui.components.AzraelButton
import xyz.azraellab.shared.ui.components.AzraelButtonTone
import xyz.azraellab.shared.ui.components.AzraelCard
import xyz.azraellab.shared.ui.components.AzraelChoiceChip
import xyz.azraellab.shared.ui.components.AzraelTextField
import xyz.azraellab.shared.ui.theme.AzraelSpace

/** Список строк настроек — рисует каждую [ConfigSpec] по её типу. */
@Composable
fun ConfigurableList(
    specs: List<ConfigSpec>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AzraelSpace.cardGap)
    ) {
        specs.forEach { spec ->
            when (spec) {
                is ToggleSpec -> ToggleRow(spec)
                is ChoiceSpec -> ChoiceRow(spec)
                is FieldSpec -> FieldRow(spec)
                is ActionSpec -> ActionRow(spec)
            }
        }
    }
}

/** Заголовок группы настроек над блоком строк. */
@Composable
fun ConfigGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AzraelSpace.listGap)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

@Composable
private fun ToggleRow(spec: ToggleSpec) {
    AzraelCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val onSurface = MaterialTheme.colorScheme.onSurface
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AzraelSpace.xxs)
            ) {
                Text(spec.title, style = MaterialTheme.typography.titleSmall, color = onSurface)
                if (spec.desc != null) {
                    Text(
                        spec.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Checkbox(
                checked = spec.value,
                onCheckedChange = spec.onSet,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(spec: ChoiceSpec) {
    AzraelCard {
        val onSurface = MaterialTheme.colorScheme.onSurface
        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.sm)) {
            Text(spec.title, style = MaterialTheme.typography.titleSmall, color = onSurface)
            if (spec.desc != null) {
                Text(
                    spec.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurface.copy(alpha = 0.6f)
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AzraelSpace.sm)) {
                spec.options.forEach { option ->
                    AzraelChoiceChip(
                        label = option.label,
                        selected = option.value == spec.value,
                        onClick = { spec.onSet(option.value) },
                        enabled = option.value != spec.value
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldRow(spec: FieldSpec) {
    AzraelCard {
        val onSurface = MaterialTheme.colorScheme.onSurface
        var visible by rememberSaveable(spec.key) { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.fieldGap)) {
            Text(spec.title, style = MaterialTheme.typography.titleSmall, color = onSurface)
            AzraelTextField(
                value = spec.value,
                onValueChange = spec.onSet,
                label = spec.label,
                accent = MaterialTheme.colorScheme.primary,
                isPassword = spec.secret,
                visible = visible,
                onToggleVisibility = if (spec.secret) ({ visible = !visible }) else null
            )
            if (spec.desc != null) {
                Text(
                    spec.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ActionRow(spec: ActionSpec) {
    AzraelCard {
        val onSurface = MaterialTheme.colorScheme.onSurface
        Column(verticalArrangement = Arrangement.spacedBy(AzraelSpace.sm)) {
            Text(spec.title, style = MaterialTheme.typography.titleSmall, color = onSurface)
            if (spec.desc != null) {
                Text(
                    spec.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurface.copy(alpha = 0.6f)
                )
            }
            AzraelButton(
                label = spec.label,
                onClick = spec.onClick,
                tone = AzraelButtonTone.Accent,
                busy = spec.busy,
                busyLabel = spec.busyLabel ?: spec.label,
                fullWidth = true
            )
        }
    }
}