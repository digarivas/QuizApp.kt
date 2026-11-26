package com.digarivas.quizapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digarivas.quizapp.R
import com.digarivas.quizapp.data.db.FailedQuestionEntity
import com.digarivas.quizapp.domain.ExamDraftSummary
import com.digarivas.quizapp.domain.SetSummary
import com.digarivas.quizapp.ui.components.AppCard
import com.digarivas.quizapp.ui.components.EmptyState
import com.digarivas.quizapp.ui.components.PrimaryButton
import com.digarivas.quizapp.ui.components.SecondaryButton
import com.digarivas.quizapp.ui.components.SectionTitle
import com.digarivas.quizapp.ui.components.SegmentedControl
import com.digarivas.quizapp.ui.components.SegmentedOption
import com.digarivas.quizapp.ui.components.SetDot
import com.digarivas.quizapp.ui.theme.Geist
import com.digarivas.quizapp.ui.theme.SpaceGrotesk
import com.digarivas.quizapp.ui.theme.dangerColor
import com.digarivas.quizapp.ui.theme.successColor
import com.digarivas.quizapp.ui.viewmodels.QuizViewModel

@Composable
fun ExamSetupScreen(
    sets: List<SetSummary>,
    failed: List<FailedQuestionEntity>,
    drafts: List<ExamDraftSummary>,
    selectedSetId: Long?,
    onSelectSet: (Long) -> Unit,
    onStartQuiz: (
        setId: Long,
        mode: String,
        order: String,
        timeLimit: Boolean,
        practice: Boolean
    ) -> Unit,
    onResumeQuiz: (ExamDraftSummary) -> Unit,
    onGoImport: () -> Unit
) {
    val currentSetId = selectedSetId ?: sets.firstOrNull()?.id
    val selectedSet = sets.firstOrNull { it.id == currentSetId } ?: sets.firstOrNull()

    var mode by rememberSaveable { mutableStateOf(QuizViewModel.MODE_ALL) }
    var order by rememberSaveable { mutableStateOf(QuizViewModel.ORDER_RANDOM) }
    var timeLimit by rememberSaveable { mutableStateOf(false) }
    var practice by rememberSaveable { mutableStateOf(true) }

    val currentDraft = selectedSet?.let { set ->
        drafts
            .filter { it.setId == set.id && it.mode == mode }
            .maxByOrNull { it.updatedAt }
    }

    LaunchedEffect(sets, selectedSetId) {
        if (sets.isNotEmpty() && sets.none { it.id == selectedSetId }) {
            onSelectSet(sets.first().id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(successColor(), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.exam_engine).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.exam_mode_active).uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (sets.isEmpty() || selectedSet == null) {
            EmptyState(
                title = stringResource(R.string.exam_no_sets),
                body = stringResource(R.string.exam_no_sets_body),
                actionLabel = stringResource(R.string.tab_import),
                onAction = onGoImport
            )
        } else {
            val failedInSet = failed.count { it.setName == selectedSet.name }
            val available = if (mode == QuizViewModel.MODE_FAILED) {
                failedInSet
            } else {
                selectedSet.questionCount
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    text = stringResource(R.string.exam_set_study),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.question_count,
                        available,
                        available
                    ).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            sets.forEach { set ->
                DeckRow(
                    set = set,
                    failedCount = failed.count { it.setName == set.name },
                    draft = drafts.firstOrNull {
                        it.setId == set.id && it.mode == mode
                    },
                    selected = set.id == selectedSet.id,
                    onClick = { onSelectSet(set.id) }
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(18.dp))

            SectionTitle(stringResource(R.string.exam_extraction))
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        title = stringResource(R.string.exam_all_short),
                        subtitle = stringResource(
                            R.string.exam_total_count,
                            selectedSet.questionCount
                        )
                    ),
                    SegmentedOption(
                        title = stringResource(R.string.exam_failed_short),
                        subtitle = stringResource(
                            R.string.exam_failed_count,
                            failedInSet
                        )
                    )
                ),
                selectedIndex = if (mode == QuizViewModel.MODE_FAILED) 1 else 0,
                onSelect = {
                    mode = if (it == 1) QuizViewModel.MODE_FAILED else QuizViewModel.MODE_ALL
                }
            )

            if (mode == QuizViewModel.MODE_FAILED && failedInSet == 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.exam_no_failed),
                    color = dangerColor(),
                    fontFamily = Geist,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(18.dp))

            SectionTitle(stringResource(R.string.exam_distribution))
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        title = stringResource(R.string.order_random),
                        icon = Icons.Default.Refresh
                    ),
                    SegmentedOption(
                        title = stringResource(R.string.order_sequential),
                        icon = Icons.AutoMirrored.Filled.List
                    )
                ),
                selectedIndex = if (order == QuizViewModel.ORDER_SEQUENTIAL) 1 else 0,
                onSelect = {
                    order = if (it == 1) {
                        QuizViewModel.ORDER_SEQUENTIAL
                    } else {
                        QuizViewModel.ORDER_RANDOM
                    }
                }
            )

            Spacer(Modifier.height(18.dp))

            SectionTitle(stringResource(R.string.exam_extra))
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.exam_time_limit),
                subtitle = stringResource(R.string.exam_time_limit_body),
                checked = timeLimit,
                onCheckedChange = { timeLimit = it }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.exam_practice),
                subtitle = stringResource(R.string.exam_practice_body),
                checked = practice,
                onCheckedChange = { practice = it }
            )

            Spacer(Modifier.height(20.dp))

            val orderLabel = if (order == QuizViewModel.ORDER_SEQUENTIAL) {
                stringResource(R.string.exam_digest_seq)
            } else {
                stringResource(R.string.exam_digest_random)
            }
            val modeLabel = if (mode == QuizViewModel.MODE_FAILED) {
                stringResource(R.string.exam_digest_failed)
            } else {
                stringResource(R.string.exam_digest_all)
            }

            AppCard(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (stringResource(R.string.quiz_map_items, available) +
                            " · $orderLabel · $modeLabel").uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.exam_ready).uppercase(),
                        color = successColor(),
                        fontFamily = Geist,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (currentDraft != null) {
                PrimaryButton(
                    text = stringResource(R.string.draft_continue),
                    onClick = { onResumeQuiz(currentDraft) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.PlayArrow
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.draft_start_new),
                    onClick = {
                        onStartQuiz(selectedSet.id, mode, order, timeLimit, practice)
                    },
                    enabled = available > 0,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.draft_start_new_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.exam_start),
                    onClick = { onStartQuiz(selectedSet.id, mode, order, timeLimit, practice) },
                    enabled = available > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.exam_session_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeckRow(
    set: SetSummary,
    failedCount: Int,
    draft: ExamDraftSummary?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = set.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = Geist,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (draft != null) {
                    stringResource(
                        R.string.exam_draft_progress,
                        draft.answered,
                        draft.total
                    )
                } else {
                    pluralStringResource(
                        R.plurals.question_count,
                        set.questionCount,
                        set.questionCount
                    ) + " · " + stringResource(R.string.import_failed_short, failedCount)
                },
                color = if (draft != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}
