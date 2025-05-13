package com.example.groupproject

import android.app.Activity
import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * AdController is responsible for managing the logic of when ads should be shown.
 * It uses SharedPreferences to track usage data across sessions.
 */
class AdController(private val context: Context) {
    private val prefs = context.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE)

    /**
     * Called on app launch. Shows an ad if the app hasn't been opened yet today.
     * @param activity - the current activity context used to show the ad.
     */
    fun handleAppLaunch(activity: Activity) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val lastShown = prefs.getString("lastAppOpenDate", "")
        Log.d("AdManager", today)
        Log.d("AdManager", lastShown.toString())

        if (today != lastShown) {
            prefs.edit().putString("lastAppOpenDate", today).apply()
            // Load and then show the ad once it's ready
            AdManager.loadAd(context) {
                Log.d("AdManager", "Ad loaded callback received, showing ad")
                AdManager.showAd(activity) {}
            }
        } else {
            // Load an ad silently for later use
            AdManager.loadAd(context)
        }
    }

    /**
     * Checks if an ad should be shown before the user adds a course.
     * Shows an ad every third course added.
     * @param activity - current activity context.
     * @param onContinue - lambda function to call after ad or if no ad is needed.
     */
    fun showAdBeforeAddingCourse(activity: Activity, onContinue: () -> Unit) {
        val courseAdCount = prefs.getInt("coursesAdded", 0)
        Log.d("AdController", "Courses: $courseAdCount")
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
            // Skip ad and continue
            onContinue()
        }
    }

    /**
     * Checks if an ad should be shown before the user adds a task.
     * Shows an ad every third task added.
     * @param activity - current activity context.
     * @param onContinue - lambda function to call after ad or if no ad is needed.
     */
    fun showAdBeforeAddingTask(activity: Activity, onContinue: () -> Unit) {
        val taskAdCount = prefs.getInt("tasksAdded", 0)
        Log.d("AdController", "Tasks: $taskAdCount")
        if (taskAdCount == 2) {
            prefs.edit().putInt("tasksAdded", 0).apply()
            if (AdManager.isAdReady()) {
                AdManager.showAd(activity, onContinue)
            } else {
                AdManager.loadAd(context) {
                    AdManager.showAd(activity, onContinue)
                }
            }
        } else {
            onContinue()
        }
    }
}
