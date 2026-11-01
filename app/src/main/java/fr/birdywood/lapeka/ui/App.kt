package fr.birdywood.lapeka.ui

import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import fr.birdywood.lapeka.BuildConfig
import fr.birdywood.lapeka.LapekaApp.Companion.auth
import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.birdyauth.Account
import fr.birdywood.lapeka.birdyauth.BirdyAuthListener
import fr.birdywood.lapeka.ui.theme.DefaultGradient
import fr.birdywood.lapeka.ui.theme.GradientBackgroundBrush
import fr.birdywood.lapeka.utils.AppPreferences
import fr.birdywood.lapeka.utils.isOffline
import fr.birdywood.lapeka.utils.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun App(viewModel: ListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val account by auth.account.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "list"
    val context = LocalContext.current
    var welcomeDone by remember { mutableStateOf(AppPreferences.welcomeDone) }
    val lastVersionCode = remember { AppPreferences.lastVersionCode }
    val downloadUrl = stringResource(R.string.app_homepage)
    val IS_FOSS by remember { mutableStateOf(BuildConfig.IS_FOSS) }
    var isOffline by remember { mutableStateOf(context.isOffline) }
    var accountNotAuthorized by remember { mutableStateOf(false) }

    val windowSizeClass = calculateWindowSizeClass(context as Activity)

    val goTo: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val accountPainter = if (account?.img?.isNotEmpty() == true) {
        rememberAsyncImagePainter(account?.img)
    } else {
        null
    }


    LaunchedEffect(Unit) {
        auth.listener = object : BirdyAuthListener {
            override fun onNotAuthorized() {
                accountNotAuthorized = true
            }

            override fun onLoggedIn(account: Account) {
                accountNotAuthorized = false
                viewModel.refresh()
            }
        }
    }

    var showDialog by remember {
        mutableStateOf(lastVersionCode != -1 && lastVersionCode < BuildConfig.VERSION_CODE)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    if (isOffline) {
        OfflineState { isOffline = context.isOffline }
    } else if (!welcomeDone && IS_FOSS) {
        WelcomeScreenFOSS(onGetStarted = {
            viewModel.setManifestUrl(it)
            AppPreferences.welcomeDone = true
            welcomeDone = true
        })
    } else if (!welcomeDone) {
        WelcomeScreen(onGetStarted = {
            AppPreferences.welcomeDone = true
            welcomeDone = true
        })
    } else if (account != null || IS_FOSS) {
        ExpandedNavbar(
            viewModel = viewModel,
            windowSizeClass = windowSizeClass,
            currentRoute = currentRoute,
            accountPainter = accountPainter,
            goTo = goTo
        ) {
            Scaffold(
                modifier = Modifier
                    //.padding(WindowInsets.navigationBars.asPaddingValues())
                    .background(
                        brush = GradientBackgroundBrush(
                            true,
                            DefaultGradient()
                        )
                    ),
                containerColor = Color.Transparent,
                topBar = {
                    NewTopSearchBar(
                        queryValue = uiState.searchQuery,
                        isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                        onQueryChange = { newValue ->
                            viewModel.setSearchQuery(newValue)
                            // C'est ici que vous pouvez déclencher le filtrage de vos produits à la volée !
                        },
                        onShareClick = { viewModel.shareLapeka() },
                        onSettingsClick = { viewModel.toggleSettings(true) }
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                bottomBar = {
                    if (!IS_FOSS) {
                        ResponsiveNavbar(
                            windowSizeClass = windowSizeClass,
                            currentRoute = currentRoute,
                            accountPainter = accountPainter,
                            modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
                            onClick = goTo
                        )
                    }
                }
            ) { padding ->
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(padding.update(bottom = 0.dp))
                            .widthIn(max = 1000.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        NavHost(
                            navController = navController, startDestination = "list",
                            /*enterTransition = {
                        EnterTransition.None
                    },
                    exitTransition = {
                        ExitTransition.None
                    }*/
                        ) {
                            composable(
                                "list",
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(400)
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(400)
                                    )
                                }) {

                                var isRefreshing by remember { mutableStateOf(false) }
                                val coroutineScope = rememberCoroutineScope()
                                PullToRefreshBox(
                                    isRefreshing = isRefreshing,
                                    onRefresh = {
                                        coroutineScope.launch {
                                            isRefreshing = true
                                            delay(200.milliseconds)
                                            viewModel.refresh()
                                            delay(1000.milliseconds)
                                            isRefreshing = false
                                        }
                                    },
                                ) {
                                    when {
                                        uiState.apps.isEmpty() && !uiState.isLoadingApps && false -> {
                                            EmptyState(onConfigure = { viewModel.toggleSettings(true) })
                                        }

                                        else -> {
                                            ListScreen(viewModel)
                                        }
                                    }
                                }
                            }
                            composable(
                                "account",
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(400)
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(400)
                                    )
                                }) {
                                AccountScreen()
                            }
                        }
                    }
                }
            }
        }
    } else if (accountNotAuthorized) {
        CantLogIn(onLogin = { auth.login() }, onEdit = { auth.openAccountSettings() })
    } else {
        NotLoggedInView(onLogin = { auth.login() })
    }

    if (uiState.showSettings) {
        SettingsSheet(
            viewModel,
            windowSizeClass,
            initialUrl = uiState.manifestUrl,
            onDismiss = { viewModel.toggleSettings(false) },
            onSave = { url ->
                viewModel.setManifestUrl(url)
                viewModel.toggleSettings(false)
            }
        )
    }
    if (showDialog) {
        WhatsNewDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = {
                showDialog = false;
                scope.launch {
                    AppPreferences.lastVersionCode = BuildConfig.VERSION_CODE
                }
            }
        )
    }
    LaunchedEffect(lastVersionCode) {
        Log.d("Lapeka", "lastVersionCode: $lastVersionCode")
        if (lastVersionCode == -1) {
            AppPreferences.lastVersionCode = BuildConfig.VERSION_CODE
        }
    }

}