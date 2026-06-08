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
import com.sned.worldcuppredictor.BuildConfig
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke

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
    val savedMatches by storage.matchesFlow.collectAsState(initial = emptyList())

    var userNameInput by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(savedMatches) }
    val resultService = remember { MatchResultService() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedUserName) {
        if (userNameInput.isBlank()) {
            userNameInput = savedUserName
        }
    }

    LaunchedEffect(savedMatches) {
        if (matches.isEmpty() && savedMatches.isNotEmpty()) {
            matches = savedMatches
        }
    }

    LaunchedEffect(savedUserName) {
        if (savedUserName.isNotBlank() && matches.isEmpty()) {
            isLoading = true
            errorMessage = null

            try {
                val fetchedMatches = resultService.fetchMatchesFromApi(
                    apiKey = BuildConfig.API_FOOTBALL_KEY
                )

                matches = fetchedMatches
                storage.saveMatches(fetchedMatches)
            } catch (e: Exception) {
                android.util.Log.e("API_TEST", "Auto fetch failed", e)
                errorMessage = "Could not load matches: ${e.message}"
            } finally {
                isLoading = false
            }
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
            isLoading = isLoading,
            errorMessage = errorMessage,
            onPredictionChange = { prediction ->
                val updatedPredictions = savedPredictions + (prediction.matchId to prediction)

                scope.launch {
                    storage.savePredictions(updatedPredictions)
                }
            },
            onSimulateResultUpdate = {
                Toast.makeText(context, "Checking latest results...", Toast.LENGTH_SHORT).show()
                android.util.Log.d("API_TEST", "Button clicked")

                scope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        android.util.Log.d("API_TEST", "Calling API")
                        val fetchedMatches = resultService.fetchMatchesFromApi(
                            apiKey = BuildConfig.API_FOOTBALL_KEY
                        )

                        matches = fetchedMatches
                        storage.saveMatches(fetchedMatches)
                        android.util.Log.d("API_TEST", "Loaded ${matches.size} matches")
                    } catch (e: Exception) {
                        android.util.Log.e("API_TEST", "API failed", e)
                        errorMessage = "Could not update matches: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            onResetApp = {
                matches = emptyList()

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
    isLoading: Boolean,
    errorMessage: String?,
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
                isLoading = isLoading,
                errorMessage = errorMessage,
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
    isLoading: Boolean,
    errorMessage: String?,
    onPredictionChange: (Prediction) -> Unit,
    onSimulateResultUpdate: () -> Unit,

) {
    val sections = matches
        .map { it.kickoffTime.take(10) }
        .distinct()
        .sorted()

    var selectedSection by remember(sections) {
        mutableStateOf(sections.firstOrNull())
    }

    val visibleMatches = matches.filter { match ->
        match.kickoffTime.take(10) == selectedSection
    }

    val totalPoints = matches.sumOf { match ->
        predictions[match.id]?.let { calculatePoints(match, it) } ?: 0
    }



    if (isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
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
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Updating..." else "Check latest results")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sections) { date ->
                FilterChip(
                    selected = selectedSection == date,
                    onClick = { selectedSection = date },
                    label = { Text(date) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isLoading && matches.isEmpty()) {
            Text("No matches loaded yet. Tap Check latest results to update.")
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleMatches) { match ->
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

    val borderColor = when (match.status) {
        MatchStatus.SCHEDULED -> Color(0xFF4CAF50) // Green
        MatchStatus.LIVE -> Color(0xFFF44336)      // Red
        MatchStatus.FINISHED -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (match.status == MatchStatus.FINISHED) 0.dp else 2.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(match.group.substringBefore("_") + " " + match.group.substringAfter("_"), style = MaterialTheme.typography.labelLarge)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TeamInfo(
                    name = match.homeTeam,
                    flag = match.homeFlag,
                    logoUrl = match.homeLogoUrl,
                    modifier = Modifier.weight(1f)
                )

                Text("vs", modifier = Modifier.padding(horizontal = 8.dp))

                TeamInfo(
                    name = match.awayTeam,
                    flag = match.awayFlag,
                    logoUrl = match.awayLogoUrl,
                    modifier = Modifier.weight(1f)
                )
            }



            Text(match.kickoffTime.substringBefore("T") + " at " + match.kickoffTime.substringAfter("T").substringBefore(":00Z") + " (UTC)")

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp)
            )

            when (match.status) {
                MatchStatus.SCHEDULED -> {
                    Text(
                        text = "Prediction open",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                MatchStatus.LIVE -> {
                    Text(
                        text = "Prediction locked — match has started",
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                MatchStatus.FINISHED -> {
                    Text("Result: ${match.actualHomeGoals}-${match.actualAwayGoals}")
                    Text("Points: $points")
                }
            }

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


            match.venue?.let {
                Text("Venue: $it")
            }
        }
    }
}

@Composable
fun TeamInfo(
    name: String,
    flag: String,
    logoUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = name,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(flag)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(name)
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