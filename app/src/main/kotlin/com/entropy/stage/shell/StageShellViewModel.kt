package com.entropy.stage.shell

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.entropy.stage.data.StagePreferencesRepository
import com.entropy.stage.device.DeviceProfile
import com.entropy.stage.launcher.InstalledAppsRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StageShellViewModel(
    application: Application,
    initialProfile: DeviceProfile,
) : AndroidViewModel(application), StageShellActions {
    private val preferences = StagePreferencesRepository(application)
    private val installedApps = InstalledAppsRepository(application)
    private val _uiState = MutableStateFlow(StageShellUiState(deviceProfile = initialProfile))
    private var restoredSession = false

    val uiState: StateFlow<StageShellUiState> = _uiState.asStateFlow()

    init {
        installedApps.start(viewModelScope)

        viewModelScope.launch {
            preferences.state.collect { saved ->
                val restoredSurface = if (restoredSession) {
                    _uiState.value.activeSurface
                } else {
                    saved.lastSurface
                }
                restoredSession = true
                _uiState.value = _uiState.value.copy(
                    preferencesLoaded = true,
                    onboardingComplete = saved.onboardingComplete,
                    pinnedAppIds = saved.pinnedAppIds,
                    activeSurface = restoredSurface,
                    reduceMotion = saved.reduceMotion,
                    desktopGrain = saved.desktopGrain,
                    dockMagnification = saved.dockMagnification,
                )
            }
        }
        viewModelScope.launch {
            installedApps.apps.collect { apps ->
                _uiState.value = _uiState.value.copy(apps = apps)
            }
        }
        viewModelScope.launch {
            installedApps.loading.collect { loading ->
                _uiState.value = _uiState.value.copy(appsLoading = loading)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                refreshStatusOnly()
                delay(30_000)
            }
        }
        refreshPlatformState()
    }

    fun updateDeviceProfile(profile: DeviceProfile) {
        _uiState.value = _uiState.value.copy(deviceProfile = profile)
    }

    override fun completeOnboarding() {
        viewModelScope.launch { preferences.setOnboardingComplete(true) }
    }

    override fun openSurface(surface: StageSurface) {
        _uiState.value = _uiState.value.copy(
            activeSurface = surface,
            commandCenterOpen = false,
            controlCenterOpen = false,
        )
        viewModelScope.launch { preferences.setLastSurface(surface) }
    }

    override fun closeSurface() {
        openSurface(StageSurface.NONE)
    }

    override fun setCommandCenterOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            commandCenterOpen = open,
            controlCenterOpen = if (open) false else _uiState.value.controlCenterOpen,
            searchQuery = if (open) _uiState.value.searchQuery else "",
        )
    }

    override fun setControlCenterOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            controlCenterOpen = open,
            commandCenterOpen = if (open) false else _uiState.value.commandCenterOpen,
        )
    }

    override fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    override fun launchApp(app: InstalledApp) {
        installedApps.launch(app).onSuccess {
            _uiState.value = _uiState.value.copy(
                commandCenterOpen = false,
                message = "Opening ${app.label}",
            )
        }.onFailure { failure ->
            showError("Could not open ${app.label}", failure)
        }
    }

    override fun togglePinned(app: InstalledApp) {
        viewModelScope.launch { preferences.togglePinned(app.id) }
    }

    override fun openAppInfo(app: InstalledApp) {
        installedApps.openAppInfo(app).onFailure { failure ->
            showError("App information is unavailable", failure)
        }
    }

    override fun requestUninstall(app: InstalledApp) {
        installedApps.requestUninstall(app).onFailure { failure ->
            showError("Android could not open uninstall", failure)
        }
    }

    override fun openSystemDestination(destination: SystemDestination) {
        val action = when (destination) {
            SystemDestination.HOME -> Settings.ACTION_HOME_SETTINGS
            SystemDestination.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SystemDestination.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SystemDestination.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SystemDestination.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SystemDestination.APP_DETAILS -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        runCatching {
            getApplication<Application>().startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { failure -> showError("Android Settings could not open", failure) }
    }

    override fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceMotion(enabled) }
    }

    override fun setDesktopGrain(enabled: Boolean) {
        viewModelScope.launch { preferences.setDesktopGrain(enabled) }
    }

    override fun setDockMagnification(enabled: Boolean) {
        viewModelScope.launch { preferences.setDockMagnification(enabled) }
    }

    override fun refreshPlatformState() {
        refreshStatusOnly()
        installedApps.refresh()
    }

    override fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun refreshStatusOnly() {
        val application = getApplication<Application>()
        val roleManager = application.getSystemService(RoleManager::class.java)
        val homeAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_HOME)
        val homeHeld = homeAvailable && roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        _uiState.value = _uiState.value.copy(
            homeRoleAvailable = homeAvailable,
            homeRoleHeld = homeHeld,
            systemStatus = readSystemStatus(application),
        )
    }

    private fun readSystemStatus(context: Context): SystemStatus {
        val batteryIntent = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val formatter = DateTimeFormatter.ofPattern("EEE d MMM  HH:mm", Locale.getDefault())
        return SystemStatus(
            clockLabel = LocalDateTime.now().format(formatter),
            batteryPercent = batteryPercent,
            isOnline = isOnline,
        )
    }

    private fun showError(message: String, failure: Throwable) {
        _uiState.value = _uiState.value.copy(
            message = "$message: ${failure.localizedMessage ?: "unknown error"}",
        )
    }

    override fun onCleared() {
        installedApps.stop()
    }

    class Factory(
        private val application: Application,
        private val initialProfile: DeviceProfile,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(StageShellViewModel::class.java))
            return StageShellViewModel(application, initialProfile) as T
        }
    }
}
