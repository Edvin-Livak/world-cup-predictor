package com.sned.worldcuppredictor.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiFootballApi {
    @GET("competitions/WC/matches/")
    suspend fun getWorldCupFixtures(
        @Header("X-Auth-Token") apiKey: String,
        @Query("season") season: Int = 2026
    ): ApiFootballResponse
}

data class ApiFootballResponse(
    val response: List<ApiFootballFixtureItem>
)

data class ApiFootballFixtureItem(
    val fixture: ApiFootballFixture,
    val league: ApiFootballLeague,
    val teams: ApiFootballTeams,
    val goals: ApiFootballGoals
)

data class ApiFootballFixture(
    val id: Int,
    val date: String,
    val status: ApiFootballStatus,
    val venue: ApiFootballVenue?
)

data class ApiFootballVenue(
    val name: String?,
    val city: String?
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

data class ApiFootballLeague(
    val round: String?
)