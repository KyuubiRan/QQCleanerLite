@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package me.kyuubiran.qqcleanerlite.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.runOnMainThread
import me.kyuubiran.qqcleanerlite.R
import me.kyuubiran.qqcleanerlite.data.CleanData
import me.kyuubiran.qqcleanerlite.util.CleanManager
import me.kyuubiran.qqcleanerlite.util.Shared
import me.kyuubiran.qqcleanerlite.util.wrapped

class MainConfigDialog(activity: Activity) : AlertDialog.Builder(activity.wrapped) {

    init {
        setTitle("管理配置")

        val fragment = MainConfigFragment()
        activity.fragmentManager.beginTransaction().add(fragment, "Config").commit()
        activity.fragmentManager.executePendingTransactions()

        fragment.onActivityCreated(null)
        setView(fragment.view)

        setPositiveButton("关闭", null)

        show()
    }

    class ConfigPreference(
        private val activity: Activity,
        private val category: PreferenceCategory,
        private val cleanData: CleanData
    ) : CheckBoxPreference(activity), OnClickListener {

        init {
            title = cleanData.title
            summary = "作者：${cleanData.author}"
            isChecked = cleanData.enable
        }

        private fun tv(text: String): TextView = TextView(activity).apply {
            setText(text)
            setPadding(0, 20, 0, 20)
            textSize = 17.0f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        private lateinit var toggle: TextView
        private lateinit var execute: TextView
        private lateinit var edit: TextView
        private lateinit var delete: TextView
        private lateinit var exportToDownload: TextView
        private lateinit var exportToClipboard: TextView

        private val manageDialog by lazy {
            toggle = tv(if (cleanData.enable) "禁用" else "启用")
            execute = tv("执行")
            edit = tv("编辑")
            delete = tv("删除")
            exportToDownload = tv("导出至下载文件夹")
            exportToClipboard = tv("导出至剪贴板")

            AlertDialog.Builder(activity.wrapped).run {
                setTitle("操作")

                val ll = LinearLayout(activity).apply {
                    setPadding(80, 50, 50, 40)
                    orientation = LinearLayout.VERTICAL

                    addView(toggle)
                    addView(execute)
                    addView(edit)
                    addView(delete)
                    addView(exportToDownload)
                    addView(exportToClipboard)
                }

                setView(ll)
                create().apply d@{
                    toggle.setOnClickListener(this@ConfigPreference)
                    execute.setOnClickListener(this@ConfigPreference)
                    edit.setOnClickListener(this@ConfigPreference)
                    delete.setOnClickListener(this@ConfigPreference)
                    exportToDownload.setOnClickListener(this@ConfigPreference)
                    exportToClipboard.setOnClickListener(this@ConfigPreference)
                }
            }
        }

        override fun onClick() {
            manageDialog.show()
            toggle.text = if (cleanData.enable) "禁用" else "启用"
        }

        override fun onClick(p0: View?) {
            when (p0) {
                toggle -> {
                    cleanData.enable = !cleanData.enable
                    cleanData.save()
                    isChecked = !isChecked
                    manageDialog.dismiss()
                }
                execute -> {
                    cleanData.pushToExecutionQueue(showFinishedToast = true)
                    manageDialog.dismiss()
                }
                edit -> {
                    Shared.currentModify = cleanData
                    Log.i("Current select : ${cleanData.title}")
                    ModifyConfigDialog(activity)
                    manageDialog.dismiss()
                }
                delete -> {
                    AlertDialog.Builder(activity.wrapped).run {
                        setTitle("注意")
                        setMessage("你真的要删除配置： ${cleanData.title} 吗？")
                        setPositiveButton("确认") { _, _ ->
                            cleanData.delete()
                            category.removePreference(this@ConfigPreference)
                            Log.toast("删除成功！")
                            manageDialog.dismiss()
                        }
                        setNegativeButton("取消") { _, _ ->
                            manageDialog.dismiss()
                        }
                        show()
                    }
                }
                exportToDownload -> {
                    cleanData.exportToDownload()
                    Log.toast("导出成功！")
                    manageDialog.dismiss()
                }
                exportToClipboard -> {
                    cleanData.exportToClipboard()
                    Log.toast("导出成功！")
                    manageDialog.dismiss()
                }
            }
        }
    }

    class MainConfigFragment : PreferenceFragment(),
        Preference.OnPreferenceClickListener {

        private lateinit var configList: PreferenceCategory

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.config_main_prefs)
            configList = (findPreference("config_list") as PreferenceCategory).apply cate@{
                CleanManager.getAllConfigsAsync {
                    if (it.isEmpty()) {

                        AlertDialog.Builder(activity.wrapped).run {
                            setTitle("提示")
                            setMessage("没有可用的配置文件，是否创建默认的配置文件？")
                            setPositiveButton("是") { _, _ ->
                                val data = CleanData.createDefaultCleanData()
                                data.save()
                                addPreference(ConfigPreference(activity, this@cate, data))
                            }
                            setNegativeButton("否", null)
                            setCancelable(false)

                            runOnMainThread { show() }
                        }
                    }

                    runOnMainThread {
                        it.forEach { c -> addPreference(ConfigPreference(activity, this@cate, c)) }
                    }
                }
            }
//            findPreference("from_file").apply {
//                onPreferenceClickListener = this@MainConfigFragment
//                isEnabled = false
//                summary = "暂不支持此操作"
//            }
            findPreference("from_clipboard").apply {
                onPreferenceClickListener = this@MainConfigFragment
            }
        }

        override fun onPreferenceClick(p0: Preference?): Boolean {
            when (p0?.key) {
//                "from_file" -> {
//                    val intent = Intent(Intent.ACTION_GET_CONTENT)
//                    intent.type = "application/json"
//                    startActivityForResult(intent, 1)
//                }
                "from_clipboard" -> {
                    val data = CleanData.fromClipboard() ?: run {
                        Log.toast("导入失败！请检查剪切板中的配置是否正确！")
                        return true
                    }

                    configList.addPreference(ConfigPreference(activity, configList, data))
                    Log.toast("导入成功！")
                }
            }
            return true
        }
    }
}
