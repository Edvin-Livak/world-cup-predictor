package com.sned.worldcuppredictor.repository

import com.sned.worldcuppredictor.api.SupabaseClient
import com.sned.worldcuppredictor.model.Prediction
import com.sned.worldcuppredictor.model.SupabasePrediction
import io.github.jan.supabase.postgrest.from

class PredictionRepository {

    suspend fun savePrediction(
        userId: String,
        prediction: Prediction
    ) {
        val existingPrediction = SupabaseClient.client
            .from("predictions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("match_id", prediction.matchId)
                }
            }
            .decodeList<SupabasePrediction>()
            .firstOrNull()

        if (existingPrediction == null) {
            val onlinePrediction = SupabasePrediction(
                userId = userId,
                matchId = prediction.matchId,
                homeGoals = prediction.homeGoals,
                awayGoals = prediction.awayGoals,
                penaltyWinner = prediction.penaltyWinner
            )

            SupabaseClient.client
                .from("predictions")
                .insert(onlinePrediction)
        } else {
            SupabaseClient.client
                .from("predictions")
                .update(
                    {
                        set("home_goals", prediction.homeGoals)
                        set("away_goals", prediction.awayGoals)
                        set("penalty_winner", prediction.penaltyWinner)
                    }
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("match_id", prediction.matchId)
                    }
                }
        }
    }

    suspend fun getAllPredictions(): List<SupabasePrediction> {
        return SupabaseClient.client
            .from("predictions")
            .select()
            .decodeList<SupabasePrediction>()
    }

    suspend fun getPredictionsForUser(userId: String): List<SupabasePrediction> {
        return SupabaseClient.client
            .from("predictions")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<SupabasePrediction>()
    }
}