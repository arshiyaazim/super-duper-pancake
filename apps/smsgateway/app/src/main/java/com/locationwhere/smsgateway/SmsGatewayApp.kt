package com.locationwhere.smsgateway

import android.app.Application
import timber.log.Timber

class SmsGatewayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Production: optionally plant a CrashReportingTree here
    }
}
