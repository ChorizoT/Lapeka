package fr.birdywood.lapeka.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.BuildConfig
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


    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        /*LazyColumn(modifier = Modifier.padding(24.dp)) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Text(
                        stringResource(R.string.action_settings),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            /*item {
                Text(
                    stringResource(R.string.text_manifest_endpoint),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                /*Row {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.text_api_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onSave(url) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = url.isNotBlank()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }*/
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.text_api_url)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    FloatingActionButton(
                        modifier = Modifier.fillMaxHeight(),
                        onClick = {
                            onSave(url)
                        },
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.rounded_save_24),
                            contentDescription = stringResource(R.string.action_save)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }*/
            item {
                ToggleSetting(
                    prefSystem,
                    stringResource(R.string.dynamic_theme),
                    "dynamicTheme"
                ) {
                    onDismiss()
                    activity?.recreate()
                }
            }
            item {
                SelectSetting(
                    prefSystem, stringResource(R.string.dark_mode), "darkmode", listOf(
                        stringResource(R.string.settings_select_system_mode),
                        stringResource(R.string.settings_select_light),
                        stringResource(R.string.settings_select_dark)
                    )
                ) {
                    onDismiss()
                    activity?.recreate()
                }
            }
            item {
                Spacer(modifier = Modifier.height(48.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(72.dp))
            }

            item {
                var buttonEnabled by rememberSaveable { mutableStateOf(true) }
                SettingsRow(stringResource(R.string.force_reload_danger)) {
                    Button(
                        enabled = buttonEnabled,
                        onClick = {
                            scope.launch {
                                prefSystem.set("forceReload", true)
                                buttonEnabled = false
                                viewModel.refresh()
                                delay(1000)
                                buttonEnabled = true
                                prefSystem.set("forceReload", false)
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.rounded_refresh_24),
                            contentDescription = "Refresh"
                        )
                    }
                }
            }
            item { SettingsRow(stringResource(R.string.version)) { Text("${BuildConfig.VERSION_NAME}") } }
            item { SettingsRow(stringResource(R.string.credit)) { Text(stringResource(R.string.build_with_love_by_birdywood)) } }
            item { Text(stringResource(R.string.credit_app), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)}

        }*/
        val titleDisplay = stringResource(R.string.settings_title_display)
        val titleNetwork = stringResource(R.string.settings_title_network)
        val titleUpdate = stringResource(R.string.settings_title_update)
        val titleOther = stringResource(R.string.settings_title_other)
        LazyColumn(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 0.dp)) {
            stickyHeader {
                Row (modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.action_settings),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(
                        {
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.CenterVertically).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)

                    ) {
                        Icon(
                            painterResource(R.drawable.rounded_close_24),
                            contentDescription = "Close"
                        )
                    }
                }
            }

            // --- SECTION RESEAU ---
            SettingsSection(title = "Réseau & API") {
                SettingsTextFieldTile(
                    title = stringResource(R.string.text_manifest_endpoint),
                    value = url,
                    onValueSave = { newUrl -> url = newUrl ;if (newUrl.isNotBlank()) onSave(newUrl) },
                    label = "URL de l'API",
                    icon = painterResource(R.drawable.cloud_24)
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

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
                SettingsSwitchTile(
                    title = stringResource(R.string.dynamic_theme),
                    subtitle = stringResource(R.string.dynamic_theme_subtitle),
                    checked = dynamicTheme,
                    onCheckedChange = {
                        dynamicTheme = it
                        scope.launch {
                            prefSystem.set("dynamicTheme", it)
                            delay(200)
                            onDismiss()
                            activity?.recreate()
                        }
                    },
                    icon = painterResource(R.drawable.palette_24)
                )

                SettingsTile(
                    title = "Mode sombre",
                    subtitle = selectedDarkModeOption,
                    icon = painterResource(R.drawable.dark_mode_24),
                    onClick = { showDarkModeDialog = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
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
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            // --- SECTION AUTRES ---
            SettingsSection(title = titleOther) {
                val context = LocalContext.current
                SettingsTile(
                    title = stringResource(R.string.notifications),
                    subtitle = stringResource(R.string.notifications_subtitle),
                    icon = painterResource(R.drawable.notifications_24),
                    modifier = Modifier,
                    onClick = {
                        openAppNotificationSettings(context)
                    }
                )
                SettingsTile(
                    title = stringResource(R.string.version_actuelle),
                    subtitle = BuildConfig.VERSION_NAME,
                    icon = painterResource(R.drawable.info_24),
                    onClick = null
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
                            delay(200)
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

