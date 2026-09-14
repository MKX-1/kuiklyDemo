package com.example.aistock

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.aistock.adapter.KRImageAdapter
import com.example.aistock.adapter.KRLogAdapter
import com.example.aistock.adapter.KRRouterAdapter
import com.example.aistock.adapter.KRThreadAdapter
import com.example.aistock.adapter.KRUncaughtExceptionHandlerAdapter
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import org.json.JSONObject

/**
 * 承载 Kuikly 页面的宿主 Activity。
 *
 * 完整链路：Adapter 注册 -> ContextCodeHandler 实例化 -> 容器 View 挂载 -> Kuikly 页面渲染
 */
class KuiklyRenderActivity : AppCompatActivity() {

    private lateinit var hrContainerView: ViewGroup
    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator
    private lateinit var contextCodeHandler: ContextCodeHandler

    /** 要打开的 Kuikly 页面名，默认打开 Demo 首页 */
    private val pageName: String
        get() {
            val pn = intent.getStringExtra(KEY_PAGE_NAME) ?: ""
            return pn.ifEmpty { DEFAULT_PAGE_NAME }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 注册宿主适配器（图片、日志、路由、线程、异常）
        setupAdapterManager()

        // 2. 创建页面打开封装处理器并实例化委托者
        contextCodeHandler = ContextCodeHandler(pageName)
        kuiklyRenderViewDelegator = contextCodeHandler.initContextHandler()

        setContentView(R.layout.activity_hr)
        setupImmersiveMode()

        // 3. 获取承载 Kuikly 的容器 View
        hrContainerView = findViewById(R.id.hr_container)

        // 4. 触发 Kuikly View 实例化并打开页面
        contextCodeHandler.openPage(hrContainerView, pageName, createPageData())
    }

    // 5/6/7. 把宿主生命周期透传给 Kuikly 页面
    override fun onResume() {
        super.onResume()
        kuiklyRenderViewDelegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        kuiklyRenderViewDelegator.onDetach()
    }

    /** 返回键优先交给 Kuikly 页面处理（页内路由回退） */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            if (kuiklyRenderViewDelegator.onBackPressed()) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun createPageData(): Map<String, Any> {
        return mapOf(
            "appId" to 1,
            "sysLang" to (resources.configuration.locale.language ?: "zh"),
        )
    }

    private fun setupAdapterManager() {
        if (KuiklyRenderAdapterManager.krImageAdapter == null) {
            KuiklyRenderAdapterManager.krImageAdapter = KRImageAdapter(applicationContext)
        }
        if (KuiklyRenderAdapterManager.krLogAdapter == null) {
            KuiklyRenderAdapterManager.krLogAdapter = KRLogAdapter
        }
        if (KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter == null) {
            KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter =
                KRUncaughtExceptionHandlerAdapter
        }
        if (KuiklyRenderAdapterManager.krRouterAdapter == null) {
            KuiklyRenderAdapterManager.krRouterAdapter = KRRouterAdapter()
        }
        if (KuiklyRenderAdapterManager.krThreadAdapter == null) {
            KuiklyRenderAdapterManager.krThreadAdapter = KRThreadAdapter()
        }
    }

    private fun setupImmersiveMode() {
        setDecorFitsSystemWindows(window)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = if (Build.VERSION.SDK_INT >= 26) Color.TRANSPARENT else 0x66000000
        if (Build.VERSION.SDK_INT >= 28) {
            val newMode = if (Build.VERSION.SDK_INT >= 30) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            val attrs = window.attributes
            if (attrs.layoutInDisplayCutoutMode != newMode) {
                attrs.layoutInDisplayCutoutMode = newMode
                window.attributes = attrs
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setAppearanceLightStatusBars(window)
    }

    private fun setAppearanceLightStatusBars(window: Window) {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            )
        } else if (Build.VERSION.SDK_INT >= 23) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun setDecorFitsSystemWindows(window: Window) {
        if (Build.VERSION.SDK_INT < 35) {
            val flag = if (Build.VERSION.SDK_INT >= 30) {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or flag
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }
    }

    companion object {
        private const val KEY_PAGE_NAME = "pageName"
        private const val KEY_PAGE_DATA = "pageData"
        private const val DEFAULT_PAGE_NAME = "watchlist"

        /** 供路由适配器调用的启动入口 */
        fun start(context: Context, pageName: String, pageData: JSONObject) {
            val starter = Intent(context, KuiklyRenderActivity::class.java)
            starter.putExtra(KEY_PAGE_NAME, pageName)
            starter.putExtra(KEY_PAGE_DATA, pageData.toString())
            context.startActivity(starter)
        }
    }
}
