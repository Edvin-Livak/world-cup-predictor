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
import com.sned.worldcuppredictor.model.MatchStatus
import com.sned.worldcuppredictor.scoring.calculatePoints
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.sned.worldcuppredictor.storage.PredictionStorage
import kotlinx.coroutines.launch
import com.sned.worldcuppredictor.api.MatchResultService
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import com.sned.worldcuppredictor.repository.ProfileRepository
import com.sned.worldcuppredictor.repository.PredictionRepository
import com.sned.worldcuppredictor.model.MatchPredictionView

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldCupPredictorApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = remember { PredictionStorage(context) }
    val scope = rememberCoroutineScope()

    val savedUserName by storage.userNameFlow.collectAsState(initial = null)
    val savedUserId by storage.userIdFlow.collectAsState(initial = null)
    val savedPredictions by storage.predictionsFlow.collectAsState(initial = emptyMap())
    val savedMatches by storage.matchesFlow.collectAsState(initial = emptyList())

    val predictionRepository = remember { PredictionRepository() }

    var userNameInput by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    val resultService = remember { MatchResultService() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var profileRepository = remember { ProfileRepository() }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isCreatingProfile by remember { mutableStateOf(false) }

    var leaderboardUsers by remember { mutableStateOf<List<UserScore>>(emptyList()) }
    var selectedMatch by remember { mutableStateOf<Match?>(null) }
    var selectedMatchPredictions by remember { mutableStateOf<List<MatchPredictionView>>(emptyList()) }

    var pinInput by remember { mutableStateOf("") }

    fun refreshLeaderboard() {
        scope.launch {
            try {
                val profiles = profileRepository.getAllProfiles()
                val onlinePredictions = predictionRepository.getAllPredictions()

                leaderboardUsers = profiles.map { profile ->
                    val userPredictions = onlinePredictions
                        .filter { it.userId == profile.id }
                        .associate {
                            it.matchId to Prediction(
                                matchId = it.matchId,
                                homeGoals = it.homeGoals,
                                awayGoals = it.awayGoals,
                                penaltyWinner = it.penaltyWinner
                            )
                        }

                    val points = matches.sumOf { match ->
                        userPredictions[match.id]?.let { prediction ->
                            calculatePoints(match, prediction)
                        } ?: 0
                    }

                    UserScore(
                        name = profile.username,
                        points = points
                    )
                }.sortedByDescending { it.points }

            } catch (e: Exception) {
                android.util.Log.e("SUPABASE_TEST", "Could not refresh leaderboard", e)
            }
        }
    }

    fun showPredictionsForMatch(match: Match) {
        scope.launch {
            val profiles = profileRepository.getAllProfiles()
            val onlinePredictions = predictionRepository.getAllPredictions()

            selectedMatch = match

            selectedMatchPredictions = profiles.map { profile ->
                val prediction = onlinePredictions.firstOrNull {
                    it.userId == profile.id && it.matchId == match.id
                }

                MatchPredictionView(
                    username = profile.username,
                    homeGoals = prediction?.homeGoals,
                    awayGoals = prediction?.awayGoals
                )
            }.sortedBy { it.username }
        }
    }

    fun syncPredictionsForCurrentUser(userId: String) {
        scope.launch {
            try {
                val onlinePredictions = predictionRepository.getPredictionsForUser(userId)

                val localPredictions = onlinePredictions.associate {
                    it.matchId to Prediction(
                        matchId = it.matchId,
                        homeGoals = it.homeGoals,
                        awayGoals = it.awayGoals,
                        penaltyWinner = it.penaltyWinner
                    )
                }

                storage.savePredictions(localPredictions)
            } catch (e: Exception) {
                android.util.Log.e("SUPABASE_TEST", "Could not sync user predictions", e)
            }
        }
    }

    if (savedUserName == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val currentUserName = savedUserName ?: return

    LaunchedEffect(savedMatches) {
        if (matches.isEmpty() && savedMatches.isNotEmpty()) {
            matches = savedMatches
        }
    }

    LaunchedEffect(currentUserName) {
        if (currentUserName.isBlank()) {
            userNameInput = ""
            return@LaunchedEffect
        }

        if (matches.isEmpty()) {
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

    LaunchedEffect(currentUserName, matches) {
        if (currentUserName.isNotBlank() && matches.isNotEmpty()) {
            refreshLeaderboard()
        }
    }

    LaunchedEffect(savedUserId) {
        if (!savedUserId.isNullOrBlank()) {
            syncPredictionsForCurrentUser(savedUserId!!)
        }
    }

    if (currentUserName.isBlank()) {
        NameScreen(
            userName = userNameInput,
            errorMessage = loginError,
            isLoading = isCreatingProfile,
            onNameChange = {
                userNameInput = it
                loginError = null
            },
            onJoin = {
                if (userNameInput.isNotBlank()) {
                    scope.launch {
                        isCreatingProfile = true
                        loginError = null

                        try {
                            val username = userNameInput.trim()

                            if (profileRepository.usernameExists(username) || pinInput.length != 4) {
                                loginError = "Username is already taken or PIN code not 4 numbers long."
                            } else {
                                val createdProfile = profileRepository.createProfile(username = username, pin = pinInput)

                                storage.saveUserName(username)
                                createdProfile.id?.let { storage.saveUserId(it) }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SUPABASE_TEST", "Could not create profile", e)
                            loginError = "Could not create profile. Try again."
                        } finally {
                            isCreatingProfile = false
                        }
                    }
                }
            },
            onLogin = {
                if (userNameInput.isNotBlank()) {
                    scope.launch {
                        isCreatingProfile = true
                        loginError = null
                        try {
                            val username = userNameInput.trim()

                            val profile = profileRepository.verifyLogin(username, pinInput)

                            if (profile != null && profile.id != null) {
                                storage.saveUserName(profile.username)
                                storage.saveUserId(profile.id)
                            } else {
                                loginError = "Invalid username or PIN."
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SUPABASE_TGEST", "Could not log in", e)
                            loginError = "Could not log in. Try again."
                        } finally {
                            isCreatingProfile = false
                        }
                    }
                }
            },
            pin = pinInput,
            onPinChange = {
                pinInput = it
                loginError = null
            }
        )
    } else {
        MainScreen(
            userName = currentUserName,
            matches = matches,
            predictions = savedPredictions,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onPredictionChange = { prediction ->
                val updatedPredictions = savedPredictions + (prediction.matchId to prediction)

                scope.launch {
                    storage.savePredictions(updatedPredictions)

                    savedUserId?.let { userId ->
                        try {
                            predictionRepository.savePrediction(userId, prediction)
                            refreshLeaderboard()
                        } catch (e: Exception) {
                            android.util.Log.e("SUPABASE_TEST", "Could not save online prediction", e)
                        }

                    }
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
            },
            leaderboardUsers = leaderboardUsers,
            onRefreshLeaderboard = { refreshLeaderboard() },
            onTestFinishFirstMatch = {
                val firstMatch = matches.firstOrNull()

                if (firstMatch != null) {
                    matches = matches.map { match ->
                        if (match.id == firstMatch.id) {
                            match.copy(
                                group = "LAST_16",
                                stage = "LAST_16",
                                status = MatchStatus.FINISHED,
                                actualHomeGoals = 2,
                                actualAwayGoals = 1,
                                penaltyWinner = null
                            )
                        } else {
                            match
                        }
                    }

                    scope.launch {
                        storage.saveMatches(matches)
                        refreshLeaderboard()
                    }
                }
            },
            onSetFinishFirstMatch = {
                val firstMatch = matches.firstOrNull()

                if (firstMatch != null) {
                    matches = matches.map { match ->
                        if (match.id == firstMatch.id) {
                            match.copy(
                                group = "LAST_16",
                                stage = "LAST_16",
                                status = MatchStatus.SCHEDULED,
                                actualHomeGoals = null,
                                actualAwayGoals = null,
                                penaltyWinner = null
                            )
                        } else {
                            match
                        }
                    }

                    scope.launch {
                        storage.saveMatches(matches)
                        refreshLeaderboard()
                    }
                }
            },
            onViewMatchPredictions = { match ->
                showPredictionsForMatch(match)
            },
            onLogOut = {
                scope.launch {
                    storage.logout()
                }
            }
        )
        selectedMatch?.let { match ->
            ModalBottomSheet(
                onDismissRequest = {
                    selectedMatch = null
                    selectedMatchPredictions = emptyList()
                }
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "${match.homeTeam} vs ${match.awayTeam}",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    selectedMatchPredictions.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.username)

                            val predictionText =
                                if (item.homeGoals == null || item.awayGoals == null) {
                                    "No prediction"
                                } else {
                                    "${item.homeGoals} - ${item.awayGoals}"
                                }

                            Text(predictionText)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun NameScreen(
    userName: String,
    errorMessage: String?,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onJoin: () -> Unit,
    onLogin: () -> Unit,
    pin: String,
    onPinChange: (String) -> Unit
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

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    onPinChange(it.filter { c -> c.isDigit() }.take(4))
                },
                label = { Text("4-digit PIN") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogin,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Logging in..." else "Log in")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onJoin,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Creating profile..." else "Create New Profile")
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
    onResetApp: () -> Unit,
    leaderboardUsers: List<UserScore>,
    onRefreshLeaderboard: () -> Unit,
    onTestFinishFirstMatch: () -> Unit,
    onViewMatchPredictions: (Match) -> Unit,
    onLogOut: () -> Unit,
    onSetFinishFirstMatch: () -> Unit
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
                onSimulateResultUpdate = onSimulateResultUpdate,
                onTestFinishFirstMatch = onTestFinishFirstMatch,
                onSetFinishFirstMatch = onSetFinishFirstMatch,
                onViewMatchPredictions = onViewMatchPredictions
            )

            AppTab.Leaderboard -> LeaderboardScreen(
                modifier = Modifier.padding(padding),
                users = leaderboardUsers,
                onRefreshLeaderboard = onRefreshLeaderboard
            )

            AppTab.Profile -> ProfileScreen(
                modifier = Modifier.padding(padding),
                userName = userName,
                onResetApp = onResetApp,
                onLogOut = onLogOut
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
    onTestFinishFirstMatch: () -> Unit,
    onSetFinishFirstMatch: () -> Unit,
    onViewMatchPredictions: (Match) -> Unit
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

        OutlinedButton(
            onClick = onSetFinishFirstMatch,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set first match to knockout match")
        }

        OutlinedButton(
            onClick = onTestFinishFirstMatch,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test finish first match 1-1 pens HOME")
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
                    onPredictionChange = onPredictionChange,
                    onViewPredictions = { onViewMatchPredictions(match) }
                )
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    users: List<UserScore>,
    onRefreshLeaderboard: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRefreshLeaderboard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh leaderboard")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (users.isEmpty()) {
            Text("No leaderboard data yet.")
        } else {
            users.forEachIndexed { index, user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${index + 1}. ${user.name}")
                        Text("${user.points} pts")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    userName: String,
    onResetApp: () -> Unit,
    onLogOut: () -> Unit
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Scoring rules",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("• Correct winner or draw: 1 point")
                Text("• Exact score: +3 points")
                Text("• Maximum per match: 4 points")
                Text("• Penalty shootouts do not count toward the predicted score")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log out")
        }

        Spacer(modifier = Modifier.height(12.dp))

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
    onPredictionChange: (Prediction) -> Unit,
    onViewPredictions: () -> Unit
) {
    val isLocked = match.status != MatchStatus.SCHEDULED
    var homeGoalsText by remember(match.id, prediction) {
        mutableStateOf(prediction?.homeGoals?.toString() ?: "")
    }

    var awayGoalsText by remember(match.id, prediction) {
        mutableStateOf(prediction?.awayGoals?.toString() ?: "")
    }

    val points = prediction?.let { calculatePoints(match, it) } ?: 0

    val borderColor = when (match.status) {
        MatchStatus.SCHEDULED -> Color(0xFF4CAF50) // Green
        MatchStatus.LIVE -> Color(0xFFF44336)      // Red
        MatchStatus.FINISHED -> Color.Transparent
    }

    var penaltyWinner by remember(match.id, prediction) {
        mutableStateOf(prediction?.penaltyWinner)
    }

    val isKnockout = match.stage != null && match.stage != "GROUP_STAGE"
    val isDrawPrediction =
        homeGoalsText.toIntOrNull() != null &&
                homeGoalsText.toIntOrNull() == awayGoalsText.toIntOrNull()

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
                        savePrediction(match.id, homeGoalsText, awayGoalsText, penaltyWinner, onPredictionChange)
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
                        savePrediction(match.id, homeGoalsText, awayGoalsText, penaltyWinner, onPredictionChange)
                    },
                    label = { Text(match.awayTeam) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            if (isKnockout && isDrawPrediction) {
                Spacer(modifier = Modifier.height(12.dp))

                Text("If penalty shootout, who wins?")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !isLocked,
                        onClick = {
                            penaltyWinner = "HOME"
                            savePrediction(match.id, homeGoalsText, awayGoalsText, penaltyWinner, onPredictionChange)
                        },
                        border = BorderStroke(
                            width = if (penaltyWinner == "HOME") 3.dp else 1.dp,
                            color = if (penaltyWinner == "HOME") Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(match.homeTla ?: match.homeTeam)
                    }

                    OutlinedButton(
                        enabled = !isLocked,
                        onClick = {
                            penaltyWinner = "AWAY"
                            savePrediction(match.id, homeGoalsText, awayGoalsText, penaltyWinner, onPredictionChange)
                        },
                        border = BorderStroke(
                            width = if (penaltyWinner == "AWAY") 3.dp else 1.dp,
                            color = if (penaltyWinner == "AWAY") Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(match.awayTla ?: match.awayTeam)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (match.status != MatchStatus.SCHEDULED) {
                OutlinedButton(
                    onClick = onViewPredictions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View other predictions")
                }
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
    penaltyWinner: String?,
    onPredictionChange: (Prediction) -> Unit
) {
    val homeGoals = homeGoalsText.toIntOrNull()
    val awayGoals = awayGoalsText.toIntOrNull()

    if (homeGoals != null && awayGoals != null) {
        onPredictionChange(
            Prediction(
                matchId = matchId,
                homeGoals = homeGoals,
                awayGoals = awayGoals,
                penaltyWinner = penaltyWinner
            )
        )
    }
}