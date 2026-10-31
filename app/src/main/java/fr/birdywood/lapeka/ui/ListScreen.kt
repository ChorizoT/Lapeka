package fr.birdywood.lapeka.ui

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.data.AppStatus
import fr.birdywood.lapeka.data.FeaturedApp
import fr.birdywood.lapeka.data.TrackedApp
import fr.birdywood.lapeka.utils.AppPreferences
import fr.birdywood.lapeka.utils.FilterSelect
import fr.birdywood.lapeka.utils.filterApps
import java.text.DateFormat.getDateInstance
import java.util.Date


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListScreen(viewModel: ListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val search = uiState.searchQuery

    var hideKeyboard by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val state = rememberLazyStaggeredGridState()

    val labs by remember { mutableStateOf(AppPreferences.labs) }


    Column {
        val scrollState = rememberScrollState()
        if (labs) {
            FlowRow(
                Modifier
                    .padding(horizontal = 0.dp)
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Spacer(modifier = Modifier.width(14.dp))
                FilterSelect.entries.forEachIndexed { index, filter ->
                    ToggleButton(
                        checked = uiState.filter == filter,
                        onCheckedChange = { viewModel.setFilter(filter) },
                        shapes =
                            when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                FilterSelect.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        if (uiState.filter == filter) {
                            Icon(
                                painterResource(R.drawable.rounded_check_24),
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        }
                        Text(stringResource(filter.idString))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 320.dp),
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 14.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!labs) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (uiState.isLoadingApps) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        Modifier.fillMaxSize().padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            } else {
                val itemsFiltered = uiState.apps
                    .filterApps(uiState.filter)
                    .filter {
                        it.remote.packageName.includeSearch(search) || it.remote.name.includeSearch(
                            search
                        )
                    }
                if (itemsFiltered.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = if (uiState.showFeaturedApps) {
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            } else {
                                null
                            },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(
                                Modifier
                                    .padding(16.dp)
                                    .padding(vertical = 100.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painterResource(R.drawable.planet_24),
                                        modifier = Modifier.size(72.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        contentDescription = null
                                    )

                                    Spacer(modifier = Modifier.height(32.dp))

                                    Text(
                                        stringResource(R.string.text_no_matching_app),
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.toggleSettings(true) }) {
                                        Text(
                                            stringResource(R.string.action_configure)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        itemsFiltered,
                        key = { it.remote.id }
                    ) { app ->
                        AppCard(
                            app = app,
                            onAction = { viewModel.addToQueue(app) },
                            onCancel = { viewModel.removeToQueue(app) })
                    }
                }

            }
            item(span = StaggeredGridItemSpan.FullLine) {
                AnimatedVisibility(
                    search == "" && uiState.filter == FilterSelect.DEFAULT && uiState.showFeaturedApps && !uiState.isLoadingFeaturedApps && uiState.featuredApps.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { -it }) + expandVertically(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + shrinkVertically()
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.featured_apps),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            FeaturedAppsCarouselSection(uiState.featuredApps) { id ->
                                val app = uiState.featuredApps.find { it.id == id }
                                if (app != null) {
                                    val intent = CustomTabsIntent.Builder().build()
                                    intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent.launchUrl(
                                        context,
                                        "https://vps.birdywood.fr/project/${app.id}".toUri()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Spacer(
                    Modifier.padding(
                        WindowInsets.navigationBars.asPaddingValues()
                            .plus(PaddingValues(top = 100.dp))
                    )
                )
            }
        }
    }

}

/**
 * Returns true if the string contains any of the words in the search query (case-insensitive).
 */
fun String.includeSearch(search: String): Boolean {
    if (search.isBlank()) return true
    val words = search.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return words.any { this.contains(it, ignoreCase = true) }
}


@Composable
fun EmptyState(onConfigure: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.text_no_manifest_configured_yet),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.text_set_your_api_endpoint_to_start_tracking_apps),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onConfigure) { Text(stringResource(R.string.action_configure)) }
    }
}

@Composable
fun OfflineState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painterResource(R.drawable.globe_2_cancel_24),
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            stringResource(R.string.text_offline),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.subtitle_offline),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun AppCard(app: TrackedApp, onAction: () -> Unit, onCancel: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val appCond =
        app.status is AppStatus.Downloading || app.status is AppStatus.Installing || app.status is AppStatus.Queued
    val sizeIcon by animateIntAsState(
        targetValue = if (appCond) 28 else 52,
        animationSpec = tween(durationMillis = 300),
        label = "IntegerAnimation"
    )

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val clip = if (appCond) CircleShape else RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(clip)
                    /*.background(MaterialTheme.colorScheme.secondaryContainer)*/,
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = app.remote.iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(sizeIcon.dp)
                            .clip(clip)
                    )

                    if (app.status is AppStatus.Downloading) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = appCond,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CircularWavyProgressIndicator({ app.status.progress })
                        }
                    } else if (app.status is AppStatus.Queued) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = appCond,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    } else {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = appCond,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CircularWavyProgressIndicator()
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.remote.name,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        versionSubtitle(LocalContext.current, app),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusAction(app = app, onInstall = onAction, onCancel = onCancel)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                app.remote.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            /*AnimatedVisibility(visible = app.status == AppStatus.DOWNLOADING || app.status == AppStatus.INSTALLING) {
                AppProgressIndicator()
            }*/

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(
                        label = stringResource(R.string.item_package),
                        value = app.remote.packageName
                    )
                    DetailRow(
                        label = stringResource(R.string.item_latest_update),
                        value = getDateInstance().format(Date(app.remote.lastUpdate * 1000))
                    )
                    DetailRow(
                        label = stringResource(R.string.item_latest_version_code),
                        value = app.remote.versionCode.toString()
                    )
                    app.installedVersionCode?.let {
                        DetailRow(
                            label = stringResource(R.string.item_installed_version_code),
                            value = it.toString()
                        )
                    }

                    app.remote.changelog?.takeIf { it.isNotBlank() }?.let { changelog ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.item_changelog),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val annotatedString = buildAnnotatedString {

                            withLink(LinkAnnotation.Url(url = changelog)) {
                                withStyle(
                                    style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                                ) {
                                    append(changelog)
                                }
                            }
                        }
                        Text(annotatedString, style = MaterialTheme.typography.bodySmall)
                    }

                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatusAction(app: TrackedApp, onInstall: () -> Unit, onCancel: () -> Unit) {
    when (app.status) {
        is AppStatus.NotInstalled -> Button(onClick = onInstall) { Text(stringResource(R.string.action_install)) }
        is AppStatus.UpdateAvailable -> Button(
            onClick = onInstall,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ) { Text(stringResource(R.string.action_update)) }

        is AppStatus.UpToDate -> {
            val context = LocalContext.current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (app.remote.id != "lapeka") {
                    FilledTonalButton(
                        onClick = {
                            context.packageManager
                                .getLaunchIntentForPackage(app.remote.packageName)
                                ?.let {
                                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(it)
                                }
                        }
                    ) {
                        Text(stringResource(R.string.action_open))
                    }
                } else {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.state_up_to_date)) })
                }


            }
        }

        is AppStatus.Downloading -> Text(
            stringResource(R.string.state_downloading),
            style = MaterialTheme.typography.labelMedium
        )

        is AppStatus.Installing -> Text(
            stringResource(R.string.state_installing),
            style = MaterialTheme.typography.labelMedium
        )

        is AppStatus.Failed, is AppStatus.Cancelled -> Button(
            onClick = onInstall, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) { Text(stringResource(R.string.action_retry)) }

        is AppStatus.Queued -> OutlinedButton(onClick = onCancel) {
            Text("Annuler")
        }

        is AppStatus.Success ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.state_up_to_date)) }
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedAppsCarouselSection(
    featuredApps: List<FeaturedApp>,
    onAppClick: (String) -> Unit
) {
    if (featuredApps.isEmpty()) return

    val carouselState = rememberCarouselState { featuredApps.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        preferredItemWidth = 100.dp,
        itemSpacing = 8.dp
    ) { index ->
        val app = featuredApps[index]

        Card(
            onClick = { onAppClick(app.id) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AsyncImage(
                    model = app.img,
                    contentDescription = "Bannière de ${app.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.sp,
                            maxFontSize = 16.sp,
                            stepSize = 1.sp
                        )
                    )
                }
            }
        }
    }
}

private fun versionSubtitle(ctx: Context, app: TrackedApp): AnnotatedString {
    val installed = app.installedVersionName ?: ctx.getString(R.string.state_not_installed)
    //return "Installed: $installed \nLatest: ${app.remote.versionName}"
    return buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(ctx.getString(R.string.state_installed))
        }
        append(" ")
        append(installed)

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("\n" + ctx.getString(R.string.state_latest))
        }
        append(" ")
        append(app.remote.versionName)
    }
}
