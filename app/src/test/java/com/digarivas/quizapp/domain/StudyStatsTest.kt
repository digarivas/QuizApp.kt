package com.digarivas.quizapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyStatsTest {

    private fun set(
        id: Long,
        name: String,
        group: String? = null,
        questions: Int = 10
    ) = SetSummary(
        id = id,
        name = name,
        questionCount = questions,
        createdAt = id,
        groupName = group
    )

    private fun attempt(
        setName: String,
        date: Long,
        score: Int,
        total: Int
    ) = AttemptRecord(
        setName = setName,
        date = date,
        score = score,
        total = total,
        mode = "all"
    )

    @Test
    fun `agrupa por grupo y ordena los intentos por fecha`() {
        val sets = listOf(
            set(1, "A", "Curso"),
            set(2, "B", "Curso"),
            set(3, "C")
        )
        val history = listOf(
            attempt("A", 10, 5, 10),
            attempt("A", 30, 8, 10),
            attempt("B", 20, 10, 10),
            attempt("C", 40, 3, 3)
        )

        val stats = StudyStatsBuilder.build(sets, history)

        assertEquals(2, stats.groups.size)
        val course = stats.groups.first()
        assertEquals("Curso", course.name)
        assertEquals(listOf(50, 100, 80), course.attempts.map { it.percent })
        assertEquals(3, course.count)
        assertEquals(77, course.average)
        assertEquals(100, course.best)
        assertEquals(listOf("A", "B"), course.sets.map { it.name })

        val noGroup = stats.groups.last()
        assertNull(noGroup.name)
        assertEquals(listOf("C"), noGroup.sets.map { it.name })
    }

    @Test
    fun `ordena los examenes por ultimo intento y calcula sus metricas`() {
        val sets = listOf(
            set(1, "A", "Curso"),
            set(2, "B", "Curso"),
            set(3, "C")
        )
        val history = listOf(
            attempt("A", 10, 5, 10),
            attempt("A", 30, 8, 10),
            attempt("B", 20, 10, 10),
            attempt("C", 40, 3, 3),
            attempt("Borrado", 50, 1, 2)
        )

        val stats = StudyStatsBuilder.build(sets, history)

        assertEquals(
            listOf("Borrado", "C", "A", "B"),
            stats.exams.map { it.name }
        )

        val examA = stats.exams.first { it.name == "A" }
        assertEquals(2, examA.count)
        assertEquals(65, examA.average)
        assertEquals(80, examA.best)
        assertEquals(80, examA.last?.percent)

        val orphan = stats.exams.first { it.name == "Borrado" }
        assertTrue(orphan.isOrphan)
        assertNull(orphan.groupName)
        assertEquals(50, orphan.last?.percent)
    }

    @Test
    fun `sin historial los examenes siguen apareciendo sin intentos`() {
        val sets = listOf(set(1, "A", "Curso"), set(2, "B"))

        val stats = StudyStatsBuilder.build(sets, emptyList())

        assertEquals(2, stats.exams.size)
        assertTrue(stats.exams.all { it.attempts.isEmpty() })
        assertEquals(2, stats.groups.size)
        assertTrue(stats.groups.all { it.attempts.isEmpty() })
    }

    @Test
    fun `los totales cero no rompen el porcentaje`() {
        val stats = StudyStatsBuilder.build(
            sets = listOf(set(1, "A")),
            history = listOf(attempt("A", 1, 0, 0))
        )

        assertEquals(0, stats.exams.single().last?.percent)
    }
}
