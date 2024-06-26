package com.grindrplus.hooks

import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook

class DisableAnalytics: Hook("Disable analytics",
    "Disable Grindr analytics (data collection)") {
    private val retrofit = "retrofit2.Retrofit"
    private val analyticsRestService = "u4.b"

    override fun init() {
        val analyticsRestServiceClass = findClass(analyticsRestService)

        findClass(retrofit)?.hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service?.javaClass?.let { analyticsRestServiceClass?.isAssignableFrom(it) } == true) {
                param.setResult(analyticsRestServiceClass?.let {
                    createServiceProxy(context, service, it) // Blacklist all methods
                })
            }
        }
    }
}