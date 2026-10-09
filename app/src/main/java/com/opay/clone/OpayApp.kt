package com.opay.clone

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

class OpayApp : Application(), Application.ActivityLifecycleCallbacks {

    private var startedCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        // App coming to foreground from background
        if (startedCount == 0 && AppPrefs.isBackgrounded(this)) {
            AppPrefs.clearBackgrounded(this)

            // Skip the lock gate for these screens
            val simpleName = activity.javaClass.simpleName
            val skip = simpleName == "FingerprintActivity" ||
                       simpleName == "PasswordLoginActivity" ||
                       simpleName == "LoginActivity" ||
                       simpleName == "SignupActivity" ||
                       simpleName == "SplashActivity" ||
                       simpleName == "OnboardingActivity"

            if (!skip) {
                val i = Intent(activity, FingerprintActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                i.putExtra("lock_mode", true)
                activity.startActivity(i)
            }
        }
        startedCount++
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount--
        if (startedCount <= 0) {
            startedCount = 0
            // All activities stopped => app went to background
            AppPrefs.markBackgrounded(this)
        }
    }

    // Unused callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}