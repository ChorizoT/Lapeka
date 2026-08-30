package fr.birdywood.lapeka.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import fr.birdywood.lapeka.R

@Composable
fun SettingsTile(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    color: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    position: TilePosition = TilePosition.MIDDLE,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .padding(position.padding())
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = position.shape(),
        color = color,

        ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    Modifier
                        .padding(end = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (action != null) {
                Spacer(modifier = Modifier.width(16.dp))
                action()
            }
        }
    }
}

enum class TilePosition {
    START,
    MIDDLE,
    END,
    ALONE;

    fun shape() =
        when (this) {
            START -> RoundedCornerShape(20.dp, 20.dp, 8.dp, 8.dp)
            MIDDLE -> RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp)
            END -> RoundedCornerShape(8.dp, 8.dp, 20.dp, 20.dp)
            ALONE -> RoundedCornerShape(20.dp, 20.dp, 20.dp, 20.dp)
        }


    fun padding() =
        when (this) {
            START -> PaddingValues(top = 8.dp, bottom = 2.dp)
            MIDDLE -> PaddingValues(vertical = 2.dp)
            END -> PaddingValues(top = 2.dp, bottom = 8.dp)
            ALONE -> PaddingValues(vertical = 8.dp)
        }

}

@Composable
fun SettingsSwitchTile(
    title: String,
    checked: Boolean,
    position: TilePosition = TilePosition.MIDDLE,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
) {
    SettingsTile(
        title = title,
        subtitle = subtitle,
        icon = icon,
        position = position,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        action = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}


@Composable
fun SettingsTextFieldTile(
    title: String,
    value: String,
    onValueSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    position: TilePosition = TilePosition.MIDDLE,
    subtitle: String? = null,
    icon: Painter? = null,
    label: String = ""
) {
    var textState by androidx.compose.runtime.remember(value) {
        androidx.compose.runtime.mutableStateOf(
            value
        )
    }
    val isChanged = textState != value

    Column(
        modifier = modifier
            .padding(position.padding())
            .fillMaxWidth()
            .clip(position.shape())
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        SettingsTile(
            title = title,
            subtitle = subtitle,
            icon = icon,
            color = Color.Transparent
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isChanged || true) {
                val trailingIconWidth by animateDpAsState(
                    targetValue = if (isChanged) 48.dp else 0.dp,
                    label = "trailingIconWidth"
                )
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = if (trailingIconWidth > 0.dp) {
                        {
                            Box(
                                modifier = Modifier
                                    .width(trailingIconWidth)
                                    .clipToBounds(), // Empêche l'icône de dépasser pendant qu'elle se réduit
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isChanged,
                                    enter = slideInHorizontally(initialOffsetX = { it }) + expandHorizontally(),
                                    exit = slideOutHorizontally(targetOffsetX = { it }) + shrinkHorizontally(),
                                ) {
                                    IconButton(
                                        onClick = { onValueSave(textState) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.rounded_save_24),
                                            contentDescription = stringResource(R.string.action_save),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else null
                )
            } else {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun LazyListScope.SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    item {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 0.dp, top = 16.dp, bottom = 8.dp)
            )
            content()
        }
    }
}