package com.digarivas.quizapp.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.digarivas.quizapp.R
import com.digarivas.quizapp.data.db.FailedQuestionEntity
import com.digarivas.quizapp.data.db.QuizHistoryEntity
import com.digarivas.quizapp.domain.ExamDraftSummary
import com.digarivas.quizapp.domain.QuestionParser
import com.digarivas.quizapp.domain.SetSummary
import com.digarivas.quizapp.ui.components.AppCard
import com.digarivas.quizapp.ui.components.AppTextField
import com.digarivas.quizapp.ui.components.ChoiceChip
import com.digarivas.quizapp.ui.components.CodeBlock
import com.digarivas.quizapp.ui.components.EmptyState
import com.digarivas.quizapp.ui.components.FieldLabel
import com.digarivas.quizapp.ui.components.Metric
import com.digarivas.quizapp.ui.components.MetricStrip
import com.digarivas.quizapp.ui.components.Pill
import com.digarivas.quizapp.ui.components.PrimaryButton
import com.digarivas.quizapp.ui.components.ProgressBar
import com.digarivas.quizapp.ui.components.SecondaryButton
import com.digarivas.quizapp.ui.components.SectionTitle
import com.digarivas.quizapp.ui.components.SegmentedControl
import com.digarivas.quizapp.ui.components.SegmentedOption
import com.digarivas.quizapp.ui.components.SetDot
import com.digarivas.quizapp.ui.components.formatSetDate
import com.digarivas.quizapp.ui.components.scorePercent
import com.digarivas.quizapp.ui.theme.Geist
import com.digarivas.quizapp.ui.theme.SpaceGrotesk
import com.digarivas.quizapp.ui.theme.dangerColor
import com.digarivas.quizapp.ui.theme.scoreColor
import com.digarivas.quizapp.ui.viewmodels.ImportOutcome
import com.digarivas.quizapp.ui.viewmodels.MainViewModel
import com.digarivas.quizapp.ui.viewmodels.QuizViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ImportScreen(
    viewModel: MainViewModel,
    sets: List<SetSummary>,
    failed: List<FailedQuestionEntity>,
    history: List<QuizHistoryEntity>,
    drafts: List<ExamDraftSummary>,
    onOpenExam: (Long) -> Unit,
    onStartQuiz: (
        setId: Long,
        mode: String,
        order: String,
        timeLimit: Boolean,
        practice: Boolean
    ) -> Unit,
    onResumeQuiz: (ExamDraftSummary) -> Unit,
    onDiscardDraft: (setId: Long, mode: String) -> Unit,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importing by remember { mutableStateOf(false) }
    var importDone by remember { mutableStateOf(0) }
    var importTotal by remember { mutableStateOf(0) }
    var importMode by rememberSaveable { mutableStateOf(0) }
    var pastedJson by rememberSaveable { mutableStateOf("") }
    var showFormatHelp by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var groupFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<SetSummary?>(null) }
    var pendingRename by remember { mutableStateOf<SetSummary?>(null) }
    var moveRequest by remember { mutableStateOf<MoveRequest?>(null) }
    var renamingGroup by remember { mutableStateOf<String?>(null) }
    var batchGroupIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingDiscard by remember { mutableStateOf<ExamDraftSummary?>(null) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    val drag = remember { SetDragState() }

    val historyBySet = remember(history) { history.groupBy { it.setName } }

    val groups = remember(sets) {
        sets.mapNotNull { it.groupName }.distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    val totalQuestions = sets.sumOf { it.questionCount }
    val average = if (history.isEmpty()) {
        null
    } else {
        history.map { scorePercent(it.score, it.total) }.average().roundToInt()
    }

    val filteredSets = if (groupFilter == null) {
        sets
    } else {
        sets.filter { it.groupName == groupFilter }
    }
    val sortedSets = when (sortMode) {
        1 -> filteredSets.sortedBy { it.name.lowercase() }
        2 -> filteredSets.sortedByDescending { it.questionCount }
        else -> filteredSets.sortedByDescending { it.createdAt }
    }

    fun toggleSelection(setId: Long) {
        selectedIds = if (setId in selectedIds) selectedIds - setId else selectedIds + setId
    }

    fun nextGroupName(): String {
        var index = 1
        while (true) {
            val candidate = context.getString(R.string.group_default_name, index)
            if (groups.none { it.equals(candidate, ignoreCase = true) }) return candidate
            index++
        }
    }

    fun dropOn(draggedId: Long, targetId: Long) {
        viewModel.groupSetsTogether(draggedId, targetId, nextGroupName()) { result ->
            when {
                result == null -> Unit
                result.alreadyGrouped -> showMessage(context.getString(R.string.group_same))
                result.merged -> showMessage(
                    context.getString(R.string.group_merged, result.groupName, result.setCount)
                )

                else -> showMessage(
                    context.resources.getQuantityString(
                        R.plurals.group_moved,
                        result.setCount,
                        result.setCount,
                        result.groupName
                    )
                )
            }
        }
    }

    // Si el grupo filtrado desaparece, vuelve a mostrar todos los conjuntos.
    LaunchedEffect(groups, groupFilter) {
        if (groupFilter != null && groupFilter !in groups) groupFilter = null
    }

    // Los conjuntos borrados no deben quedarse seleccionados.
    LaunchedEffect(sets) {
        val validIds = sets.mapTo(mutableSetOf()) { it.id }
        if (!validIds.containsAll(selectedIds)) {
            selectedIds = selectedIds.intersect(validIds)
        }
        if (sets.isEmpty() && selectionMode) selectionMode = false
    }

    fun importText(raw: String, preferredName: String?) {
        viewModel.importJson(raw, preferredName) { outcome ->
            when (outcome) {
                is ImportOutcome.Success -> {
                    val message = if (outcome.skipped > 0) {
                        context.getString(
                            R.string.import_partial,
                            outcome.imported,
                            outcome.skipped
                        )
                    } else {
                        context.getString(
                            R.string.import_success,
                            outcome.imported,
                            outcome.setName
                        )
                    }
                    showMessage(message)
                }

                is ImportOutcome.Error -> {
                    showMessage(context.getString(R.string.import_error, outcome.message))
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            importTotal = uris.size
            importDone = 0

            var created = 0
            var questions = 0
            var failures = 0
            val createdIds = mutableListOf<Long>()

            uris.forEach { uri ->
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        val name = cleanFileName(queryDisplayName(context, uri))
                        val raw = readJsonText(context, uri)
                        name to raw
                    }.getOrNull()
                }

                if (loaded == null || loaded.second.isBlank()) {
                    failures++
                } else {
                    when (val outcome = viewModel.importOne(loaded.second, loaded.first)) {
                        is ImportOutcome.Success -> {
                            created++
                            questions += outcome.imported
                            createdIds += outcome.setId
                        }

                        is ImportOutcome.Error -> failures++
                    }
                }
                importDone++
            }

            importing = false

            if (created == 0) {
                showMessage(context.getString(R.string.import_batch_none))
            } else {
                val res = context.resources
                val parts = mutableListOf(
                    res.getQuantityString(R.plurals.batch_sets, created, created),
                    res.getQuantityString(R.plurals.batch_questions, questions, questions)
                )
                if (failures > 0) {
                    parts += res.getQuantityString(R.plurals.batch_failures, failures, failures)
                }
                showMessage(parts.joinToString(" · "))
                if (createdIds.size > 1) batchGroupIds = createdIds
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.boundsInRoot().topLeft }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            MetricStrip(
                metrics = listOf(
                    Metric(
                        value = totalQuestions.toString(),
                        suffix = stringResource(R.string.import_metrics_total_unit),
                        label = stringResource(R.string.import_metrics_total)
                    ),
                    Metric(
                        value = sets.size.toString(),
                        suffix = stringResource(R.string.import_metrics_sets_unit),
                        label = stringResource(R.string.import_metrics_sets)
                    ),
                    Metric(
                        value = average?.toString() ?: "—",
                        suffix = if (average != null) "%" else null,
                        label = stringResource(R.string.import_metrics_efficacy),
                        color = average?.let { MaterialTheme.colorScheme.primary }
                    )
                )
            )

            if (drafts.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.draft_in_progress_section))
                Spacer(Modifier.height(8.dp))
                drafts.forEach { draft ->
                    ActiveDraftCard(
                        draft = draft,
                        onResume = { onResumeQuiz(draft) },
                        onDiscard = { pendingDiscard = draft },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.import_card_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SpaceGrotesk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.import_card_tag),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Geist,
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp
                        )
                    }
                    Pill(text = stringResource(R.string.import_spec_tag))
                }

                Spacer(Modifier.height(12.dp))

                SegmentedControl(
                    options = listOf(
                        SegmentedOption(title = stringResource(R.string.import_file_tab)),
                        SegmentedOption(title = stringResource(R.string.import_paste_tab))
                    ),
                    selectedIndex = importMode,
                    onSelect = { importMode = it }
                )

                Spacer(Modifier.height(12.dp))

                if (importMode == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(enabled = !importing) {
                                filePicker.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_upload),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.import_drop_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SpaceGrotesk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.import_drop_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Geist,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    AppTextField(
                        value = pastedJson,
                        onValueChange = { pastedJson = it },
                        singleLine = false,
                        minLines = 6,
                        maxLines = 10,
                        placeholder = stringResource(R.string.import_paste_hint),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { pastedJson = QuestionParser.exampleJson() }) {
                        Text(
                            text = stringResource(R.string.import_example),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Geist,
                            fontSize = 11.sp
                        )
                    }
                }

                if (importing) {
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(
                        progress = if (importTotal == 0) 0f
                        else importDone.toFloat() / importTotal
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.import_progress, importDone, importTotal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(
                        text = stringResource(R.string.import_process),
                        leadingIcon = Icons.Default.Check,
                        onClick = {
                            if (importMode == 0) {
                                filePicker.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            } else if (pastedJson.isNotBlank()) {
                                val raw = pastedJson
                                pastedJson = ""
                                importText(raw, null)
                            }
                        },
                        enabled = !importing && (importMode == 0 || pastedJson.isNotBlank()),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showFormatHelp = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.import_help),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    text = stringResource(R.string.import_repo),
                    modifier = Modifier.weight(1f)
                )
                if (sets.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectionMode = !selectionMode
                            selectedIds = emptySet()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (selectionMode) R.string.selection_cancel
                                else R.string.select_sets
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Geist,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChoiceChip(
                    label = stringResource(R.string.failed_filter_all, sets.size),
                    selected = groupFilter == null,
                    onClick = { groupFilter = null }
                )
                groups.forEach { group ->
                    val count = sets.count { it.groupName == group }
                    ChoiceChip(
                        label = "$group ($count)",
                        selected = groupFilter == group,
                        onClick = { groupFilter = group }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.import_sort_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 10.sp
                )
                Spacer(Modifier.width(4.dp))
                Box {
                    TextButton(onClick = { sortMenu = true }) {
                        Text(
                            text = stringResource(
                                when (sortMode) {
                                    1 -> R.string.import_sort_name
                                    2 -> R.string.import_sort_questions
                                    else -> R.string.import_sort_recent
                                }
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = Geist,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    DropdownMenuContent(
                        expanded = sortMenu,
                        onDismiss = { sortMenu = false },
                        items = listOf(
                            stringResource(R.string.import_sort_recent) to {
                                sortMode = 0
                                sortMenu = false
                            },
                            stringResource(R.string.import_sort_name) to {
                                sortMode = 1
                                sortMenu = false
                            },
                            stringResource(R.string.import_sort_questions) to {
                                sortMode = 2
                                sortMenu = false
                            }
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.import_reorder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 10.sp
                )
            }

            if (!selectionMode) {
                Text(
                    text = stringResource(
                        if (drag.draggedId != null) R.string.group_drop_hint
                        else R.string.import_reorder_hint
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            if (selectionMode) {
                SelectionBar(
                    count = selectedIds.size,
                    onGroup = {
                        moveRequest = MoveRequest(
                            ids = selectedIds.toList(),
                            initial = nextGroupName(),
                            allowNone = false
                        )
                    },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            if (sets.isEmpty() || sortedSets.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_sets_title),
                    body = stringResource(R.string.no_sets_body)
                )
            } else {
                val hasGroups = groups.isNotEmpty()
                val sections = buildSetSections(sortedSets)

                sections.forEach { section ->
                    if (section.name != null) {
                        GroupHeader(
                            name = section.name,
                            count = section.sets.size,
                            onRename = { renamingGroup = section.name },
                            onUngroup = {
                                viewModel.moveToGroup(section.sets.map { it.id }, null)
                                showMessage(context.getString(R.string.group_removed))
                            }
                        )
                    } else if (hasGroups) {
                        GroupHeader(
                            name = stringResource(R.string.group_none),
                            count = section.sets.size,
                            onRename = null,
                            onUngroup = null
                        )
                    }

                    section.sets.forEach { set ->
                        val setHistory = historyBySet[set.name].orEmpty()
                        val lastAttempt = setHistory.maxByOrNull { it.date }
                        val setDraft = drafts
                            .filter { it.setId == set.id }
                            .maxByOrNull { it.updatedAt }

                        SetRowCard(
                            set = set,
                            failedCount = failed.count { it.setName == set.name },
                            attemptCount = setHistory.size,
                            lastPercent = lastAttempt?.let {
                                scorePercent(it.score, it.total)
                            },
                            draft = setDraft,
                            selectionMode = selectionMode,
                            selected = set.id in selectedIds,
                            dragState = drag,
                            dragEnabled = !selectionMode,
                            isDropTarget = drag.hoveredId == set.id,
                            onDragFinished = { from, to ->
                                val target = to?.let { drag.boundsOf(it) }
                                scope.launch {
                                    if (to != null && target != null) {
                                        drag.animateDropTo(target)
                                        drag.finish()
                                        dropOn(from, to)
                                    } else {
                                        drag.animateCancel()
                                        drag.finish()
                                    }
                                }
                            },
                            onOpen = {
                                if (selectionMode) toggleSelection(set.id) else onOpenExam(set.id)
                            },
                            onStart = {
                                onStartQuiz(
                                    set.id,
                                    QuizViewModel.MODE_ALL,
                                    QuizViewModel.ORDER_RANDOM,
                                    false,
                                    true
                                )
                            },
                            onRetryFailed = {
                                onStartQuiz(
                                    set.id,
                                    QuizViewModel.MODE_FAILED,
                                    QuizViewModel.ORDER_RANDOM,
                                    false,
                                    true
                                )
                            },
                            onResume = { setDraft?.let(onResumeQuiz) },
                            onRename = { pendingRename = set },
                            onMove = {
                                moveRequest = MoveRequest(
                                    ids = listOf(set.id),
                                    initial = set.groupName.orEmpty(),
                                    allowNone = true
                                )
                            },
                            onDelete = { pendingDelete = set }
                        )
                    }
                }
            }
        }

        DragOverlay(
            dragState = drag,
            sets = sets,
            origin = rootOrigin
        )
    }

    if (showFormatHelp) {
        AlertDialog(
            onDismissRequest = { showFormatHelp = false },
            title = {
                Text(
                    text = stringResource(R.string.import_help),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = { CodeBlock(QuestionParser.exampleJson()) },
            confirmButton = {
                TextButton(onClick = { showFormatHelp = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    pendingDelete?.let { set ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_set_title),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(stringResource(R.string.delete_set_message, set.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSet(set.id)
                        pendingDelete = null
                        showMessage(context.getString(R.string.set_deleted))
                    }
                ) {
                    Text(stringResource(R.string.delete), color = dangerColor())
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    pendingDiscard?.let { draft ->
        AlertDialog(
            onDismissRequest = { pendingDiscard = null },
            title = {
                Text(
                    text = stringResource(R.string.draft_discard_title),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(stringResource(R.string.draft_discard_message, draft.setName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDiscardDraft(draft.setId, draft.mode)
                        pendingDiscard = null
                    }
                ) {
                    Text(stringResource(R.string.draft_discard), color = dangerColor())
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDiscard = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    pendingRename?.let { set ->
        NameDialog(
            title = stringResource(R.string.rename_set_title),
            label = stringResource(R.string.rename_set_label),
            initial = set.name,
            onDismiss = { pendingRename = null },
            onConfirm = { newName ->
                pendingRename = null
                viewModel.renameSet(set.id, newName) { finalName ->
                    showMessage(
                        if (finalName == null) {
                            context.getString(R.string.name_required)
                        } else {
                            context.getString(R.string.set_renamed, finalName)
                        }
                    )
                }
            }
        )
    }

    renamingGroup?.let { groupName ->
        NameDialog(
            title = stringResource(R.string.group_rename_title),
            label = stringResource(R.string.group_rename_label),
            initial = groupName,
            onDismiss = { renamingGroup = null },
            onConfirm = { newName ->
                renamingGroup = null
                viewModel.renameGroup(groupName, newName) { result ->
                    showMessage(
                        when {
                            result == null -> context.getString(R.string.name_required)
                            result.merged -> context.getString(
                                R.string.group_merge_rename,
                                result.name
                            )

                            else -> context.getString(R.string.group_renamed, result.name)
                        }
                    )
                }
            }
        )
    }

    moveRequest?.let { request ->
        GroupDialog(
            title = when {
                request.ids.size == 1 -> stringResource(R.string.group_move_title)
                request.allowNone -> stringResource(
                    R.string.group_move_title_many,
                    request.ids.size
                )

                else -> stringResource(R.string.group_create_title_many, request.ids.size)
            },
            body = null,
            initial = request.initial,
            groups = groups,
            allowNone = request.allowNone,
            onDismiss = { moveRequest = null },
            onConfirm = { group ->
                val ids = request.ids
                moveRequest = null
                selectionMode = false
                selectedIds = emptySet()
                scope.launch {
                    val finalGroup = viewModel.moveToGroupNow(ids, group)
                    showMessage(
                        if (finalGroup == null) {
                            context.getString(R.string.group_removed)
                        } else {
                            context.resources.getQuantityString(
                                R.plurals.group_moved,
                                ids.size,
                                ids.size,
                                finalGroup
                            )
                        }
                    )
                }
            }
        )
    }

    if (batchGroupIds.isNotEmpty()) {
        GroupDialog(
            title = stringResource(R.string.import_batch_group_title),
            body = stringResource(R.string.import_batch_group_body, batchGroupIds.size),
            initial = nextGroupName(),
            groups = groups,
            allowNone = false,
            onDismiss = { batchGroupIds = emptyList() },
            onConfirm = { group ->
                val ids = batchGroupIds
                batchGroupIds = emptyList()
                if (!group.isNullOrBlank()) {
                    scope.launch {
                        val finalGroup = viewModel.moveToGroupNow(ids, group)
                        if (finalGroup != null) {
                            showMessage(
                                context.resources.getQuantityString(
                                    R.plurals.group_moved,
                                    ids.size,
                                    ids.size,
                                    finalGroup
                                )
                            )
                        }
                    }
                }
            }
        )
    }
}

/**
 * Aísla la lectura de la posición del arrastre para que la lista no se
 * recomponga en cada fotograma mientras se mueve la tarjeta flotante.
 */
@Composable
private fun DragOverlay(
    dragState: SetDragState,
    sets: List<SetSummary>,
    origin: Offset
) {
    val draggedId = dragState.draggedId ?: return
    val alpha = dragState.ghostAlpha
    if (alpha <= 0f) return
    val set = sets.firstOrNull { it.id == draggedId } ?: return

    DragPreview(
        set = set,
        position = dragState.position,
        origin = origin,
        scale = dragState.ghostScale,
        alpha = alpha
    )
}

/** Tarjeta flotante que sigue al dedo mientras se arrastra un conjunto. */
@Composable
private fun DragPreview(
    set: SetSummary,
    position: Offset,
    origin: Offset,
    scale: Float,
    alpha: Float
) {
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val previewWidth = 280.dp
    val widthPx = with(density) { previewWidth.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val liftPx = with(density) { 56.dp.toPx() }
    val marginPx = with(density) { 8.dp.toPx() }
    val maxX = (screenWidthPx - widthPx - marginPx).coerceAtLeast(marginPx)

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (position.x - origin.x - widthPx / 2f)
                        .coerceIn(marginPx, maxX)
                        .roundToInt(),
                    y = (position.y - origin.y - liftPx).coerceAtLeast(marginPx).roundToInt()
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .width(previewWidth),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SetDot(set.name)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = set.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.question_count,
                        set.questionCount,
                        set.questionCount
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Geist,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Estado del arrastre de conjuntos: qué conjunto se está moviendo, sobre cuál
 * está el dedo y los límites de cada tarjeta en coordenadas de la raíz.
 */
private class SetDragState {
    var draggedId by mutableStateOf<Long?>(null)
        private set
    var hoveredId by mutableStateOf<Long?>(null)
        private set
    var position by mutableStateOf(Offset.Zero)
        private set
    var ghostAlpha by mutableStateOf(0f)
        private set
    var ghostScale by mutableStateOf(1f)
        private set
    private var releasing = false
    private val coords = mutableMapOf<Long, LayoutCoordinates>()

    fun attach(id: Long, coordinates: LayoutCoordinates) {
        coords[id] = coordinates
    }

    fun originOf(id: Long): Offset = boundsOf(id)?.topLeft ?: Offset.Zero

    fun boundsOf(id: Long): Rect? {
        val c = coords[id]?.takeIf { it.isAttached } ?: return null
        return c.boundsInRoot()
    }

    fun start(id: Long, at: Offset) {
        draggedId = id
        hoveredId = null
        position = at
        ghostAlpha = 1f
        ghostScale = 1f
        releasing = false
    }

    fun move(at: Offset) {
        if (releasing) return
        val dragged = draggedId ?: return
        position = at
        hoveredId = coords.keys.firstOrNull { id ->
            id != dragged && boundsOf(id)?.contains(at) == true
        }
    }

    /** Lleva la tarjeta flotante hasta la tarjeta de destino y la desvanece. */
    suspend fun animateDropTo(target: Rect) {
        releasing = true
        hoveredId = null
        val start = position
        val end = target.center
        val progress = Animatable(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        ) {
            position = Offset(
                x = start.x + (end.x - start.x) * value,
                y = start.y + (end.y - start.y) * value
            )
            ghostScale = 1f - 0.12f * value
            ghostAlpha = 1f - value
        }
    }

    /** Si se suelta fuera de una tarjeta, la vista previa se apaga en el sitio. */
    suspend fun animateCancel() {
        releasing = true
        hoveredId = null
        val progress = Animatable(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 120)
        ) {
            ghostAlpha = 1f - value
            ghostScale = 1f - 0.08f * value
        }
    }

    fun finish() {
        draggedId = null
        hoveredId = null
        ghostAlpha = 0f
        ghostScale = 1f
        releasing = false
    }
}

private data class MoveRequest(
    val ids: List<Long>,
    val initial: String,
    val allowNone: Boolean
)

private data class SetSection(
    val name: String?,
    val sets: List<SetSummary>
)

private fun buildSetSections(sets: List<SetSummary>): List<SetSection> {
    val grouped = sets.filter { it.groupName != null }.groupBy { it.groupName!! }
    val result = grouped.keys
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .map { SetSection(it, grouped.getValue(it)) }
        .toMutableList()
    val ungrouped = sets.filter { it.groupName == null }
    if (ungrouped.isNotEmpty()) result += SetSection(null, ungrouped)
    return result
}

@Composable
private fun ActiveDraftCard(
    draft: ExamDraftSummary,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeLabel = if (draft.mode == QuizViewModel.MODE_FAILED) {
        stringResource(R.string.history_mode_failed)
    } else {
        stringResource(R.string.history_mode_all)
    }
    val currentNumber = if (draft.total <= 0) {
        0
    } else {
        (draft.current + 1).coerceAtMost(draft.total)
    }
    val percent = (draft.progress * 100).roundToInt()

    AppCard(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
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
                text = stringResource(R.string.draft_in_progress).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Pill(text = modeLabel)
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SetDot(draft.setName)
            Spacer(Modifier.width(8.dp))
            Text(
                text = draft.setName,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceGrotesk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(R.string.draft_question, currentNumber, draft.total),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = Geist,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.draft_answered,
                    draft.answered,
                    draft.total
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = Geist,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$percent%",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Geist,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(6.dp))

        ProgressBar(
            progress = draft.progress,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(
                text = stringResource(R.string.draft_continue),
                onClick = onResume,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = stringResource(R.string.draft_discard),
                onClick = onDiscard,
                contentColor = dangerColor(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GroupHeader(
    name: String,
    count: Int,
    onRename: (() -> Unit)?,
    onUngroup: (() -> Unit)?
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name.uppercase(),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = Geist,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Pill(
            text = pluralStringResource(R.plurals.set_count, count, count)
        )
        Spacer(Modifier.weight(1f))

        if (onRename != null && onUngroup != null) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.group_menu),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenuContent(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    items = listOf(
                        stringResource(R.string.rename) to {
                            menuOpen = false
                            onRename()
                        },
                        stringResource(R.string.group_ungroup) to {
                            menuOpen = false
                            onUngroup()
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pluralStringResource(R.plurals.selected_sets, count, count),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = Geist,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onGroup, enabled = count >= 2) {
                Text(
                    text = stringResource(R.string.group_action),
                    fontFamily = Geist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
        if (count < 2) {
            Text(
                text = stringResource(R.string.group_select_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Geist,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SetRowCard(
    set: SetSummary,
    failedCount: Int,
    attemptCount: Int,
    lastPercent: Int?,
    draft: ExamDraftSummary?,
    selectionMode: Boolean,
    selected: Boolean,
    dragState: SetDragState,
    dragEnabled: Boolean,
    isDropTarget: Boolean,
    onDragFinished: (draggedId: Long, targetId: Long?) -> Unit,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onRetryFailed: () -> Unit,
    onResume: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isDragged = dragState.draggedId == set.id
    val progress = when {
        draft != null && draft.total > 0 -> draft.answered.toFloat() / draft.total
        lastPercent != null -> lastPercent / 100f
        else -> 0f
    }
    val progressColor = when {
        draft != null -> MaterialTheme.colorScheme.primary
        lastPercent != null -> scoreColor(lastPercent)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val statusText = when {
        draft != null -> stringResource(
            R.string.draft_answered,
            draft.answered,
            draft.total
        )
        attemptCount > 0 && lastPercent != null -> pluralStringResource(
            R.plurals.import_attempts,
            attemptCount,
            attemptCount,
            lastPercent
        )
        else -> stringResource(R.string.import_no_attempts)
    }
    val statusColor = when {
        draft != null -> MaterialTheme.colorScheme.primary
        lastPercent != null -> scoreColor(lastPercent)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val scale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.012f else 1f,
        animationSpec = tween(140)
    )
    val cardColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.surfaceContainerHigh
            isDropTarget -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(140)
    )

    AppCard(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .alpha(if (isDragged) 0.35f else 1f)
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onGloballyPositioned { coords ->
                dragState.attach(set.id, coords)
            }
            .pointerInput(set.id, dragEnabled) {
                if (!dragEnabled) return@pointerInput
                fun rootPosition(local: Offset): Offset {
                    return dragState.originOf(set.id) + local
                }
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragState.start(set.id, rootPosition(offset))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragState.move(rootPosition(change.position))
                    },
                    onDragEnd = {
                        val from = dragState.draggedId
                        val to = dragState.hoveredId
                        if (from != null) onDragFinished(from, to)
                    },
                    onDragCancel = { dragState.finish() }
                )
            }
            .clickable(onClick = onOpen),
        color = cardColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                SelectionMark(selected)
                Spacer(Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (set.groupName != null) {
                        Pill(
                            text = set.groupName,
                            color = MaterialTheme.colorScheme.onSurface,
                            background = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = formatSetDate(set.createdAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 9.sp,
                        letterSpacing = 0.3.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SetDot(set.name)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = set.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = SpaceGrotesk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.question_count,
                            set.questionCount,
                            set.questionCount
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontFamily = Geist,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(5.dp))
                ProgressBar(
                    progress = progress,
                    color = progressColor
                )
            }
        }

        if (!selectionMode && isDropTarget) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.group_drop_target),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Geist,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
        }

        if (!selectionMode) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenuContent(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        items = listOf(
                            stringResource(R.string.rename) to {
                                menuOpen = false
                                onRename()
                            },
                            stringResource(R.string.move_to_group) to {
                                menuOpen = false
                                onMove()
                            },
                            stringResource(R.string.delete) to {
                                menuOpen = false
                                onDelete()
                            }
                        ),
                        destructiveLabel = stringResource(R.string.delete)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = dangerColor(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (draft != null) {
                PrimaryButton(
                    text = stringResource(R.string.draft_continue),
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (failedCount > 0) {
                        SecondaryButton(
                            text = stringResource(R.string.stats_review),
                            onClick = onRetryFailed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PrimaryButton(
                        text = stringResource(R.string.start),
                        onClick = onStart,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) accent else Color.Transparent)
            .border(
                1.5.dp,
                if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initial) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column {
                FieldLabel(label)
                Spacer(Modifier.height(6.dp))
                AppTextField(
                    value = value,
                    onValueChange = { value = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun GroupDialog(
    title: String,
    body: String?,
    initial: String?,
    groups: List<String>,
    allowNone: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var value by remember(title, initial) { mutableStateOf(initial.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (body != null) {
                    Text(
                        text = body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(14.dp))
                }

                FieldLabel(stringResource(R.string.group_name_label))
                Spacer(Modifier.height(6.dp))
                AppTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = stringResource(R.string.group_name_hint)
                )

                if (groups.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groups.forEach { group ->
                            ChoiceChip(
                                label = group,
                                selected = value.equals(group, ignoreCase = true),
                                onClick = { value = group }
                            )
                        }
                    }
                }

                if (allowNone) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onConfirm(null) }) {
                        Text(
                            text = stringResource(R.string.group_none),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = allowNone || value.isNotBlank()
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun DropdownMenuContent(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<Pair<String, () -> Unit>>,
    destructiveLabel: String? = null
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        items.forEach { (label, action) ->
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(
                        text = label,
                        color = if (label == destructiveLabel) dangerColor()
                        else MaterialTheme.colorScheme.onSurface,
                        fontFamily = Geist,
                        fontSize = 13.sp
                    )
                },
                onClick = action
            )
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) name = cursor.getString(index)
        }
    }
    return name
}

private fun readJsonText(context: Context, uri: Uri): String {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > QuestionParser.MAX_JSON_CHARS) {
                throw IllegalArgumentException("El archivo supera el máximo de 10 MB")
            }
            output.write(buffer, 0, read)
        }
        output.toString(Charsets.UTF_8.name())
    }.orEmpty()
}

private fun cleanFileName(name: String?): String? {
    val trimmed = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (trimmed.endsWith(".json", ignoreCase = true)) {
        trimmed.dropLast(5)
    } else {
        trimmed
    }
}
