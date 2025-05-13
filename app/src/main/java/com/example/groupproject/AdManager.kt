package com.example.groupproject

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError

/**
 * AdManager is a singleton object that handles the loading, readiness,
 * and displaying of interstitial ads using Google AdMob.
 */
object AdManager {
    private var ad: InterstitialAd? = null
    // Flag to prevent multiple simultaneous ad loading operations
    private var isLoading = false
    // Test Ad Unit ID
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Loads an interstitial ad if one is not already loaded or being loaded.
     * If a callback is passed, it is called after the ad is successfully loaded.
     * @param context - the application or activity context
     * @param onAdLoaded - optional callback invoked after ad is loaded
     */
    fun loadAd(context: Context, onAdLoaded: (() -> Unit)? = null) {
        if (isLoading || ad != null) {
            // Ad is already loaded or loading, call callback immediately if provided
            onAdLoaded?.invoke()
            return
        }

        isLoading = true
        val request = AdRequest.Builder().build() // Create an ad request
        InterstitialAd.load(context, AD_UNIT_ID, request, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(loadedAd: InterstitialAd) {
                super.onAdLoaded(loadedAd)
                ad = loadedAd // Store the loaded ad
                isLoading = false
                Log.d("AdManager", "Ad successfully loaded")
                onAdLoaded?.invoke() // Notify that ad is loaded
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                Log.d("AdManager", "Ad failed to load: ${error.message}")
                ad = null
                isLoading = false
                // We don't call the callback here since the ad wasn't successfully loaded
            }
        })
    }

    /**
     * Returns true if an ad is currently ready to be shown.
     */
    fun isAdReady(): Boolean = ad != null

    /**
     * Shows the interstitial ad if it is ready.
     * Sets up a FullScreenContentCallback to handle ad lifecycle events.
     * @param activity - the activity context used to display the ad
     * @param onAdFinished - function to call once the ad is dismissed or fails
     */
    fun showAd(activity: Activity, onAdFinished: () -> Unit) {
        Log.d("AdManager", "Trying to show ad. InterstitialAd is ${if (ad == null) "null" else "available"}")
        if (ad != null) {
            ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
                // Called when the ad is closed/dismissed
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdManager", "Ad dismissed")
                    ad = null
                    loadAd(activity)
                    onAdFinished()
                }

                // Called when the ad fails to show
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("AdManager", "Ad failed to show: ${adError.message}")
                    ad = null
                    onAdFinished()
                }

                override fun onAdClicked() {
                    Log.d("AdManager", "Ad clicked")
                }

                override fun onAdImpression() {
                    Log.d("AdManager", "Ad impression recorded")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("AdManager", "Ad showed full screen content")
                }
            }
            ad?.show(activity) // Display the ad
        } else {
            onAdFinished()
            loadAd(activity)
        }
    }
}