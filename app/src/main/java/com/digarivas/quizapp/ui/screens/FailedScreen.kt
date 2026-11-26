package com.digarivas.quizapp.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digarivas.quizapp.R
import com.digarivas.quizapp.data.db.FailedQuestionEntity
import com.digarivas.quizapp.domain.FailedAggregator
import com.digarivas.quizapp.domain.FailedGroup
import com.digarivas.quizapp.domain.FailedItem
import com.digarivas.quizapp.domain.FailedRef
import com.digarivas.quizapp.domain.FailedSetGroup
import com.digarivas.quizapp.domain.SetSummary
import com.digarivas.quizapp.ui.components.AppCard
import com.digarivas.quizapp.ui.components.ChoiceChip
import com.digarivas.quizapp.ui.components.EmptyState
import com.digarivas.quizapp.ui.components.FieldLabel
import com.digarivas.quizapp.ui.components.Pill
import com.digarivas.quizapp.ui.components.SecondaryButton
import com.digarivas.quizapp.ui.components.SectionTitle
import com.digarivas.quizapp.ui.components.SetChip
import com.digarivas.quizapp.ui.components.SetDot
import com.digarivas.quizapp.ui.theme.Geist
import com.digarivas.quizapp.ui.theme.SpaceGrotesk
import com.digarivas.quizapp.ui.theme.dangerColor
import com.digarivas.quizapp.ui.theme.successColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FailedScreen(
    sets: List<SetSummary>,
    failed: List<FailedQuestionEntity>,
    onRemove: (List<FailedRef>) -> Unit,
    onRetry: (setName: String) -> Unit,
    onClearAll: () -> Unit,
    exportTxt: () -> String,
    exportJson: () -> String,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }
    var filterGroup by remember { mutableStateOf<String?>(null) }

    val items = remember(failed, sets) { FailedAggregator.aggregate(failed, sets) }
    val groups = remember(items) { FailedAggregator.group(items) }
    val groupKeys = remember(groups) { groups.map { it.name ?: "" }.toSet() }
    val activeFilter = filterGroup?.takeIf { it in groupKeys }
    val visibleGroups = if (activeFilter == null) {
        groups
    } else {
        groups.filter { (it.name ?: "") == activeFilter }
    }
    val visibleItems = visibleGroups.flatMap { it.items }
    val totalFailures = items.sumOf { it.failCount }

    LaunchedEffect(groupKeys) {
        if (filterGroup != null && filterGroup !in groupKeys) {
            filterGroup = null
        }
    }

    fun saveExport(uri: Uri, content: String) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(content.toByteArray(Charsets.UTF_8))
                    } ?: error("No output stream")
                }.isSuccess
            }
            showMessage(
                if (ok) context.getString(R.string.export_success, items.size)
                else context.getString(R.string.export_error)
            )
        }
    }

    val txtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) saveExport(uri, exportTxt())
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) saveExport(uri, exportJson())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        AppCard(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dangerColor(), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.failed_review).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.weight(1f))
                Pill(
                    text = pluralStringResource(
                        R.plurals.stats_failures,
                        totalFailures,
                        totalFailures
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = items.size.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = SpaceGrotesk,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.3).sp,
                    lineHeight = 46.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.failed_count_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.failed_summary_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.export_txt),
                    onClick = { txtLauncher.launch("preguntas_falladas.txt") },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = stringResource(R.string.export_json),
                    onClick = { jsonLauncher.launch("preguntas_falladas.json") },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = stringResource(R.string.clear_all),
                    onClick = { showClearDialog = true },
                    enabled = items.isNotEmpty(),
                    contentColor = dangerColor(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.failed_empty_title),
                body = stringResource(R.string.failed_empty_body)
            )
        } else {
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    text = stringResource(R.string.failed_filter_label),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(
                        R.string.failed_showing,
                        visibleItems.size,
                        items.size
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChoiceChip(
                    label = stringResource(R.string.failed_filter_all, items.size),
                    selected = activeFilter == null,
                    onClick = { filterGroup = null }
                )
                groups.forEach { group ->
                    val key = group.name ?: ""
                    ChoiceChip(
                        label = "${group.name ?: stringResource(R.string.group_none)} " +
                            "(${group.itemCount})",
                        selected = activeFilter == key,
                        onClick = { filterGroup = key }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            visibleGroups.forEach { group ->
                FailedGroupHeader(group)
                group.sets.forEach { setGroup ->
                    FailedSetHeader(setGroup)
                    setGroup.items.forEach { item ->
                        FailedCard(
                            item = item,
                            retryEnabled = sets.any { it.name == item.primarySetName },
                            onRemove = { onRemove(item.refs) },
                            onRetry = { onRetry(item.primarySetName) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.clear_all_title),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = { Text(stringResource(R.string.clear_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear_all), color = dangerColor())
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun FailedGroupHeader(group: FailedGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (group.name ?: stringResource(R.string.group_none)).uppercase(),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = Geist,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Pill(
            text = pluralStringResource(
                R.plurals.question_count,
                group.itemCount,
                group.itemCount
            )
        )
    }
}

@Composable
private fun FailedSetHeader(setGroup: FailedSetGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetDot(setGroup.setName, size = 6)
        Spacer(Modifier.width(7.dp))
        Text(
            text = setGroup.setName,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceGrotesk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = pluralStringResource(
                R.plurals.stats_failures,
                setGroup.failCount,
                setGroup.failCount
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun FailedCard(
    item: FailedItem,
    retryEnabled: Boolean,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill(
                text = pluralStringResource(
                    R.plurals.failed_times,
                    item.failCount,
                    item.failCount
                ),
                color = dangerColor(),
                background = dangerColor().copy(alpha = 0.10f)
            )
            Spacer(Modifier.weight(1f))
            SetChip(item.primarySetName)
            Spacer(Modifier.width(2.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = item.question,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = Geist,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp
        )

        if (item.otherSetNames.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.failed_also_in,
                    item.otherSetNames.joinToString(", ")
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        if (item.wrongAnswer.isNotBlank() && item.wrongAnswer != "—") {
            AnswerBlock(
                label = stringResource(R.string.failed_your_answer),
                text = item.wrongAnswer,
                color = dangerColor(),
                correct = false
            )
            Spacer(Modifier.height(6.dp))
        }

        AnswerBlock(
            label = stringResource(R.string.failed_correct_label),
            text = item.correctAnswer,
            color = successColor(),
            correct = true
        )

        if (item.explanation.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    FieldLabel(stringResource(R.string.failed_reason))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.explanation,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryButton(
                text = stringResource(R.string.failed_retry),
                onClick = onRetry,
                enabled = retryEnabled,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = stringResource(R.string.failed_discard),
                onClick = onRemove,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AnswerBlock(
    label: String,
    text: String,
    color: Color,
    correct: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (correct) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = color,
                fontFamily = Geist,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = Geist,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
