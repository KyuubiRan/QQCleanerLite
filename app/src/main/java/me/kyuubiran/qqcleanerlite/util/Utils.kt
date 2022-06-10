package me.kyuubiran.qqcleanerlite.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ContextThemeWrapper
import java.text.SimpleDateFormat

enum class HostAppType {
    QQ,
    TIM,
    WECHAT
}

lateinit var HOST_APP: HostAppType

val HostAppType.isQqOrTim: Boolean
    get() = this == HostAppType.QQ || this == HostAppType.TIM

val HostAppType.isWeChat: Boolean
    get() = this == HostAppType.WECHAT

fun HostAppType.validFor(s: String) = when {
    this == HostAppType.QQ && s.contains("qq") -> true
    this == HostAppType.TIM && s.contains("tim") -> true
    this == HostAppType.WECHAT && s.contains("wechat") -> true
    else -> false
}

fun getFormatCleanTime(): String = ConfigManager.lastCleanTime.let { if (it > 0) SimpleDateFormat.getInstance().format(it) else "还没有清理过哦~" }

val Context.wrapped: Context
    get() = ContextThemeWrapper(this, android.R.style.Theme_Material_Dialog_Alert)

fun Context.openUrl(uriString: String) {
    this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
}