package com.digarivas.quizapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionSetDao {

    @Query("SELECT * FROM question_sets ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<QuestionSetEntity>>

    @Query("SELECT * FROM question_sets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): QuestionSetEntity?

    @Query("SELECT * FROM question_sets WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): QuestionSetEntity?

    @Query("SELECT COUNT(*) FROM question_sets")
    suspend fun count(): Int

    @Insert
    suspend fun insert(set: QuestionSetEntity): Long

    @Query("UPDATE question_sets SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE question_sets SET groupName = :groupName WHERE id = :id")
    suspend fun setGroup(id: Long, groupName: String?)

    @Query("UPDATE question_sets SET groupName = :newName WHERE groupName = :oldName")
    suspend fun renameGroup(oldName: String, newName: String)

    @Query("SELECT DISTINCT groupName FROM question_sets WHERE groupName IS NOT NULL")
    suspend fun allGroupNames(): List<String>

    @Query("SELECT COUNT(*) FROM question_sets WHERE groupName = :groupName")
    suspend fun countInGroup(groupName: String): Int

    @Query("DELETE FROM question_sets WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions ORDER BY setId ASC, position ASC")
    fun observeAll(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE setId = :setId ORDER BY position ASC")
    fun observeBySet(setId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE setId = :setId ORDER BY position ASC")
    suspend fun getBySet(setId: Long): List<QuestionEntity>

    @Insert
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE setId = :setId")
    suspend fun deleteBySet(setId: Long)
}

@Dao
interface FailedQuestionDao {

    @Query("SELECT * FROM failed_questions ORDER BY lastFailed DESC")
    fun observeAll(): Flow<List<FailedQuestionEntity>>

    @Query("SELECT * FROM failed_questions ORDER BY lastFailed DESC")
    suspend fun getAll(): List<FailedQuestionEntity>

    @Query("SELECT * FROM failed_questions WHERE setName = :setName ORDER BY lastFailed DESC")
    suspend fun getBySet(setName: String): List<FailedQuestionEntity>

    @Query("SELECT * FROM failed_questions WHERE setName = :setName AND questionId = :questionId LIMIT 1")
    suspend fun find(setName: String, questionId: String): FailedQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(question: FailedQuestionEntity)

    @Query("DELETE FROM failed_questions WHERE setName = :setName AND questionId = :questionId")
    suspend fun deleteOne(setName: String, questionId: String)

    @Query(
        "DELETE FROM failed_questions WHERE setName = :newName AND questionId IN " +
            "(SELECT questionId FROM failed_questions WHERE setName = :oldName)"
    )
    suspend fun dropRenameCollisions(oldName: String, newName: String)

    @Query("UPDATE failed_questions SET setName = :newName WHERE setName = :oldName")
    suspend fun renameSet(oldName: String, newName: String)

    @Query("DELETE FROM failed_questions")
    suspend fun clear()
}

@Dao
interface ExamDraftDao {

    @Query("SELECT * FROM exam_drafts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ExamDraftEntity>>

    @Query("SELECT * FROM exam_drafts WHERE setId = :setId AND mode = :mode LIMIT 1")
    suspend fun find(setId: Long, mode: String): ExamDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: ExamDraftEntity)

    @Query("DELETE FROM exam_drafts WHERE setId = :setId AND mode = :mode")
    suspend fun delete(setId: Long, mode: String)

    @Query("DELETE FROM exam_drafts WHERE setId = :setId")
    suspend fun deleteBySet(setId: Long)

    @Query("UPDATE exam_drafts SET setName = :name WHERE setId = :setId")
    suspend fun renameSet(setId: Long, name: String)

    @Query("DELETE FROM exam_drafts")
    suspend fun clear()
}

@Dao
interface QuizHistoryDao {

    @Query("SELECT * FROM quiz_history ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<QuizHistoryEntity>>

    @Insert
    suspend fun insert(session: QuizHistoryEntity)

    @Query("UPDATE quiz_history SET setName = :newName WHERE setName = :oldName")
    suspend fun renameSet(oldName: String, newName: String)

    @Query("DELETE FROM quiz_history")
    suspend fun clear()
}
