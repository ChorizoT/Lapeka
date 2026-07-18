package fr.birdywood.lapeka

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import fr.birdywood.lapeka.birdyauth.BirdyAuth
import fr.birdywood.lapeka.birdyauth.PreferenceSystem
import fr.birdywood.lapeka.ui.App
import fr.birdywood.lapeka.ui.theme.LapekaTheme
import fr.birdywood.lapeka.ui.theme.DefaultGradient
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    private lateinit var ba: BirdyAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ba = BirdyAuth(this)
        val handled = handleIntent(intent)
        val pref = PreferenceSystem(this)

        setContent {
            val darkTheme = when (pref.get("darkmode", stringResource(R.string.settings_select_system_mode))){
                stringResource(R.string.settings_select_light) -> false
                stringResource(R.string.settings_select_dark) -> true
                stringResource(R.string.settings_select_system_mode) -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme()
            }
            LapekaTheme (darkTheme = darkTheme, dynamicColor = pref.get("dynamicTheme", false)) {
                val color = DefaultGradient().last().copy(alpha = 0.5f)
                enableEdgeToEdge(
                    navigationBarStyle = SystemBarStyle.light(
                        color.toArgb(),
                        color.toArgb()
                    )
                )

                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }

        if (!handled) {
            lifecycleScope.launch {
                    ba.check(){
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent): Boolean {
        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data?.scheme == "lapeka" && data.host == "auth") {
                val token = data.getQueryParameter("token")
                if (token != null) {
                    ba.token = token
                    lifecycleScope.launch {
                        try {
                            ba.check(){
                                //shwo snackbar error with viewmodel . showErrorMessage
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    return true
                }
            }
        }
        return false
    }
}
