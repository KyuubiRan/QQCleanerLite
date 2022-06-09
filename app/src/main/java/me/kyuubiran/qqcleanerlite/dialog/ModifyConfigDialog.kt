@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package me.kyuubiran.qqcleanerlite.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import com.github.kyuubiran.ezxhelper.utils.Log
import me.kyuubiran.qqcleanerlite.R
import me.kyuubiran.qqcleanerlite.data.CleanData
import me.kyuubiran.qqcleanerlite.util.wrapped

class ModifyConfigDialog(activity: Activity, private val cleanData: CleanData) :
    AlertDialog.Builder(activity.wrapped) {

    init {
        setTitle("编辑配置")

        val fragment = ModifyConfigFragment()
        activity.fragmentManager.beginTransaction().add(fragment, "Modify").commit()
        activity.fragmentManager.executePendingTransactions()

        val bundle = Bundle()
        bundle.putSerializable("cleanData", cleanData)
        fragment.arguments = bundle

        fragment.onActivityCreated(null)
        setView(fragment.view)

        setCancelable(false)
        setNeutralButton("放弃更改", null)

        setPositiveButton("保存") { _, _ ->
            cleanData.save()
            Log.toast("保存成功！")
        }
    }

    class PathPreference(
        ctx: Context,
        private val path: CleanData.PathData
    ) : CheckBoxPreference(ctx) {
        init {
            isChecked = path.enable
        }

        override fun onClick() {
            super.onClick()
            path.enable = isChecked
        }
    }

    class ModifyConfigFragment : PreferenceFragment() {
        private lateinit var cleanData: CleanData

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.config_modify_prefs)

            arguments.getSerializable("cleanData")?.let {
                cleanData = it as CleanData
            } ?: run { Log.toast("无法读取配置文件，请检查配置文件是否正确！");return }

            findPreference("title").apply {
                summary = cleanData.title
            }

            findPreference("author").apply {
                summary = cleanData.author
            }

            (findPreference("options") as PreferenceCategory).apply {
                if (!cleanData.valid) {
                    Log.toast("无效的配置文件！")
                    return
                }

                cleanData.content.forEach {
                    val preference = PathPreference(activity, it)
                    addPreference(preference)
                }
            }
        }
    }
}
