package com.pageturner.core.network.api

import com.pageturner.core.network.dto.openlib.SearchResponseDto
import com.pageturner.core.network.dto.openlib.WorkDetailDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit interface for the Open Library public API. No authentication required. */
interface OpenLibraryApiService {

    /**
     * Searches books by subject/genre.
     * Used to build the swipe queue from the user's genre preferences.
     */
    @GET("search.json")
    suspend fun searchBySubject(
        @Query("subject") subject: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,subject,number_of_pages_median"
    ): SearchResponseDto

    /**
     * Searches books by free-text query.
     * Used to fetch wildcard candidates from genres outside the user's preferences.
     */
    @GET("search.json")
    suspend fun searchByQuery(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,subject,number_of_pages_median"
    ): SearchResponseDto

    /**
     * Fetches the full work detail, including the raw description.
     * [workId] is the bare ID without the "/works/" prefix (e.g. "OL45804W").
     */
    @GET("works/{workId}.json")
    suspend fun getWorkDetail(@Path("workId") workId: String): WorkDetailDto
}
