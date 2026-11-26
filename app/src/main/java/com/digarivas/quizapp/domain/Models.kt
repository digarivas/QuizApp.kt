package com.digarivas.quizapp.domain

// ---------------------------------------------------------------------------
// Modelos de dominio: independientes de Room y de Compose.
// ---------------------------------------------------------------------------

data class ParsedQuestion(
    val externalId: String,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val correctIndices: Set<Int>,
    val isMulti: Boolean
)

data class ParseOutcome(
    val questions: List<ParsedQuestion>,
    val errors: List<String>
)

class QuestionParseException(message: String) : Exception(message)

data class SetSummary(
    val id: Long,
    val name: String,
    val questionCount: Int,
    val createdAt: Long,
    val groupName: String? = null
)

/** Resultado de soltar un conjunto sobre otro para agruparlos. */
data class GroupJoinResult(
    val groupName: String,
    val setCount: Int,
    val merged: Boolean,
    val alreadyGrouped: Boolean = false
)

/** Resultado de renombrar un grupo, incluida la fusión con uno existente. */
data class GroupRenameResult(
    val name: String,
    val merged: Boolean
)

data class QuizQuestion(
    val externalId: String,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val correctIndices: Set<Int>,
    val isMulti: Boolean
)

data class QuestionState(
    val selected: Set<Int> = emptySet(),
    val checked: Boolean = false,
    val correct: Boolean? = null
)

data class QuizResult(
    val setName: String,
    val mode: String,
    val correct: Int,
    val wrong: Int,
    val unanswered: Int,
    val total: Int
) {
    val percent: Int get() = if (total == 0) 0 else (correct * 100) / total
}

data class FailureInput(
    val question: QuizQuestion,
    val selected: Set<Int>
)

enum class QuizLoadError {
    SET_NOT_FOUND,
    NO_QUESTIONS,
    NO_FAILED
}

data class QuizUiState(
    val loading: Boolean = true,
    val setName: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val states: List<QuestionState> = emptyList(),
    val current: Int = 0,
    val result: QuizResult? = null,
    val error: QuizLoadError? = null,
    val timeLimit: Boolean = false,
    val practice: Boolean = true
) {
    val question: QuizQuestion? get() = questions.getOrNull(current)
    val currentState: QuestionState? get() = states.getOrNull(current)
    val checkedCount: Int get() = states.count { it.checked }
    val correctCount: Int get() = states.count { it.correct == true }
}
