package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarRose

enum class SolarNavigationDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Overview", Icons.Default.Dashboard, "nav_dashboard"),
    AI_COPILOT("Gemini AI", Icons.AutoMirrored.Filled.Chat, "nav_gemini"),
    MULTI_DAY("7-Day Forecast", Icons.Default.CalendarMonth, "nav_multiday"),
    MODELS("Models & AI", Icons.Default.AutoGraph, "nav_models"),
    WEATHER("Weather & API", Icons.Default.WbSunny, "nav_weather"),
    ALERTS("Alerts", Icons.Default.Notifications, "nav_alerts")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarMainScreen(
    viewModel: SolarViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val geminiMessages by viewModel.geminiRepository.messages.collectAsStateWithLifecycle()
    val isGeminiGenerating by viewModel.geminiRepository.isGenerating.collectAsStateWithLifecycle()
    val selectedGeminiRole by viewModel.geminiRepository.selectedRole.collectAsStateWithLifecycle()
    val selectedGeminiModel by viewModel.geminiRepository.selectedModel.collectAsStateWithLifecycle()
    val useSearchGrounding by viewModel.geminiRepository.useSearchGrounding.collectAsStateWithLifecycle()
    val useMapsGrounding by viewModel.geminiRepository.useMapsGrounding.collectAsStateWithLifecycle()
    val currentUser by viewModel.firebaseManager.user.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.firebaseManager.isAuthenticating.collectAsStateWithLifecycle()
    val firestoreSyncStatus by viewModel.firebaseManager.syncStatus.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(SolarNavigationDestination.DASHBOARD) }
    var showAuthDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Display user messages via snackbar
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(SolarAmber, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SolarPower,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SolarCast",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = SolarAmber.copy(alpha = 0.2f),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "CNN-LSTM + ENN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarAmber,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = state.selectedPlant?.name ?: "Solar PV Forecast",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Firebase Auth & Cloud Sync Profile Button
                    IconButton(onClick = { showAuthDialog = true }) {
                        if (currentUser != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(SolarAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser?.displayName?.take(1)?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Firebase Account & Sync",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val unackAlerts = state.alerts.count { !it.isAcknowledged }
                    IconButton(onClick = { currentDestination = SolarNavigationDestination.ALERTS }) {
                        BadgedBox(
                            badge = {
                                if (unackAlerts > 0) {
                                    Badge(containerColor = SolarRose) {
                                        Text(text = "$unackAlerts")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = if (unackAlerts > 0) SolarRose else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                SolarNavigationDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    val unackAlerts = if (destination == SolarNavigationDestination.ALERTS) state.alerts.count { !it.isAcknowledged } else 0

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            if (unackAlerts > 0) {
                                BadgedBox(badge = { Badge(containerColor = SolarRose) { Text("$unackAlerts") } }) {
                                    Icon(imageVector = destination.icon, contentDescription = destination.title)
                                }
                            } else {
                                Icon(imageVector = destination.icon, contentDescription = destination.title)
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag(destination.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = SolarAmber,
                            indicatorColor = SolarAmber,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                SolarNavigationDestination.DASHBOARD -> {
                    DashboardScreen(
                        state = state,
                        onPlantSelected = { viewModel.selectPlant(it) },
                        onPersonaSelected = { viewModel.selectPersona(it) },
                        onHourSelected = { viewModel.selectHourPoint(it) },
                        onNavigateToMultiDay = { currentDestination = SolarNavigationDestination.MULTI_DAY },
                        onNavigateToModels = { currentDestination = SolarNavigationDestination.MODELS },
                        onSyncAreaWeather = { viewModel.syncWeatherForLocation(state.activeLatitude, state.activeLongitude, state.activeLocationName) },
                        onSyncGpsWeather = { viewModel.syncWeatherFromPlayServicesGps() }
                    )
                }
                SolarNavigationDestination.AI_COPILOT -> {
                    GeminiChatScreen(
                        messages = geminiMessages,
                        isGenerating = isGeminiGenerating,
                        selectedRole = selectedGeminiRole,
                        selectedModel = selectedGeminiModel,
                        useSearchGrounding = useSearchGrounding,
                        useMapsGrounding = useMapsGrounding,
                        currentPlant = state.selectedPlant,
                        currentHourPoint = state.selectedHourPoint ?: state.daySummaries.firstOrNull()?.hourlyPoints?.firstOrNull(),
                        onSendMessage = { viewModel.sendGeminiMessage(it) },
                        onRoleSelected = { viewModel.setGeminiRole(it) },
                        onModelSelected = { viewModel.setGeminiModel(it) },
                        onToggleSearchGrounding = { viewModel.toggleSearchGrounding() },
                        onToggleMapsGrounding = { viewModel.toggleMapsGrounding() },
                        onClearHistory = { viewModel.clearGeminiHistory() },
                        onSaveToFirestore = { viewModel.saveChatToFirestore() },
                        firestoreSyncStatus = firestoreSyncStatus
                    )
                }
                SolarNavigationDestination.MULTI_DAY -> {
                    MultiDayForecastScreen(
                        state = state,
                        onDaySelected = { viewModel.selectDay(it) },
                        onHourSelected = { viewModel.selectHourPoint(it) },
                        onExportCsv = { currentDestination = SolarNavigationDestination.WEATHER }
                    )
                }
                SolarNavigationDestination.MODELS -> {
                    ModelComparisonScreen(state = state)
                }
                SolarNavigationDestination.WEATHER -> {
                    WeatherAndDataScreen(
                        state = state,
                        onSimulateWeather = { c, t -> viewModel.simulateWeatherOverride(c, t) },
                        onResetSimulation = { viewModel.resetWeatherSimulation() },
                        onExportCsv = { viewModel.exportForecastCsv() },
                        onTestBackend = { viewModel.testBackendConnection() },
                        onUpdateBaseUrl = { viewModel.updateBackendBaseUrl(it) },
                        onSyncAreaWeather = { lat, lon, name -> viewModel.syncWeatherForLocation(lat, lon, name) },
                        onSelectPresetLocation = { viewModel.selectPresetLocation(it) },
                        onSyncGpsWeather = { viewModel.syncWeatherFromCurrentGps(context) },
                        onSyncOpenWeather = { lat, lon -> viewModel.syncOpenWeather(lat, lon) },
                        onSyncPlayServicesGps = { viewModel.syncWeatherFromPlayServicesGps() },
                        onUpdateOpenWeatherApiKey = { viewModel.updateOpenWeatherApiKey(it) }
                    )
                }
                SolarNavigationDestination.ALERTS -> {
                    AlertsScreen(
                        state = state,
                        onAcknowledgeAlert = { viewModel.acknowledgeAlert(it) }
                    )
                }
            }
        }
    }

    if (showAuthDialog) {
        FirebaseAuthDialog(
            user = currentUser,
            isAuthenticating = isAuthenticating,
            syncStatus = firestoreSyncStatus,
            onSignInWithGoogle = { viewModel.signInWithGoogle() },
            onSignInAsDemo = { viewModel.signInAsDemoOperator() },
            onSignOut = { viewModel.signOutFirebase() },
            onSyncWithFirestore = { viewModel.saveChatToFirestore() },
            onDismiss = { showAuthDialog = false }
        )
    }
}
