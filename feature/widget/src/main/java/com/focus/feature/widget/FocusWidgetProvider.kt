package com.focus.feature.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Home screen widget. Rendering is delegated to [WidgetUpdater]. */
@AndroidEntryPoint
class FocusWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var updater: WidgetUpdater

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updater.refresh()
    }
}
