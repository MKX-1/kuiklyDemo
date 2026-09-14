package com.example.aistock

import android.util.Log
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate

/**
 * Kuikly 页面打开封装处理器。
 *
 * 职责：
 * 1. 实现 [KuiklyRenderViewBaseDelegatorDelegate]，处理页面加载回调与异常回调；
 * 2. 通过 [KuiklyRenderViewBaseDelegator] 把 Kuikly 视图挂载到宿主容器 View 上。
 */
class ContextCodeHandler(
    private val pageName: String,
) {

    lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator
        private set

    /** 实例化委托者 */
    fun initContextHandler(): KuiklyRenderViewBaseDelegator {
        val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {

            /** 指定 Kuikly 页面的执行模式，Android 上默认 JVM 模式（页面代码已编译进 AAR） */
            override fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase {
                return KuiklyRenderCoreExecuteModeBase.JVM
            }

            override fun onUnhandledException(
                throwable: Throwable,
                errorReason: ErrorReason,
                executeMode: KuiklyRenderCoreExecuteModeBase,
            ) {
                Log.e(TAG, "Kuikly 页面异常, reason=$errorReason\n${throwable.stackTraceToString()}")
            }

            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase,
            ) {
                Log.i(TAG, "页面 [$pageName] 加载完成: isSucceed=$isSucceed, reason=$errorReason")
            }
        }
        kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(delegate)
        return kuiklyRenderViewDelegator
    }

    /** 打开 Kuikly 页面 */
    fun openPage(container: ViewGroup, pageName: String, pageData: Map<String, Any>) {
        // contextCode 在 Android（JVM 模式）下固定传空字符串
        kuiklyRenderViewDelegator.onAttach(container, "", pageName, pageData)
    }

    companion object {
        private const val TAG = "ContextCodeHandler"
    }
}
