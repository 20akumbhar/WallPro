package com.aadevelopers.bestwalls

import android.app.Application
import com.aadevelopers.bestwalls.utils.AppOpenManager
import com.google.android.gms.ads.MobileAds


class WallPaperApp : Application() {
    private var appOpenManager: AppOpenManager? = null
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(
            this
        ) { }
        appOpenManager = AppOpenManager(this);
    }
}