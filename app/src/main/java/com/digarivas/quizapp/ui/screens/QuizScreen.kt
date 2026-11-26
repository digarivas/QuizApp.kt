package com.digarivas.quizapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digarivas.quizapp.R
import com.digarivas.quizapp.domain.QuestionState
import com.digarivas.quizapp.domain.QuizLoadError
import com.digarivas.quizapp.domain.QuizQuestion
import com.digarivas.quizapp.domain.QuizResult
import com.digarivas.quizapp.domain.QuizUiState
import com.digarivas.quizapp.ui.components.AppCard
import com.digarivas.quizapp.ui.components.BrandMark
import com.digarivas.quizapp.ui.components.FieldLabel
import com.digarivas.quizapp.ui.components.Metric
import com.digarivas.quizapp.ui.components.MetricStrip
import com.digarivas.quizapp.ui.components.Pill
import com.digarivas.quizapp.ui.components.PrimaryButton
import com.digarivas.quizapp.ui.components.ProgressBar
import com.digarivas.quizapp.ui.components.SecondaryButton
import com.digarivas.quizapp.ui.components.SectionTitle
import com.digarivas.quizapp.ui.theme.Geist
import com.digarivas.quizapp.ui.theme.SpaceGrotesk
import com.digarivas.quizapp.ui.theme.dangerColor
import com.digarivas.quizapp.ui.theme.scoreColor
import com.digarivas.quizapp.ui.theme.successColor
import com.digarivas.quizapp.ui.viewmodels.QuizViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onExit: () -> Unit,
    onSeeFailed: () -> Unit,
    onGoHome: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (ui.result != null) {
            onExit()
        } else {
            showExitDialog = true
        }
    }

    when {
        ui.loading -> LoadingScreen()
        ui.error != null -> QuizErrorScreen(error = ui.error!!, onExit = onExit)
        ui.result != null -> ResultsContent(
            result = ui.result!!,
            onRepeat = viewModel::restart,
            onSeeFailed = onSeeFailed,
            onHome = onGoHome
        )

        else -> {
            QuizContent(
                ui = ui,
                onBack = { showExitDialog = true },
                onSelect = viewModel::selectOption,
                onCheck = viewModel::checkAnswer,
                onTimeOut = viewModel::timeOut,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onGoTo = viewModel::goTo,
                onFinish = {
                    val unanswered = ui.states.count { !it.checked }
                    if (unanswered > 0) {
                        showFinishDialog = true
                    } else {
                        viewModel.finish()
                    }
                }
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.exit_title),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = { Text(stringResource(R.string.exit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.saveAndExit(onExit)
                    }
                ) {
                    Text(stringResource(R.string.exit_confirm), color = dangerColor())
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    if (showFinishDialog) {
        val unanswered = ui.states.count { !it.checked }
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.finish_title),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.finish_message,
                        unanswered,
                        unanswered
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finish()
                    }
                ) {
                    Text(stringResource(R.string.finish_confirm), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun QuizErrorScreen(error: QuizLoadError, onExit: () -> Unit) {
    val message = when (error) {
        QuizLoadError.SET_NOT_FOUND -> stringResource(R.string.quiz_set_missing)
        QuizLoadError.NO_QUESTIONS -> stringResource(R.string.quiz_no_questions)
        QuizLoadError.NO_FAILED -> stringResource(R.string.quiz_no_failed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceGrotesk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        SecondaryButton(
            text = stringResource(R.string.back),
            onClick = onExit
        )
    }
}

@Composable
private fun QuizContent(
    ui: QuizUiState,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onTimeOut: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoTo: (Int) -> Unit,
    onFinish: () -> Unit
) {
    val question = ui.question
    val state = ui.currentState
    if (question != null && state != null) {
        QuizContentBody(
            ui = ui,
            question = question,
            state = state,
            onBack = onBack,
            onSelect = onSelect,
            onCheck = onCheck,
            onTimeOut = onTimeOut,
            onPrevious = onPrevious,
            onNext = onNext,
            onGoTo = onGoTo,
            onFinish = onFinish
        )
    }
}

@Composable
private fun QuizContentBody(
    ui: QuizUiState,
    question: QuizQuestion,
    state: QuestionState,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onTimeOut: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoTo: (Int) -> Unit,
    onFinish: () -> Unit
) {
    val total = ui.questions.size
    val progress = if (total == 0) 0f else (ui.current + 1).toFloat() / total
    val isLast = ui.current == ui.questions.lastIndex

    var elapsed by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsed++
        }
    }

    var remaining by remember(ui.current) { mutableStateOf(60) }
    LaunchedEffect(ui.current, state.checked, ui.timeLimit) {
        if (!ui.timeLimit || state.checked) return@LaunchedEffect
        remaining = 60
        while (remaining > 0 && !state.checked) {
            delay(1000)
            remaining--
        }
        if (remaining <= 0 && !state.checked) onTimeOut()
    }
    val timeText = formatDuration(if (ui.timeLimit) remaining else elapsed)
    val timeColor = if (ui.timeLimit && remaining <= 10) dangerColor() else successColor()

    var marked by remember(ui.current) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            BrandMark(size = 28)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QUIZAPP",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = stringResource(R.string.quiz_session_active),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onFinish) {
                Text(
                    text = stringResource(R.string.finish_exam).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        ProgressBar(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            QuizMetricRow(
                question = "${ui.current + 1} / $total",
                module = ui.setName,
                time = timeText,
                timeColor = timeColor
            )

            Spacer(Modifier.height(10.dp))

            AppCard(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill(
                        text = stringResource(
                            if (question.isMulti) R.string.quiz_multi_badge
                            else R.string.quiz_single_badge
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        background = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { marked = !marked }) {
                        Text(
                            text = stringResource(
                                if (marked) R.string.quiz_marked else R.string.quiz_mark
                            ),
                            color = if (marked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Geist,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = question.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 28.sp
                )

                if (question.isMulti) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.quiz_multi_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            question.options.forEachIndexed { index, option ->
                OptionRow(
                    index = index,
                    text = option,
                    state = state,
                    isCorrect = index in question.correctIndices,
                    multi = question.isMulti,
                    enabled = !state.checked,
                    onClick = { onSelect(index) }
                )
                if (index != question.options.lastIndex) {
                    Spacer(Modifier.height(7.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.previous),
                    onClick = onPrevious,
                    enabled = ui.current > 0,
                    modifier = Modifier.weight(1f)
                )
                if (!state.checked) {
                    PrimaryButton(
                        text = stringResource(R.string.check),
                        onClick = onCheck,
                        enabled = state.selected.isNotEmpty(),
                        leadingIcon = Icons.Default.Check,
                        modifier = Modifier.weight(1.5f)
                    )
                } else {
                    PrimaryButton(
                        text = if (isLast) {
                            stringResource(R.string.see_results)
                        } else {
                            stringResource(R.string.next)
                        },
                        onClick = { if (isLast) onFinish() else onNext() },
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }

            if (state.checked) {
                Spacer(Modifier.height(10.dp))
                FeedbackBox(question = question, state = state, practice = ui.practice)
            }

            Spacer(Modifier.height(12.dp))

            QuestionMapCard(
                ui = ui,
                onGoTo = onGoTo,
                onFinish = onFinish
            )
        }
    }
}

@Composable
private fun OptionRow(
    index: Int,
    text: String,
    state: QuestionState,
    isCorrect: Boolean,
    multi: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val selected = index in state.selected
    val correct = state.checked && isCorrect
    val wrong = state.checked && !isCorrect && selected
    val missed = state.checked && isCorrect && !selected
    val selectedPending = !state.checked && selected

    val success = successColor()
    val danger = dangerColor()

    val borderColor = when {
        correct || missed -> success
        wrong -> danger
        selectedPending -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val background = when {
        correct -> success.copy(alpha = 0.10f)
        wrong -> danger.copy(alpha = 0.10f)
        missed -> success.copy(alpha = 0.06f)
        selectedPending -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val letterBackground = when {
        correct || missed -> success
        wrong -> danger
        selectedPending -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val letterColor = when {
        correct || missed -> MaterialTheme.colorScheme.onTertiary
        wrong -> MaterialTheme.colorScheme.onError
        selectedPending -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = MaterialTheme.shapes.large

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(MaterialTheme.shapes.small)
                .background(letterBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ('A' + index).toString(),
                color = letterColor,
                fontFamily = Geist,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = Geist,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(if (multi) MaterialTheme.shapes.small else CircleShape)
                .background(
                    when {
                        correct || missed -> success
                        wrong -> danger
                        selectedPending -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }
                )
                .border(
                    1.5.dp,
                    when {
                        correct || missed -> success
                        wrong -> danger
                        selectedPending -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    if (multi) MaterialTheme.shapes.small else CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val mark = when {
                correct || missed -> "✓"
                wrong -> "✕"
                selectedPending -> "✓"
                else -> ""
            }
            if (mark.isNotEmpty()) {
                Text(
                    text = mark,
                    color = when {
                        correct || missed -> MaterialTheme.colorScheme.onTertiary
                        wrong -> MaterialTheme.colorScheme.onError
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeedbackBox(
    question: QuizQuestion,
    state: QuestionState,
    practice: Boolean
) {
    if (!practice) return

    val correct = state.correct == true
    val accent = if (correct) successColor() else dangerColor()
    val badge = stringResource(
        if (correct) R.string.quiz_correct_badge else R.string.quiz_incorrect_badge
    )

    AppCard(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.quiz_explanation_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceGrotesk,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Pill(
                text = badge.uppercase(),
                color = if (correct) MaterialTheme.colorScheme.onTertiary
                else MaterialTheme.colorScheme.onError,
                background = accent
            )
        }

        if (question.explanation.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = question.explanation,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        val wrongOptions = question.options.withIndex()
            .filter { it.index !in question.correctIndices }
        if (wrongOptions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                wrongOptions.forEach { (index, option) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = dangerColor(),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(14.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "${('A' + index)}. $option",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Geist,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionMapCard(
    ui: QuizUiState,
    onGoTo: (Int) -> Unit,
    onFinish: () -> Unit
) {
    AppCard(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(
                text = stringResource(R.string.question_map),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.quiz_map_items, ui.questions.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 9.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ui.questions.forEachIndexed { index, _ ->
                val state = ui.states[index]
                MapDot(
                    number = index + 1,
                    state = state,
                    current = index == ui.current,
                    onClick = { onGoTo(index) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(successColor(), stringResource(R.string.map_correct))
            Spacer(Modifier.weight(1f))
            LegendItem(dangerColor(), stringResource(R.string.map_wrong))
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.map_selected))
            Spacer(Modifier.weight(1f))
            LegendItem(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                stringResource(R.string.map_unanswered)
            )
        }

        Spacer(Modifier.height(14.dp))

        SecondaryButton(
            text = stringResource(R.string.finish_exam),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MapDot(
    number: Int,
    state: QuestionState,
    current: Boolean,
    onClick: () -> Unit
) {
    val correct = state.checked && state.correct == true
    val wrong = state.checked && state.correct == false
    val pending = !state.checked && state.selected.isNotEmpty()
    val dotColor = when {
        correct -> successColor()
        wrong -> dangerColor()
        pending -> MaterialTheme.colorScheme.primary
        else -> null
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                if (current) 1.5.dp else 1.dp,
                if (current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number.toString(),
                color = if (current) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 9.sp,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium
            )
            if (dotColor != null) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(dotColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun QuizMetricRow(
    question: String,
    module: String,
    time: String,
    timeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuizMetric(
            label = stringResource(R.string.quiz_question_label),
            value = question,
            modifier = Modifier.weight(1f)
        )
        QuizMetric(
            label = stringResource(R.string.quiz_module_label),
            value = module,
            modifier = Modifier.weight(1.35f)
        )
        QuizMetric(
            label = stringResource(R.string.quiz_time_label),
            value = time,
            valueColor = timeColor,
            modifier = Modifier.weight(0.9f)
        )
    }
}

@Composable
private fun QuizMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = SpaceGrotesk,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val secs = safe % 60
    return "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

@Composable
private fun ResultsContent(
    result: QuizResult,
    onRepeat: () -> Unit,
    onSeeFailed: () -> Unit,
    onHome: () -> Unit
) {
    val grade = when {
        result.percent >= 80 -> stringResource(R.string.grade_excellent)
        result.percent >= 60 -> stringResource(R.string.grade_good)
        result.percent >= 40 -> stringResource(R.string.grade_study)
        else -> stringResource(R.string.grade_review)
    }
    val accent = scoreColor(result.percent)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp)
    ) {
        SectionTitle(stringResource(R.string.results_grade))
        Spacer(Modifier.height(8.dp))

        AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${result.percent}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = SpaceGrotesk,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    lineHeight = 68.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = grade,
                    color = accent,
                    fontFamily = SpaceGrotesk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.result_summary,
                        result.correct,
                        result.total
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                ProgressBar(
                    progress = result.percent / 100f,
                    color = accent
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        MetricStrip(
            metrics = listOf(
                Metric(
                    result.correct.toString(),
                    stringResource(R.string.stat_correct),
                    successColor()
                ),
                Metric(
                    result.wrong.toString(),
                    stringResource(R.string.stat_wrong),
                    dangerColor()
                ),
                Metric(
                    result.unanswered.toString(),
                    stringResource(R.string.stat_unanswered)
                ),
                Metric(result.total.toString(), stringResource(R.string.stat_total))
            )
        )

        Spacer(Modifier.height(18.dp))

        PrimaryButton(
            text = stringResource(R.string.result_repeat),
            onClick = onRepeat,
            leadingIcon = Icons.Default.Refresh,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryButton(
                text = stringResource(R.string.result_failed),
                onClick = onSeeFailed,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = stringResource(R.string.result_home),
                onClick = onHome,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
