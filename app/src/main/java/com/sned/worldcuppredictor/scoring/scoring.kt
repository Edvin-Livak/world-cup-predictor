package com.sned.worldcuppredictor.scoring

import com.sned.worldcuppredictor.model.Match
import com.sned.worldcuppredictor.model.MatchStatus
import com.sned.worldcuppredictor.model.Prediction

fun calculatePoints(match: Match, prediction: Prediction): Int {
    if (match.status != MatchStatus.FINISHED) return 0
    if (match.actualHomeGoals == null || match.actualAwayGoals == null) return 0

    val predictedWinner = prediction.homeGoals.compareTo(prediction.awayGoals)
    val actualWinner = match.actualHomeGoals.compareTo(match.actualAwayGoals)

    var points = 0

    if (predictedWinner == actualWinner) {
        points += 1
    }

    if (
        prediction.homeGoals == match.actualHomeGoals &&
        prediction.awayGoals == match.actualAwayGoals
    ) {
        points += 3
    }

    return points
}