package com.pageturner.core.network.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Handles the Open Library `description` field which is either:
 * - A plain string: "A novel about redemption..."
 * - An object:      {"type": "/type/text", "value": "A novel about redemption..."}
 *
 * Both forms are normalised to a plain String (or null).
 */
class WorkDescriptionAdapter {

    @FromJson
    fun fromJson(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.BEGIN_OBJECT -> {
                var value: String? = null
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "value" -> value = reader.nextString()
                        else    -> reader.skipValue()
                    }
                }
                reader.endObject()
                value
            }
            JsonReader.Token.NULL -> { reader.nextNull<Unit>(); null }
            else -> { reader.skipValue(); null }
        }
    }

    @ToJson
    @Suppress("UNUSED_PARAMETER")
    fun toJson(writer: JsonWriter, value: String?) {
        writer.value(value)
    }
}
