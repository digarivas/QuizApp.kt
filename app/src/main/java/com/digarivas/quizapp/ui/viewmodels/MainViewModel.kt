package com.digarivas.quizapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digarivas.quizapp.data.db.FailedQuestionEntity
import com.digarivas.quizapp.data.db.QuizHistoryEntity
import com.digarivas.quizapp.data.repository.QuizRepository
import com.digarivas.quizapp.domain.ExamDraftSummary
import com.digarivas.quizapp.domain.FailedAggregator
import com.digarivas.quizapp.domain.FailedRef
import com.digarivas.quizapp.domain.GroupJoinResult
import com.digarivas.quizapp.domain.GroupRenameResult
import com.digarivas.quizapp.domain.QuestionParseException
import com.digarivas.quizapp.domain.QuestionParser
import com.digarivas.quizapp.domain.SetSummary
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportOutcome {
    data class Success(
        val setId: Long,
        val setName: String,
        val imported: Int,
        val skipped: Int
    ) : ImportOutcome()

    data class Error(val message: String) : ImportOutcome()
}

class MainViewModel(private val repository: QuizRepository) : ViewModel() {

    val sets: StateFlow<List<SetSummary>> = repository.observeSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val failed: StateFlow<List<FailedQuestionEntity>> = repository.observeFailed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<QuizHistoryEntity>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val drafts: StateFlow<List<ExamDraftSummary>> = repository.observeDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedSetId = MutableStateFlow<Long?>(null)
    val selectedSetId: StateFlow<Long?> = _selectedSetId.asStateFlow()

    private val _pendingTab = MutableStateFlow<Int?>(null)
    val pendingTab: StateFlow<Int?> = _pendingTab.asStateFlow()

    fun selectSet(setId: Long) {
        _selectedSetId.value = setId
    }

    fun requestTab(index: Int) {
        _pendingTab.value = index
    }

    fun consumeTabRequest() {
        _pendingTab.value = null
    }

    fun importJson(
        raw: String,
        preferredName: String?,
        onResult: (ImportOutcome) -> Unit
    ) {
        viewModelScope.launch { onResult(importOne(raw, preferredName)) }
    }

    /** Importa un JSON y guarda el conjunto. Pensado para encadenar varios archivos. */
    suspend fun importOne(
        raw: String,
        preferredName: String?,
        groupName: String? = null
    ): ImportOutcome {
        if (raw.isBlank()) return ImportOutcome.Error("El JSON está vacío")

        val outcome = try {
            withContext(Dispatchers.Default) { QuestionParser.parse(raw) }
        } catch (e: QuestionParseException) {
            return ImportOutcome.Error(e.message ?: "Formato incorrecto")
        } catch (e: Exception) {
            return ImportOutcome.Error("Formato incorrecto")
        }

        if (outcome.questions.isEmpty()) {
            return ImportOutcome.Error(
                outcome.errors.firstOrNull() ?: "No hay preguntas válidas"
            )
        }

        return try {
            val summary = repository.importSet(preferredName, outcome.questions, groupName)
            _selectedSetId.value = summary.id
            ImportOutcome.Success(
                setId = summary.id,
                setName = summary.name,
                imported = outcome.questions.size,
                skipped = outcome.errors.size
            )
        } catch (e: Exception) {
            ImportOutcome.Error("No se pudo guardar el conjunto")
        }
    }

    fun renameSet(setId: Long, name: String, onDone: (String?) -> Unit) {
        viewModelScope.launch { onDone(repository.renameSet(setId, name)) }
    }

    fun moveToGroup(setIds: Collection<Long>, group: String?) {
        if (setIds.isEmpty()) return
        viewModelScope.launch { repository.moveToGroup(setIds, group) }
    }

    /** Igual que [moveToGroup], pero espera y devuelve el nombre canónico. */
    suspend fun moveToGroupNow(setIds: Collection<Long>, group: String?): String? {
        return repository.moveToGroup(setIds, group)
    }

    fun groupSetsTogether(
        sourceId: Long,
        targetId: Long,
        defaultGroupName: String,
        onDone: (GroupJoinResult?) -> Unit
    ) {
        viewModelScope.launch {
            onDone(repository.groupSetsTogether(sourceId, targetId, defaultGroupName))
        }
    }

    fun renameGroup(oldName: String, newName: String, onDone: (GroupRenameResult?) -> Unit) {
        viewModelScope.launch { onDone(repository.renameGroup(oldName, newName)) }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            repository.deleteSet(setId)
            if (_selectedSetId.value == setId) {
                _selectedSetId.value = null
            }
        }
    }

    fun removeFailed(refs: List<FailedRef>) {
        viewModelScope.launch {
            repository.removeFailed(refs)
        }
    }

    fun discardDraft(setId: Long, mode: String) {
        viewModelScope.launch {
            repository.deleteDraft(setId, mode)
        }
    }

    fun clearFailed() {
        viewModelScope.launch {
            repository.clearFailed()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun exportFailedTxt(): String {
        return aggregatedFailed().mapIndexed { index, item ->
            buildString {
                append("${index + 1}. ${item.question}\n")
                append("   Correcta: ${item.correctAnswer}\n")
                if (item.wrongAnswer.isNotBlank()) {
                    append("   Tu respuesta: ${item.wrongAnswer}\n")
                }
                if (item.explanation.isNotBlank()) {
                    append("   Explicación: ${item.explanation}\n")
                }
                val veces = if (item.failCount == 1) "vez" else "veces"
                append("   Fallada: ${item.failCount} $veces\n")
                append("   Conjuntos: ${item.setNames.joinToString(", ")}\n")
            }
        }.joinToString("\n")
    }

    fun exportFailedJson(): String {
        val data = aggregatedFailed().map { item ->
            FailedExport(
                question = item.question,
                options = item.options,
                correct_answer = item.correctAnswer,
                wrong_answer = item.wrongAnswer,
                explanation = item.explanation,
                set = item.primarySetName,
                sets = item.setNames,
                group = item.groupName,
                fail_count = item.failCount
            )
        }
        return GsonBuilder().setPrettyPrinting().create().toJson(data)
    }

    private fun aggregatedFailed() = FailedAggregator.aggregate(failed.value, sets.value)

    private data class FailedExport(
        val question: String,
        val options: List<String>,
        val correct_answer: String,
        val wrong_answer: String,
        val explanation: String,
        val set: String,
        val sets: List<String>,
        val group: String?,
        val fail_count: Int
    )

    class Factory(private val repository: QuizRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
