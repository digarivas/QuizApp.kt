package com.digarivas.quizapp.domain

import kotlin.math.roundToInt

/** Una sesión terminada, ya normalizada para las gráficas. */
data class AttemptRecord(
    val setName: String,
    val date: Long,
    val score: Int,
    val total: Int,
    val mode: String
)

/** Un punto de la evolución: % de acierto de una sesión. */
data class ScorePoint(
    val date: Long,
    val percent: Int,
    val score: Int,
    val total: Int,
    val mode: String
)

/** Evolución de un conjunto concreto. */
data class SetEvolution(
    val name: String,
    val groupName: String?,
    val questionCount: Int,
    val attempts: List<ScorePoint>
) {
    val count: Int get() = attempts.size
    val average: Int? get() = attempts.map { it.percent }.averageOrNull()
    val best: Int? get() = attempts.maxOfOrNull { it.percent }
    val last: ScorePoint? get() = attempts.lastOrNull()

    val isOrphan: Boolean get() = questionCount == 0
}

/** Evolución agregada de todos los conjuntos de un grupo. */
data class GroupEvolution(
    val name: String?,
    val sets: List<SetEvolution>,
    val attempts: List<ScorePoint>
) {
    val count: Int get() = attempts.size
    val average: Int? get() = attempts.map { it.percent }.averageOrNull()
    val best: Int? get() = attempts.maxOfOrNull { it.percent }
    val last: ScorePoint? get() = attempts.lastOrNull()
}

data class StudyStats(
    val groups: List<GroupEvolution>,
    val exams: List<SetEvolution>
)

/**
 * Construye las evoluciones por grupo y por examen sin tocar Room ni Compose.
 * Los nombres de conjunto del historial se cruzan con los conjuntos actuales;
 * si un conjunto se borró, su historial se conserva como "sin conjunto".
 */
object StudyStatsBuilder {

    fun build(sets: List<SetSummary>, history: List<AttemptRecord>): StudyStats {
        val byName = history.groupBy { it.setName }
        val knownNames = sets.mapTo(mutableSetOf()) { it.name }

        val exams = sets.map { set ->
            SetEvolution(
                name = set.name,
                groupName = set.groupName,
                questionCount = set.questionCount,
                attempts = points(byName[set.name].orEmpty())
            )
        } + byName.keys
            .filter { it !in knownNames }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .map { name ->
                SetEvolution(
                    name = name,
                    groupName = null,
                    questionCount = 0,
                    attempts = points(byName.getValue(name))
                )
            }

        val groups = exams
            .groupBy { it.groupName }
            .entries
            .sortedWith(
                compareBy<Map.Entry<String?, List<SetEvolution>>> { it.key == null }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.key ?: "" }
            )
            .map { (groupName, groupSets) ->
                GroupEvolution(
                    name = groupName,
                    sets = groupSets.sortedWith(
                        compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    ),
                    attempts = groupSets
                        .flatMap { it.attempts }
                        .sortedBy { it.date }
                )
            }

        return StudyStats(
            groups = groups,
            exams = exams.sortedWith(
                compareByDescending<SetEvolution> { it.last?.date ?: 0L }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        )
    }

    private fun points(records: List<AttemptRecord>): List<ScorePoint> {
        return records
            .sortedBy { it.date }
            .map { record ->
                ScorePoint(
                    date = record.date,
                    percent = percentOf(record.score, record.total),
                    score = record.score,
                    total = record.total,
                    mode = record.mode
                )
            }
    }

    private fun percentOf(score: Int, total: Int): Int {
        if (total <= 0) return 0
        return (score * 100) / total
    }
}

private fun List<Int>.averageOrNull(): Int? {
    if (isEmpty()) return null
    return average().roundToInt()
}
