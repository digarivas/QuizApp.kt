package com.digarivas.quizapp.data.repository

import androidx.room.withTransaction
import com.digarivas.quizapp.data.db.AppDatabase
import com.digarivas.quizapp.data.db.ExamDraftEntity
import com.digarivas.quizapp.data.db.FailedQuestionEntity
import com.digarivas.quizapp.data.db.QuestionEntity
import com.digarivas.quizapp.data.db.QuestionSetEntity
import com.digarivas.quizapp.data.db.QuizHistoryEntity
import com.digarivas.quizapp.domain.DraftAnswer
import com.digarivas.quizapp.domain.ExamDraftSummary
import com.digarivas.quizapp.domain.FailedRef
import com.digarivas.quizapp.domain.FailureInput
import com.digarivas.quizapp.domain.GroupJoinResult
import com.digarivas.quizapp.domain.GroupRenameResult
import com.digarivas.quizapp.domain.ParsedQuestion
import com.digarivas.quizapp.domain.QuestionParser
import com.digarivas.quizapp.domain.SetSummary
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Único punto de acceso a datos. Sin DI: se construye una vez en AppNavigation.
 */
class QuizRepository(private val db: AppDatabase) {

    private val setDao = db.questionSetDao()
    private val questionDao = db.questionDao()
    private val failedDao = db.failedQuestionDao()
    private val historyDao = db.quizHistoryDao()
    private val draftDao = db.examDraftDao()
    private val gson = Gson()

    fun observeSets(): Flow<List<SetSummary>> {
        return combine(setDao.observeAll(), questionDao.observeAll()) { sets, questions ->
            val counts = questions.groupingBy { it.setId }.eachCount()
            sets.map { set ->
                SetSummary(
                    id = set.id,
                    name = set.name,
                    questionCount = counts[set.id] ?: 0,
                    createdAt = set.createdAt,
                    groupName = set.groupName
                )
            }
        }
    }

    fun observeFailed(): Flow<List<FailedQuestionEntity>> = failedDao.observeAll()

    fun observeHistory(): Flow<List<QuizHistoryEntity>> = historyDao.observeAll()

    fun observeDrafts(): Flow<List<ExamDraftSummary>> {
        return draftDao.observeAll().map { drafts ->
            drafts.map { draft ->
                ExamDraftSummary(
                    setId = draft.setId,
                    setName = draft.setName,
                    mode = draft.mode,
                    order = draft.order,
                    timeLimit = draft.timeLimit,
                    practice = draft.practice,
                    answered = draft.answered,
                    total = draft.total,
                    current = draft.current,
                    updatedAt = draft.updatedAt
                )
            }
        }
    }

    suspend fun getDraft(setId: Long, mode: String): ExamDraftEntity? =
        draftDao.find(setId, mode)

