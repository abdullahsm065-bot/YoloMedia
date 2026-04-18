package com.yolomedia

import android.app.Application

class YoloMediaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: YoloMediaApp
            private set
    }
}
