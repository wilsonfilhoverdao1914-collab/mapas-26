package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
  private val appContext = context.applicationContext
  private val connectivityManager =
    appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private var registered = false

  private val callback =
    object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        _isOnline.value = checkCurrentlyOnline()
      }

      override fun onLost(network: Network) {
        _isOnline.value = checkCurrentlyOnline()
      }

      override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
      ) {
        val hasNet =
          networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        _isOnline.value = hasNet || checkCurrentlyOnline()
      }

      override fun onUnavailable() {
        _isOnline.value = false
      }
    }

  fun start() {
    if (registered) return
    val cm = connectivityManager ?: return
    try {
      val request =
        NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build()
      cm.registerNetworkCallback(request, callback)
      registered = true
      _isOnline.value = checkCurrentlyOnline()
    } catch (_: Exception) {
      _isOnline.value = checkCurrentlyOnline()
    }
  }

  fun stop() {
    if (!registered) return
    val cm = connectivityManager ?: return
    try {
      cm.unregisterNetworkCallback(callback)
    } catch (_: Exception) {
      // ignore if already unregistered
    } finally {
      registered = false
    }
  }

  private fun checkCurrentlyOnline(): Boolean {
    val cm = connectivityManager ?: return true
    return try {
      val activeNetwork = cm.activeNetwork ?: return false
      val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      true
    }
  }
}
