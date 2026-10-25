package fr.birdywood.lapeka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.birdywood.lapeka.birdyauth.Account
import fr.birdywood.lapeka.R
import fr.birdywood.lapeka.ui.theme.LapekaTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun AccountScreen(
    viewModel: AccountViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            val account = uiState.account
            if (account != null) {
                LoggedInView(
                    account = account,
                    onLogout = { viewModel.logout() },
                    onRefresh = {viewModel.checkAuth()},
                    onEdit = { viewModel.edit() }
                )
            } else {
                NotLoggedInView(onLogin = { viewModel.login() })
            }
        }

        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = error)
            }
        }
    }
}

@Composable
fun LoggedInView(
    account: Account,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit
) {
    val monthFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("dd", Locale.getDefault()) }

    val birthDate = remember(account.birthdate) {
        if (account.birthdate != 0L) Date(account.birthdate * 1000) else null
    }

    val daysUntilBirthday = remember(account.birthdate) {
        if (account.birthdate != 0L) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val birthCalendar = Calendar.getInstance().apply { timeInMillis = account.birthdate * 1000 }

            val nextBirthday = Calendar.getInstance().apply {
                set(Calendar.YEAR, today.get(Calendar.YEAR))
                set(Calendar.MONTH, birthCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, birthCalendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (nextBirthday.before(today)) {
                nextBirthday.add(Calendar.YEAR, 1)
            }

            val diff = nextBirthday.timeInMillis - today.timeInMillis
            TimeUnit.MILLISECONDS.toDays(diff)
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = account.img,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.rounded_account_24),
                error = painterResource(R.drawable.rounded_account_24)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (account.firstname == "" && account.lastname == "") {
                    stringResource(R.string.action_account)
                } else {
                    "${account.firstname} ${account.lastname}"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${account.username}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = account.email,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- COMPOSANT ANNIVERSAIRE FAÇON WIDGET ---
            if (birthDate != null && daysUntilBirthday != null) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge, // Bords très arrondis
                    color = MaterialTheme.colorScheme.secondaryContainer, // Fond teinté pastel
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icône Calendrier stylisée
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = monthFormatter.format(birthDate).replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = dayFormatter.format(birthDate),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Texte principal
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (daysUntilBirthday == 0L) stringResource(R.string.account_birthday_today) else pluralStringResource(
                                    R.plurals.account_birthday_not_today,
                                    daysUntilBirthday.toInt(), daysUntilBirthday
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                //color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            // ------------------------------------------

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painterResource(R.drawable.rounded_person_edit_24),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_edit))
                }
                Button(
                    onClick = onLogout,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(stringResource(R.string.action_logout))
                }
            }
        }
    }
}


@Preview
@Composable
fun LoggedInViewPreview() {
    LapekaTheme {
        LoggedInView(Account(
            "id",
            "username",
            "firstname",
            "lastname",
            "email",
            "img",
            listOf(),
            10,
            "creationDate",
            "lastUpdated"
        ), {}, {}, {})
    }
}

        @Composable
fun NotLoggedInView(onLogin: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_account_24),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(R.string.text_connect_to_birdyauth),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.text_log_in_to_manage_your_account_and_access_more_features),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text(stringResource(R.string.action_login_with_birdyauth))
        }
    }
}


@Composable
fun CantLogIn(onLogin: () -> Unit){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_dangerous_24),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(id = R.string.error_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section "Que pouvez-vous faire ?"
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.error_cta_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "• " + stringResource(id = R.string.error_hint_1),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• " + stringResource(id = R.string.error_hint_2),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text(stringResource(R.string.error_btn_retry))
        }
    }
}
