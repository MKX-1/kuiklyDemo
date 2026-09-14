package com.example.aistock.adapter

import android.util.Log
import com.tencent.kuikly.core.render.android.adapter.IKRUncaughtExceptionHandlerAdapter

/**
 * 异常适配器：Kuikly 运行时未捕获异常的兜底处理，可在这里接崩溃上报。
 */
object KRUncaughtExceptionHandlerAdapter : IKRUncaughtExceptionHandlerAdapter {

    private const val TAG = "KRExceptionHandler"

    override fun uncaughtException(throwable: Throwable) {
        Log.e(TAG, "Kuikly 未捕获异常: ${throwable.stackTraceToString()}")
    }
}
