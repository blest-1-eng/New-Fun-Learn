package com.example

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private var interstitialAd: InterstitialAd? = null
  private var isInterstitialShowing = false
  private var hasShownInitialAd = false
  private var lastAdClosedTime: Long = System.currentTimeMillis()

  private val primaryInterstitialUnitId = "ca-app-pub-5908506671976841/4727051710"
  private val fallbackInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Google Mobile Ads SDK with child-directed settings (Google Play Families Policy compliant)
    MobileAds.initialize(this) {}
    val requestConfig = RequestConfiguration.Builder()
      .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
      .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
      .build()
    MobileAds.setRequestConfiguration(requestConfig)

    // Preload the first interstitial ad
    loadInterstitialAd()

    // Run a single controlled timer loop:
    // 1. Initial ad on app launch (strictly once)
    // 2. Then strictly every 30 seconds of active gameplay (never triggered by clicks)
    lifecycleScope.launch {
      // Allow 3 seconds for the app's first screen to render
      delay(3000)
      if (!hasShownInitialAd && interstitialAd != null) {
        hasShownInitialAd = true
        interstitialAd?.show(this@MainActivity)
      }

      // Check periodically for the 30-second interval
      while (isActive) {
        delay(3000)
        val now = System.currentTimeMillis()
        val elapsedSinceLastAd = now - lastAdClosedTime

        // Only show if at least 30 full seconds have passed since the last ad was closed,
        // and the app is actively on screen (RESUMED)
        if (elapsedSinceLastAd >= 30_000L &&
            !isInterstitialShowing &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
          if (interstitialAd != null) {
            interstitialAd?.show(this@MainActivity)
          } else {
            loadInterstitialAd()
          }
        }
      }
    }

    setContent {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          KidsPlayWebApp()
        }
        BannerAdView(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        )
      }
    }
  }

  private fun loadInterstitialAd(useFallback: Boolean = false) {
    val unitId = if (useFallback) fallbackInterstitialUnitId else primaryInterstitialUnitId
    val adRequest = AdRequest.Builder().build()

    InterstitialAd.load(
      this,
      unitId,
      adRequest,
      object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
          interstitialAd = ad
          android.util.Log.d("AdMob", "Interstitial ad loaded successfully ($unitId)!")
          setupFullScreenCallback(ad)

          // Show initial ad once if ready and not yet shown
          if (!hasShownInitialAd && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            hasShownInitialAd = true
            interstitialAd?.show(this@MainActivity)
          }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
          interstitialAd = null
          android.util.Log.e(
            "AdMob",
            "Interstitial ad failed to load ($unitId): code=${loadAdError.code}, message=${loadAdError.message}"
          )
          // If primary unit returned no-fill/not ready, fallback to sample unit so user always gets working ads
          if (!useFallback) {
            loadInterstitialAd(useFallback = true)
          }
        }
      }
    )
  }

  private fun setupFullScreenCallback(ad: InterstitialAd) {
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdShowedFullScreenContent() {
        isInterstitialShowing = true
      }

      override fun onAdDismissedFullScreenContent() {
        isInterstitialShowing = false
        // Start 30-second cooldown timer from the moment the ad is closed
        lastAdClosedTime = System.currentTimeMillis()
        interstitialAd = null
        // Preload next ad in background so it's ready for the next 30-second window
        loadInterstitialAd()
      }

      override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
        isInterstitialShowing = false
        lastAdClosedTime = System.currentTimeMillis()
        interstitialAd = null
        loadInterstitialAd()
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KidsPlayWebApp(modifier: Modifier = Modifier) {
  var webViewRef: WebView? = remember { null }

  BackHandler {
    if (webViewRef?.canGoBack() == true) {
      webViewRef?.goBack()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("kids_play_webview_container")
  ) {
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .testTag("kids_play_webview"),
      factory = { context ->
        WebView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          setBackgroundColor(Color.TRANSPARENT)
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
          }
          webChromeClient = WebChromeClient()
          webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
            }
          }
          loadUrl("file:///android_asset/index.html")
          webViewRef = this
        }
      },
      update = {
        webViewRef = it
      }
    )
  }
}

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
  var adViewRef: AdView? = remember { null }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> adViewRef?.pause()
        Lifecycle.Event.ON_RESUME -> adViewRef?.resume()
        Lifecycle.Event.ON_DESTROY -> adViewRef?.destroy()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      adViewRef?.destroy()
    }
  }

  // Persistent bottom banner container
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(50.dp)
      .background(androidx.compose.ui.graphics.Color.White)
      .testTag("admob_banner_container"),
    contentAlignment = Alignment.Center
  ) {
    AndroidView(
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("admob_banner_view"),
      factory = { context ->
        AdView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          setAdSize(AdSize.BANNER)
          // Primary: User's real AdMob Banner Ad Unit ID
          adUnitId = "ca-app-pub-5908506671976841/9587234376"
          adListener = object : AdListener() {
            override fun onAdLoaded() {
              android.util.Log.d("AdMob", "Banner ad loaded successfully!")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
              android.util.Log.e(
                "AdMob",
                "Banner ad failed to load: code=${error.code}, message=${error.message}"
              )
              // If real unit is still propagating / unfulfilled, switch to official sample banner
              // so the user has a banner visible at the bottom every time
              if (adUnitId != "ca-app-pub-3940256099942544/6300978111") {
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
              }
            }
          }
          loadAd(AdRequest.Builder().build())
          adViewRef = this
        }
      },
      update = {
        adViewRef = it
      }
    )
  }
}

