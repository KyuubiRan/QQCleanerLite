package me.kyuubiran.qqcleanerlite.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.moduleRes
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.Log.logeIfThrow
import com.github.kyuubiran.ezxhelper.utils.getBooleanOrDefault
import com.github.kyuubiran.ezxhelper.utils.getJSONArrayOrEmpty
import com.github.kyuubiran.ezxhelper.utils.getStringOrDefault
import me.kyuubiran.qqcleanerlite.util.CleanManager
import me.kyuubiran.qqcleanerlite.util.CleanManager.getConfigDir
import me.kyuubiran.qqcleanerlite.util.CleanManager.pool
import me.kyuubiran.qqcleanerlite.util.HOST_APP
import me.kyuubiran.qqcleanerlite.util.isQqOrTim
import me.kyuubiran.qqcleanerlite.util.validFor
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL


class CleanData(private val jsonObject: JSONObject) : Serializable, Cloneable {

    class PathData(private val jsonObject: JSONObject) {

        class Path {
            private val jsonObject: JSONObject

            constructor(jsonObject: JSONObject) {
                this.jsonObject = jsonObject
            }

            constructor(jsonString: String) : this(JSONObject(jsonString))

            constructor(prefix: String, suffix: String) {
                this.jsonObject = JSONObject().put("prefix", prefix).put("suffix", suffix)
            }

            var prefix: String
                set(value) {
                    jsonObject.put("prefix", value)
                }
                get() = jsonObject.getStringOrDefault("prefix")

            var suffix: String
                set(value) {
                    jsonObject.put("suffix", value)
                }
                get() = jsonObject.getStringOrDefault("suffix")

            override fun toString(): String {
                return jsonObject.toString()
            }

            fun toFormatString(indentSpaces: Int = 2): String {
                return jsonObject.toString(indentSpaces)
            }
        }

        // 标题
        var title: String
            set(value) {
                jsonObject.put("title", value)
            }
            get() = jsonObject.getStringOrDefault("title", "一个没有名字的配置文件")

        // 是否启用
        var enable: Boolean
            set(value) {
                jsonObject.put("enable", value)
            }
            get() = jsonObject.getBooleanOrDefault("enable", false)

        // 路径
        val pathList = jsonObject.getJSONArrayOrEmpty("path").run {
            Log.i("Load path list of $title")
            arrayListOf<Path>().apply {
                for (i in 0 until this@run.length()) {
                    runCatching {
                        add(Path(this@run.getJSONObject(i)))
                    }.logeIfThrow("Load path list of $title failed") {
                        enable = false
                        Log.toast("$title 加载失败，请检查配置文件是否合法！")
                        return@apply
                    }
                }
            }
        }

        // 添加路径
        fun addPath(path: Path) {
            pathList.add(path)
        }

        //删除路径
        fun removePath(idx: Int) = runCatching {
            pathList.removeAt(idx)
        }.logeIfThrow()

        //删除路径
        fun removePath(path: Path) {
            pathList.remove(path)
        }

        override fun toString(): String = jsonObject.toString()

        fun toFormatString(indentSpaces: Int = 2): String = jsonObject.toString(indentSpaces)
    }

    private var file: File? = null

    // 配置文件标题
    var title: String
        set(value) {
            jsonObject.put("title", value)
        }
        get() = jsonObject.getStringOrDefault("title", "一个没有名字的配置文件")

    // 作者
    var author: String
        set(value) {
            jsonObject.put("author", value)
        }
        get() = jsonObject.getStringOrDefault("author", "无名氏")

    // 是否启用
    var enable: Boolean
        set(value) {
            jsonObject.put("enable", value)
        }
        get() = jsonObject.getBooleanOrDefault("enable", false)

    // 宿主类型
    var hostApp: String
        set(value) {
            jsonObject.put("hostApp", value)
        }
        get() = jsonObject.getStringOrDefault("hostApp")

    val valid: Boolean
        get() = HOST_APP.validFor(hostApp)

    // 内容
    val content = jsonObject.getJSONArrayOrEmpty("content").run {
        arrayListOf<PathData>().apply {
            for (i in 0 until this@run.length()) {
                add(PathData(this@run.getJSONObject(i)))
            }
        }
    }

    fun addContent(pathData: PathData) {
        content.add(pathData)
    }

    fun removeContent(idx: Int) {
        content.removeAt(idx)
    }

    fun removeContent(pathData: PathData) {
        content.remove(pathData)
    }

    override fun toString(): String {
        return jsonObject.toString()
    }

    /**
     * 格式化的JSONString
     * @param indentSpaces 缩进
     */
    fun toFormatString(indentSpaces: Int = 2): String {
        return jsonObject.toString(indentSpaces)
    }

    /**
     * 复制到剪切板
     */
    fun exportToClipboard() {
        (appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).run {
            setPrimaryClip(ClipData.newPlainText(title, toFormatString()))
        }
    }

    /**
     * 导出配置文件到下载目录
     */
    fun exportToDownload() {
        val f = File("/storage/emulated/0/Download/${this.title}.json")
        if (!f.exists()) f.createNewFile()
        f.writeText(this.toFormatString())
    }

    /**
     * 将配置文件推至队列执行
     */
    fun pushToExecutionQueue(showToast: Boolean = true, showFinishedToast: Boolean = false) {
        CleanManager.execute(this, showToast, true)
        if (showToast && showFinishedToast) pool.execute { Log.toast("执行完毕") }
    }

    /**
     * 保存配置文件 一般在返回的时候调用
     */
    @Synchronized
    fun save() {
        file?.let {
            if (!it.exists()) it.createNewFile()
            it.writeText(toFormatString())
            return
        }
        file = File("${getConfigDir().path}/${title}.json").apply {
            if (!exists()) createNewFile()
            writeText(toFormatString())
        }
    }

    /**
     * 删除配置文件
     */
    @Synchronized
    fun delete() {
        file?.let {
            if (it.exists()) it.delete()
        }
    }

    companion object {
        @JvmStatic
        fun fromJson(jsonString: String): CleanData {
            return CleanData(JSONObject(jsonString))
        }

        @JvmStatic
        fun fromClipboard(alsoSave: Boolean = true): CleanData? {
            (appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).run {
                primaryClip?.let { clipData ->
                    if (clipData.itemCount > 0) {
                        clipData.getItemAt(0).text.run {
                            return runCatching { fromJson(this.toString()) }.getOrNull()?.also {
                                if (alsoSave) it.save()
                            }
                        }
                    }
                }
            }
            return null
        }

        @JvmStatic
        fun fromGithub(link: String): CleanData {
            val url = URL(
                link.replace("www.github.com", "raw.githubusercontent.com")
                    .replace("github.com", "raw.githubusercontent.com")
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connect()
            val inputStream = conn.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
            conn.disconnect()
            return fromJson(sb.toString())
        }

        @JvmStatic
        fun createDefaultCleanData(): CleanData {
            moduleRes.assets.open(
                "${if (HOST_APP.isQqOrTim) "qq" else "wechat"}.json"
            ).use {
                return fromJson(it.bufferedReader().readText())
            }
        }
    }

    constructor(jsonFile: File) : this(JSONObject(jsonFile.readText())) {
        file = jsonFile
    }
}