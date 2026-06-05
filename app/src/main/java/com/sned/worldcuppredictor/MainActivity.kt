package com.sned.worldcuppredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sned.worldcuppredictor.ui.theme.WorldCupPredictorTheme
import com.sned.worldcuppredictor.model.Match
import com.sned.worldcuppredictor.model.Prediction
import com.sned.worldcuppredictor.data.mockMatches

fun calculatePoints(match: Match, prediction: Prediction): Int {
    if (match.actualHomeGoals == null || match.actualAwayGoals == null) return 0

    val predictedWinner = prediction.homeGoals.compareTo(prediction.awayGoals)
    val actualWinner = match.actualHomeGoals.compareTo(match.actualAwayGoals)

    var points = 0

    if (predictedWinner == actualWinner) points += 1

    if (
        prediction.homeGoals == match.actualHomeGoals &&
        prediction.awayGoals == match.actualAwayGoals
    ) {
        points += 3
    }

    return points
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorldCupPredictorTheme {
                WorldCupPredictorApp()
            }
        }
    }
}

@Composable
fun WorldCupPredictorApp() {
    var userName by remember { mutableStateOf("") }
    var hasJoined by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<Map<Int, Prediction>>(emptyMap()) }

    if (!hasJoined) {
        NameScreen(
            userName = userName,
            onNameChange = { userName = it },
            onJoin = {
                if (userName.isNotBlank()) {
                    hasJoined = true
                }
            }
        )
    } else {
        MainScreen(
            userName = userName,
            predictions = predictions,
            onPredictionChange = { prediction ->
                predictions = predictions + (prediction.matchId to prediction)
            }
        )
    }
}

@Composable
fun NameScreen(
    userName: String,
    onNameChange: (String) -> Unit,
    onJoin: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "World Cup Predictor",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start predicting")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userName: String,
    predictions: Map<Int, Prediction>,
    onPredictionChange: (Prediction) -> Unit
) {
    val totalPoints = mockMatches.sumOf { match ->
        predictions[match.id]?.let { calculatePoints(match, it) } ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hi, $userName") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your points", style = MaterialTheme.typography.titleMedium)
                    Text("$totalPoints", style = MaterialTheme.typography.headlineLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mockMatches) { match ->
                    MatchCard(
                        match = match,
                        prediction = predictions[match.id],
                        onPredictionChange = onPredictionChange
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    prediction: Prediction?,
    onPredictionChange: (Prediction) -> Unit
) {
    var homeGoalsText by remember(match.id) {
        mutableStateOf(prediction?.homeGoals?.toString() ?: "")
    }

    var awayGoalsText by remember(match.id) {
        mutableStateOf(prediction?.awayGoals?.toString() ?: "")
    }

    val points = prediction?.let { calculatePoints(match, it) } ?: 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(match.group, style = MaterialTheme.typography.labelLarge)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${match.homeFlag} ${match.homeTeam}")
                Text("vs")
                Text("${match.awayFlag} ${match.awayTeam}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = homeGoalsText,
                    onValueChange = {
                        homeGoalsText = it.filter { char -> char.isDigit() }
                        savePrediction(match.id, homeGoalsText, awayGoalsText, onPredictionChange)
                    },
                    label = { Text(match.homeTeam) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = awayGoalsText,
                    onValueChange = {
                        awayGoalsText = it.filter { char -> char.isDigit() }
                        savePrediction(match.id, homeGoalsText, awayGoalsText, onPredictionChange)
                    },
                    label = { Text(match.awayTeam) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (match.actualHomeGoals != null && match.actualAwayGoals != null) {
                Text("Result: ${match.actualHomeGoals}-${match.actualAwayGoals}")
                Text("Points: $points")
            } else {
                Text("Result not available yet")
            }
        }
    }
}

fun savePrediction(
    matchId: Int,
    homeGoalsText: String,
    awayGoalsText: String,
    onPredictionChange: (Prediction) -> Unit
) {
    val homeGoals = homeGoalsText.toIntOrNull()
    val awayGoals = awayGoalsText.toIntOrNull()

    if (homeGoals != null && awayGoals != null) {
        onPredictionChange(
            Prediction(
                matchId = matchId,
                homeGoals = homeGoals,
                awayGoals = awayGoals
            )
        )
    }
}