package me.kyuubiran.qqcleanerlite.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import com.github.kyuubiran.ezxhelper.utils.*
import com.github.kyuubiran.ezxhelper.utils.Log.logeIfThrow
import me.kyuubiran.qqcleanerlite.dialog.ModuleDialog
import me.kyuubiran.qqcleanerlite.util.*
import java.lang.reflect.Method

object EntryHook : BaseHook() {
    private fun showSettingDialog(act: Activity) {
        if (ConfigManager.isFirstRun) {
            AlertDialog.Builder(act.wrapped).run {
                setTitle("注意事项")
                setMessage("本插件采用删除文件的方式，以达成减少3A大作App的体积的目的，同时默认的配置文件会清理图片！！！如果你不需要的话记得关掉！本插件开发旨在学习，请勿用于非法用途，否则后果自负。")
                setCancelable(false)
                setPositiveButton("取消", null)
                setNeutralButton("我已知晓") { _, _ ->
                    ConfigManager.isFirstRun = false
                    ModuleDialog(act)
                }
                show()
            }
        } else {
            ModuleDialog(act)
        }
    }

    private fun initForQqOrTim() {
        getMethodByDesc("Lcom/tencent/mobileqq/activity/AboutActivity;->doOnCreate(Landroid/os/Bundle;)Z").hookAfter { param ->
            val cFormSimpleItem = loadClassAny(
                "com.tencent.mobileqq.widget.FormSimpleItem",
                "com.tencent.mobileqq.widget.FormCommonSingleLineItem"
            )

            //获取ViewGroup
            val vg: ViewGroup = try {
                param.thisObject.getObjectAs("a", cFormSimpleItem)
            } catch (e: Exception) {
                param.thisObject.getObjectOrNullByTypeAs<View>(cFormSimpleItem)!!
            }.parent as ViewGroup
            //创建入口
            val entry = cFormSimpleItem.newInstanceAs<View>(
                args(param.thisObject),
                argTypes(Context::class.java)
            )!!.also {
                it.invokeMethod(
                    "setLeftText",
                    args("瘦身模块轻量版"),
                    argTypes(CharSequence::class.java)
                )
                it.invokeMethod(
                    "setRightText",
                    args("芜狐~"),
                    argTypes(CharSequence::class.java)
                )
            }
            //设置点击事件
            entry.setOnClickListener {
                showSettingDialog(param.thisObject as Activity)
            }
            //添加入口
            vg.addView(entry, 2)
        }
    }

    private fun initForWeChat() = runCatching {
        val actClass = loadClassAny(
            "com.tencent.mm.plugin.setting.ui.setting.SettingsAboutMicroMsgUI",
            "com.tencent.mm.ui.setting.SettingsAboutMicroMsgUI"
        )
        val preferenceClass = loadClass("com.tencent.mm.ui.base.preference.Preference")

        fun getKey(preference: Any): Any = preference.invokeMethod("getKey")
            ?: preference.getObject("mKey")

        actClass.getDeclaredMethod("onCreate", Bundle::class.java).hookAfter {
            val ctx = it.thisObject
            val listView = it.thisObject.invokeMethod("getListView") as? ListView
                ?: it.thisObject.getObjectAs("list", ListView::class.java)
            val adapter = listView.adapter as BaseAdapter
            val addMethod: Method = findMethod(adapter.javaClass) {
                returnType == Void.TYPE && parameterTypes.sameAs(preferenceClass, Int::class.java)
            }
            // 构建一个入口
            val entry = loadClass("com.tencent.mm.ui.base.preference.IconPreference")
                .getConstructor(Context::class.java)
                .newInstance(ctx).apply {
                    // 设置入口的属性
                    invokeMethod(
                        "setKey",
                        args("QQCleanerLite"),
                        argTypes(String::class.java)
                    )
                    // 新版微信这里坏了
                    invokeMethod(
                        "setSummary",
                        args("芜狐~"),
                        argTypes(CharSequence::class.java)
                    )
                    invokeMethod(
                        "setTitle",
                        args("瘦身模块轻量版"),
                        argTypes(java.lang.CharSequence::class.java)
                    )
                }

            // 在adapter数据变化前添加entry
            findMethod(adapter.javaClass) {
                name == "notifyDataSetChanged"
            }.hookBefore {
                if (adapter.count == 0) return@hookBefore
                val position = adapter.count - 2
                if ("QQCleanerLite" != getKey(adapter.getItem(position))) {
                    addMethod.invoke(adapter, entry, position)
                }
            }
        }

        // Hook Preference点击事件
        findMethod(actClass) {
            name == "onPreferenceTreeClick"
                    && parameterTypes[1].isAssignableFrom(preferenceClass)
        }.hookBefore {
            if ("QQCleanerLite" == getKey(it.args[1])) {
                showSettingDialog(it.thisObject as Activity)
                it.result = true
            }
        }
    }.logeIfThrow()

    override fun init() {
        when {
            HOST_APP.isQqOrTim -> initForQqOrTim()
            HOST_APP.isWeChat -> initForWeChat()
        }
    }
}