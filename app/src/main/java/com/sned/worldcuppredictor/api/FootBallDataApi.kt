package com.sned.worldcuppredictor.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface FootballDataApi {
    @GET("competitions/{competitionCode}/matches")
    suspend fun getMatches(
        @Path("competitionCode") competitionCode: String,
        @Header("X-Auth-Token") apiKey: String
    ): FootballDataResponse
}

data class FootballDataResponse(
    val matches: List<FootballDataMatch>
)

data class FootballDataMatch(
    val id: Int,
    val utcDate: String,
    val status: String,
    val homeTeam: FootballDataTeam,
    val awayTeam: FootballDataTeam,
    val score: FootballDataScore
)

data class FootballDataTeam(
    val name: String,
    val tla: String?
)

data class FootballDataScore(
    val fullTime: FootballDataFullTimeScore
)

data class FootballDataFullTimeScore(
    val home: Int?,
    val away: Int?
)