package dev.speakerkeepalive

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Генерирует короткий тихий синус. Для детектора тишины в колонке важен сам факт ненулевого
 * цифрового сигнала, а не громкость: на 40 Гц динамики саундбара почти ничего не воспроизводят.
 * Частота, амплитуда и длительность задаются в настройках (см. [Prefs]). Если колонка всё равно
 * засыпает — поднять амплитуду; если слышно — снизить или сменить частоту.
 */
object Beeper {
    private const val SAMPLE_RATE = 48_000
    private const val FADE_MS = 100

    /** Блокирующий вызов — запускать не из main thread. */
    fun play(context: Context) {
        val freqHz = Prefs.freqHz.get(context).toDouble()
        val amplitude = Prefs.amplitudePct.get(context) / 100.0
        val durationMs = Prefs.durationSec.get(context) * 1000
        val samples = SAMPLE_RATE * durationMs / 1000
        val fade = SAMPLE_RATE * FADE_MS / 1000
        val buf = ShortArray(samples) { i ->
            val envelope = minOf(1.0, i.toDouble() / fade, (samples - 1 - i).toDouble() / fade)
            (sin(2 * PI * freqHz * i / SAMPLE_RATE) * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples * 2 * 2)
            .build()
        try {
            // Стерео: дублируем моно-сэмпл в оба канала.
            val stereo = ShortArray(samples * 2) { buf[it / 2] }
            track.write(stereo, 0, stereo.size)
            track.play()
            Thread.sleep(durationMs + 200L)
            track.stop()
        } finally {
            track.release()
        }
    }
}
