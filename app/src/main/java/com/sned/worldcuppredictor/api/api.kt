package com.sned.worldcuppredictor.api

import com.sned.worldcuppredictor.model.Match
import com.sned.worldcuppredictor.model.MatchStatus

class MatchResultService {

    fun fetchLatestMatches(currentMatches: List<Match>): List<Match> {
        return currentMatches.map { match ->
            when (match.id) {
                1 -> match.copy(
                    status = MatchStatus.FINISHED,
                    actualHomeGoals = 2,
                    actualAwayGoals = 1
                )

                2 -> match.copy(
                    status = MatchStatus.FINISHED,
                    actualHomeGoals = 0,
                    actualAwayGoals = 0
                )

                else -> match
            }
        }
    }
}