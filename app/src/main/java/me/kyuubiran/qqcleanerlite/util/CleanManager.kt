package me.kyuubiran.qqcleanerlite.util

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.Log.logeIfThrow
import com.github.kyuubiran.ezxhelper.utils.mainHandler
import me.kyuubiran.qqcleanerlite.data.CleanData
import me.kyuubiran.qqcleanerlite.util.path.CommonPath
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object CleanManager {
    private val pool = ThreadPoolExecutor(1, 1, 5L, TimeUnit.MINUTES, LinkedBlockingQueue(256))

    fun execute(data: CleanData, showToast: Boolean = true, forceExec: Boolean = false) {
        if (!data.valid) return
        if (!data.enable && !forceExec) return
        pool.execute e@{
            if (showToast) Log.toast("正在执行 ${data.title}")
            runCatching {
                data.content.forEach f@{ data ->
                    if (!data.enable) return@f
                    data.pathList.forEach { path -> deleteAll(PathParser.getFullPath(path)) }
                }
            }.logeIfThrow("Execute failed, skipped: ${data.title}") {
                if (showToast) Log.toast("执行 ${data.title} 时发生错误，已跳过剩余部分")
            }
        }
    }

    fun executeAll(showToast: Boolean = !ConfigManager.dontShowCleanToast) {
        if (showToast) Log.toast("开始执行瘦身...")
        ConfigManager.lastCleanTime = System.currentTimeMillis()
        getAllConfigsAsync {
            if (it.isEmpty() || it.all { c -> !c.enable }) {
                Log.toast("没有可执行的瘦身配置")
                return@getAllConfigsAsync
            }

            it.forEach { data -> execute(data, showToast) }
            pool.execute { if (showToast) Log.toast("执行完毕") }
        }
    }

    private fun deleteAll(path: String) {
        deleteAll(f = File(path))
    }

    private fun deleteAll(
        f: File,
        keepTime: Long = ConfigManager.keepFileDays.coerceIn(0..365) * 24 * 60 * 60 * 1000L,
        ts: Long = System.currentTimeMillis()
    ) {
        runCatching {
            if (!f.exists()) return
            if (f.isFile && ts - f.lastModified() > keepTime) f.delete()
            else f.listFiles()?.forEach { deleteAll(it, keepTime, ts) } ?: f.delete()
        }.logeIfThrow()
    }

    fun getConfigDir(): File {
        val path = "${CommonPath.publicData.second}/qqcleaner"
        val f = File(path)
        if (f.exists()) return f
        f.mkdir()
        return f
    }

    fun getAllConfigsAsync(onFinish: (List<CleanData>) -> Unit) = thread {
        onFinish(getAllConfigs())
    }

    fun getAllConfigs(): List<CleanData> {
        val arr = ArrayList<CleanData>()
        runCatching {
            getConfigDir().listFiles()?.forEach { f -> runCatching { arr.add(CleanData(f)) }.logeIfThrow() }
        }.logeIfThrow()
        return arr
    }

    fun isConfigEmpty(): Boolean = getConfigDir().listFiles()?.isEmpty() ?: true

    private object AutoClean : Runnable {
        override fun run() {
            if (!ConfigManager.enableAutoClean) return
            if ((ConfigManager.lastCleanTime + ConfigManager.autoCleanDelay * 3600L) - System.currentTimeMillis() <= 0) return
            executeAll()
            mainHandler.postDelayed(this, 30000L)
        }
    }

    val initAutoClean = mainHandler.postDelayed(AutoClean, 30000)
}