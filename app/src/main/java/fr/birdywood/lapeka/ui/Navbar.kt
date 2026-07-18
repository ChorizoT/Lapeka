package fr.birdywood.lapeka.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.birdywood.lapeka.R

@Composable
fun Navbar(
    modifier: Modifier = Modifier,
    accountPainter: Painter?,
    currentRoute: String?,
    onClick: (String) -> Unit = {},

    ){
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        HorizontalFloatingToolbar(
            modifier = modifier
                .offset(y = -ScreenOffset)
                .border(1.dp,
                    MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            expanded = true,
            content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToolbarTextIconButton(
                        label = stringResource(R.string.action_apps),
                        icon = painterResource(R.drawable.rounded_apps_24),
                        selectedIcon = painterResource(R.drawable.rounded_apps_24),
                        selected = currentRoute == "list",
                        onClick = { onClick("list") }
                    )

                    ToolbarTextIconButton(
                        label = stringResource(R.string.action_account),
                        icon = accountPainter ?: painterResource(R.drawable.rounded_account_24),
                        selectedIcon = accountPainter ?: painterResource(R.drawable.rounded_account_fill_24),
                        selected = currentRoute == "account",
                        onClick = { onClick("account") },
                        iconModifier = if (accountPainter != null) Modifier
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape
                            ) else Modifier,
                        tint = if (accountPainter != null) Color.Unspecified else null
                    )
                }
            }
        )
    }
}



/**
 * An icon-above-label button for use inside a [HorizontalFloatingToolbar].
 *
 * **Selected effect:** when [selected] is `true` the button draws a pill-shaped
 * container filled with [MaterialTheme.colorScheme.secondaryContainer], the icon
 * and label switch to [MaterialTheme.colorScheme.onSecondaryContainer], and the
 * pill smoothly animates in/out via [animateColorAsState] + [animateDpAsState].
 *
 * This matches the M3 Expressive "indicator" pattern used in navigation bars and
 * tab rows — keeping visual language consistent across the whole app.
 *
 * @param label     Visible text under the icon.
 * @param icon      Icon shown above the label.
 * @param selected  Whether this item is currently active / selected.
 * @param onClick   Called when the button is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarTextIconButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: Painter,
    selectedIcon : Painter = icon,
    onClick: () -> Unit,
    selected: Boolean = false,
    iconModifier: Modifier = Modifier,
    tint: Color? = null,
){
    // ── Animated colours ──────────────────────────────────────────────────────
    val containerColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 0),
        label = "toolbar_button_container",
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onSecondaryContainer
        else
            LocalContentColor.current,
        animationSpec = tween(durationMillis = 250),
        label = "toolbar_button_content",
    )

    // Pill vertical padding expands slightly when selected for a "pop" feel
    val pillVerticalPadding by animateDpAsState(
        targetValue = if (selected) 6.dp else 4.dp,
        animationSpec = tween(durationMillis = 250),
        label = "toolbar_button_padding",
    )
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        TextButton(
            onClick = onClick,
            modifier = modifier,
            // Remove TextButton's own padding — the pill Box handles spacing instead
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        ) {
            // Pill-shaped indicator that fades in when selected
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge) // fully-rounded pill
                    .background(containerColor)
                    .padding(horizontal = 16.dp, vertical = pillVerticalPadding),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            painter = if (selected) selectedIcon else icon,
                            contentDescription = null, // label already describes the action
                            modifier = iconModifier.size(28.dp),
                            tint = tint ?: contentColor,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}