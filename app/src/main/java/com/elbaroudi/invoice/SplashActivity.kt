// SplashActivity.kt
package com.elbaroudi.invoice

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 3000 // Reduced to 2 seconds
    private lateinit var animatorSet: AnimatorSet

    // Declare views
    private lateinit var logoContainer: RelativeLayout
    private lateinit var appName: TextView
    private lateinit var tagline: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var rootLayout: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize views
        logoContainer = findViewById(R.id.logoContainer)
        appName = findViewById(R.id.appName)
        tagline = findViewById(R.id.tagline)
        loadingProgress = findViewById(R.id.loadingProgress)
        loadingText = findViewById(R.id.loadingText)
        rootLayout = findViewById(R.id.rootLayout)

        // Hide status bar for immersive experience
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setupAnimations()
        startAnimations()
        navigateToMain()
    }

    private fun setupAnimations() {
        // Initially hide all views
        logoContainer.alpha = 0f
        logoContainer.scaleX = 0f
        logoContainer.scaleY = 0f

        appName.alpha = 0f
        appName.translationY = 100f

        tagline.alpha = 0f
        tagline.translationY = 50f

        loadingProgress.alpha = 0f
        loadingText.alpha = 0f
    }

    private fun startAnimations() {
        animatorSet = AnimatorSet()

        // Logo animation - Scale and fade in with bounce (faster)
        val logoScaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0f, 1.2f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0f, 1.2f, 1f)
        val logoAlpha = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f)
        val logoRotation = ObjectAnimator.ofFloat(logoContainer, "rotation", 0f, 360f, 0f)

        logoScaleX.interpolator = BounceInterpolator()
        logoScaleY.interpolator = BounceInterpolator()
        logoAlpha.interpolator = AccelerateDecelerateInterpolator()
        logoRotation.interpolator = AccelerateDecelerateInterpolator()

        logoScaleX.duration = 700  // Reduced duration
        logoScaleY.duration = 700  // Reduced duration
        logoAlpha.duration = 500   // Reduced duration
        logoRotation.duration = 800 // Reduced duration

        // App name animation - Slide up and fade in (faster)
        val nameAlpha = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f)
        val nameTransY = ObjectAnimator.ofFloat(appName, "translationY", 100f, 0f)

        nameAlpha.interpolator = AccelerateDecelerateInterpolator()
        nameTransY.interpolator = OvershootInterpolator()

        nameAlpha.duration = 500   // Reduced duration
        nameTransY.duration = 600  // Reduced duration
        nameAlpha.startDelay = 200  // Reduced delay
        nameTransY.startDelay = 200 // Reduced delay

        // Tagline animation - Slide up and fade in (faster)
        val taglineAlpha = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f)
        val taglineTransY = ObjectAnimator.ofFloat(tagline, "translationY", 50f, 0f)

        taglineAlpha.interpolator = AccelerateDecelerateInterpolator()
        taglineTransY.interpolator = OvershootInterpolator()

        taglineAlpha.duration = 400  // Reduced duration
        taglineTransY.duration = 500  // Reduced duration
        taglineAlpha.startDelay = 400  // Reduced delay
        taglineTransY.startDelay = 400  // Reduced delay

        // Loading elements animation (faster)
        val loadingAlpha = ObjectAnimator.ofFloat(loadingProgress, "alpha", 0f, 1f)
        val loadingTextAlpha = ObjectAnimator.ofFloat(loadingText, "alpha", 0f, 1f)

        loadingAlpha.duration = 300  // Reduced duration
        loadingTextAlpha.duration = 300  // Reduced duration
        loadingAlpha.startDelay = 600  // Reduced delay
        loadingTextAlpha.startDelay = 600  // Reduced delay

        // Progress bar animation (faster)
        val progressAnimator = ValueAnimator.ofInt(0, 100)
        progressAnimator.duration = 1000  // Reduced duration
        progressAnimator.startDelay = 700  // Reduced delay
        progressAnimator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            loadingProgress.progress = progress
            loadingText.text = "جاري التحميل... $progress%"
        }

        // Combine all animations
        animatorSet.playTogether(
            logoScaleX, logoScaleY, logoAlpha, logoRotation,
            nameAlpha, nameTransY,
            taglineAlpha, taglineTransY,
            loadingAlpha, loadingTextAlpha
        )

        animatorSet.start()

        // Start progress animation immediately
        Handler(Looper.getMainLooper()).postDelayed({
            progressAnimator.start()
        }, 0)

        // Add floating animation to logo after initial animation
        Handler(Looper.getMainLooper()).postDelayed({
            startFloatingAnimation()
        }, 800)  // Reduced delay
    }

    private fun startFloatingAnimation() {
        val floatUp = ObjectAnimator.ofFloat(logoContainer, "translationY", 0f, -20f)
        val floatDown = ObjectAnimator.ofFloat(logoContainer, "translationY", -20f, 0f)

        floatUp.duration = 1000  // Reduced duration
        floatDown.duration = 1000  // Reduced duration

        floatUp.interpolator = AccelerateDecelerateInterpolator()
        floatDown.interpolator = AccelerateDecelerateInterpolator()

        val floatingSet = AnimatorSet()
        floatingSet.playSequentially(floatUp, floatDown)
        floatingSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                if (!isFinishing) {
                    animation.start() // Repeat the floating animation
                }
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        floatingSet.start()
    }

    private fun navigateToMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Fade out animation before navigation
            val fadeOut = ObjectAnimator.ofFloat(rootLayout, "alpha", 1f, 0f)
            fadeOut.duration = 300  // Reduced duration
            fadeOut.doOnEnd {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            fadeOut.start()
        }, splashTimeOut)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::animatorSet.isInitialized) {
            animatorSet.cancel()
        }
    }
}