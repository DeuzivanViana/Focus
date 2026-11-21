package com.focus.feature.blocker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [usageRestricted]: Android likely blocks the Usage Access switch (Restricted settings). */
data class Permissions(val usageAccess: Boolean = false, val blockScreen: Boolean = false, val usageRestricted: Boolean = false)

data class BlockerUiState(
    val settings: BlockerSettings = BlockerSettings(),
    val permissions: Permissions = Permissions(),
    val apps: List<InstalledApp>? = null,
)

@HiltViewModel
class BlockerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BlockerRepository,
) : ViewModel() {

    private val permissions = MutableStateFlow(readPermissions())
    private val apps = MutableStateFlow<List<InstalledApp>?>(null)

    val uiState: StateFlow<BlockerUiState> =
        combine(repository.settings, permissions, apps) { s, p, a -> BlockerUiState(s, p, a) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockerUiState(permissions = permissions.value))

    init {
        viewModelScope.launch { apps.value = runCatching { loadLaunchableApps(context) }.getOrDefault(emptyList()) }
    }

    /** Called on resume: the user may have just changed a permission in system settings. */
    fun refreshPermissions() {
        permissions.value = readPermissions()
    }

    fun setEnabled(enabled: Boolean) = viewModelScope.launch { repository.setEnabled(enabled) }
    fun setMode(mode: BlockMode) = viewModelScope.launch { repository.setMode(mode) }
    fun setBlocked(packageName: String, blocked: Boolean) = viewModelScope.launch { repository.setBlocked(packageName, blocked) }

    private fun readPermissions() = Permissions(
        usageAccess = BlockerPermissions.hasUsageAccess(context),
        blockScreen = BlockerPermissions.canShowBlockScreen(context),
        usageRestricted = BlockerPermissions.usageAccessMayBeRestricted(context),
    )
}
