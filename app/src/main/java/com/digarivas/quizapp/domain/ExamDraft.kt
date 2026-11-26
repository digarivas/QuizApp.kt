package com.digarivas.quizapp.domain

/**
 * Respuesta guardada de una pregunta dentro de un examen en curso.
 * `selected` son índices de opción, no letras, para no depender del formato.
 */
data class DraftAnswer(
    val externalId: String,
    val selected: List<Int> = emptyList(),
    val checked: Boolean = false,
    val correct: Boolean? = null
)

/** Resumen de un examen en curso para pintarlo en Inicio. */
data class ExamDraftSummary(
    val setId: Long,
    val setName: String,
    val mode: String,
    val order: String,
    val timeLimit: Boolean,
    val practice: Boolean,
    val answered: Int,
    val total: Int,
    val current: Int,
    val updatedAt: Long
) {
    val progress: Float
        get() = if (total <= 0) 0f else answered.toFloat() / total
}
