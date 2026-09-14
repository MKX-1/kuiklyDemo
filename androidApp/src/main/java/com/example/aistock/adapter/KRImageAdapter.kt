package com.example.aistock.adapter

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import com.tencent.kuikly.core.render.android.KuiklyRenderViewContext
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.IKRImageAdapter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * 图片加载适配器。
 *
 * 本 Demo 不引入 Glide/Fresco 等第三方库，用系统 API 实现三类来源：
 *  - base64 内联图
 *  - assets / 本地文件
 *  - http(s) 网络图
 */
class KRImageAdapter(private val context: Context) : IKRImageAdapter {

    override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        val src = imageLoadOption.src
        when {
            imageLoadOption.isBase64() -> decodeBase64(src, callback)
            imageLoadOption.isAssets() -> decodeBytes(readAssets(src), callback)
            imageLoadOption.isFile() -> decodeBytes(readFile(src), callback)
            imageLoadOption.isWebUrl() -> loadFromNetwork(src, callback)
            else -> callback.invoke(null)
        }
    }

    override fun getDrawableWidth(
        kuiklyRenderViewContext: KuiklyRenderViewContext,
        drawable: Drawable,
    ): Float = drawable.intrinsicWidth.toFloat()

    override fun getDrawableHeight(
        kuiklyRenderViewContext: KuiklyRenderViewContext,
        drawable: Drawable,
    ): Float = drawable.intrinsicHeight.toFloat()

    private fun decodeBase64(src: String, callback: (Drawable?) -> Unit) {
        executor.execute {
            val bytes = runCatching {
                Base64.decode(src.substringAfter(","), Base64.DEFAULT)
            }.getOrNull()
            decodeBytes(bytes, callback)
        }
    }

    private fun decodeBytes(bytes: ByteArray?, callback: (Drawable?) -> Unit) {
        if (bytes == null || bytes.isEmpty()) {
            callback.invoke(null)
            return
        }
        val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        callback.invoke(bitmap?.let { BitmapDrawable(Resources.getSystem(), it) })
    }

    private fun readAssets(src: String): ByteArray? {
        val path = src.substringAfter(HRImageLoadOption.SCHEME_ASSETS)
        return runCatching { context.assets.open(path).use { it.readBytes() } }
            .onFailure { Log.e(TAG, "读取 assets 图片失败: $path", it) }
            .getOrNull()
    }

    private fun readFile(src: String): ByteArray? {
        val path = src.substringAfter("file://")
        return runCatching { java.io.File(path).readBytes() }
            .onFailure { Log.e(TAG, "读取本地图片失败: $path", it) }
            .getOrNull()
    }

    private fun loadFromNetwork(url: String, callback: (Drawable?) -> Unit) {
        executor.execute {
            val bytes = runCatching {
                (URL(url).openConnection() as HttpURLConnection).run {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    inputStream.use { it.readBytes() }.also { disconnect() }
                }
            }.onFailure { Log.e(TAG, "下载图片失败: $url", it) }.getOrNull()
            decodeBytes(bytes, callback)
        }
    }

    companion object {
        private const val TAG = "KRImageAdapter"
        private val executor by lazy { Executors.newFixedThreadPool(2) }
    }
}
