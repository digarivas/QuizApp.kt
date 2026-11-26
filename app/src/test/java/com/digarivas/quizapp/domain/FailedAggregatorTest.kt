package com.digarivas.quizapp.domain

import com.digarivas.quizapp.data.db.FailedQuestionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FailedAggregatorTest {

    private fun failed(
        setName: String,
        questionId: String,
        question: String,
        correctAnswer: String = "A",
        failCount: Int = 1,
        lastFailed: Long = 1
    ) = FailedQuestionEntity(
        setName = setName,
        questionId = questionId,
        question = question,
        options = listOf("A", "B"),
        correctAnswer = correctAnswer,
        explanation = "",
        wrongAnswer = "B",
        failCount = failCount,
        lastFailed = lastFailed
    )

    private fun set(id: Long, name: String, group: String? = null) = SetSummary(
        id = id,
        name = name,
        questionCount = 2,
        createdAt = id,
        groupName = group
    )

    @Test
    fun `la misma pregunta en dos conjuntos se une y suma los fallos`() {
        val sets = listOf(
            set(1, "Tema 1", "Curso"),
            set(2, "Tema 2", "Curso")
        )
        val items = FailedAggregator.aggregate(
            failed = listOf(
                failed("Tema 1", "q1", "¿Capital de Francia?", failCount = 2, lastFailed = 10),
                failed("Tema 2", "q9", "¿Capital de Francia?", failCount = 1, lastFailed = 30)
            ),
            sets = sets
        )

        assertEquals(1, items.size)
        val item = items.single()
        assertEquals(3, item.failCount)
        assertEquals(listOf("Tema 1", "Tema 2"), item.setNames)
        assertEquals("Tema 2", item.primarySetName)
        assertEquals("Curso", item.groupName)
        assertEquals(2, item.refs.size)
    }

    @Test
    fun `mismo texto con respuesta distinta no se mezcla`() {
        val items = FailedAggregator.aggregate(
            failed = listOf(
                failed("Tema 1", "q1", "¿Cuál es?", correctAnswer = "A"),
                failed("Tema 1", "q2", "¿Cuál es?", correctAnswer = "B")
            ),
            sets = listOf(set(1, "Tema 1"))
        )

        assertEquals(2, items.size)
    }

    @Test
    fun `agrupa por grupo y por conjunto`() {
        val sets = listOf(
            set(1, "Tema 1", "Curso"),
            set(2, "Tema 2", "Curso"),
            set(3, "Suelto")
        )
        val items = FailedAggregator.aggregate(
            failed = listOf(
                failed("Tema 1", "q1", "Uno", failCount = 1),
                failed("Tema 2", "q2", "Dos", failCount = 5),
                failed("Suelto", "q3", "Tres", failCount = 2)
            ),
            sets = sets
        )
        val groups = FailedAggregator.group(items)

        assertEquals(2, groups.size)
        assertEquals("Curso", groups.first().name)
        assertEquals(listOf("Tema 1", "Tema 2"), groups.first().sets.map { it.setName })
        assertEquals(6, groups.first().failCount)
        assertNull(groups.last().name)
        assertEquals(1, groups.last().itemCount)
    }

    @Test
    fun `sin falladas devuelve una lista vacia`() {
        assertTrue(FailedAggregator.aggregate(emptyList(), emptyList()).isEmpty())
        assertTrue(FailedAggregator.group(emptyList()).isEmpty())
    }
}
