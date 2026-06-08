package com.sned.worldcuppredictor.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sned.worldcuppredictor.model.Prediction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sned.worldcuppredictor.model.Match

private val Context.dataStore by preferencesDataStore(name = "world_cup_predictor")

class PredictionStorage(private val context: Context) {

    private val userNameKey = stringPreferencesKey("user_name")
    private val predictionsKey = stringPreferencesKey("predictions")

    private val MATCHES_KEY = stringPreferencesKey("matches")
    private val USER_ID_KEY = stringPreferencesKey("user_id")


    val userNameFlow: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[userNameKey] ?: ""
        }

    val predictionsFlow: Flow<Map<Int, Prediction>> =
        context.dataStore.data.map { preferences ->
            val raw = preferences[predictionsKey] ?: ""
            decodePredictions(raw)
        }

    val matchesFlow = context.dataStore.data.map { preferences ->
        val json = preferences[MATCHES_KEY] ?: return@map emptyList<Match>()

        val type = object : TypeToken<List<Match>>() {}.type
        Gson().fromJson<List<Match>>(json, type)
    }

    val userIdFlow = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
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
            listOf(
                prediction.matchId,
                prediction.homeGoals,
                prediction.awayGoals,
                prediction.penaltyWinner ?: ""
            ).joinToString(",")
        }
    }

    private fun decodePredictions(raw: String): Map<Int, Prediction> {
        if (raw.isBlank()) return emptyMap()

        return raw.split("|")
            .mapNotNull { item ->
                val parts = item.split(",")

                if (parts.size < 3) return@mapNotNull null

                val matchId = parts[0].toIntOrNull()
                val homeGoals = parts[1].toIntOrNull()
                val awayGoals = parts[2].toIntOrNull()
                val penaltyWinner = parts.getOrNull(3)?.ifBlank { null }

                if (matchId == null || homeGoals == null || awayGoals == null) {
                    null
                } else {
                    matchId to Prediction(
                        matchId = matchId,
                        homeGoals = homeGoals,
                        awayGoals = awayGoals,
                        penaltyWinner = penaltyWinner
                    )
                }
            }
            .toMap()
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.remove(userNameKey)
            preferences.remove(USER_ID_KEY)
            preferences.remove(predictionsKey)
        }
    }

    suspend fun saveMatches(matches: List<Match>) {
        context.dataStore.edit { preferences ->
            preferences[MATCHES_KEY] = Gson().toJson(matches)
        }
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }
}