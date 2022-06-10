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
import me.kyuubiran.qqcleanerlite.util.Shared
import me.kyuubiran.qqcleanerlite.util.wrapped

class ModifyConfigDialog(activity: Activity) :
    AlertDialog.Builder(activity.wrapped) {

    init {
        setTitle("编辑配置")

        val fragment = ModifyConfigFragment()
        activity.fragmentManager.beginTransaction().add(fragment, "Modify").commit()
        activity.fragmentManager.executePendingTransactions()

        fragment.onActivityCreated(null)
        setView(fragment.view)

        setCancelable(false)
        setNeutralButton("放弃更改", null)

        setPositiveButton("保存") { _, _ ->
            Shared.currentModify.save()
            Log.toast("保存成功！")
        }

        show()
    }

    class PathPreference(
        ctx: Context,
        private val path: CleanData.PathData
    ) : CheckBoxPreference(ctx) {
        init {
            isChecked = path.enable
            title = path.title
        }

        override fun onClick() {
            super.onClick()
            path.enable = isChecked
        }
    }

    class ModifyConfigFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.config_modify_prefs)

            findPreference("title").apply {
                summary = Shared.currentModify.title
            }

            findPreference("author").apply {
                summary = Shared.currentModify.author
            }

            (findPreference("options") as PreferenceCategory).apply {
                if (!Shared.currentModify.valid) {
                    Log.toast("无效的配置文件！")
                    return
                }

                Shared.currentModify.content.forEach {
                    val preference = PathPreference(activity, it)
                    addPreference(preference)
                }
            }
        }
    }
}
