package com.beam.app

import android.app.Application
import com.beam.app.di.initKoin
import com.beam.app.discovery.appContext

class BeamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        initKoin()
    }
}
