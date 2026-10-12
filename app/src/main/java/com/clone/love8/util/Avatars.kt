package com.clone.love8.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/** Ảnh đại diện: ảnh chụp do người dùng chọn hoặc emoji trên nền tròn. */
object Avatars {
    val ME_BG = 0xFFFFD6E2.toInt()
    val PARTNER_BG = 0xFFE6DAFF.toInt()

    fun file(ctx: Context, isMe: Boolean): File =
        File(ctx.filesDir, if (isMe) "avatar_me.jpg" else "avatar_partner.jpg")

    fun savePhoto(ctx: Context, uri: Uri, isMe: Boolean): Boolean = try {
        val src = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        if (src == null) {
            false
        } else {
            val size = 512
            val scale = size.toFloat() / min(src.width, src.height)
            val w = (src.width * scale).roundToInt().coerceAtLeast(size)
            val h = (src.height * scale).roundToInt().coerceAtLeast(size)
            val scaled = Bitmap.createScaledBitmap(src, w, h, true)
            val cropped = Bitmap.createBitmap(scaled, (w - size) / 2, (h - size) / 2, size, size)
            FileOutputStream(file(ctx, isMe)).use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        }
    } catch (e: Exception) {
        false
    }

    fun deletePhoto(ctx: Context, isMe: Boolean) {
        file(ctx, isMe).delete()
    }

    fun loadPhoto(ctx: Context, isMe: Boolean): Bitmap? {
        val f = file(ctx, isMe)
        return if (f.exists()) runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull() else null
    }

    fun circle(ctx: Context, isMe: Boolean, emoji: String, sizePx: Int, border: Boolean = true): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val r = sizePx / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val photo = loadPhoto(ctx, isMe)
        if (photo != null) {
            val scaled = Bitmap.createScaledBitmap(photo, sizePx, sizePx, true)
            paint.shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            c.drawCircle(r, r, r, paint)
        } else {
            paint.color = if (isMe) ME_BG else PARTNER_BG
            c.drawCircle(r, r, r, paint)
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sizePx * 0.55f
                textAlign = Paint.Align.CENTER
            }
            val fm = tp.fontMetrics
            c.drawText(emoji, r, r - (fm.ascent + fm.descent) / 2f, tp)
        }
        if (border) {
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.WHITE
                strokeWidth = sizePx * 0.06f
            }
            c.drawCircle(r, r, r - bp.strokeWidth / 2f, bp)
        }
        return out
    }
}
