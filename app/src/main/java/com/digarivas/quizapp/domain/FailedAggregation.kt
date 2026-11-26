package com.digarivas.quizapp.domain

import com.digarivas.quizapp.data.db.FailedQuestionEntity

/** Referencia mínima para borrar una fallada concreta de la base de datos. */
data class FailedRef(
    val setName: String,
    val questionId: String
)

/**
 * Pregunta fallada ya agregada: si la misma pregunta aparece en varios
 * conjuntos, se muestra una sola vez y se suman sus fallos.
 */
data class FailedItem(
    val key: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val wrongAnswer: String,
    val failCount: Int,
    val lastFailed: Long,
    val refs: List<FailedRef>,
    val setNames: List<String>,
    val primarySetName: String,
    val groupName: String?
) {
    val otherSetNames: List<String>
        get() = setNames.filter { it != primarySetName }
}

data class FailedSetGroup(
    val setName: String,
    val items: List<FailedItem>
) {
    val itemCount: Int get() = items.size
    val failCount: Int get() = items.sumOf { it.failCount }
}

data class FailedGroup(
    val name: String?,
    val sets: List<FailedSetGroup>,
    val items: List<FailedItem>
) {
    val itemCount: Int get() = items.size
    val failCount: Int get() = items.sumOf { it.failCount }
}

/**
 * Agrupa las falladas por grupo y por conjunto. La clave de deduplicación es
 * el texto de la pregunta + la respuesta correcta: el mismo reactivo importado
 * en dos conjuntos se une, pero dos preguntas distintas con el mismo texto y
 * respuestas distintas no.
 */
object FailedAggregator {

    fun aggregate(
        failed: List<FailedQuestionEntity>,
        sets: List<SetSummary>
    ): List<FailedItem> {
        if (failed.isEmpty()) return emptyList()

        val groupBySet = sets.associate { it.name to it.groupName }

        return failed
            .groupBy { keyOf(it) }
            .map { (key, entries) ->
                val primary = entries.maxByOrNull { it.lastFailed } ?: entries.first()
                val refs = entries
                    .map { FailedRef(it.setName, it.questionId) }
                    .distinct()
                FailedItem(
                    key = key,
                    question = primary.question,
                    options = primary.options,
                    correctAnswer = primary.correctAnswer,
                    explanation = primary.explanation,
                    wrongAnswer = primary.wrongAnswer,
                    failCount = entries.sumOf { it.failCount.coerceAtLeast(1) },
                    lastFailed = primary.lastFailed,
                    refs = refs,
                    setNames = refs.map { it.setName }
                        .distinct()
                        .sortedWith(String.CASE_INSENSITIVE_ORDER),
                    primarySetName = primary.setName,
                    groupName = groupBySet[primary.setName]
                )
            }
            .sortedWith(itemComparator)
    }

    fun group(items: List<FailedItem>): List<FailedGroup> {
        return items
            .groupBy { it.groupName }
            .entries
            .sortedWith(
                compareBy<Map.Entry<String?, List<FailedItem>>> { it.key == null }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.key ?: "" }
            )
            .map { (groupName, groupItems) ->
                val sets = groupItems
                    .groupBy { it.primarySetName }
                    .entries
                    .sortedWith(
                        compareBy(String.CASE_INSENSITIVE_ORDER) { it.key }
                    )
                    .map { (setName, setItems) ->
                        FailedSetGroup(
                            setName = setName,
                            items = setItems.sortedWith(itemComparator)
                        )
                    }
                FailedGroup(
                    name = groupName,
                    sets = sets,
                    items = groupItems.sortedWith(itemComparator)
                )
            }
    }

    private val itemComparator =
        compareByDescending<FailedItem> { it.failCount }
            .thenByDescending { it.lastFailed }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.question }

    private fun keyOf(entity: FailedQuestionEntity): String {
        val question = entity.question.trim().lowercase()
        val answer = entity.correctAnswer.trim().lowercase()
        return if (question.isEmpty()) {
            "${entity.setName}\u0000${entity.questionId}"
        } else {
            "$question\u0000$answer"
        }
    }
}
