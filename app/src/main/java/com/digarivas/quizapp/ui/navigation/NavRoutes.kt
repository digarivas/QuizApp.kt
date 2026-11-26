package com.digarivas.quizapp.ui.navigation

object MainTabs {
    const val IMPORT = 0
    const val EXAM = 1
    const val FAILED = 2
    const val STATS = 3
    const val COUNT = 4
}

sealed class NavRoutes(val route: String) {

    object Main : NavRoutes("main")

    object Quiz : NavRoutes("quiz/{setId}/{mode}/{order}/{timeLimit}/{practice}/{resume}") {
        fun createRoute(
            setId: Long,
            mode: String,
            order: String,
            timeLimit: Boolean,
            practice: Boolean,
            resume: Boolean = false
        ): String = "quiz/$setId/$mode/$order/$timeLimit/$practice/$resume"
    }
}
