package com.chatledger.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * 将 Uri 转为 Base64 字符串（用于发送给 AI API）
     */
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 1024): Pair<String, String>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // 压缩图片
            val bitmap = if (originalBitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / originalBitmap.width
                val newHeight = (originalBitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            Pair(base64, "image/jpeg")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取图片的 MIME 类型
     */
    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "image/jpeg"
    }
}
