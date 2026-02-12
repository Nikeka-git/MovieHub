package com.moviehub.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moviehub.domain.model.Cast
import com.moviehub.domain.model.Genre

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromGenreList(value: List<Genre>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toGenreList(value: String): List<Genre> {
        val listType = object : TypeToken<List<Genre>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromCastList(value: List<Cast>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCastList(value: String): List<Cast> {
        val listType = object : TypeToken<List<Cast>>() {}.type
        return gson.fromJson(value, listType)
    }
}