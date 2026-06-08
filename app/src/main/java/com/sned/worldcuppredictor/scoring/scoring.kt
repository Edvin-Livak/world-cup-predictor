package com.sned.worldcuppredictor.scoring

import com.sned.worldcuppredictor.model.Match
import com.sned.worldcuppredictor.model.MatchStatus
import com.sned.worldcuppredictor.model.Prediction

fun calculatePoints(match: Match, prediction: Prediction): Int {
    if (match.status != MatchStatus.FINISHED) return 0

    val actualHome = match.actualHomeGoals ?: return 0
    val actualAway = match.actualAwayGoals ?: return 0

    val predictedOutcome = getOutcome(
        homeGoals = prediction.homeGoals,
        awayGoals = prediction.awayGoals
    )

    val actualOutcome = getOutcome(
        homeGoals = actualHome,
        awayGoals = actualAway
    )

    val exactScore =
        prediction.homeGoals == actualHome &&
                prediction.awayGoals == actualAway

    val wentToPenalties = match.penaltyWinner != null

    return if (wentToPenalties) {
        android.util.Log.d(
            "SCORING_TEST",
            "prediction pens=${prediction.penaltyWinner}, match pens=${match.penaltyWinner}"
        )
        var points = 0

        if (exactScore) {
            points += 3
        } else if (predictedOutcome == "DRAW" || predictedOutcome == match.penaltyWinner) {
            points += 1
        }

        if (prediction.penaltyWinner == match.penaltyWinner) {
            points += 1
        }

        points
    } else {
        var points = 0

        if (predictedOutcome == actualOutcome || prediction.penaltyWinner == actualOutcome) {
            points += 1
        }

        if (exactScore) {
            points += 3
        }

        points
    }
}

private fun getOutcome(
    homeGoals: Int,
    awayGoals: Int
): String {
    return when {
        homeGoals > awayGoals -> "HOME"
        homeGoals < awayGoals -> "AWAY"
        else -> "DRAW"
    }
}