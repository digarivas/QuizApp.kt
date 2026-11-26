package com.digarivas.quizapp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_sets",
    indices = [Index(value = ["name"], unique = true)]
)
data class QuestionSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("setId")]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setId: Long,
    val externalId: String,
    val position: Int,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

@Entity(
    tableName = "failed_questions",
    primaryKeys = ["setName", "questionId"]
)
data class FailedQuestionEntity(
    val setName: String,
    val questionId: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val wrongAnswer: String,
    val failCount: Int,
    val lastFailed: Long
)

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val setName: String,
    val score: Int,
    val total: Int,
    val mode: String
)

/**
 * Examen empezado y no terminado. Se guarda uno por conjunto y modo
 * (completo / solo falladas), con la lista de respuestas en JSON.
 */
@Entity(
    tableName = "exam_drafts",
    primaryKeys = ["setId", "mode"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("setId")]
)
data class ExamDraftEntity(
    val setId: Long,
    val mode: String,
    val setName: String,
    val order: String,
    val timeLimit: Boolean,
    val practice: Boolean,
    val answersJson: String,
    val current: Int,
    val answered: Int,
    val total: Int,
    val startedAt: Long,
    val updatedAt: Long
)
