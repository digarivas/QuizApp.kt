package com.digarivas.quizapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digarivas.quizapp.R
import com.digarivas.quizapp.data.db.FailedQuestionEntity
import com.digarivas.quizapp.data.db.QuizHistoryEntity
import com.digarivas.quizapp.domain.AttemptRecord
import com.digarivas.quizapp.domain.FailedAggregator
import com.digarivas.quizapp.domain.GroupEvolution
import com.digarivas.quizapp.domain.SetEvolution
import com.digarivas.quizapp.domain.SetSummary
import com.digarivas.quizapp.domain.StudyStatsBuilder
import com.digarivas.quizapp.ui.components.AppCard
import com.digarivas.quizapp.ui.components.EmptyState
import com.digarivas.quizapp.ui.components.Pill
import com.digarivas.quizapp.ui.components.ProgressBar
import com.digarivas.quizapp.ui.components.SecondaryButton
import com.digarivas.quizapp.ui.components.SectionTitle
import com.digarivas.quizapp.ui.components.SetDot
import com.digarivas.quizapp.ui.components.formatHistoryDate
import com.digarivas.quizapp.ui.components.scorePercent
import com.digarivas.quizapp.ui.theme.Geist
import com.digarivas.quizapp.ui.theme.SpaceGrotesk
import com.digarivas.quizapp.ui.theme.dangerColor
import com.digarivas.quizapp.ui.theme.scoreColor
import com.digarivas.quizapp.ui.theme.successColor

