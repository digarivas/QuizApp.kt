package com.digarivas.quizapp.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.digarivas.quizapp.data.db.AppDatabase
import com.digarivas.quizapp.domain.DraftAnswer
import com.digarivas.quizapp.domain.ParsedQuestion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExamDraftRepositoryTest {

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
    fun guardarYRecuperarUnExamenEnCurso() = runBlocking {
        val set = repository.importSet("Tema", listOf(question("1"), question("2")))

        repository.saveDraft(
            setId = set.id,
            setName = set.name,
            mode = "all",
            order = "random",
            timeLimit = false,
            practice = true,
            answers = listOf(
                DraftAnswer("1", selected = listOf(0)),
                DraftAnswer("2", selected = listOf(1), checked = true, correct = false)
            ),
            current = 1,
            startedAt = 1000L
        )

        val draft = repository.getDraft(set.id, "all")
        assertNotNull(draft)
        assertEquals(2, draft?.total)
        assertEquals(2, draft?.answered)
        assertEquals(1, draft?.current)
        assertEquals(1000L, draft?.startedAt)

        val answers = repository.decodeDraftAnswers(draft!!)
        assertEquals(listOf("1", "2"), answers.map { it.externalId })
        assertEquals(listOf(1), answers[1].selected)
        assertEquals(false, answers[1].correct)

        val summary = repository.observeDrafts().first().single()
        assertEquals(set.id, summary.setId)
        assertEquals("Tema", summary.setName)
        assertEquals(2, summary.answered)
        assertEquals(2, summary.total)
    }

    @Test
    fun losDosModosDelMismoConjuntoConviven() = runBlocking {
        val set = repository.importSet("Tema", listOf(question("1")))

        repository.saveDraft(set.id, "Tema", "all", "random", false, true, emptyList(), 0, 1L)
        repository.saveDraft(set.id, "Tema", "failed", "sequential", false, true, emptyList(), 0, 1L)

        assertEquals(2, repository.observeDrafts().first().size)
        assertNotNull(repository.getDraft(set.id, "all"))
        assertNotNull(repository.getDraft(set.id, "failed"))
    }

    @Test
    fun renombrarElConjuntoActualizaElBorrador() = runBlocking {
        val set = repository.importSet("Viejo", listOf(question("1")))
        repository.saveDraft(set.id, "Viejo", "all", "random", false, true, emptyList(), 0, 1L)

        repository.renameSet(set.id, "Nuevo")

        assertEquals("Nuevo", repository.getDraft(set.id, "all")?.setName)
    }

    @Test
    fun borrarElConjuntoBorraSuBorrador() = runBlocking {
        val set = repository.importSet("Tema", listOf(question("1")))
        repository.saveDraft(set.id, "Tema", "all", "random", false, true, emptyList(), 0, 1L)

        repository.deleteSet(set.id)

        assertNull(repository.getDraft(set.id, "all"))
    }

    @Test
    fun guardarDeNuevoReemplazaElBorradorDelMismoModo() = runBlocking {
        val set = repository.importSet("Tema", listOf(question("1")))
        repository.saveDraft(set.id, "Tema", "all", "random", false, true, emptyList(), 0, 1L)

        repository.saveDraft(
            setId = set.id,
            setName = "Tema",
            mode = "all",
            order = "random",
            timeLimit = true,
            practice = false,
            answers = listOf(DraftAnswer("1", selected = listOf(0), checked = true, correct = true)),
            current = 0,
            startedAt = 5000L
        )

        val draft = repository.getDraft(set.id, "all")
        assertEquals(1, draft?.answered)
        assertEquals(true, draft?.timeLimit)
        assertEquals(false, draft?.practice)
        assertEquals(5000L, draft?.startedAt)
        assertEquals(1, repository.observeDrafts().first().size)
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
