package fr.birdywood.lapeka.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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


    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.padding(24.dp)) {
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
            item {
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
            }
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

        }
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
