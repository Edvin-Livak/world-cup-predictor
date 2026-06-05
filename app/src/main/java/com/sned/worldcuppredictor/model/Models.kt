package com.sned.worldcuppredictor.model

data class Match(
    val id: Int,
    val group: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeFlag: String,
    val awayFlag: String,
    val actualHomeGoals: Int? = null,
    val actualAwayGoals: Int? = null
)

data class Prediction(
    val matchId: Int,
    val homeGoals: Int,
    val awayGoals: Int
)