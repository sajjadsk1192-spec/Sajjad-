package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.LuckyDrawRepository
import com.example.ui.screens.AdminControlScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BuyCoinsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.signOutUser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
    const val ROUTE_HOME = "home"
    const val ROUTE_BUY_COINS = "buy_coins"
    const val ROUTE_RESULTS = "results"
    const val ROUTE_ADMIN = "admin"
}

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun LuckyDrawApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: LuckyDrawViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        LuckyDrawViewModel(
                            repository = LuckyDrawRepository(db),
                            currentUserId = uid
                        )
                    }
                }
            )
            AuthenticatedLuckyDrawContent(viewModel = viewModel)
        }
    }
}

@Composable
private fun AuthenticatedLuckyDrawContent(viewModel: LuckyDrawViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()
    val isDrawingWinner by viewModel.isDrawingWinner.collectAsStateWithLifecycle()
    val shufflingParticipant by viewModel.shufflingParticipant.collectAsStateWithLifecycle()
    val latestDrawnWinner by viewModel.latestDrawnWinner.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    fun handleSignOut() {
        signOutUser(
            credentialManager = credentialManager,
            scope = scope
        )
    }

    when (val state = uiState) {
        LuckyDrawUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is LuckyDrawUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = viewModel::retrySync) {
                            Text("Retry")
                        }
                        OutlinedButton(onClick = ::handleSignOut) {
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        is LuckyDrawUiState.Ready -> {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: Routes.ROUTE_HOME

            val destinations = remember(state.isAdmin) {
                buildList {
                    add(
                        TopDestination(
                            route = Routes.ROUTE_HOME,
                            labelRes = R.string.nav_home,
                            selectedIcon = Icons.Filled.Home,
                            unselectedIcon = Icons.Outlined.Home,
                            testTag = "nav_home_tab"
                        )
                    )
                    add(
                        TopDestination(
                            route = Routes.ROUTE_BUY_COINS,
                            labelRes = R.string.nav_buy_coins,
                            selectedIcon = Icons.Filled.ShoppingBag,
                            unselectedIcon = Icons.Outlined.ShoppingBag,
                            testTag = "nav_buy_coins_tab"
                        )
                    )
                    add(
                        TopDestination(
                            route = Routes.ROUTE_RESULTS,
                            labelRes = R.string.nav_results,
                            selectedIcon = Icons.Filled.EmojiEvents,
                            unselectedIcon = Icons.Outlined.EmojiEvents,
                            testTag = "nav_results_tab"
                        )
                    )
                    if (state.isAdmin) {
                        add(
                            TopDestination(
                                route = Routes.ROUTE_ADMIN,
                                labelRes = R.string.nav_admin,
                                selectedIcon = Icons.Filled.AdminPanelSettings,
                                unselectedIcon = Icons.Outlined.AdminPanelSettings,
                                testTag = "nav_admin_tab"
                            )
                        )
                    }
                }
            }

            fun navigateToTab(route: String) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 600.dp

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar {
                                destinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    val label = stringResource(dest.labelRes)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) {
                                                    dest.selectedIcon
                                                } else {
                                                    dest.unselectedIcon
                                                },
                                                contentDescription = label
                                            )
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavRail) {
                            NavigationRail {
                                Spacer(modifier = Modifier.weight(1f))
                                destinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    val label = stringResource(dest.labelRes)
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTab(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) {
                                                    dest.selectedIcon
                                                } else {
                                                    dest.unselectedIcon
                                                },
                                                contentDescription = label
                                            )
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = Routes.ROUTE_HOME,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(Routes.ROUTE_HOME) {
                                HomeScreen(
                                    user = state.user,
                                    isAdmin = state.isAdmin,
                                    configuredPrizeCoins = state.appConfig.configuredDrawPrizeCoins,
                                    transactions = state.transactions,
                                    timerState = timerState,
                                    hasEnteredToday = state.hasEnteredToday,
                                    onBuyCoinsClick = { navigateToTab(Routes.ROUTE_BUY_COINS) },
                                    onEnterDraw = viewModel::enterTodaysDraw,
                                    onSwitchProfile = ::handleSignOut
                                )
                            }
                            composable(Routes.ROUTE_BUY_COINS) {
                                BuyCoinsScreen(
                                    currentBalance = state.user.coinBalance,
                                    paymentState = paymentState,
                                    paymentOrders = state.paymentOrders,
                                    onPayAndBuy = viewModel::purchaseCoins,
                                    onResetPaymentState = viewModel::resetPaymentState,
                                    onDoneToHome = { navigateToTab(Routes.ROUTE_HOME) }
                                )
                            }
                            composable(Routes.ROUTE_RESULTS) {
                                ResultsScreen(
                                    currentUserId = state.user.id,
                                    isAdmin = state.isAdmin,
                                    configuredPrizeCoins = state.appConfig.configuredDrawPrizeCoins,
                                    drawResults = state.drawResults,
                                    latestDrawnWinner = latestDrawnWinner,
                                    isDrawingWinner = isDrawingWinner,
                                    shufflingParticipant = shufflingParticipant,
                                    onDrawWinner = { viewModel.drawWinnerByAdmin(null) }
                                )
                            }
                            if (state.isAdmin) {
                                composable(Routes.ROUTE_ADMIN) {
                                    AdminControlScreen(
                                        appConfig = state.appConfig,
                                        players = state.players,
                                        paymentOrders = state.paymentOrders,
                                        drawResults = state.drawResults,
                                        todayDateKey = state.todayDateKey,
                                        isDrawingWinner = isDrawingWinner,
                                        shufflingParticipant = shufflingParticipant,
                                        onUpdatePrizePool = viewModel::updatePrizePoolCoins,
                                        onTriggerWinnerSelection = viewModel::drawWinnerByAdmin
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
