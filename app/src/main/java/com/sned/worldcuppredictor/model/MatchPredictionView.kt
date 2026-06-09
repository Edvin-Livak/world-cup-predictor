package com.sned.worldcuppredictor.model

data class MatchPredictionView(
    val username: String,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val penaltyWinner: String? = null,
    val points: Int? = null
)