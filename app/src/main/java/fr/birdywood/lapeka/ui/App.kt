package fr.birdywood.lapeka.ui

import android.content.Intent
import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import fr.birdywood.lapeka.BuildConfig
import fr.birdywood.lapeka.ui.theme.DefaultGradient
import fr.birdywood.lapeka.ui.theme.GradientBackgroundBrush
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import fr.birdywood.lapeka.R
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App(viewModel: ListViewModel = viewModel(), accountViewModel: AccountViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val uiStateAccount by accountViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(uiState.manifestUrl.isBlank()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "list"
    val context = LocalContext.current
    val prefSystem = remember { fr.birdywood.lapeka.birdyauth.PreferenceSystem(context) }
    var welcomeDone by remember { mutableStateOf(prefSystem.get("welcomeDone", false)) }
    val lastVersionCode = remember { prefSystem.get("lastVersionCode", -1) }
    val downloadUrl = stringResource(R.string.app_homepage)
    val shareMessage = stringResource(R.string.share_message, downloadUrl)
    val shareTitle = stringResource(R.string.share_title)

    var showDialog by remember {
        mutableStateOf(lastVersionCode != -1 && lastVersionCode < BuildConfig.VERSION_CODE || true)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (!welcomeDone) {
        WelcomeScreen(onGetStarted = {
            prefSystem.set("welcomeDone", true)
            welcomeDone = true
        })
    } else if (uiStateAccount.account != null) {
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
                TopAppBar(
                    modifier = Modifier.clip(
                        shape = RoundedCornerShape(
                            0.dp,
                            0.dp,
                            16.dp,
                            16.dp
                        )
                    ),
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                painterResource(R.drawable.rounded_refresh_24),
                                contentDescription = stringResource(R.string.action_refresh)
                            )
                        }
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    shareMessage
                                )
                                putExtra(Intent.EXTRA_TITLE, shareTitle)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) {
                            Icon(
                                painterResource(R.drawable.share_24),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                painterResource(R.drawable.rounded_settings_24),
                                contentDescription = stringResource(R.string.action_settings)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.inverseOnSurface)
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            bottomBar = {
                val accountPainter = if (uiStateAccount.account?.img?.isNotEmpty() == true) {
                    rememberImagePainter(uiStateAccount.account?.img)
                } else {
                    null
                }

                Navbar(
                    currentRoute = currentRoute,
                    accountPainter = accountPainter,
                    modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
                ) { route ->
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
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
                    .padding(bottom = 0.dp)
                    .fillMaxSize()
            ) {

                NavHost(
                    navController = navController, startDestination = "list",
                    enterTransition = {
                        EnterTransition.None
                    },
                    exitTransition = {
                        ExitTransition.None
                    }) {
                    composable("list") {

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
                                uiState.isLoading -> {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ContainedLoadingIndicator()
                                    }
                                }

                                uiState.apps.isEmpty() -> {
                                    EmptyState(onConfigure = { showSettings = true })
                                }

                                else -> {
                                    ListScreen(viewModel)
                                }
                            }
                        }
                    }
                    composable("account") {
                        AccountScreen(accountViewModel)
                    }
                }

            }
        }
    } else if (uiStateAccount.hasProblem) {
        CantLogIn(onLogin = { accountViewModel.login() })
    } else {
        NotLoggedInView(onLogin = { accountViewModel.login() })
    }

    if (showSettings) {
        SettingsSheet(
            viewModel,
            initialUrl = uiState.manifestUrl,
            onDismiss = { showSettings = false },
            onSave = { url ->
                viewModel.setManifestUrl(url)
                showSettings = false
            }
        )
    }
    if (showDialog) {
        WhatsNewDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = {
                showDialog = false; scope.launch {
                prefSystem.set(
                    "lastVersionCode",
                    BuildConfig.VERSION_CODE
                )
            }
            }
        )
    }
    LaunchedEffect(lastVersionCode) {
        Log.d("Lapeka", "lastVersionCode: $lastVersionCode")
        if (lastVersionCode == -1) {
            prefSystem.set("lastVersionCode", BuildConfig.VERSION_CODE)
        }
    }

}