package com.sned.worldcuppredictor.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sned.worldcuppredictor.model.Prediction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "world_cup_predictor")

class PredictionStorage(private val context: Context) {

    private val userNameKey = stringPreferencesKey("user_name")
    private val predictionsKey = stringPreferencesKey("predictions")

    val userNameFlow: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[userNameKey] ?: ""
        }

    val predictionsFlow: Flow<Map<Int, Prediction>> =
        context.dataStore.data.map { preferences ->
            val raw = preferences[predictionsKey] ?: ""
            decodePredictions(raw)
        }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[userNameKey] = name
        }
    }

    suspend fun savePredictions(predictions: Map<Int, Prediction>) {
        context.dataStore.edit { preferences ->
            preferences[predictionsKey] = encodePredictions(predictions)
        }
    }

    private fun encodePredictions(predictions: Map<Int, Prediction>): String {
        return predictions.values.joinToString(separator = "|") { prediction ->
            "${prediction.matchId},${prediction.homeGoals},${prediction.awayGoals}"
        }
    }

    private fun decodePredictions(raw: String): Map<Int, Prediction> {
        if (raw.isBlank()) return emptyMap()

        return raw.split("|")
            .mapNotNull { item ->
                val parts = item.split(",")
                if (parts.size != 3) return@mapNotNull null

                val matchId = parts[0].toIntOrNull()
                val homeGoals = parts[1].toIntOrNull()
                val awayGoals = parts[2].toIntOrNull()

                if (matchId == null || homeGoals == null || awayGoals == null) {
                    null
                } else {
                    matchId to Prediction(matchId, homeGoals, awayGoals)
                }
            }
            .toMap()
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}