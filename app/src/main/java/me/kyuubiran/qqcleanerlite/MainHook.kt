package me.kyuubiran.qqcleanerlite

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.Log.logexIfThrow
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.kyuubiran.qqcleanerlite.hook.AppHook
import me.kyuubiran.qqcleanerlite.hook.BaseHook
import me.kyuubiran.qqcleanerlite.hook.EntryHook
import me.kyuubiran.qqcleanerlite.util.HOST_APP
import me.kyuubiran.qqcleanerlite.util.HostAppType

private val PACKAGE_NAME_HOOKED = listOf("com.tencent.mobileqq", "com.tencent.tim", "com.tencent.mm")
private const val TAG = "QQCleanerLite"

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in PACKAGE_NAME_HOOKED) return
        if (lpparam.packageName != lpparam.processName) return
        HOST_APP = when (lpparam.packageName) {
            "com.tencent.mobileqq" -> HostAppType.QQ
            "com.tencent.mm" -> HostAppType.WECHAT
            "com.tencent.tim" -> HostAppType.TIM
            else -> return
        }
        EzXHelperInit.initHandleLoadPackage(lpparam)
        EzXHelperInit.setLogTag(TAG)
        EzXHelperInit.setToastTag(TAG)
        initHooks(AppHook, EntryHook)
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    private fun initHooks(vararg hook: BaseHook) {
        hook.forEach {
            runCatching {
                if (it.isInit) return@forEach
                it.init()
                it.isInit = true
                Log.i("Inited hook: ${it.javaClass.simpleName}")
            }.logexIfThrow("Failed init hook: ${it.javaClass.simpleName}")
        }
    }
}