    suspend fun saveDraft(
        setId: Long,
        setName: String,
        mode: String,
        order: String,
        timeLimit: Boolean,
        practice: Boolean,
        answers: List<DraftAnswer>,
        current: Int,
        startedAt: Long
    ) {
        draftDao.upsert(
            ExamDraftEntity(
                setId = setId,
                mode = mode,
                setName = setName,
                order = order,
                timeLimit = timeLimit,
                practice = practice,
                answersJson = gson.toJson(answers),
                current = current,
                answered = answers.count { it.checked || it.selected.isNotEmpty() },
                total = answers.size,
                startedAt = startedAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun decodeDraftAnswers(draft: ExamDraftEntity): List<DraftAnswer> {
        return runCatching {
            gson.fromJson(draft.answersJson, Array<DraftAnswer>::class.java)?.toList()
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun deleteDraft(setId: Long, mode: String) {
        draftDao.delete(setId, mode)
    }

    suspend fun getSet(setId: Long): QuestionSetEntity? = setDao.findById(setId)

    suspend fun getQuestions(setId: Long): List<QuestionEntity> = questionDao.getBySet(setId)

    suspend fun getFailedBySet(setName: String): List<FailedQuestionEntity> =
        failedDao.getBySet(setName)

    suspend fun importSet(
        preferredName: String?,
        questions: List<ParsedQuestion>,
        groupName: String? = null
    ): SetSummary {
        return db.withTransaction {
            val baseName = preferredName?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Conjunto ${setDao.count() + 1}"
            val name = uniqueSetName(baseName)
            val group = groupName?.trim()?.takeIf { it.isNotEmpty() }

            val now = System.currentTimeMillis()
            val setId = setDao.insert(
                QuestionSetEntity(name = name, groupName = group, createdAt = now)
            )
            questionDao.insertAll(
                questions.mapIndexed { index, question ->
                    QuestionEntity(
                        setId = setId,
                        externalId = question.externalId,
                        position = index,
                        question = question.text,
                        options = question.options,
                        correctAnswer = question.correctAnswer,
                        explanation = question.explanation
                    )
                }
            )
            SetSummary(
                id = setId,
                name = name,
                questionCount = questions.size,
                createdAt = now,
                groupName = group
            )
        }
    }

    /**
     * Renombra el conjunto. Si el nombre ya existe se añade un sufijo.
     * Las falladas y el historial guardan el nombre del conjunto, así que se migran
     * para que "solo falladas" siga encontrándolas.
     * @return el nombre final, o null si el nombre está vacío.
     */
    suspend fun renameSet(setId: Long, rawName: String): String? {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return null
        return db.withTransaction {
            val current = setDao.findById(setId) ?: return@withTransaction null
            if (current.name == trimmed) return@withTransaction current.name

            val name = uniqueSetName(trimmed, exceptId = setId)
            setDao.rename(setId, name)
            failedDao.dropRenameCollisions(current.name, name)
            failedDao.renameSet(current.name, name)
            historyDao.renameSet(current.name, name)
            draftDao.renameSet(setId, name)
            name
        }
    }

    /**
     * Mueve conjuntos a un grupo. Un nombre vacío los deja sin grupo.
     * @return el nombre final del grupo, o null si se han desagrupado.
     */
    suspend fun moveToGroup(setIds: Collection<Long>, rawGroup: String?): String? {
        if (setIds.isEmpty()) return null
        val requested = rawGroup?.trim()?.takeIf { it.isNotEmpty() }
        return db.withTransaction {
            val group = requested?.let { canonicalGroup(it) }
            setIds.forEach { setDao.setGroup(it, group) }
            group
        }
    }

    /**
     * Agrupa dos conjuntos al soltar uno sobre otro, con la misma semántica que
     * la versión web: si el origen ya tiene grupo se conserva su nombre y, si el
     * destino también tiene otro grupo, ambos se fusionan.
     */
    suspend fun groupSetsTogether(
        sourceId: Long,
        targetId: Long,
        defaultGroupName: String
    ): GroupJoinResult? {
        if (sourceId == targetId) return null
        return db.withTransaction {
            val source = setDao.findById(sourceId) ?: return@withTransaction null
            val target = setDao.findById(targetId) ?: return@withTransaction null
            val sourceGroup = source.groupName
            val targetGroup = target.groupName

            if (sourceGroup != null && targetGroup != null &&
                sourceGroup.equals(targetGroup, ignoreCase = true)
            ) {
                return@withTransaction GroupJoinResult(
                    groupName = sourceGroup,
                    setCount = setDao.countInGroup(sourceGroup),
                    merged = false,
                    alreadyGrouped = true
                )
            }

            val group: String
            var merged = false
            when {
                sourceGroup != null -> {
                    group = sourceGroup
                    if (targetGroup != null) {
                        setDao.renameGroup(targetGroup, sourceGroup)
                        merged = true
                    } else {
                        setDao.setGroup(target.id, sourceGroup)
                    }
                }

                targetGroup != null -> {
                    group = targetGroup
                    setDao.setGroup(source.id, targetGroup)
                }

                else -> {
                    group = uniqueGroupName(defaultGroupName)
                    setDao.setGroup(source.id, group)
                    setDao.setGroup(target.id, group)
                }
            }

            GroupJoinResult(
                groupName = group,
                setCount = setDao.countInGroup(group),
                merged = merged
            )
        }
    }

    /** Renombra un grupo completo. Si ya existe otro igual, los fusiona. */
    suspend fun renameGroup(oldName: String, rawName: String): GroupRenameResult? {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return null

        return db.withTransaction {
            val groups = setDao.allGroupNames()
            if (oldName !in groups) return@withTransaction null
            if (trimmed == oldName) return@withTransaction GroupRenameResult(oldName, false)

            val twin = groups.firstOrNull {
                it != oldName && it.equals(trimmed, ignoreCase = true)
            }
            if (twin != null) {
                setDao.renameGroup(oldName, twin)
                GroupRenameResult(twin, true)
            } else {
                setDao.renameGroup(oldName, trimmed)
                GroupRenameResult(trimmed, false)
            }
        }
    }

    /** Reutiliza el nombre exacto de un grupo existente aunque cambien mayúsculas. */
    private suspend fun canonicalGroup(raw: String): String {
        return setDao.allGroupNames().firstOrNull { it.equals(raw, ignoreCase = true) } ?: raw
    }

    /** Evita duplicados si el nombre propuesto ya existe. */
    private suspend fun uniqueGroupName(base: String): String {
        val groups = setDao.allGroupNames()
        var candidate = base
        var suffix = 2
        while (groups.any { it.equals(candidate, ignoreCase = true) }) {
            candidate = "$base ($suffix)"
            suffix++
        }
        return candidate
    }

    private suspend fun uniqueSetName(base: String, exceptId: Long? = null): String {
        var name = base
        var suffix = 2
        while (true) {
            val existing = setDao.findByName(name)
            if (existing == null || existing.id == exceptId) return name
            name = "$base ($suffix)"
            suffix++
        }
    }

    suspend fun deleteSet(setId: Long) {
        db.withTransaction {
            draftDao.deleteBySet(setId)
            setDao.deleteById(setId)
        }
    }

    suspend fun recordFailures(setName: String, failures: List<FailureInput>) {
        if (failures.isEmpty()) return
        db.withTransaction {
            val now = System.currentTimeMillis()
            failures.forEach { failure ->
                val question = failure.question
                val existing = failedDao.find(setName, question.externalId)
                failedDao.upsert(
                    FailedQuestionEntity(
                        setName = setName,
                        questionId = question.externalId,
                        question = question.text,
                        options = question.options,
                        correctAnswer = question.correctAnswer,
                        explanation = question.explanation,
                        wrongAnswer = if (failure.selected.isEmpty()) {
                            ""
                        } else {
                            QuestionParser.labels(failure.selected)
                        },
                        failCount = (existing?.failCount ?: 0) + 1,
                        lastFailed = now
                    )
                )
            }
        }
    }

    suspend fun removeFailed(refs: List<FailedRef>) {
        if (refs.isEmpty()) return
        db.withTransaction {
            refs.distinct().forEach { ref ->
                failedDao.deleteOne(ref.setName, ref.questionId)
            }
        }
    }

    suspend fun clearFailed() {
        failedDao.clear()
    }

    suspend fun insertHistory(setName: String, score: Int, total: Int, mode: String) {
        historyDao.insert(
            QuizHistoryEntity(
                date = System.currentTimeMillis(),
                setName = setName,
                score = score,
                total = total,
                mode = mode
            )
        )
    }

    suspend fun clearHistory() {
        historyDao.clear()
    }
}
