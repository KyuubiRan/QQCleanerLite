package me.kyuubiran.qqcleanerlite.util

import me.kyuubiran.qqcleanerlite.data.CleanData
import me.kyuubiran.qqcleanerlite.util.path.CommonPath
import me.kyuubiran.qqcleanerlite.util.path.QQPath
import me.kyuubiran.qqcleanerlite.util.path.WeChatPath

object PathParser {
    fun getFullPath(path: CleanData.PathData.Path): String {
        var tmp = path.suffix

        when (path.prefix) {
            CommonPath.publicData.first ->
                tmp = CommonPath.publicData.second + path.suffix
            CommonPath.privateData.first ->
                tmp = CommonPath.privateData.second + path.suffix
            QQPath.tencentDir.first ->
                if (HOST_APP.isQqOrTim) tmp = QQPath.tencentDir.second + path.suffix
            WeChatPath.publicUserData.first ->
                if (HOST_APP.isWeChat) tmp = WeChatPath.publicUserData.second + path.suffix
            WeChatPath.privateUserData.first ->
                if (HOST_APP.isWeChat) tmp = WeChatPath.privateUserData.second + path.suffix
        }

        if (tmp == path.suffix) throw IllegalArgumentException("Unsupported path prefix: ${path.prefix}")
        return tmp
    }
}