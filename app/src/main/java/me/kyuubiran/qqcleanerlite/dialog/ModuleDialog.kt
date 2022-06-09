@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package me.kyuubiran.qqcleanerlite.dialog

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.Observe
import com.github.kyuubiran.ezxhelper.utils.addModuleAssetPath
import com.github.kyuubiran.ezxhelper.utils.runOnMainThread
import me.kyuubiran.qqcleanerlite.BuildConfig
import me.kyuubiran.qqcleanerlite.R
import me.kyuubiran.qqcleanerlite.util.CleanManager
import me.kyuubiran.qqcleanerlite.util.ConfigManager
import me.kyuubiran.qqcleanerlite.util.getFormatCleanTime

class ModuleDialog(activity: Activity) : AlertDialog.Builder(activity) {
    companion object {
        val observer by lazy {
            Observe(false)
        }
    }

    init {
        activity.addModuleAssetPath()

        val prefsFragment = PrefsFragment()
        activity.fragmentManager.beginTransaction().add(prefsFragment, "Setting").commit()
        activity.fragmentManager.executePendingTransactions()

        prefsFragment.onActivityCreated(null)

        setView(prefsFragment.view)
        setTitle("瘦身模块")

        setPositiveButton("关闭", null)
        setNeutralButton("执行瘦身") { _, _ ->
            AlertDialog.Builder(activity).run {
                setTitle("注意")
                setMessage("确定要执行瘦身吗？")
                setPositiveButton("确定") { _, _ ->
                    ConfigManager.lastCleanTime = System.currentTimeMillis()
                    CleanManager.executeAll()
                    observer.value = !observer.value
                }
                setNegativeButton("取消", null)
                show()
            }
        }
        setCancelable(false)
    }

    class PrefsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

        private val onAutoCleanChanged by lazy {
            Observe(false)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.setting_dialog_prefs)


            (findPreference("enable_auto_clean") as CheckBoxPreference).apply {
                isChecked = ConfigManager.enableAutoClean
                onPreferenceChangeListener = this@PrefsFragment
            }

            findPreference("auto_clean_delay").apply {
                isEnabled = ConfigManager.enableAutoClean
                summary = if (ConfigManager.enableAutoClean) "${ConfigManager.autoCleanDelay} 小时" else "自动瘦身已关闭"
                onPreferenceClickListener = this@PrefsFragment
                onAutoCleanChanged.onValueChanged += {
                    runOnMainThread {
                        summary = if (it) "${ConfigManager.autoCleanDelay} 小时" else "自动瘦身已关闭"
                        isEnabled = it
                    }
                }
            }

            (findPreference("dont_show_clean_toast") as CheckBoxPreference).apply {
                isChecked = ConfigManager.enableAutoClean
                onPreferenceChangeListener = this@PrefsFragment
            }

            findPreference("keep_file_days").apply {
                summary = if (ConfigManager.keepFileDays > 0) "保留 ${ConfigManager.keepFileDays} 天以上的文件" else "关闭"
                onPreferenceClickListener = this@PrefsFragment
            }

            findPreference("module_version").apply {
                summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
            }

            findPreference("last_clean_time").apply {
                summary = if (ConfigManager.lastCleanTime > 0) getFormatCleanTime() else "还没有执行过清理哦~"
                observer.onValueChanged += { runOnMainThread { this.summary = getFormatCleanTime() } }
            }
        }

        override fun onPreferenceChange(p0: Preference?, p1: Any?): Boolean {
            when (p0?.key) {
                "enable_auto_clean" -> {
                    val b = p1 as Boolean
                    if (b == ConfigManager.enableAutoClean) return true
                    ConfigManager.enableAutoClean = b
                    onAutoCleanChanged.value = !onAutoCleanChanged.value
                }
                "dont_show_clean_toast" -> ConfigManager.dontShowCleanToast = p1 as Boolean
            }
            return true
        }

        private fun showEditTextDialog(
            title: String,
            hint: String,
            defaultValue: String,
            inputType: Int? = null,
            onConfirm: (String) -> Unit
        ) {
            AlertDialog.Builder(activity).run {
                setTitle(title)

                val et = EditText(activity).apply {
                    setHint(hint)
                    setText(defaultValue)
                    inputType?.let { this.inputType = it }

                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    this.layoutParams = lp
                }

                val ll = LinearLayout(activity).apply {
                    setPadding(20, 20, 20, 20)
                    addView(et)
                }

                setView(ll)

                setPositiveButton("确定") { _, _ ->
                    onConfirm(et.text.toString())
                }
                setNegativeButton("取消", null)
                show()
            }
        }

        override fun onPreferenceClick(p0: Preference?): Boolean {
            when (p0?.key) {
                "auto_clean_delay" -> showEditTextDialog(
                    "自动清理瘦身",
                    "请输入自动清理间隔(单位：小时)",
                    ConfigManager.autoCleanDelay.toString(),
                    InputType.TYPE_CLASS_NUMBER
                ) {
                    val i = it.toIntOrNull() ?: -1
                    if (i < 1) {
                        Log.e("不合法的数字或自动清理间隔不能小于1小时！")
                        return@showEditTextDialog
                    }
                    ConfigManager.autoCleanDelay = i
                    p0.summary = "$i 小时"
                    Log.toast("自动清理间隔已更新为 $i 小时")
                }
                "keep_file_days" -> showEditTextDialog("设置保留天数", "请输入保留天数", ConfigManager.keepFileDays.toString(), InputType.TYPE_CLASS_NUMBER) {
                    val i = it.toIntOrNull() ?: -1
                    if (i < 0) {
                        Log.toast("不合法的数字！")
                        return@showEditTextDialog
                    }
                    p0.summary = if (ConfigManager.keepFileDays > 0) "保留 ${ConfigManager.keepFileDays} 天以上的文件" else "关闭"
                    ConfigManager.keepFileDays = i
                    Log.toast("保留天数已更新为 $i 天")
                }
            }
            return true
        }
    }
}