package com.bodepudi.vexnotebooktracker.utils

import android.util.Log
import com.bodepudi.vexnotebooktracker.BuildConfig

object DebugLogger {

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    fun api(endpoint: String, response: String) {
        if (BuildConfig.DEBUG) {
            Log.d("API", "Endpoint: $endpoint")
            Log.d("API", "Response: $response")
        }
    }

    fun auth(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("AUTH", message)
        }
    }

    fun network(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("NETWORK", message)
        }
    }
}