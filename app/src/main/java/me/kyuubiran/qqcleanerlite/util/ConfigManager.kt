package me.kyuubiran.qqcleanerlite.util

import android.content.Context
import com.github.kyuubiran.ezxhelper.init.InitFields

object ConfigManager {
    private val prefs by lazy {
        InitFields.appContext.getSharedPreferences("qqcleanerlite", Context.MODE_PRIVATE)
    }

    var isFirstRun: Boolean
        get() = prefs.getBoolean("isFirstRun", true)
        set(value) = prefs.edit().putBoolean("isFirstRun", value).apply()

    var lastCleanTime: Long
        get() = prefs.getLong("lastCleanTime", 0)
        set(value) = prefs.edit().putLong("lastCleanTime", value).apply()

    var enableAutoClean: Boolean
        get() = prefs.getBoolean("enableAutoClean", false)
        set(value) = prefs.edit().putBoolean("enableAutoClean", value).apply()

    var autoCleanDelay: Int
        get() = prefs.getInt("autoCleanDelay", 24)
        set(value) = prefs.edit().putInt("autoCleanDelay", value).apply()

    var keepFileDays: Int
        get() = prefs.getInt("keepFileDays", 0)
        set(value) = prefs.edit().putInt("keepFileDays", value).apply()

    var dontShowCleanToast: Boolean
        get() = prefs.getBoolean("showCleanToast", false)
        set(value) = prefs.edit().putBoolean("showCleanToast", value).apply()

}