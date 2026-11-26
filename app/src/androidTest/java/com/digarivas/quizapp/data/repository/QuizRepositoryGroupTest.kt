package com.digarivas.quizapp.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.digarivas.quizapp.data.db.AppDatabase
import com.digarivas.quizapp.domain.ParsedQuestion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizRepositoryGroupTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: QuizRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = QuizRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun droppingOnUngroupedSetGroupsBothSets() = runBlocking {
        val first = repository.importSet("Primero", listOf(question("1")))
        val second = repository.importSet("Segundo", listOf(question("1")))

        val result = repository.groupSetsTogether(first.id, second.id, "Grupo 1")

        assertEquals("Grupo 1", result?.groupName)
        assertEquals(2, result?.setCount)
        assertEquals("Grupo 1", db.questionSetDao().findById(first.id)?.groupName)
        assertEquals("Grupo 1", db.questionSetDao().findById(second.id)?.groupName)
    }

    @Test
    fun droppingUngroupedSetOnGroupedSetJoinsTargetGroup() = runBlocking {
        val grouped = repository.importSet("Agrupado", listOf(question("1")))
        val loose = repository.importSet("Suelto", listOf(question("1")))
        repository.moveToGroup(listOf(grouped.id), "Curso")

        val result = repository.groupSetsTogether(loose.id, grouped.id, "Grupo 1")

        assertEquals("Curso", result?.groupName)
        assertEquals(2, result?.setCount)
        assertEquals("Curso", db.questionSetDao().findById(loose.id)?.groupName)
    }

    @Test
    fun droppingSetFromAnotherGroupMergesBothGroups() = runBlocking {
        val source = repository.importSet("Origen", listOf(question("1")))
        val target = repository.importSet("Destino", listOf(question("1")))
        val targetSibling = repository.importSet("Hermano", listOf(question("1")))
        repository.moveToGroup(listOf(source.id), "Origen")
        repository.moveToGroup(listOf(target.id, targetSibling.id), "Destino")

        val result = repository.groupSetsTogether(source.id, target.id, "Grupo 1")

        assertEquals("Origen", result?.groupName)
        assertEquals(3, result?.setCount)
        assertTrue(result?.merged == true)
        assertEquals("Origen", db.questionSetDao().findById(target.id)?.groupName)
        assertEquals("Origen", db.questionSetDao().findById(targetSibling.id)?.groupName)
    }

    @Test
    fun droppingSetsFromSameGroupDoesNothing() = runBlocking {
        val first = repository.importSet("Primero", listOf(question("1")))
        val second = repository.importSet("Segundo", listOf(question("1")))
        repository.moveToGroup(listOf(first.id, second.id), "Curso")

        val result = repository.groupSetsTogether(first.id, second.id, "Grupo 1")

        assertTrue(result?.alreadyGrouped == true)
        assertEquals("Curso", db.questionSetDao().findById(first.id)?.groupName)
    }

    @Test
    fun renamingGroupToExistingNameMergesThem() = runBlocking {
        val first = repository.importSet("Primero", listOf(question("1")))
        val second = repository.importSet("Segundo", listOf(question("1")))
        repository.moveToGroup(listOf(first.id), "Uno")
        repository.moveToGroup(listOf(second.id), "Dos")

        val result = repository.renameGroup("Dos", "UNO")

        assertEquals("Uno", result?.name)
        assertTrue(result?.merged == true)
        assertEquals("Uno", db.questionSetDao().findById(second.id)?.groupName)
    }

    @Test
    fun moveToGroupReusesExistingNameIgnoringCase() = runBlocking {
        val first = repository.importSet("Primero", listOf(question("1")))
        val second = repository.importSet("Segundo", listOf(question("1")))
        repository.moveToGroup(listOf(first.id), "Curso")

        repository.moveToGroup(listOf(second.id), "curso")

        assertEquals("Curso", db.questionSetDao().findById(second.id)?.groupName)
    }

    @Test
    fun renameGroupKeepsCaseOnlyRename() = runBlocking {
        val set = repository.importSet("Primero", listOf(question("1")))
        repository.moveToGroup(listOf(set.id), "curso")

        val result = repository.renameGroup("curso", "Curso")

        assertEquals("Curso", result?.name)
        assertEquals(false, result?.merged)
        assertEquals("Curso", db.questionSetDao().findById(set.id)?.groupName)
    }

    @Test
    fun movingToEmptyGroupRemovesGroup() = runBlocking {
        val set = repository.importSet("Primero", listOf(question("1")))
        repository.moveToGroup(listOf(set.id), "Curso")

        repository.moveToGroup(listOf(set.id), null)

        assertNull(db.questionSetDao().findById(set.id)?.groupName)
    }

    private fun question(id: String) = ParsedQuestion(
        externalId = id,
        text = "Pregunta $id",
        options = listOf("A", "B"),
        correctAnswer = "A",
        explanation = "",
        correctIndices = setOf(0),
        isMulti = false
    )
}
