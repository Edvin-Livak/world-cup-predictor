package com.sned.worldcuppredictor.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface FootballDataApi {
    @GET("competitions/WC/matches/")
    suspend fun getWorldCupMatches(
        @Header("X-Auth-Token") apiKey: String,
        @Query("season") season: Int = 2026
    ): FootballDataResponse
}

data class FootballDataResponse(
    val matches: List<FootballDataMatch>
)

data class FootballDataMatch(
    val id: Int,
    val utcDate: String,
    val status: String,
    val stage: String?,
    val group: String?,
    val homeTeam: FootballDataTeam,
    val awayTeam: FootballDataTeam,
    val score: FootballDataScore,
    val matchday: Int?
)

data class FootballDataTeam(
    val id: Int?,
    val name: String?,
    val shortName: String?,
    val tla: String?,
    val crest: String?
)

data class FootballDataScore(
    val fullTime: FootballDataFullTimeScore
)

data class FootballDataFullTimeScore(
    val home: Int?,
    val away: Int?
)