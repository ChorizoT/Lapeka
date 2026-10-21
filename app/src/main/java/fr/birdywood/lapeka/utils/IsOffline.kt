package fr.birdywood.lapeka.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Propriété d'extension globale
val Context.isOffline: Boolean
    get() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return true
        val capabilities = cm.getNetworkCapabilities(network) ?: return true

        // On vérifie si l'appareil MANQUE d'Internet ou de validation
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return !hasInternet
    }