package com.sned.worldcuppredictor.api

import com.sned.worldcuppredictor.model.Match
import com.sned.worldcuppredictor.model.MatchStatus

class MatchResultService {

    suspend fun fetchMatchesFromApi(apiKey: String): List<Match> {
        android.util.Log.d("API_TEST", "fetchMatchesFromApi started")
        android.util.Log.d("API_TEST", "API key length: ${apiKey.length}")

        val response = FootballDataClient.api.getWorldCupMatches(
            apiKey = apiKey.trim()
        )

        android.util.Log.d("API_TEST", "API returned ${response.matches.size} matches")

        return response.matches
            .filter { item ->
                item.homeTeam.name != null && item.awayTeam.name != null
            }
            .map { item ->

                android.util.Log.d(
                "MATCH_STATUS_TEST",
                "${item.homeTeam.name} vs ${item.awayTeam.name} -> ${item.status}"
            )

                android.util.Log.d(
                    "SCORE_TEST",
                    "${item.homeTeam.name} vs ${item.awayTeam.name} " +
                            "status=${item.status}, " +
                            "fullTime=${item.score.fullTime.home}-${item.score.fullTime.away}, "
                )
                Match(
                    id = item.id,
                    group = item.group ?: item.stage ?: "World Cup",
                    homeTeam = item.homeTeam.name!!,
                    awayTeam = item.awayTeam.name!!,
                    homeFlag = "🏳️",
                    awayFlag = "🏳️",
                    kickoffTime = item.utcDate,
                    status = mapFootballDataStatus(item.status),
                    actualHomeGoals = item.score.fullTime.home,
                    actualAwayGoals = item.score.fullTime.away,
                    homeLogoUrl = item.homeTeam.crest,
                    awayLogoUrl = item.awayTeam.crest,
                    venue = null,
                    matchday = item.matchday,
                    homeTla = item.homeTeam.tla,
                    awayTla = item.awayTeam.tla,
                    stage = item.stage


                )
            }
    }

    private fun mapFootballDataStatus(status: String): MatchStatus {
        return when (status) {
            "FINISHED" -> MatchStatus.FINISHED
            "LIVE", "IN_PLAY", "PAUSED" -> MatchStatus.LIVE
            else -> MatchStatus.SCHEDULED
        }
    }
}