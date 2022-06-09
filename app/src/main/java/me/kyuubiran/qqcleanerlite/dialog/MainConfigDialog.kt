@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package me.kyuubiran.qqcleanerlite.dialog

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.Log
import me.kyuubiran.qqcleanerlite.R
import me.kyuubiran.qqcleanerlite.data.CleanData
import me.kyuubiran.qqcleanerlite.util.CleanManager
import me.kyuubiran.qqcleanerlite.util.wrapped

class MainConfigDialog(activity: Activity) : AlertDialog.Builder(activity.wrapped) {

    init {
        setTitle("管理配置")

        val fragment = MainConfigFragment()
        activity.fragmentManager.beginTransaction().add(fragment, "Config").commit()
        activity.fragmentManager.executePendingTransactions()

        fragment.onActivityCreated(null)
        setView(fragment.view)

        setNeutralButton("导入", null)
        setPositiveButton("关闭", null)

        show().apply {
            getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {

            }
        }
    }

    class ConfigPreference(
        private val activity: Activity,
        private val category: PreferenceCategory,
        private val cleanData: CleanData
    ) : CheckBoxPreference(activity) {

        init {
            title = cleanData.title
            summary = "作者：${cleanData.author}"
            isChecked = cleanData.enable
        }

        override fun onClick() {
            fun tv(text: String): TextView = TextView(activity).apply {
                setText(text)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            AlertDialog.Builder(activity.wrapped).run {
                setTitle("操作")

                val toggle = tv(if (cleanData.enable) "禁用" else "启用").apply {
                    setOnClickListener {
                        cleanData.enable = !cleanData.enable
                        isChecked = cleanData.enable
                    }
                }

                val execute = tv("执行").apply {
                    setOnClickListener {
                        cleanData.pushToExecutionQueue(showFinishedToast = true)
                    }
                }

                val edit = tv("编辑").apply {
                    setOnClickListener { ModifyConfigDialog(activity, cleanData) }
                }

                val delete = tv("删除").apply {
                    setOnClickListener {
                        AlertDialog.Builder(activity.wrapped).run {
                            setTitle("确认")
                            setMessage("确认删除？")
                            setPositiveButton("确认") { _, _ ->
                                cleanData.delete()
                                category.removePreference(this@ConfigPreference)
                                Log.toast("删除成功！")
                            }
                            setNegativeButton("取消", null)
                            show()
                        }
                    }
                }

                val exportToDownload = tv("导出至下载文件夹").apply {
                    setOnClickListener {
                        cleanData.exportToDownload()
                        Log.toast("导出成功！")
                    }
                }

                val exportToClipboard = tv("导出至剪贴板").apply {
                    setOnClickListener {
                        cleanData.exportToClipboard()
                        Log.toast("导出成功！")
                    }
                }

                val ll = LinearLayout(activity).apply {
                    setPadding(20, 10, 20, 10)
                    addView(toggle)
                    addView(execute)
                    addView(edit)
                    addView(delete)
                    addView(exportToDownload)
                    addView(exportToClipboard)
                }

                setView(ll)
                show()
            }
        }
    }

    class MainConfigFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.config_main_prefs)

            (findPreference("config_list") as PreferenceCategory).apply cate@{
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
                        }
                    }

                    it.forEach { c -> addPreference(ConfigPreference(activity, this@cate, c)) }
                }
            }
        }

        override fun onPreferenceChange(p0: Preference?, p1: Any?): Boolean {
            return true
        }

        override fun onPreferenceClick(p0: Preference?): Boolean {
            return true
        }
    }
}
