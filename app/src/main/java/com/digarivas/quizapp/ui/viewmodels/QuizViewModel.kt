package com.digarivas.quizapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digarivas.quizapp.data.db.ExamDraftEntity
import com.digarivas.quizapp.data.repository.QuizRepository
import com.digarivas.quizapp.domain.DraftAnswer
import com.digarivas.quizapp.domain.FailureInput
import com.digarivas.quizapp.domain.QuestionParser
import com.digarivas.quizapp.domain.QuestionState
import com.digarivas.quizapp.domain.QuizLoadError
import com.digarivas.quizapp.domain.QuizQuestion
import com.digarivas.quizapp.domain.QuizResult
import com.digarivas.quizapp.domain.QuizUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QuizViewModel(
    private val repository: QuizRepository,
    private val setId: Long,
    val mode: String,
    val order: String,
    private val timeLimit: Boolean = false,
    private val practice: Boolean = true,
    private val resume: Boolean = false
) : ViewModel() {

    private val _ui = MutableStateFlow(QuizUiState())
    val ui: StateFlow<QuizUiState> = _ui.asStateFlow()

    private var finishing = false
    private var startedAt = System.currentTimeMillis()
    private var saveGeneration = 0
    private val saveMutex = Mutex()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _ui.value = QuizUiState(loading = true, timeLimit = timeLimit, practice = practice)

            val set = repository.getSet(setId)
            if (set == null) {
                _ui.value = QuizUiState(
                    loading = false,
                    error = QuizLoadError.SET_NOT_FOUND,
                    timeLimit = timeLimit,
                    practice = practice
                )
                return@launch
            }

            val allQuestions = repository.getQuestions(setId).mapNotNull { entity ->
                val indices = QuestionParser.resolveCorrectIndices(
                    entity.correctAnswer,
                    entity.options
                )
                if (indices.isEmpty()) {
                    null
                } else {
                    QuizQuestion(
                        externalId = entity.externalId,
                        text = entity.question,
                        options = entity.options,
                        correctAnswer = entity.correctAnswer,
                        explanation = entity.explanation,
                        correctIndices = indices,
                        isMulti = indices.size > 1
                    )
                }
            }

            if (allQuestions.isEmpty()) {
                _ui.value = QuizUiState(
                    loading = false,
                    setName = set.name,
                    error = QuizLoadError.NO_QUESTIONS,
                    timeLimit = timeLimit,
                    practice = practice
                )
                return@launch
            }

            val restored = if (resume) {
                repository.getDraft(setId, mode)?.let { draft ->
                    restoreDraft(draft, allQuestions)
                }
            } else {
                null
            }

            if (restored != null) {
                startedAt = restored.startedAt
                val ui = QuizUiState(
                    loading = false,
                    setName = set.name,
                    questions = restored.questions,
                    states = restored.states,
                    current = restored.current,
                    timeLimit = restored.timeLimit,
                    practice = restored.practice
                )
                _ui.value = ui
                persist(ui)
            } else {
                val failedIds = if (mode == MODE_FAILED) {
                    repository.getFailedBySet(set.name).map { it.questionId }.toSet()
                } else {
                    emptySet()
                }

                var questions = if (mode == MODE_FAILED) {
                    allQuestions.filter { it.externalId in failedIds }
                } else {
                    allQuestions
                }

                if (questions.isEmpty()) {
                    _ui.value = QuizUiState(
                        loading = false,
                        setName = set.name,
                        error = if (mode == MODE_FAILED) {
                            QuizLoadError.NO_FAILED
                        } else {
                            QuizLoadError.NO_QUESTIONS
                        },
                        timeLimit = timeLimit,
                        practice = practice
                    )
                    return@launch
                }

                if (order == ORDER_RANDOM) {
                    questions = questions.shuffled()
                }

                startedAt = System.currentTimeMillis()
                val ui = QuizUiState(
                    loading = false,
                    setName = set.name,
                    questions = questions,
                    states = List(questions.size) { QuestionState() },
                    current = 0,
                    timeLimit = timeLimit,
                    practice = practice
                )
                _ui.value = ui
                persist(ui)
            }

            finishing = false
        }
    }

    fun restart() {
        finishing = false
        saveGeneration++
        viewModelScope.launch {
            saveMutex.withLock { repository.deleteDraft(setId, mode) }
            load()
        }
    }

    private data class RestoredDraft(
        val questions: List<QuizQuestion>,
        val states: List<QuestionState>,
        val current: Int,
        val timeLimit: Boolean,
        val practice: Boolean,
        val startedAt: Long
    )

    private fun restoreDraft(
        draft: ExamDraftEntity,
        allQuestions: List<QuizQuestion>
    ): RestoredDraft? {
        val answers = repository.decodeDraftAnswers(draft)
        if (answers.isEmpty()) return null

        val byId = allQuestions.associateBy { it.externalId }
        val pairs = answers.mapNotNull { answer ->
            byId[answer.externalId]?.let { question -> answer to question }
        }
        if (pairs.isEmpty()) return null

        return RestoredDraft(
            questions = pairs.map { it.second },
            states = pairs.map { (answer, question) ->
                val selected = answer.selected
                    .filter { it in question.options.indices }
                    .toSet()
                QuestionState(
                    selected = selected,
                    checked = answer.checked,
                    correct = answer.correct ?: if (answer.checked) {
                        selected == question.correctIndices
                    } else {
                        null
                    }
                )
            },
            current = draft.current.coerceIn(0, pairs.lastIndex),
            timeLimit = draft.timeLimit,
            practice = draft.practice,
            startedAt = draft.startedAt
        )
    }

    private fun persist(state: QuizUiState = _ui.value) {
        val snapshot = state
        val generation = saveGeneration
        viewModelScope.launch {
            runCatching { saveSnapshot(snapshot, generation) }
        }
    }

    /** Guarda el estado actual y solo después sale. Evita perder el último cambio. */
    fun saveAndExit(onDone: () -> Unit) {
        val snapshot = _ui.value
        val generation = saveGeneration
        viewModelScope.launch {
            runCatching { saveSnapshot(snapshot, generation) }
            onDone()
        }
    }

    private suspend fun saveSnapshot(state: QuizUiState, generation: Int) {
        if (finishing || generation != saveGeneration) return
        if (state.loading || state.error != null || state.result != null || state.questions.isEmpty()) {
            return
        }

        val answers = state.questions.mapIndexed { index, question ->
            val answer = state.states.getOrNull(index) ?: QuestionState()
            DraftAnswer(
                externalId = question.externalId,
                selected = answer.selected.sorted(),
                checked = answer.checked,
                correct = answer.correct
            )
        }
        val start = startedAt

        saveMutex.withLock {
            if (finishing || generation != saveGeneration) return@withLock
            repository.saveDraft(
                setId = setId,
                setName = state.setName,
                mode = mode,
                order = order,
                timeLimit = state.timeLimit,
                practice = state.practice,
                answers = answers,
                current = state.current,
                startedAt = start
            )
        }
    }

    fun selectOption(index: Int) {
        val state = _ui.value
        val question = state.question ?: return
        val current = state.currentState ?: return
        if (current.checked || index !in question.options.indices) return

        val selected = if (question.isMulti) {
            if (index in current.selected) current.selected - index
            else current.selected + index
        } else {
            setOf(index)
        }

        updateCurrentState { it.copy(selected = selected) }
        persist()
    }

    fun checkAnswer() {
        val state = _ui.value
        val question = state.question ?: return
        val current = state.currentState ?: return
        if (current.checked || current.selected.isEmpty()) return

        val isCorrect = current.selected == question.correctIndices
        updateCurrentState { it.copy(checked = true, correct = isCorrect) }
        persist()

        if (!isCorrect) {
            viewModelScope.launch {
                repository.recordFailures(
                    state.setName,
                    listOf(FailureInput(question, current.selected))
                )
            }
        }
    }

    /**
     * El temporizador por pregunta ha llegado a cero: se comprueba la pregunta
     * aunque no se haya respondido. Si no hay selección, cuenta como fallada.
     */
    fun timeOut() {
        val state = _ui.value
        val question = state.question ?: return
        val current = state.currentState ?: return
        if (current.checked) return

        val isCorrect = current.selected == question.correctIndices
        updateCurrentState { it.copy(checked = true, correct = isCorrect) }
        persist()

        if (!isCorrect) {
            viewModelScope.launch {
                repository.recordFailures(
                    state.setName,
                    listOf(FailureInput(question, current.selected))
                )
            }
        }
    }

    fun goTo(index: Int) {
        val state = _ui.value
        if (index in state.questions.indices) {
            _ui.update { it.copy(current = index) }
            persist()
        }
    }

    fun previous() {
        goTo(_ui.value.current - 1)
    }

    fun next() {
        goTo(_ui.value.current + 1)
    }

    fun finish() {
        if (finishing) return
        val snapshot = _ui.value
        if (snapshot.questions.isEmpty() || snapshot.result != null) return
        finishing = true
        saveGeneration++

        viewModelScope.launch {
            try {
                val unanswered = snapshot.states.count { !it.checked }

                val checkedStates = snapshot.states.mapIndexed { index, state ->
                    if (state.checked) {
                        state
                    } else {
                        val question = snapshot.questions[index]
                        state.copy(
                            checked = true,
                            correct = state.selected == question.correctIndices
                        )
                    }
                }

                val newlyFailed = snapshot.questions.mapIndexedNotNull { index, question ->
                    val state = snapshot.states[index]
                    if (!state.checked && state.selected != question.correctIndices) {
                        FailureInput(question, state.selected)
                    } else {
                        null
                    }
                }

                val correct = checkedStates.count { it.correct == true }
                val total = checkedStates.size

                repository.recordFailures(snapshot.setName, newlyFailed)
                repository.insertHistory(snapshot.setName, correct, total, mode)
                saveMutex.withLock { repository.deleteDraft(setId, mode) }

                _ui.update {
                    it.copy(
                        states = checkedStates,
                        result = QuizResult(
                            setName = snapshot.setName,
                            mode = mode,
                            correct = correct,
                            wrong = total - correct,
                            unanswered = unanswered,
                            total = total
                        )
                    )
                }
            } catch (_: Exception) {
                // Si falla el guardado, el examen sigue abierto y se puede reintentar.
            } finally {
                finishing = false
            }
        }
    }

    private fun updateCurrentState(transform: (QuestionState) -> QuestionState) {
        _ui.update { state ->
            val index = state.current
            if (index !in state.states.indices) {
                state
            } else {
                state.copy(
                    states = state.states.mapIndexed { i, item ->
                        if (i == index) transform(item) else item
                    }
                )
            }
        }
    }

    class Factory(
        private val repository: QuizRepository,
        private val setId: Long,
        private val mode: String,
        private val order: String,
        private val timeLimit: Boolean,
        private val practice: Boolean,
        private val resume: Boolean = false
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                return QuizViewModel(
                    repository,
                    setId,
                    mode,
                    order,
                    timeLimit,
                    practice,
                    resume
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }

    companion object {
        const val MODE_ALL = "all"
        const val MODE_FAILED = "failed"
        const val ORDER_RANDOM = "random"
        const val ORDER_SEQUENTIAL = "sequential"
    }
}
