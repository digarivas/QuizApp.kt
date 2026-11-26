package com.digarivas.quizapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.digarivas.quizapp.data.db.AppDatabase
import com.digarivas.quizapp.data.repository.QuizRepository
import com.digarivas.quizapp.settings.SettingsManager
import com.digarivas.quizapp.ui.screens.MainScreen
import com.digarivas.quizapp.ui.screens.QuizScreen
import com.digarivas.quizapp.ui.viewmodels.MainViewModel
import com.digarivas.quizapp.ui.viewmodels.QuizViewModel

@Composable
fun AppNavigation(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { QuizRepository(database) }
    val mainViewModel: MainViewModel = viewModel(
        factory = remember { MainViewModel.Factory(repository) }
    )
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Main.route
    ) {
        composable(NavRoutes.Main.route) {
            MainScreen(
                viewModel = mainViewModel,
                settingsManager = settingsManager,
                onStartQuiz = { setId, mode, order, timeLimit, practice ->
                    navController.navigate(
                        NavRoutes.Quiz.createRoute(setId, mode, order, timeLimit, practice)
                    )
                },
                onResumeQuiz = { draft ->
                    navController.navigate(
                        NavRoutes.Quiz.createRoute(
                            setId = draft.setId,
                            mode = draft.mode,
                            order = draft.order,
                            timeLimit = draft.timeLimit,
                            practice = draft.practice,
                            resume = true
                        )
                    )
                }
            )
        }

        composable(
            route = NavRoutes.Quiz.route,
            arguments = listOf(
                navArgument("setId") { type = NavType.LongType },
                navArgument("mode") { type = NavType.StringType },
                navArgument("order") { type = NavType.StringType },
                navArgument("timeLimit") { type = NavType.BoolType },
                navArgument("practice") { type = NavType.BoolType },
                navArgument("resume") { type = NavType.BoolType }
            )
        ) { entry ->
            val setId = entry.arguments?.getLong("setId") ?: 0L
            val mode = entry.arguments?.getString("mode") ?: QuizViewModel.MODE_ALL
            val order = entry.arguments?.getString("order") ?: QuizViewModel.ORDER_RANDOM
            val timeLimit = entry.arguments?.getBoolean("timeLimit") ?: false
            val practice = entry.arguments?.getBoolean("practice") ?: true
            val resume = entry.arguments?.getBoolean("resume") ?: false

            val factory = remember(setId, mode, order, timeLimit, practice, resume) {
                QuizViewModel.Factory(repository, setId, mode, order, timeLimit, practice, resume)
            }
            val quizViewModel: QuizViewModel = viewModel(factory = factory)

            QuizScreen(
                viewModel = quizViewModel,
                onExit = { navController.popBackStack() },
                onSeeFailed = {
                    mainViewModel.requestTab(MainTabs.FAILED)
                    navController.popBackStack()
                },
                onGoHome = {
                    mainViewModel.requestTab(MainTabs.IMPORT)
                    navController.popBackStack()
                }
            )
        }
    }
}
