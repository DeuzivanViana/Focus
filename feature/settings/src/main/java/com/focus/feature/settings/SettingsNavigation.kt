package com.focus.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE = "settings"
const val PRIVACY_ROUTE = "privacy"
const val LICENSES_ROUTE = "licenses"

fun NavGraphBuilder.settingsScreen(onOpenBlocker: () -> Unit, onOpenPrivacy: () -> Unit, onOpenLicenses: () -> Unit) {
    composable(SETTINGS_ROUTE) { SettingsRoute(onOpenBlocker = onOpenBlocker, onOpenPrivacy = onOpenPrivacy, onOpenLicenses = onOpenLicenses) }
}

fun NavGraphBuilder.privacyPolicyScreen(onBack: () -> Unit) {
    composable(PRIVACY_ROUTE) { PrivacyPolicyScreen(onBack = onBack) }
}

fun NavGraphBuilder.licensesScreen(onBack: () -> Unit) {
    composable(LICENSES_ROUTE) { LicensesRoute(onBack = onBack) }
}
