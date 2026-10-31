package fr.birdywood.lapeka

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import fr.birdywood.lapeka.LapekaApp.Companion.auth
import fr.birdywood.lapeka.ui.App
import fr.birdywood.lapeka.ui.theme.DefaultGradient
import fr.birdywood.lapeka.ui.theme.LapekaTheme
import fr.birdywood.lapeka.utils.AppPreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppPreferences.init(this)

        if (savedInstanceState == null) handleIntent(intent)

        lifecycleScope.launch {
            auth.refresh()
        }

        //val pref = PreferenceSystem(this)

        setContent {
            val darkTheme = when (AppPreferences.themeMode){
                stringResource(R.string.settings_select_light) -> false
                stringResource(R.string.settings_select_dark) -> true
                stringResource(R.string.settings_select_system_mode) -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme()
            }
            LapekaTheme (darkTheme = darkTheme, dynamicColor = AppPreferences.dynamicTheme) {
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data ?: return
        if (auth.isAuthDeepLink(uri) && !BuildConfig.IS_FOSS) {
            lifecycleScope.launch { val result = auth.handleDeepLink(uri)
                result.onSuccess { Log.d("Lapeka", "DeepLink auth succeeded: $it") }
                    .onFailure { Log.e("Lapeka", "DeepLink auth failed", it) } }
        }else{
            Log.e("Lapeka", "Not a BirdyAuth deeplink")
        }
    }
}
