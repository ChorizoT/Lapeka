package fr.birdywood.lapeka.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import fr.birdywood.lapeka.BuildConfig
import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.birdyauth.PreferenceSystem
import fr.birdywood.lapeka.utils.openAppNotificationSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSheet(
    viewModel: ListViewModel,
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    val ctx = LocalContext.current
    val prefSystem by remember { mutableStateOf(PreferenceSystem(ctx)) }
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val skipPartiallyExpanded by rememberSaveable { mutableStateOf(true) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues =
            if (skipPartiallyExpanded) setOf(SheetValue.Hidden, SheetValue.Expanded)
            else setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
    )

    // State for the dark mode dialog
    var showDarkModeDialog by remember { mutableStateOf(false) }
    val darkModeOptions = listOf(
        stringResource(R.string.settings_select_system_mode),
        stringResource(R.string.settings_select_light),
        stringResource(R.string.settings_select_dark)
    )
    val currentDarkMode = prefSystem.get(
        "darkmode",
        stringResource(R.string.settings_select_system_mode)
    )
    var selectedDarkModeOption by remember {
        mutableStateOf(currentDarkMode)
    }
    val _onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        val titleDisplay = stringResource(R.string.settings_title_display)
        val titleNetwork = stringResource(R.string.settings_title_network)
        val titleUpdate = stringResource(R.string.settings_title_update)
        val titleOther = stringResource(R.string.settings_title_other)
        LazyColumn(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp,
                top = 0.dp
            )
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}
                        .padding(start = 0.dp, end = 0.dp, bottom = 16.dp, top = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.action_settings),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(
                        {
                            _onDismiss()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)

                    ) {
                        Icon(
                            painterResource(R.drawable.rounded_close_24),
                            contentDescription = "Close"
                        )
                    }
                }
            }

            // --- SECTION RESEAU ---
            SettingsSection(title = titleNetwork) {
                SettingsTextFieldTile(
                    title = stringResource(R.string.text_manifest_endpoint),
                    value = url,
                    onValueSave = { newUrl ->
                        url = newUrl; if (newUrl.isNotBlank()) onSave(newUrl)
                    },
                    label = stringResource(R.string.text_api_url),
                    icon = painterResource(R.drawable.cloud_24),
                    position = TilePosition.START
                )
                var show_featured_apps by remember {
                    mutableStateOf(
                        prefSystem.get(
                            "show_featured_apps",
                            true
                        )
                    )
                }
                SettingsSwitchTile(
                    title = stringResource(R.string.featured_apps),
                    subtitle = stringResource(R.string.featured_apps_subtitle),
                    checked = show_featured_apps,
                    onCheckedChange = {
                        show_featured_apps = it
                        scope.launch {
                            prefSystem.set("show_featured_apps", it)
                            delay(200.milliseconds)
                            _onDismiss()
                            activity?.finish()
                        }
                    },
                    icon = painterResource(R.drawable.editor_choice_24),
                    position = TilePosition.END
                )
            }

//            item {
//                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
//            }

            // --- SECTION APPARENCE ---
            SettingsSection(title = titleDisplay) {
                var dynamicTheme by remember {
                    mutableStateOf(
                        prefSystem.get(
                            "dynamicTheme",
                            false
                        )
                    )
                }

                var labs by remember {
                    mutableStateOf(
                        prefSystem.get(
                            "labs",
                            false
                        )
                    )
                }
                SettingsSwitchTile(
                    title = stringResource(R.string.dynamic_theme),
                    subtitle = stringResource(R.string.dynamic_theme_subtitle),
                    checked = dynamicTheme,
                    onCheckedChange = {
                        dynamicTheme = it
                        scope.launch {
                            prefSystem.set("dynamicTheme", it)
                            delay(200.milliseconds)
                            _onDismiss()
                            activity?.recreate()
                        }
                    },
                    icon = painterResource(R.drawable.palette_24),
                    position = TilePosition.START
                )

                SettingsTile(
                    title = stringResource(R.string.dark_mode),
                    subtitle = selectedDarkModeOption,
                    icon = painterResource(R.drawable.dark_mode_24),
                    onClick = { showDarkModeDialog = true },
                    position = TilePosition.MIDDLE
                )

                SettingsSwitchTile(
                    title = stringResource(R.string.labs_title),
                    subtitle = stringResource(R.string.labs_subtitle),
                    checked = labs,
                    onCheckedChange = {
                        labs = it
                        scope.launch {
                            prefSystem.set("labs", it)
                            delay(200.milliseconds)
                            _onDismiss()
                            activity?.recreate()
                        }
                    },
                    icon = painterResource(R.drawable.experiment_24),
                    position = TilePosition.END
                )
            }

