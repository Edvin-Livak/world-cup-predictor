package com.sned.worldcuppredictor.model

enum class MatchStatus {
    SCHEDULED,
    LIVE,
    FINISHED
}

data class Match(
    val id: Int,
    val group: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeFlag: String,
    val awayFlag: String,
    val kickoffTime: String,
    val status: MatchStatus,
    val actualHomeGoals: Int? = null,
    val actualAwayGoals: Int? = null,
    val homeLogoUrl: String? = null,
    val awayLogoUrl: String? = null
)

data class Prediction(
    val matchId: Int,
    val homeGoals: Int,
    val awayGoals: Int
)

data class UserScore(
    val name: String,
    val points: Int
)