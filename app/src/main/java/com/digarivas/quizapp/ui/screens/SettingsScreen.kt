package com.digarivas.quizapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digarivas.quizapp.BuildConfig
import com.digarivas.quizapp.R
import com.digarivas.quizapp.domain.QuestionParser
import com.digarivas.quizapp.settings.SettingsManager
import com.digarivas.quizapp.settings.ThemeMode
import com.digarivas.quizapp.ui.components.AppCard
import com.digarivas.quizapp.ui.components.ChoiceChip
import com.digarivas.quizapp.ui.components.CodeBlock
import com.digarivas.quizapp.ui.components.FieldLabel
import com.digarivas.quizapp.ui.components.SectionTitle

@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    val theme by settingsManager.theme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        SectionTitle(stringResource(R.string.settings_title))
        Spacer(Modifier.height(10.dp))

        AppCard {
            FieldLabel(stringResource(R.string.settings_theme))
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.values().forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM_DEFAULT -> stringResource(R.string.theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    }
                    ChoiceChip(
                        label = label,
                        selected = theme == mode,
                        onClick = { settingsManager.setTheme(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        AppCard {
            FieldLabel(stringResource(R.string.settings_about))
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_about_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        AppCard {
            FieldLabel(stringResource(R.string.settings_format))
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_format_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            FormatRow(stringResource(R.string.format_single))
            Spacer(Modifier.height(7.dp))
            FormatRow(stringResource(R.string.format_multi))
            Spacer(Modifier.height(7.dp))
            FormatRow(stringResource(R.string.format_tip))
            Spacer(Modifier.height(16.dp))
            FieldLabel(stringResource(R.string.settings_example))
            Spacer(Modifier.height(8.dp))
            CodeBlock(QuestionParser.exampleJson())
        }
    }
}

@Composable
private fun FormatRow(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "—",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
