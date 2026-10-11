package com.pocketwin.voice

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock

/** Executes an [Action] via system intents. Returns an error message to speak, or null on success. */
class ActionRunner(private val context: Context) {

    fun run(action: Action): String? = try {
        when (action) {
            is Action.SetTimer -> launch(
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
            ) ?: "No clock app can set a timer."
            is Action.WebSearch -> launch(
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, action.query),
            ) ?: "No app can search the web."
            is Action.OpenApp -> openApp(action.name)
        }
    } catch (e: Exception) {
        "That didn't work: ${e.message}"
    }

    private fun openApp(name: String): String? {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val target = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .filter { it.loadLabel(pm).toString().lowercase().let { l -> l == name || l.contains(name) } }
            .minByOrNull { it.loadLabel(pm).length }
            ?: return "I couldn't find an app called $name."
        val intent = pm.getLaunchIntentForPackage(target.activityInfo.packageName)
            ?: return "I can't open $name."
        return launch(intent)?.let { "I can't open $name." }
    }

    /** Returns null when launched, or a non-null marker when no activity could handle it. */
    private fun launch(intent: Intent): String? {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return "unresolved"
        context.startActivity(intent)
        return null
    }
}