//            item {
//                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
//            }
            // --- SECTION MISES À JOUR ---
            SettingsSection(title = titleUpdate) {
                var buttonEnabled = true
                SettingsTile(
                    title = stringResource(R.string.rechercher_des_nouvelles_versions),
                    icon = painterResource(R.drawable.rounded_refresh_24),
                    onClick = {
                        if (buttonEnabled) {
                            scope.launch {
                                buttonEnabled = false
                                viewModel.refresh(true)
                                delay(5000.milliseconds)
                                buttonEnabled = true
                            }
                        }
                    },
                    position = TilePosition.ALONE
                )
            }

//            item {
//                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
//            }
            // --- SECTION AUTRES ---
            SettingsSection(title = titleOther) {
                val context = LocalContext.current
                val email = stringResource(R.string.email)
                val homepage = stringResource(R.string.app_homepage)
                SettingsTile(
                    title = stringResource(R.string.notifications),
                    subtitle = stringResource(R.string.notifications_subtitle),
                    icon = painterResource(R.drawable.notifications_24),
                    modifier = Modifier,
                    onClick = {
                        openAppNotificationSettings(context)
                    },
                    position = TilePosition.START
                )
                SettingsTile(
                    title = stringResource(R.string.github_repository),
                    icon = painterResource(R.drawable.github_brands_solid_ful),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/ChorizoT/Lapeka".toUri()
                        )
                        context.startActivity(intent)
                    }
                )
                SettingsTile(
                    title = stringResource(R.string.github_changelog),
                    icon = painterResource(R.drawable.newspaper_24),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "$homepage/changelog".toUri()
                        )
                        context.startActivity(intent)
                    }
                )
                SettingsTile(
                    title = stringResource(R.string.support),
                    subtitle = email,
                    icon = painterResource(R.drawable.support_agent_24),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:$email".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "Support Application - v1.0.8")
                        }

                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    "Envoyer un e-mail via..."
                                )
                            )
                        }
                    }
                )
                SettingsTile(
                    title = stringResource(R.string.version_actuelle),
                    subtitle = BuildConfig.VERSION_NAME,
                    icon = painterResource(R.drawable.info_24),
                    onClick = null,
                    position = TilePosition.END
                )
            }

            // --- FOOTER / A PROPOS ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.build_with_love_by_birdywood),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.credit_app),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    if (showDarkModeDialog) {
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text(text = stringResource(R.string.dark_mode)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    darkModeOptions.forEach { text ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (text == selectedDarkModeOption),
                                    onClick = {
                                        selectedDarkModeOption = text
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (text == selectedDarkModeOption),
                                onClick = null // null recommended for accessibility with selectable modifier
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDarkModeDialog = false
                        scope.launch {
                            prefSystem.set("darkmode", selectedDarkModeOption)
                            // Small delay to allow sheet to start closing before recreate
                            delay(200.milliseconds)
                            activity?.recreate()
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDarkModeDialog = false
                        selectedDarkModeOption = currentDarkMode // Reset on cancel
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        content()
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun ToggleSetting(
    preferenceSystem: PreferenceSystem,
    label: String,
    key: String,
    defaultValue: Boolean = false,
    onChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf(preferenceSystem.get(key, defaultValue)) }
    SettingsRow(label) {
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                scope.launch {
                    preferenceSystem.set(key, it)
                    onChange(checked)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectSetting(
    preferenceSystem: PreferenceSystem,
    label: String,
    key: String,
    values: List<String>,
    onChange: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedValue by remember {
        mutableStateOf(preferenceSystem.get(key, values.first()))
    }
    val scope = rememberCoroutineScope()

    SettingsRow(label) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .widthIn(max = 200.dp),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            DropdownMenuPopup(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(),
            ) {
                val itemCount = values.size

                values.fastForEachIndexed { itemIndex, itemLabel ->
                    DropdownMenuItem(
                        text = { Text(itemLabel) },
                        shapes = MenuDefaults.itemShape(itemIndex, itemCount),
                        checked = selectedValue == itemLabel,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                selectedValue = itemLabel
                                expanded = false
                                scope.launch {
                                    preferenceSystem.set(key, itemLabel)
                                    onChange(itemLabel)
                                }
                            }
                        },
                        checkedLeadingIcon = {
                            Icon(
                                painterResource(R.drawable.rounded_check_24),
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        }
                    )
                }
            }
        }
    }
}

