package com.sned.worldcuppredictor.api

import com.sned.worldcuppredictor.model.Match
import com.sned.worldcuppredictor.model.MatchStatus

class MatchResultService {

    suspend fun fetchLatestMatches(
        currentMatches: List<Match>,
        apiKey: String
    ): List<Match> {
        return try {
            android.util.Log.d("API_TEST", "fetchLatestMatches called")

            val response = ApiFootballClient.api.getWorldCupFixtures(apiKey.trim())

            android.util.Log.d("API_TEST", "API returned ${response.response.size} fixtures")

            currentMatches.map { localMatch ->
                val apiMatch = response.response.find { remote ->
                    remote.teams.home.name.contains(localMatch.homeTeam, ignoreCase = true) &&
                            remote.teams.away.name.contains(localMatch.awayTeam, ignoreCase = true)
                }

                if (apiMatch == null) {
                    localMatch
                } else {
                    localMatch.copy(
                        status = mapStatus(apiMatch.fixture.status.short),
                        actualHomeGoals = apiMatch.goals.home,
                        actualAwayGoals = apiMatch.goals.away
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("API_TEST", "API request failed", e)
            currentMatches
        }
    }

    private fun mapStatus(status: String): MatchStatus {
        return when (status) {
            "FT", "AET", "PEN" -> MatchStatus.FINISHED
            "1H", "HT", "2H", "ET", "BT", "P", "LIVE" -> MatchStatus.LIVE
            else -> MatchStatus.SCHEDULED
        }
    }

    suspend fun fetchMatchesFromApi(apiKey: String): List<Match> {
        val response = ApiFootballClient.api.getWorldCupFixtures(apiKey.trim())

        return response.response.map { item ->
            Match(
                id = item.fixture.id,
                group = "World Cup",
                homeTeam = item.teams.home.name,
                awayTeam = item.teams.away.name,
                homeFlag = "🏳️",
                awayFlag = "🏳️",
                kickoffTime = item.fixture.date,
                status = mapStatus(item.fixture.status.short),
                actualHomeGoals = item.goals.home,
                actualAwayGoals = item.goals.away
            )
        }
    }
}