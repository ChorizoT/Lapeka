package fr.birdywood.lapeka.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.birdywood.lapeka.birdyauth.Account
import fr.birdywood.lapeka.birdyauth.BirdyAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val isLoading: Boolean = false,
    val account: Account? = null,
    val errorMessage: String? = null,
    val hasProblem: Boolean = false
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val birdyAuth = BirdyAuth(application)

    private val _uiState = MutableStateFlow(AccountUiState(account = birdyAuth.account))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        checkAuth()
    }

    fun login() {
        birdyAuth.redirectToLogin()
    }

    fun checkAuth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                birdyAuth.check(
                    logged = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            account = birdyAuth.account,
                            hasProblem = false
                        )
                    },
                    loggingFailed = { reason ->
                        val hasProblem = when (reason){
                            "Not Authorized" -> true
                            else -> false
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            account = null,
                            hasProblem = hasProblem
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Auth check failed",
                    hasProblem = true
                )
            }
        }
    }

    fun edit(){
        birdyAuth.editAccount()
    }

    fun logout() {
        birdyAuth.token = null
        birdyAuth.account = null
        _uiState.value = _uiState.value.copy(account = null)
    }
}
