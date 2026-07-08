package com.partitionsoft.bookshelf.ui.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bookshelf.R
import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontFamily
import com.partitionsoft.bookshelf.domain.reader.model.ReaderFontWeight
import com.partitionsoft.bookshelf.domain.reader.model.ReaderPageMargin
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSettings
import com.partitionsoft.bookshelf.domain.reader.model.ReaderSpacing
import com.partitionsoft.bookshelf.domain.reader.model.ReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    isPremium: Boolean,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onReset: () -> Unit,
    onPremiumRequired: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.reader_settings_title), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onReset) { Text(stringResource(R.string.reader_settings_reset)) }
            }

            SettingTitle(stringResource(R.string.reader_font_size))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSettingsChanged(settings.copy(fontSizePercent = settings.fontSizePercent - 10)) },
                    enabled = settings.fontSizePercent > ReaderSettings.MIN_FONT_SIZE_PERCENT
                ) { Text("A−") }
                Text(stringResource(R.string.reader_text_size, settings.fontSizePercent))
                IconButton(
                    onClick = { onSettingsChanged(settings.copy(fontSizePercent = settings.fontSizePercent + 10)) },
                    enabled = settings.fontSizePercent < ReaderSettings.MAX_FONT_SIZE_PERCENT
                ) { Text("A+") }
            }

            SettingTitle(stringResource(R.string.reader_theme))
            ChoiceRow {
                ReaderTheme.entries.forEach { theme ->
                    val locked = !isPremium && theme in setOf(ReaderTheme.SEPIA, ReaderTheme.AMOLED)
                    ChoiceChip(
                        label = theme.label(),
                        selected = settings.theme == theme,
                        locked = locked,
                        onClick = {
                            if (locked) onPremiumRequired()
                            else onSettingsChanged(settings.copy(theme = theme))
                        }
                    )
                }
            }

            PremiumTitle(isPremium)
            SettingTitle(stringResource(R.string.reader_font_family))
            ChoiceRow {
                ReaderFontFamily.entries.forEach { value ->
                    val locked = !isPremium && value != ReaderFontFamily.SYSTEM
                    ChoiceChip(value.label(), settings.fontFamily == value, locked) {
                        if (locked) onPremiumRequired() else onSettingsChanged(settings.copy(fontFamily = value))
                    }
                }
            }

            SettingTitle(stringResource(R.string.reader_font_weight))
            ChoiceRow {
                ReaderFontWeight.entries.forEach { value ->
                    val locked = !isPremium && value != ReaderFontWeight.NORMAL
                    ChoiceChip(value.label(), settings.fontWeight == value, locked) {
                        if (locked) onPremiumRequired() else onSettingsChanged(settings.copy(fontWeight = value))
                    }
                }
            }

            EnumSettingRow(
                title = stringResource(R.string.reader_line_spacing),
                values = ReaderSpacing.entries,
                selected = settings.lineSpacing,
                isPremium = isPremium,
                isFreeValue = { it == ReaderSpacing.NORMAL },
                label = { it.label() },
                onPremiumRequired = onPremiumRequired,
                onSelected = { onSettingsChanged(settings.copy(lineSpacing = it)) }
            )
            EnumSettingRow(
                title = stringResource(R.string.reader_paragraph_spacing),
                values = ReaderSpacing.entries,
                selected = settings.paragraphSpacing,
                isPremium = isPremium,
                isFreeValue = { it == ReaderSpacing.NORMAL },
                label = { it.label() },
                onPremiumRequired = onPremiumRequired,
                onSelected = { onSettingsChanged(settings.copy(paragraphSpacing = it)) }
            )
            EnumSettingRow(
                title = stringResource(R.string.reader_page_margins),
                values = ReaderPageMargin.entries,
                selected = settings.pageMargin,
                isPremium = isPremium,
                isFreeValue = { it == ReaderPageMargin.NORMAL },
                label = { it.label() },
                onPremiumRequired = onPremiumRequired,
                onSelected = { onSettingsChanged(settings.copy(pageMargin = it)) }
            )
        }
    }
}

@Composable private fun SettingTitle(text: String) = Text(text, style = MaterialTheme.typography.titleSmall)

@Composable
private fun PremiumTitle(isPremium: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!isPremium) Icon(Icons.Filled.Lock, contentDescription = null)
        Text(stringResource(R.string.reader_advanced_settings), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ChoiceRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (locked) {
            { Icon(Icons.Filled.Lock, contentDescription = null) }
        } else {
            null
        }
    )
}

@Composable
private fun <T> EnumSettingRow(
    title: String,
    values: List<T>,
    selected: T,
    isPremium: Boolean,
    isFreeValue: (T) -> Boolean = { false },
    label: @Composable (T) -> String,
    onPremiumRequired: () -> Unit,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingTitle(title)
        ChoiceRow {
            values.forEach { value ->
                val locked = !isPremium && !isFreeValue(value)
                ChoiceChip(label(value), selected == value, locked) {
                    if (locked) onPremiumRequired() else onSelected(value)
                }
            }
        }
    }
}

@Composable private fun ReaderTheme.label() = stringResource(when (this) {
    ReaderTheme.SYSTEM -> R.string.reader_option_system
    ReaderTheme.LIGHT -> R.string.reader_option_light
    ReaderTheme.DARK -> R.string.reader_option_dark
    ReaderTheme.SEPIA -> R.string.reader_option_sepia
    ReaderTheme.AMOLED -> R.string.reader_option_amoled
})
@Composable private fun ReaderFontFamily.label() = stringResource(when (this) {
    ReaderFontFamily.SYSTEM -> R.string.reader_option_system
    ReaderFontFamily.SERIF -> R.string.reader_option_serif
    ReaderFontFamily.SANS_SERIF -> R.string.reader_option_sans
    ReaderFontFamily.MONOSPACE -> R.string.reader_option_monospace
})
@Composable private fun ReaderFontWeight.label() = stringResource(when (this) {
    ReaderFontWeight.NORMAL -> R.string.reader_option_normal
    ReaderFontWeight.MEDIUM -> R.string.reader_option_medium
    ReaderFontWeight.BOLD -> R.string.reader_option_bold
})
@Composable private fun ReaderSpacing.label() = stringResource(when (this) {
    ReaderSpacing.COMPACT -> R.string.reader_option_compact
    ReaderSpacing.NORMAL -> R.string.reader_option_normal
    ReaderSpacing.RELAXED -> R.string.reader_option_relaxed
})
@Composable private fun ReaderPageMargin.label() = stringResource(when (this) {
    ReaderPageMargin.NARROW -> R.string.reader_option_narrow
    ReaderPageMargin.NORMAL -> R.string.reader_option_normal
    ReaderPageMargin.WIDE -> R.string.reader_option_wide
})
