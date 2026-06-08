package com.sned.worldcuppredictor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabasePrediction(
    val id: String? = null,

    @SerialName("user_id")
    val userId: String,

    @SerialName("match_id")
    val matchId: Int,

    @SerialName("home_goals")
    val homeGoals: Int,

    @SerialName("away_goals")
    val awayGoals: Int,

    @SerialName("penalty_winner")
    val penaltyWinner: String? = null
)