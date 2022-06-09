package me.kyuubiran.qqcleanerlite.hook

import android.app.Application
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import me.kyuubiran.qqcleanerlite.util.CleanManager

object AppHook : BaseHook() {
    override fun init() {
        findMethod(Application::class.java) { name == "onCreate" }.hookAfter {
            EzXHelperInit.initAppContext(it.thisObject as Application, addPath = true, initModuleResources = true)
            CleanManager.initAutoClean
        }
    }
}