package com.example.groupproject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError

object AdManager {
    private var ad: InterstitialAd? = null
    private var isLoading = false
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

//    fun loadAd(context: Context) {
//        if (isLoading || ad != null) return
//
//        isLoading = true
//        val request = AdRequest.Builder().build()
//        InterstitialAd.load(context, AD_UNIT_ID, request, object : InterstitialAdLoadCallback() {
//            override fun onAdLoaded(loadedAd: InterstitialAd) {
//                super.onAdLoaded(loadedAd)
//                ad = loadedAd
//                isLoading = false
//                Log.d("AdManager", "Ad successfully loaded")
//            }
//
//            override fun onAdFailedToLoad(error: LoadAdError) {
//                super.onAdFailedToLoad(error)
//                Log.d("AdManager", "Ad failed to load: ${error.message}")
//                ad = null
//                isLoading = false
//            }
//        })
//    }

    fun loadAd(context: Context, onAdLoaded: (() -> Unit)? = null) {
        if (isLoading || ad != null) {
            // Ad is already loaded or loading, call callback immediately if provided
            onAdLoaded?.invoke()
            return
        }

        isLoading = true
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, AD_UNIT_ID, request, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(loadedAd: InterstitialAd) {
                super.onAdLoaded(loadedAd)
                ad = loadedAd
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

    fun isAdReady(): Boolean = ad != null

    fun showAd(activity: Activity, onAdFinished: () -> Unit) {
        Log.d("AdManager", "Trying to show ad. InterstitialAd is ${if (ad == null) "null" else "available"}")
        if (ad != null) {
            ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdManager", "Ad dismissed")
                    ad = null
                    loadAd(activity)
                    onAdFinished()
                }

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
            ad?.show(activity)
        } else {
            onAdFinished()
            loadAd(activity)
        }
    }
}