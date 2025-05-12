package com.example.groupproject

import android.app.Activity
import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class AdController(private val context: Context) {
    private val prefs = context.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE)

    fun handleAppLaunch(activity: Activity) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val lastShown = prefs.getString("lastAppOpenDate", "")
        Log.d("AdManager", today)
        Log.d("AdManager", lastShown.toString())

        if (today != lastShown) {
            prefs.edit().putString("lastAppOpenDate", today).apply()
            AdManager.loadAd(context) {
                Log.d("AdManager", "Ad loaded callback received, showing ad")
                AdManager.showAd(activity) {}
            }
        } else {
            AdManager.loadAd(context)
        }
    }

    fun showAdBeforeAddingCourse(activity: Activity, onContinue: () -> Unit) {
        val courseAdCount = prefs.getInt("coursesAdded", 0)
        if (courseAdCount == 2) {
            prefs.edit().putInt("coursesAdded", 0).apply()
            if (AdManager.isAdReady()) {
                AdManager.showAd(activity, onContinue)
            } else {
                AdManager.loadAd(context) {
                    AdManager.showAd(activity, onContinue)
                }
            }
        } else {
            prefs.edit().putInt("coursesAdded", (courseAdCount+1)).apply()
            onContinue()
        }
    }

    fun showAdBeforeAddingTask(activity: Activity, onContinue: () -> Unit) {
        val taskAdCount = prefs.getInt("tasksAdded", 0)
        if (taskAdCount == 2) {
            prefs.edit().putInt("tasks", 0).apply()
            if (AdManager.isAdReady()) {
                AdManager.showAd(activity, onContinue)
            } else {
                AdManager.loadAd(context) {
                    AdManager.showAd(activity, onContinue)
                }
            }
        } else {
            prefs.edit().putInt("tasksAdded", (taskAdCount+1)).apply()
            onContinue()
        }
    }
}
