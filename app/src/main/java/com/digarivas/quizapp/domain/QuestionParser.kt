package com.digarivas.quizapp.domain

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Parser del JSON de preguntas. Acepta el mismo formato que la versión web:
 *
 * [
 *   { "id": 1, "question": "...", "options": ["A. ...", "B. ..."],
 *     "correct_answer": "A. ...", "explanation": "opcional" }
 * ]
 *
 * `correct_answer` puede ser el texto exacto de una opción, una letra ("A"),
 * o varias letras ("A, C" / "A and C") para multirrespuesta.
 */
object QuestionParser {

    /** Límite alineado con el aviso de la pantalla de importación. */
    const val MAX_JSON_CHARS = 10 * 1024 * 1024

    private val letterRegex = Regex("\\b([A-Ha-h])\\b")
    private val example by lazy { buildExampleJson() }

    fun parse(raw: String): ParseOutcome {
        if (raw.length > MAX_JSON_CHARS) {
            throw QuestionParseException("El JSON supera el máximo de 10 MB")
        }

        // Algunos editores de Windows guardan el archivo con BOM UTF-8.
        val clean = raw.removePrefix("\uFEFF")
        val root = try {
            JsonParser.parseString(clean)
        } catch (e: Exception) {
            throw QuestionParseException("El JSON no es válido")
        }

        if (!root.isJsonArray) {
            throw QuestionParseException("El JSON debe ser un array de preguntas")
        }

        val array = root.asJsonArray
        if (array.size() == 0) {
            throw QuestionParseException("El array de preguntas está vacío")
        }

        val questions = mutableListOf<ParsedQuestion>()
        val errors = mutableListOf<String>()
        val usedIds = mutableSetOf<String>()

        array.forEachIndexed { index, element ->
            try {
                val parsed = parseQuestion(element, index, usedIds)
                questions += parsed
            } catch (e: Exception) {
                errors += "Pregunta ${index + 1}: ${e.message ?: "formato incorrecto"}"
            }
        }

        return ParseOutcome(questions = questions, errors = errors)
    }

    private fun parseQuestion(element: com.google.gson.JsonElement, index: Int, usedIds: MutableSet<String>): ParsedQuestion {
        if (!element.isJsonObject) {
            throw IllegalArgumentException("debe ser un objeto")
        }
        val obj = element.asJsonObject

        val text = obj.stringOrNull("question")?.trim().orEmpty()
        if (text.isEmpty()) throw IllegalArgumentException("falta el campo question")

        val optionsElement = obj.get("options")
        if (optionsElement == null || !optionsElement.isJsonArray) {
            throw IllegalArgumentException("options debe ser un array de textos")
        }
        val optionsArray = optionsElement.asJsonArray
        val options = (0 until optionsArray.size()).map { optionIndex ->
            val option = optionsArray.get(optionIndex)
            if (!option.isJsonPrimitive || !option.asJsonPrimitive.isString) {
                throw IllegalArgumentException("la opción ${optionIndex + 1} debe ser texto")
            }
            option.asString.trim()
        }
        if (options.size < 2) {
            throw IllegalArgumentException("options necesita al menos 2 respuestas")
        }

        val correctAnswer = obj.stringOrNull("correct_answer")?.trim().orEmpty()
        if (correctAnswer.isEmpty()) throw IllegalArgumentException("falta el campo correct_answer")

        val correctIndices = resolveCorrectIndices(correctAnswer, options)
        if (correctIndices.isEmpty()) {
            throw IllegalArgumentException("correct_answer no coincide con ninguna opción")
        }

        val baseId = obj.stringOrNull("id")?.trim()?.takeIf { it.isNotEmpty() }
            ?: (index + 1).toString()
        var id = baseId
        var suffix = 2
        while (!usedIds.add(id)) {
            id = "$baseId#$suffix"
            suffix++
        }

        val explanation = obj.stringOrNull("explanation")?.trim().orEmpty()

        return ParsedQuestion(
            externalId = id,
            text = text,
            options = options,
            correctAnswer = correctAnswer,
            explanation = explanation,
            correctIndices = correctIndices,
            isMulti = correctIndices.size > 1
        )
    }

    /**
     * Devuelve los índices (base 0) de las opciones correctas.
     * El orden de las comprobaciones replica la app web:
     * texto exacto -> letra única -> letras sueltas -> coincidencia por texto.
     */
    fun resolveCorrectIndices(correctAnswer: String, options: List<String>): Set<Int> {
        val answer = correctAnswer.trim()
        if (answer.isEmpty()) return emptySet()

        val exact = options.indexOfFirst { it == answer }
        if (exact >= 0) return setOf(exact)

        if (answer.length == 1) {
            val letter = answer[0].uppercaseChar()
            if (letter in 'A'..'H') {
                val idx = letter - 'A'
                if (idx in options.indices) return setOf(idx)
            }
        }

        val letters = letterRegex.findAll(answer)
            .map { it.groupValues[1][0].uppercaseChar() - 'A' }
            .filter { it in options.indices }
            .toSet()
        if (letters.isNotEmpty()) return letters

        // Último recurso: si la respuesta contiene el texto de una opción.
        return options.indices.filter { idx ->
            options[idx].isNotBlank() && answer.contains(options[idx], ignoreCase = true)
        }.toSet()
    }

    fun labels(indices: Set<Int>): String {
        if (indices.isEmpty()) return "—"
        return indices.sorted().joinToString(", ") { index ->
            if (index in 0..25) ('A' + index).toString() else (index + 1).toString()
        }
    }

    fun exampleJson(): String = example

    private fun JsonObject.stringOrNull(key: String): String? {
        val value = get(key) ?: return null
        if (value.isJsonNull || !value.isJsonPrimitive) return null
        return runCatching { value.asString }.getOrNull()
    }

    private fun buildExampleJson(): String = """
[
  {
    "id": 1,
    "question": "¿Cuál es el animal terrestre más rápido?",
    "options": ["León", "Guepardo", "Tigre", "Canguro"],
    "correct_answer": "Guepardo",
    "explanation": "El guepardo alcanza hasta 120 km/h en distancias cortas."
  },
  {
    "id": 2,
    "question": "¿En qué ciudad se encuentra la Torre Eiffel?",
    "options": ["Londres", "Roma", "París", "Madrid"],
    "correct_answer": "París"
  },
  {
    "id": 3,
    "question": "¿Cuáles de los siguientes son planetas del sistema solar? (marca 2)",
    "options": ["Marte", "Plutón", "Saturno", "Halley"],
    "correct_answer": "A, C",
    "explanation": "Marte y Saturno son planetas. Plutón es un planeta enano y Halley es un cometa."
  }
]
""".trimIndent()
}
