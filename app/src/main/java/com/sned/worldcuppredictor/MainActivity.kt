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
import com.sned.worldcuppredictor.model.UserScore
import com.sned.worldcuppredictor.data.mockMatches
import com.sned.worldcuppredictor.model.MatchStatus
import com.sned.worldcuppredictor.scoring.calculatePoints
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.sned.worldcuppredictor.storage.PredictionStorage
import kotlinx.coroutines.launch
import com.sned.worldcuppredictor.api.MatchResultService

enum class AppTab {
    Predictions,
    Leaderboard,
    Profile
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = remember { PredictionStorage(context) }
    val scope = rememberCoroutineScope()

    val savedUserName by storage.userNameFlow.collectAsState(initial = "")
    val savedPredictions by storage.predictionsFlow.collectAsState(initial = emptyMap())

    var userNameInput by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(mockMatches) }
    val resultService = remember { MatchResultService() }

    LaunchedEffect(savedUserName) {
        if (userNameInput.isBlank()) {
            userNameInput = savedUserName
        }
    }

    if (savedUserName.isBlank()) {
        NameScreen(
            userName = userNameInput,
            onNameChange = { userNameInput = it },
            onJoin = {
                if (userNameInput.isNotBlank()) {
                    scope.launch {
                        storage.saveUserName(userNameInput)
                    }
                }
            }
        )
    } else {
        MainScreen(
            userName = savedUserName,
            matches = matches,
            predictions = savedPredictions,
            onPredictionChange = { prediction ->
                val updatedPredictions = savedPredictions + (prediction.matchId to prediction)

                scope.launch {
                    storage.savePredictions(updatedPredictions)
                }
            },
            onSimulateResultUpdate = {
                matches = resultService.fetchLatestMatches(matches)
            },
            onResetApp = {
                matches = mockMatches

                scope.launch {
                    storage.clearAll()
                }
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
    matches: List<Match>,
    predictions: Map<Int, Prediction>,
    onPredictionChange: (Prediction) -> Unit,
    onSimulateResultUpdate: () -> Unit,
    onResetApp: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AppTab.Predictions) }
    val currentUserPoints = matches.sumOf { match ->
        predictions[match.id]?.let { calculatePoints(match, it) } ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Cup Predictor") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.Predictions,
                    onClick = { selectedTab = AppTab.Predictions },
                    icon = { Text("⚽") },
                    label = { Text("Predictions") }
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.Leaderboard,
                    onClick = { selectedTab = AppTab.Leaderboard },
                    icon = { Text("🏆") },
                    label = { Text("Leaderboard") }
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.Profile,
                    onClick = { selectedTab = AppTab.Profile },
                    icon = { Text("👤") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.Predictions -> PredictionsScreen(
                modifier = Modifier.padding(padding),
                matches = matches,
                predictions = predictions,
                onPredictionChange = onPredictionChange,
                onSimulateResultUpdate = onSimulateResultUpdate
            )

            AppTab.Leaderboard -> LeaderboardScreen(
                modifier = Modifier.padding(padding),
                currentUserName = userName,
                currentUserPoints = currentUserPoints
            )

            AppTab.Profile -> ProfileScreen(
                modifier = Modifier.padding(padding),
                userName = userName,
                onResetApp = onResetApp
            )
        }
    }
}

@Composable
fun PredictionsScreen(
    modifier: Modifier = Modifier,
    matches: List<Match>,
    predictions: Map<Int, Prediction>,
    onPredictionChange: (Prediction) -> Unit,
    onSimulateResultUpdate: () -> Unit,

) {
    val totalPoints = matches.sumOf { match ->
        predictions[match.id]?.let { calculatePoints(match, it) } ?: 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your points", style = MaterialTheme.typography.titleMedium)
                Text("$totalPoints", style = MaterialTheme.typography.headlineLarge)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSimulateResultUpdate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate result update")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(matches) { match ->
                MatchCard(
                    match = match,
                    prediction = predictions[match.id],
                    onPredictionChange = onPredictionChange
                )
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    currentUserName: String,
    currentUserPoints: Int
) {
    val users = listOf(
        UserScore(currentUserName, currentUserPoints),
        UserScore("Dad", 6),
        UserScore("Mom", 4),
        UserScore("Brother", 2)
    ).sortedByDescending { it.points }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        users.forEachIndexed { index, user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${index + 1}. ${user.name}")
                    Text("${user.points} pts")
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    userName: String,
    onResetApp: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Name: $userName")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onResetApp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset local app data")
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    prediction: Prediction?,
    onPredictionChange: (Prediction) -> Unit
) {
    val isLocked = match.status != MatchStatus.SCHEDULED
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
            Text("Kickoff: ${match.kickoffTime}")
            Text("Status: ${match.status}")

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    enabled = !isLocked,
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
                    enabled = !isLocked,
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

            when (match.status) {
                MatchStatus.SCHEDULED -> {
                    Text("Prediction open")
                }

                MatchStatus.LIVE -> {
                    Text("Prediction locked — match has started")
                }

                MatchStatus.FINISHED -> {
                    Text("Result: ${match.actualHomeGoals}-${match.actualAwayGoals}")
                    Text("Points: $points")
                }
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