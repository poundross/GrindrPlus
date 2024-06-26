package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.setObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class DisableUpdates: Hook("Disable updates",
    "Disable forced updates") {
    private val versionInfoEndpoint = "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/master/version.json"
    private val appUpdateInfo = "com.google.android.play.core.appupdate.AppUpdateInfo"
    private val appUpgradeManager = "com.grindrapp.android.manager.AppUpgradeManager"
    private val appConfiguration = "com.grindrapp.android.base.config.AppConfiguration"
    private var versionCode: Int = 0
    private var versionName: String = ""

    override fun init() {
        findClass(appUpdateInfo)
            ?.hook("updateAvailability", HookStage.AFTER) { param ->
                param.setResult(1)
            }

        findClass(appUpgradeManager) // showDeprecatedVersionDialog()
            ?.hook("zb.o", HookStage.AFTER) { param ->
                param.setResult(null)
            }

        CoroutineScope(Dispatchers.Main).launch {
            fetchLatestVersionInfo()
            if (versionName < context.pkgVersionName) {
                findClass(appConfiguration)?.hookConstructor(HookStage.AFTER) { param ->
                    setObjectField(param.thisObject(), "a", versionName)
                    setObjectField(param.thisObject(), "b", versionCode)
                    setObjectField(param.thisObject(), "u", "$versionName.$versionCode")
                }
            }
        }
    }

    private suspend fun fetchLatestVersionInfo() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(versionInfoEndpoint).build()

        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    if (jsonData != null) {
                        val json = JSONObject(jsonData)
                        versionCode = json.getInt("versionCode")
                        versionName = json.getString("versionName")
                        context.logger.log("Fetched version info: $versionName ($versionCode)")
                    }
                } else {
                    context.logger.log("Error fetching version info: ${response.message}")
                }
            } catch (e: Exception) {
                context.logger.log("Error fetching version info: ${e.message}")
            }
        }
    }
}