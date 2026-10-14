package dev.speakerkeepalive

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private lateinit var interval: Button
    private lateinit var amplitude: Button
    private lateinit var freq: Button
    private lateinit var duration: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply { textSize = 24f }
        toggle = Button(this).apply { setOnClickListener { onToggle() } }
        interval = Button(this).apply {
            setOnClickListener {
                Prefs.intervalMin.next(this@MainActivity)
                // Перезапуск службы, чтобы новый интервал применился сразу (и обновилось уведомление).
                if (Prefs.isEnabled(this@MainActivity)) KeepAliveService.start(this@MainActivity)
                refresh()
            }
        }
        // Параметры тона читаются при каждом сигнале, перезапуск службы не нужен.
        amplitude = Button(this).apply { setOnClickListener { Prefs.amplitudePct.next(context); refresh() } }
        freq = Button(this).apply { setOnClickListener { Prefs.freqHz.next(context); refresh() } }
        duration = Button(this).apply { setOnClickListener { Prefs.durationSec.next(context); refresh() } }
        val test = Button(this).apply {
            setText(R.string.btn_test)
            setOnClickListener { executor.execute { runCatching { Beeper.play(applicationContext) } } }
        }

        val pad = (48 * resources.displayMetrics.density).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            addView(status)
            listOf(toggle, interval, amplitude, freq, duration, test).forEach {
                it.setBackgroundResource(R.drawable.button_bg)
                it.setTextColor(Color.WHITE)
                addView(it)
            }
        })

        refresh()
        toggle.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    private fun onToggle() {
        val enable = !Prefs.isEnabled(this)
        Prefs.setEnabled(this, enable)
        if (enable) KeepAliveService.start(this) else KeepAliveService.stop(this)
        refresh()
    }

    private fun refresh() {
        val on = Prefs.isEnabled(this)
        val min = Prefs.intervalMin.get(this)
        status.text = if (on) getString(R.string.status_on, min) else getString(R.string.status_off)
        toggle.setText(if (on) R.string.btn_stop else R.string.btn_start)
        interval.text = getString(R.string.btn_interval, min)
        amplitude.text = getString(R.string.btn_amplitude, Prefs.amplitudePct.get(this))
        freq.text = getString(R.string.btn_freq, Prefs.freqHz.get(this))
        duration.text = getString(R.string.btn_duration, Prefs.durationSec.get(this))
    }
}
