package com.pageturner.core.ai.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExtractJsonTest {

    @Nested
    inner class `when input is plain JSON` {

        @Test
        fun `it is returned unchanged (trimmed)`() {
            val json = """{"key": "value"}"""
            assertEquals(json, extractJson(json))
        }
    }

    @Nested
    inner class `when input is wrapped in json code fences` {

        @Test
        fun `fences are stripped and JSON is returned`() {
            val input = "```json\n{\"key\": \"value\"}\n```"
            assertEquals("""{"key": "value"}""", extractJson(input))
        }
    }

    @Nested
    inner class `when input is wrapped in plain code fences` {

        @Test
        fun `fences are stripped and JSON is returned`() {
            val input = "```\n{\"key\": \"value\"}\n```"
            assertEquals("""{"key": "value"}""", extractJson(input))
        }
    }

    @Nested
    inner class `when input is empty` {

        @Test
        fun `empty string is returned`() {
            assertEquals("", extractJson(""))
        }
    }

    @Nested
    inner class `when fenced content has extra whitespace` {

        @Test
        fun `JSON is trimmed correctly`() {
            val input = "```json\n\n  {\"key\": \"value\"}  \n\n```"
            assertEquals("""{"key": "value"}""", extractJson(input))
        }
    }
}
