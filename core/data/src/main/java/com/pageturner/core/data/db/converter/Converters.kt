package com.pageturner.core.data.db.converter

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Room TypeConverters for List<String> fields stored as JSON. */
class Converters {

    @TypeConverter
    fun fromStringList(json: String): List<String> =
        runCatching { adapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())

    @TypeConverter
    fun toStringList(list: List<String>): String = adapter.toJson(list)

    companion object {
        private val moshi: Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        private val listType = Types.newParameterizedType(List::class.java, String::class.java)
        private val adapter: JsonAdapter<List<String>> = moshi.adapter(listType)

        /** Used by mappers outside of Room. */
        fun parseList(json: String): List<String> =
            runCatching { adapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())

        fun serializeList(list: List<String>): String = adapter.toJson(list)
    }
}
