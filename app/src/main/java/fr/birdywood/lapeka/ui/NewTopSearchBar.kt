package fr.birdywood.lapeka.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.birdywood.lapeka.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTopSearchBar(
    queryValue: String,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAppNameSplash by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible
    var isMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible && isExpanded) {
            focusManager.clearFocus()
            isExpanded = false
        }
    }

    // Lapeka Animation
    LaunchedEffect(Unit) {
        if (!isExpanded) {
            delay(300.milliseconds)
            showAppNameSplash = true
            delay(1200.milliseconds) // Reste visible pendant 1,2 seconde
            showAppNameSplash = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthDp = maxWidth


        val animatedWidth by animateDpAsState(
            targetValue = if (isExpanded) maxWidthDp - 32.dp else maxWidthDp - 124.dp,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "searchBarWidth"
        )

        // Animation de la forme des coins (de Pilule à Carré)
        val animatedCorners by animateDpAsState(
            targetValue = if (isExpanded) 8.dp else 28.dp,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "searchBarCorners"
        )

        val placeholderBias by animateFloatAsState(
            targetValue = if (isExpanded) -1f else 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "placeholderAlignment"
        )

        // Bouton Menu (Placé de manière absolue à gauche)
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Menu",
                modifier = Modifier
                    .padding(start = 12.dp, end = 8.dp)
                    .size(46.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(animatedWidth) // On anime directement la largeur ici !
                .height(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(animatedCorners)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isExpanded = true
                },
            contentAlignment = Alignment.Center
        ) {
            // Utilisation du composant officiel M3 pour conserver la logique de focus/saisie
            SearchBarDefaults.InputField(
                query = queryValue,
                onQueryChange = onQueryChange,
                onSearch = {
                    // Supprime aussi le focus lorsque l'utilisateur appuie sur la touche Entrée/Loupe du clavier
                    focusManager.clearFocus()
                    isExpanded = false
                },
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it },
                placeholder = {
                    /*if (!showAppNameSplash) {
                        Text(stringResource(R.string.action_search), textAlign = if (isExpanded) TextAlign.Center else TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }*/
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = null,
                trailingIcon = {
                    if (queryValue != "") {
                        IconButton(
                            onClick = { onQueryChange(""); focusManager.clearFocus() }
                        ) {
                            Icon(
                                painterResource(R.drawable.rounded_close_24),
                                contentDescription = "close icon"
                            )
                        }
                    }
                },
                colors = SearchBarDefaults.inputFieldColors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, // Fond de la zone sans focus
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, // Fond de la zone avec focus
                )
            )

            if (queryValue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // On applique une marge horizontale interne pour éviter que le texte colle au bord une fois à gauche
                        .padding(horizontal = 16.dp),
                    contentAlignment = BiasAlignment(
                        horizontalBias = placeholderBias,
                        verticalBias = 0f
                    )
                ) {
                    AnimatedVisibility(
                        visible = !showAppNameSplash,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(100))
                    ) {
                        Text(
                            text = stringResource(R.string.action_search),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Animation au milieu : Affichage éphémère du nom "Lapeka"
            androidx.compose.animation.AnimatedVisibility(
                visible = showAppNameSplash,
                enter = fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.5f),
                exit = fadeOut(animationSpec = tween(800)) + scaleOut(targetScale = 0.9f)
            ) {
                Text(
                    text = "Lapeka",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        AnimatedVisibility(
            visible = !isExpanded,
            exit = fadeOut(tween(150)),
            enter = fadeIn(tween(150)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(modifier = Modifier.padding(end = 16.dp)) {
                IconButton(
                    onClick = { isMenuExpanded = true },
                    modifier = Modifier
                        .padding(end = 0.dp, start = 8.dp)
                        .size(40.dp)
                        .offset(x = 4.dp)
                ) {
                    Icon(
                        painter = if (isMenuExpanded) painterResource(id = R.drawable.lunch_dining_fill_24) else painterResource(
                            id = R.drawable.lunch_dining_24
                        ), // Remplacez par votre ressource
                        contentDescription = "Profil",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                }
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    //containerColor = Color(0xFFF3EFF9), // Couleur assortie à l'application
                    // Forme asymétrique Expressive : le coin haut-droit (proche de l'avatar) est pointu, les autres très arrondis
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 4.dp,
                        bottomStart = 24.dp,
                        bottomEnd = 24.dp
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share), fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.share_24),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onShareClick()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_settings), fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.rounded_settings_24),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onSettingsClick()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}