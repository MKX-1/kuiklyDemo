package com.example.aistock.adapter

import android.app.Activity
import android.content.Context
import com.example.aistock.KuiklyRenderActivity
import com.tencent.kuikly.core.render.android.adapter.IKRRouterAdapter
import org.json.JSONObject

/**
 * 路由适配器：Kuikly 页面内部调用 openPage / closePage 时，最终由宿主决定如何跳转。
 */
class KRRouterAdapter : IKRRouterAdapter {

    override fun openPage(context: Context, pageName: String, pageData: JSONObject) {
        // 统一用 KuiklyRenderActivity 作为 Kuikly 页面的宿主容器
        KuiklyRenderActivity.start(context, pageName, pageData)
    }

    override fun closePage(context: Context) {
        (context as? Activity)?.finish()
    }
}
