package com.pageturner.core.domain.util

import com.pageturner.core.domain.error.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ResultExtensionsTest {

    @Nested
    inner class `getOrNull` {

        @Test
        fun `returns data when Result is Success`() {
            val result: Result<String> = Result.Success("hello")
            assertEquals("hello", result.getOrNull())
        }

        @Test
        fun `returns null when Result is Failure`() {
            val result: Result<String> = Result.Failure(AppError.UnknownError("oops"))
            assertNull(result.getOrNull())
        }
    }

    @Nested
    inner class `errorOrNull` {

        @Test
        fun `returns the error when Result is Failure`() {
            val error = AppError.UnknownError("something went wrong")
            val result: Result<String> = Result.Failure(error)
            assertEquals(error, result.errorOrNull())
        }

        @Test
        fun `returns null when Result is Success`() {
            val result: Result<String> = Result.Success("data")
            assertNull(result.errorOrNull())
        }
    }

    @Nested
    inner class `map` {

        @Test
        fun `transforms data when Result is Success`() {
            val result: Result<Int> = Result.Success(5)
            assertEquals(Result.Success(10), result.map { it * 2 })
        }

        @Test
        fun `passes Failure through unchanged`() {
            val error = AppError.UnknownError("error")
            val result: Result<Int> = Result.Failure(error)
            assertEquals(result, result.map { it * 2 })
        }
    }
}
