package com.example.myapplication

import android.app.Application

class App : Application() {
    companion object {
        var wasAppRestarted = false
    }

    override fun onCreate() {
        super.onCreate()
        wasAppRestarted = true
    }
}