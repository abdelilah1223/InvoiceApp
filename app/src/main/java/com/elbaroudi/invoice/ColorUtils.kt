package com.elbaroudi.invoice

import android.graphics.Color

object ColorUtils {

    fun getTextColor(backgroundColor: String): Int {
        return try {
            val color = backgroundColor.replace("#", "")
            val r = color.substring(0, 2).toInt(16)
            val g = color.substring(2, 4).toInt(16)
            val b = color.substring(4, 6).toInt(16)
            val brightness = (r * 299 + g * 587 + b * 114) / 1000
            if (brightness > 128) Color.parseColor("#333333") else Color.WHITE
        } catch (e: Exception) {
            Color.parseColor("#333333")
        }
    }

    fun isValidHexColor(colorString: String): Boolean {
        return try {
            Color.parseColor(colorString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun adjustBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}