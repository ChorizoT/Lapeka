package fr.birdywood.lapeka.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.birdywood.lapeka.data.AppStatus
import fr.birdywood.lapeka.data.TrackedApp
import java.text.DateFormat.getDateInstance
import java.util.Date
import fr.birdywood.lapeka.R
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Locale.getDefault
import androidx.compose.ui.platform.LocalLocale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(viewModel: ListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var search by rememberSaveable { mutableStateOf("") }

    var hideKeyboard by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column {

        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            stickyHeader {
                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .clickable { hideKeyboard = true }
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape
                        )
                ) {


                    TextField(
                        modifier = Modifier
                            .fillMaxWidth(), value = search,
                        onValueChange = { search = it },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onSurface
                        ),
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.rounded_search_24),
                                contentDescription = "search icon"
                            )
                        },
                        trailingIcon = {
                            if (search != "") {
                                IconButton(
                                    onClick = { search = ""; focusManager.clearFocus() }
                                ) {
                                    Icon(
                                        painterResource(R.drawable.rounded_close_24),
                                        contentDescription = "close icon"
                                    )
                                }
                            }
                        },
                        placeholder = { Text(text = stringResource(R.string.action_search)) }
                    )
                    if (hideKeyboard) {
                        focusManager.clearFocus()
                        // Call onFocusClear to reset hideKeyboard state to false
                        hideKeyboard = false
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            items(uiState.apps, key = { it.remote.id }) { app ->
                if (app.remote.name.includeSearch(search) && (app.remote.id != "lapeka" || app.status != AppStatus.UP_TO_DATE)) {
                    AppCard(
                        app = app,
                        onAction = { viewModel.installOrUpdate(app) })
                }
            }
            item {
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

@Composable
fun String.includeSearch(search: String): Boolean {
    if (search == "") return true
    val parsed = search.split(" ").filter { it != "" }

    parsed.forEach {
        if (this.lowercase(LocalLocale.current.platformLocale)
                .contains(it.lowercase(LocalLocale.current.platformLocale))
        ) {
            return true
        }
    }
    return false
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
private fun AppCard(app: TrackedApp, onAction: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val appCond = app.status == AppStatus.DOWNLOADING || app.status == AppStatus.INSTALLING
    val sizeIcon by animateIntAsState(
        targetValue = if (appCond) 28 else 52,
        animationSpec = tween(durationMillis = 300),
        label = "IntegerAnimation"
    )

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                    /*.background(MaterialTheme.colorScheme.secondaryContainer)*/,
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = app.remote.iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(sizeIcon.dp)
                            .clip(CircleShape)
                    )

                    androidx.compose.animation.AnimatedVisibility(visible = appCond,
                        enter = fadeIn(),
                        exit = fadeOut()
                        ) {
                        CircularWavyProgressIndicator()
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
                StatusAction(app = app, onAction = onAction)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppProgressIndicator() {
    Column {
        Spacer(modifier = Modifier.height(12.dp))
        LinearWavyProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
        )
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
private fun StatusAction(app: TrackedApp, onAction: () -> Unit) {
    when (app.status) {
        AppStatus.NOT_INSTALLED -> Button(onClick = onAction) { Text(stringResource(R.string.action_install)) }
        AppStatus.UPDATE_AVAILABLE -> Button(onClick = onAction) { Text(stringResource(R.string.action_update)) }
        AppStatus.UP_TO_DATE -> {
            val context = LocalContext.current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                /*AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.state_up_to_date)) })
                Spacer(modifier = Modifier.width(8.dp))*/
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
            }
        }

        AppStatus.DOWNLOADING -> Text(
            stringResource(R.string.state_downloading),
            style = MaterialTheme.typography.labelMedium
        )

        AppStatus.INSTALLING -> Text(
            stringResource(R.string.state_installing),
            style = MaterialTheme.typography.labelMedium
        )

        AppStatus.ERROR -> Button(onClick = onAction) { Text(stringResource(R.string.action_retry)) }
    }
}

private fun versionSubtitle(ctx: Context, app: TrackedApp): AnnotatedString {
    val installed = app.installedVersionName ?: ctx.getString(R.string.state_not_installed)
    //return "Installed: $installed \nLatest: ${app.remote.versionName}"
    return buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(ctx.getString(R.string.state_installed))
        }
        append(installed)

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("\n" + ctx.getString(R.string.state_latest))
        }
        append(app.remote.versionName)
    }
}
