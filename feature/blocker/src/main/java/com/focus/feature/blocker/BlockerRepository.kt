package com.focus.feature.blocker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.blockerStore: DataStore<Preferences> by preferencesDataStore(name = "blocker")

@Singleton
class BlockerRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.blockerStore
    private val ownPackage = context.packageName

    val settings: Flow<BlockerSettings> = store.data.map { p ->
        BlockerSettings(
            enabled = p[ENABLED] ?: false,
            mode = p[MODE]?.let { m -> BlockMode.entries.firstOrNull { it.name == m } } ?: BlockMode.WARN,
            packages = BlockerSettings.cleanPackages(p[PACKAGES].orEmpty()) - ownPackage,
        )
    }

    suspend fun setEnabled(enabled: Boolean) = store.edit { it[ENABLED] = enabled }

    suspend fun setMode(mode: BlockMode) = store.edit { it[MODE] = mode.name }

    suspend fun setBlocked(packageName: String, blocked: Boolean) = store.edit { p ->
        val current = p[PACKAGES].orEmpty()
        val next = if (blocked) current + packageName else current - packageName
        p[PACKAGES] = BlockerSettings.cleanPackages(next) - ownPackage
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val MODE = stringPreferencesKey("mode")
        val PACKAGES = stringSetPreferencesKey("packages")
    }
}
