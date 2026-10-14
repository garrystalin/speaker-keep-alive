package dev.speakerkeepalive

import android.content.Context

/** Настройка с фиксированным набором значений, переключаемая по кругу кнопкой пульта. */
class Choice(private val key: String, val values: IntArray, private val default: Int) {
    fun get(c: Context) = Prefs.sp(c).getInt(key, default)

    fun next(c: Context): Int {
        val i = values.indexOf(get(c))
        val v = values[(i + 1) % values.size] // если сохранённого нет в списке, indexOf = -1 -> первое значение
        Prefs.sp(c).edit().putInt(key, v).apply()
        return v
    }
}

object Prefs {
    val intervalMin = Choice("interval_min", intArrayOf(1, 2, 3, 5, 10, 15, 20, 30), 5)
    val amplitudePct = Choice("amplitude_pct", intArrayOf(1, 2, 5, 10, 20, 40), 5)
    val freqHz = Choice("freq_hz", intArrayOf(20, 40, 60, 100, 1000, 15000, 19000), 40)
    val durationSec = Choice("duration_sec", intArrayOf(1, 2, 3, 5), 2)

    internal fun sp(c: Context) = c.getSharedPreferences("keepalive", Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = sp(c).getBoolean("enabled", false)

    fun setEnabled(c: Context, value: Boolean) = sp(c).edit().putBoolean("enabled", value).apply()
}
