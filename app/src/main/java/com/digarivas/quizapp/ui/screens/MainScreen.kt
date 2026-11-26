package com.digarivas.quizapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digarivas.quizapp.R
import com.digarivas.quizapp.domain.ExamDraftSummary
import com.digarivas.quizapp.settings.SettingsManager
import com.digarivas.quizapp.settings.ThemeMode
import com.digarivas.quizapp.ui.components.AppHeader
import com.digarivas.quizapp.ui.navigation.MainTabs
import com.digarivas.quizapp.ui.theme.Geist
import com.digarivas.quizapp.ui.viewmodels.MainViewModel
import com.digarivas.quizapp.ui.viewmodels.QuizViewModel
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val labelRes: Int,
    val iconRes: Int
)

private val BottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, R.drawable.ic_tab_home),
    BottomNavItem(R.string.nav_exam, R.drawable.ic_tab_exam),
    BottomNavItem(R.string.nav_failed, R.drawable.ic_tab_failed),
    BottomNavItem(R.string.nav_stats, R.drawable.ic_tab_stats)
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    settingsManager: SettingsManager,
    onStartQuiz: (
        setId: Long,
        mode: String,
        order: String,
        timeLimit: Boolean,
        practice: Boolean
    ) -> Unit,
    onResumeQuiz: (ExamDraftSummary) -> Unit
) {
    val sets by viewModel.sets.collectAsState()
    val failed by viewModel.failed.collectAsState()
    val history by viewModel.history.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val selectedSetId by viewModel.selectedSetId.collectAsState()
    val pendingTab by viewModel.pendingTab.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(MainTabs.IMPORT) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(pendingTab) {
        pendingTab?.let { page ->
            if (page in 0 until MainTabs.COUNT) {
                selectedTab = page
            }
            viewModel.consumeTabRequest()
        }
    }

    val title = stringResource(
        when (selectedTab) {
            MainTabs.EXAM -> R.string.nav_exam
            MainTabs.FAILED -> R.string.nav_failed
            MainTabs.STATS -> R.string.nav_stats
            else -> R.string.nav_home
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = title,
                trailing = { ThemeToggle(settingsManager) }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    MainTabs.IMPORT -> ImportScreen(
                        viewModel = viewModel,
                        sets = sets,
                        failed = failed,
                        history = history,
                        drafts = drafts,
                        onOpenExam = { setId ->
                            viewModel.selectSet(setId)
                            selectedTab = MainTabs.EXAM
                        },
                        onStartQuiz = onStartQuiz,
                        onResumeQuiz = onResumeQuiz,
                        onDiscardDraft = viewModel::discardDraft,
                        showMessage = showMessage
                    )

                    MainTabs.EXAM -> ExamSetupScreen(
                        sets = sets,
                        failed = failed,
                        drafts = drafts,
                        selectedSetId = selectedSetId,
                        onSelectSet = viewModel::selectSet,
                        onStartQuiz = onStartQuiz,
                        onResumeQuiz = onResumeQuiz,
                        onGoImport = { selectedTab = MainTabs.IMPORT }
                    )

                    MainTabs.FAILED -> FailedScreen(
                        sets = sets,
                        failed = failed,
                        onRemove = viewModel::removeFailed,
                        onRetry = { setName ->
                            sets.firstOrNull { it.name == setName }?.let { set ->
                                onStartQuiz(
                                    set.id,
                                    QuizViewModel.MODE_FAILED,
                                    QuizViewModel.ORDER_RANDOM,
                                    false,
                                    true
                                )
                            }
                        },
                        onClearAll = viewModel::clearFailed,
                        exportTxt = viewModel::exportFailedTxt,
                        exportJson = viewModel::exportFailedJson,
                        showMessage = showMessage
                    )

                    MainTabs.STATS -> StatsScreen(
                        sets = sets,
                        failed = failed,
                        history = history,
                        onClearHistory = viewModel::clearHistory,
                        onOpenFailed = { selectedTab = MainTabs.FAILED }
                    )
                }
            }

            BottomNav(
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@Composable
private fun BottomNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItems.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onSelect(index) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(item.labelRes),
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Geist,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeToggle(settingsManager: SettingsManager) {
    val theme by settingsManager.theme.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM_DEFAULT -> systemDark
    }
    val description = stringResource(R.string.theme_quick_toggle)

    IconButton(
        onClick = {
            settingsManager.setTheme(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
        },
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        Icon(
            painter = painterResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}
