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
                    matchday = item.matchday
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

    private fun mapStatus(status: String): MatchStatus {
        return when (status) {
            "FT", "AET", "PEN" -> MatchStatus.FINISHED
            "1H", "HT", "2H", "ET", "BT", "P", "LIVE" -> MatchStatus.LIVE
            else -> MatchStatus.SCHEDULED
        }
    }
}