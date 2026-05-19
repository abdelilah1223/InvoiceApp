package com.elbaroudi.invoice

import ServicesFragment
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var interstitialAd: InterstitialAd? = null
    private var adShown = false
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views directly
        bottomNav = findViewById(R.id.bottom_nav)

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Check if ad was already shown in this session
        adShown = savedInstanceState?.getBoolean("AD_SHOWN") ?: false

        // Set up bottom navigation
        setupBottomNavigation()

        // Load ad if not shown yet
        if (!adShown) {
            loadInterstitialAd()
        } else {
            showMainContent()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("AD_SHOWN", adShown)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            "ca-app-pub-6107289363237127/7652055463",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    showInterstitialAd()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Ad failed to load, proceed to show main content
                    showMainContent()
                }
            }
        )
    }

    private fun showInterstitialAd() {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad was dismissed, mark as shown and proceed
                    adShown = true
                    showMainContent()
                    interstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Failed to show, proceed anyway
                    showMainContent()
                    interstitialAd = null
                }
            }
            ad.show(this)
        } ?: run {
            // No ad loaded, proceed
            showMainContent()
        }
    }

    private fun showMainContent() {
        findViewById<View>(R.id.ad_container).visibility = View.GONE
        findViewById<View>(R.id.fragment_container).visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE

        // Load default fragment if none is shown
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            loadFragment(HomeFragment<Any>())
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment: Any = when (item.itemId) {
                R.id.nav_home -> HomeFragment<Any>()
                R.id.nav_add -> AddFragment()
                R.id.nav_services ->ServicesFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment<Any>()
            }
            loadFragment(selectedFragment)
            true
        }
    }

    private fun loadFragment(fragment: Any) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment as Fragment)
            .commit()
    }
}