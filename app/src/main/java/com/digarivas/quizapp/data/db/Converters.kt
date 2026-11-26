package com.digarivas.quizapp.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson

/**
 * Room guarda las listas de opciones como JSON. Así el esquema queda simple
 * y el JSON importado se conserva tal cual.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson(value, Array<String>::class.java)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
