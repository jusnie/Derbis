package com.yasuda.derbis.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class DerbisWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        DerbisWidgetCenter.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> DerbisWidgetActions.toggle(this, context)
            ACTION_NEXT -> DerbisWidgetActions.skip(this, context, forward = true)
            ACTION_PREVIOUS -> DerbisWidgetActions.skip(this, context, forward = false)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.yasuda.derbis.widget.TOGGLE"
        const val ACTION_NEXT = "com.yasuda.derbis.widget.NEXT"
        const val ACTION_PREVIOUS = "com.yasuda.derbis.widget.PREVIOUS"
    }
}
