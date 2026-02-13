package com.moviehub.utils

import android.util.Log
import com.moviehub.BuildConfig


object AppLogger {

    private const val DEFAULT_TAG = "MovieHub"


    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.d(tag, message)
        }
    }


    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.i(tag, message)
        }
    }


    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.ENABLE_LOGGING) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }


    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {

        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun v(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.v(tag, message)
        }
    }


    fun isLoggingEnabled(): Boolean = BuildConfig.ENABLE_LOGGING


    fun separator(tag: String = DEFAULT_TAG) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.d(tag, "═══════════════════════════════════════════")
        }
    }


    fun methodStart(tag: String = DEFAULT_TAG, methodName: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.d(tag, "$methodName() started")
        }
    }


    fun methodEnd(tag: String = DEFAULT_TAG, methodName: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.d(tag, "$methodName() finished")
        }
    }
}