@Composable
fun StatsScreen(
    sets: List<SetSummary>,
    failed: List<FailedQuestionEntity>,
    history: List<QuizHistoryEntity>,
    onClearHistory: () -> Unit,
    onOpenFailed: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    val totalQuestions = sets.sumOf { it.questionCount }
    val totalCorrect = history.sumOf { it.score }
    val totalAnswered = history.sumOf { it.total }
    val average = if (totalAnswered == 0) null else (totalCorrect * 100) / totalAnswered

    val stats = remember(sets, history) {
        StudyStatsBuilder.build(
            sets = sets,
            history = history.map {
                AttemptRecord(
                    setName = it.setName,
                    date = it.date,
                    score = it.score,
                    total = it.total,
                    mode = it.mode
                )
            }
        )
    }
    val failedItems = remember(failed, sets) { FailedAggregator.aggregate(failed, sets) }
    val totalFailures = failedItems.sumOf { it.failCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.stats_consolidated).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Pill(text = stringResource(R.string.stats_historic))
        }

        Spacer(Modifier.height(12.dp))

        AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionTitle(stringResource(R.string.stats_rate))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = average?.let { "$it%" } ?: "—",
                            color = if (average == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontFamily = SpaceGrotesk,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.3).sp,
                            lineHeight = 46.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_target),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_goal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(
                        R.string.stats_rate_ok,
                        totalCorrect,
                        totalAnswered
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Geist,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(6.dp))

            ProgressBar(
                progress = (average ?: 0) / 100f,
                color = if ((average ?: 0) >= 80) successColor()
                else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                label = stringResource(R.string.stats_sets),
                value = sets.size.toString(),
                caption = stringResource(R.string.stats_sets_active),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stats_questions),
                value = totalQuestions.toString(),
                caption = stringResource(R.string.stats_questions_local),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.stats_failed_tile,
                            failedItems.size,
                            failedItems.size
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = SpaceGrotesk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(6.dp))
                    Pill(
                        text = pluralStringResource(
                            R.plurals.stats_failures,
                            totalFailures,
                            totalFailures
                        )
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.stats_failed_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            SecondaryButton(
                text = stringResource(R.string.stats_review),
                onClick = onOpenFailed,
                modifier = Modifier.width(96.dp),
                leadingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))

            SectionTitle(stringResource(R.string.stats_group_evolution))
            Spacer(Modifier.height(8.dp))
            stats.groups.forEach { group ->
                GroupEvolutionCard(
                    group = group,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle(stringResource(R.string.stats_exam_evolution))
            Spacer(Modifier.height(8.dp))
            stats.exams.forEach { exam ->
                ExamEvolutionCard(
                    exam = exam,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(
                text = stringResource(R.string.stats_history_recent),
                modifier = Modifier.weight(1f)
            )
            if (history.isNotEmpty()) {
                Pill(text = stringResource(R.string.stats_sessions, history.size))
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { showClearDialog = true }) {
                    Text(
                        text = stringResource(R.string.stats_clear_history),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.stats_empty_title),
                body = stringResource(R.string.stats_empty_body)
            )
        } else {
            history.take(20).forEach { session ->
                HistoryCard(session, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.clear_history_title),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = { Text(stringResource(R.string.clear_history_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.stats_clear_history), color = dangerColor())
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
private fun StatTile(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp)
    ) {
        SectionTitle(label)
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = SpaceGrotesk,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
            lineHeight = 32.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun GroupEvolutionCard(
    group: GroupEvolution,
    modifier: Modifier = Modifier
) {
    val displayName = group.name ?: stringResource(R.string.group_none)
    val description = stringResource(
        R.string.stats_chart_group_description,
        displayName,
        group.count,
        group.average ?: 0
    )

    AppCard(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle(stringResource(R.string.stats_group))
                Spacer(Modifier.height(2.dp))
                Text(
                    text = displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pluralStringResource(
                        R.plurals.stats_attempts,
                        group.count,
                        group.count
                    ).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = Geist,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = group.average?.let {
                        stringResource(R.string.stats_avg_value, it)
                    } ?: stringResource(R.string.stats_no_activity),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (group.attempts.isEmpty()) {
            EmptyEvolution()
        } else {
            EvolutionChart(
                values = group.attempts.map { it.percent },
                description = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
            Spacer(Modifier.height(10.dp))
            EvolutionStats(
                count = group.count,
                average = group.average,
                best = group.best
            )
            group.last?.let { last ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.stats_last_attempt,
                        formatHistoryDate(last.date)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = group.sets.joinToString(" · ") { it.name },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExamEvolutionCard(
    exam: SetEvolution,
    modifier: Modifier = Modifier
) {
    val description = stringResource(
        R.string.stats_chart_exam_description,
        exam.name,
        exam.count,
        exam.average ?: 0
    )
    val last = exam.last

    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SetDot(exam.name)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exam.isOrphan) {
                        Pill(text = stringResource(R.string.stats_orphan))
                        Spacer(Modifier.width(6.dp))
                    } else if (exam.groupName != null) {
                        Pill(text = exam.groupName)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.stats_attempts,
                            exam.count,
                            exam.count
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = last?.let { "${it.percent}%" } ?: "—",
                color = last?.let { scoreColor(it.percent) }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceGrotesk,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(10.dp))

        if (exam.attempts.isEmpty()) {
            EmptyEvolution()
        } else {
            EvolutionChart(
                values = exam.attempts.map { it.percent },
                description = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
            Spacer(Modifier.height(10.dp))
            EvolutionStats(
                count = exam.count,
                average = exam.average,
                best = exam.best
            )
            if (last != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.stats_last_attempt,
                        formatHistoryDate(last.date)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun EvolutionStats(count: Int, average: Int?, best: Int?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        EvolutionMetric(
            label = stringResource(R.string.stats_exams_short),
            value = count.toString(),
            modifier = Modifier.weight(1f)
        )
        EvolutionMetric(
            label = stringResource(R.string.stats_average),
            value = average?.let { "$it%" } ?: "—",
            modifier = Modifier.weight(1f)
        )
        EvolutionMetric(
            label = stringResource(R.string.stats_best),
            value = best?.let { "$it%" } ?: "—",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EvolutionMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.7.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceGrotesk,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyEvolution() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_no_attempts),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EvolutionChart(
    values: List<Int>,
    description: String,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val lastColor = values.lastOrNull()?.let { scoreColor(it) } ?: lineColor

    Canvas(
        modifier = modifier.semantics {
            contentDescription = description
        }
    ) {
        val inset = 4.dp.toPx()
        val chartWidth = size.width - inset * 2
        val chartHeight = size.height - inset * 2
        val left = inset
        val bottom = inset + chartHeight

        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val y = bottom - chartHeight * fraction
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(left + chartWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 6.dp.toPx()),
                    0f
                )
            )
        }

        if (values.isEmpty()) return@Canvas

        val step = if (values.size <= 1) 0f else chartWidth / (values.size - 1)
        val coords = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size <= 1) {
                    left + chartWidth / 2
                } else {
                    left + step * index
                },
                y = bottom - chartHeight * (value.coerceIn(0, 100) / 100f)
            )
        }

        if (coords.size > 1) {
            val area = Path().apply {
                moveTo(coords.first().x, bottom)
                coords.forEach { lineTo(it.x, it.y) }
                lineTo(coords.last().x, bottom)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.18f), Color.Transparent),
                    startY = inset,
                    endY = bottom
                )
            )
        }

        for (index in 0 until coords.size - 1) {
            drawLine(
                color = lineColor,
                start = coords[index],
                end = coords[index + 1],
                strokeWidth = 2.25.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val dotIndices: Iterable<Int> = if (coords.size <= 30) {
            coords.indices
        } else {
            setOf(coords.lastIndex)
        }
        dotIndices.forEach { index ->
            val offset = coords[index]
            val filled = index == coords.lastIndex
            drawCircle(
                color = if (filled) lastColor else surfaceColor,
                radius = if (filled) 3.5.dp.toPx() else 2.5.dp.toPx(),
                center = offset
            )
            if (!filled) {
                drawCircle(
                    color = lineColor,
                    radius = 2.5.dp.toPx(),
                    center = offset,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    session: QuizHistoryEntity,
    modifier: Modifier = Modifier
) {
    val percent = scorePercent(session.score, session.total)
    val modeLabel = if (session.mode == "failed") {
        stringResource(R.string.history_mode_failed)
    } else {
        stringResource(R.string.history_mode_all)
    }

    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatHistoryDate(session.date).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SetDot(session.setName, size = 6)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = session.setName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = SpaceGrotesk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = modeLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.history_score, session.score, session.total),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$percent%",
                    color = scoreColor(percent),
                    fontFamily = Geist,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        ProgressBar(
            progress = percent / 100f,
            color = scoreColor(percent)
        )
    }
}
