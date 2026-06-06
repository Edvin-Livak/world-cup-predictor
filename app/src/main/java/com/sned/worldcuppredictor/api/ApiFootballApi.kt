package com.sned.worldcuppredictor.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiFootballApi {
    @GET("fixtures")
    suspend fun getWorldCupFixtures(
        @Header("x-apisports-key") apiKey: String,
        @Query("league") league: Int = 39,
        @Query("season") season: Int = 2023
    ): ApiFootballResponse
}

data class ApiFootballResponse(
    val response: List<ApiFootballFixtureItem>
)

data class ApiFootballFixtureItem(
    val fixture: ApiFootballFixture,
    val teams: ApiFootballTeams,
    val goals: ApiFootballGoals
)

data class ApiFootballFixture(
    val id: Int,
    val date: String,
    val status: ApiFootballStatus
)

data class ApiFootballStatus(
    val short: String
)

data class ApiFootballTeams(
    val home: ApiFootballTeam,
    val away: ApiFootballTeam
)

data class ApiFootballTeam(
    val name: String,
    val logo: String?
)

data class ApiFootballGoals(
    val home: Int?,
    val away: Int?
)