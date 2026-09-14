package com.example.aistock.adapter

import android.util.Log
import com.tencent.kuikly.core.render.android.adapter.IKRLogAdapter

/**
 * 日志适配器：把 Kuikly 的日志输出接到 Android Logcat。
 */
object KRLogAdapter : IKRLogAdapter {

    override val asyncLogEnable: Boolean
        get() = true

    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }
}
