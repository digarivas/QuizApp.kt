package com.digarivas.quizapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionParserTest {

    @Test
    fun `parse acepta respuesta por texto exacto`() {
        val outcome = QuestionParser.parse(
            """
            [
              {
                "id": 1,
                "question": "¿Cuál es la capital de Francia?",
                "options": ["Londres", "París", "Roma"],
                "correct_answer": "París"
              }
            ]
            """.trimIndent()
        )

        assertEquals(0, outcome.errors.size)
        assertEquals(1, outcome.questions.size)
        val question = outcome.questions.single()
        assertEquals(setOf(1), question.correctIndices)
        assertFalse(question.isMulti)
        assertEquals("1", question.externalId)
    }

    @Test
    fun `parse detecta respuestas múltiples por letras`() {
        val outcome = QuestionParser.parse(
            """
            [
              {
                "id": 3,
                "question": "¿Qué son planetas?",
                "options": ["Marte", "Plutón", "Saturno", "Halley"],
                "correct_answer": "A, C"
              }
            ]
            """.trimIndent()
        )

        val question = outcome.questions.single()
        assertEquals(setOf(0, 2), question.correctIndices)
        assertTrue(question.isMulti)
        assertEquals("A, C", question.correctAnswer)
    }

    @Test
    fun `parse ignora el BOM UTF-8 de archivos de Windows`() {
        val raw = "\uFEFF" + """
            [
              { "question": "Uno", "options": ["A", "B"], "correct_answer": "A" }
            ]
        """.trimIndent()

        val outcome = QuestionParser.parse(raw)

        assertEquals(0, outcome.errors.size)
        assertEquals(1, outcome.questions.size)
    }

    @Test
    fun `parse resuelve ids duplicados sin perder preguntas`() {
        val outcome = QuestionParser.parse(
            """
            [
              { "id": 1, "question": "Uno", "options": ["A", "B"], "correct_answer": "A" },
              { "id": 1, "question": "Dos", "options": ["A", "B"], "correct_answer": "B" }
            ]
            """.trimIndent()
        )

        assertEquals(listOf("1", "1#2"), outcome.questions.map { it.externalId })
    }

    @Test
    fun `parse acumula errores y conserva las preguntas válidas`() {
        val outcome = QuestionParser.parse(
            """
            [
              { "question": "Válida", "options": ["A", "B"], "correct_answer": "A" },
              { "question": "Sin opciones", "correct_answer": "A" },
              { "question": "Respuesta desconocida", "options": ["A", "B"], "correct_answer": "Z" }
            ]
            """.trimIndent()
        )

        assertEquals(1, outcome.questions.size)
        assertEquals(2, outcome.errors.size)
        assertTrue(outcome.errors.first().startsWith("Pregunta 2:"))
        assertTrue(outcome.errors.last().startsWith("Pregunta 3:"))
    }
}
