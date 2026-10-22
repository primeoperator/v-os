package app.vos.launcher.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class Sfx { TAP, PICK, DROP, SWITCH }

/**
 * Мягкие звуковые эффекты без аудиофайлов: короткие тона синтезируются при первом запуске
 * в WAV во временной папке и проигрываются через SoundPool.
 */
class SoundFx(context: Context) {

    private val appContext = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val soundIds = ConcurrentHashMap<Sfx, Int>()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        thread(name = "vos-sfx-init", isDaemon = true) {
            try {
                for ((sfx, samples) in synthesize()) {
                    if (released) return@thread
                    val file = File(appContext.cacheDir, "sfx_${sfx.name.lowercase()}.wav")
                    file.writeBytes(toWav(samples))
                    soundIds[sfx] = pool.load(file.absolutePath, 1)
                }
            } catch (e: Exception) {
                // Звук — приятное дополнение; если не вышло, приложение работает без него.
            }
        }
    }

    fun play(sfx: Sfx) {
        if (released) return
        val id = soundIds[sfx] ?: return
        if (id in loaded) pool.play(id, 0.55f, 0.55f, 1, 0, 1f)
    }

    fun release() {
        released = true
        pool.release()
    }

    private fun synthesize(): Map<Sfx, ShortArray> = mapOf(
        Sfx.TAP to tone(740f, 988f, 110),
        Sfx.PICK to tone(520f, 720f, 100),
        Sfx.DROP to tone(430f, 300f, 130),
        Sfx.SWITCH to tone(660f, 880f, 70) + tone(880f, 1100f, 70),
    )

    private fun tone(freqStart: Float, freqEnd: Float, ms: Int): ShortArray {
        val n = SAMPLE_RATE * ms / 1000
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val freq = freqStart + (freqEnd - freqStart) * t
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val attack = min(1.0, i / (SAMPLE_RATE * 0.006))
            val envelope = attack * exp(-4.5 * t)
            val sample = (sin(phase) + 0.25 * sin(2.0 * phase)) * envelope * 0.5
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun toWav(samples: ShortArray): ByteArray {
        val dataLen = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(36 + dataLen)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(1) // mono
        buf.putInt(SAMPLE_RATE)
        buf.putInt(SAMPLE_RATE * 2)
        buf.putShort(2)
        buf.putShort(16)
        buf.put("data".toByteArray())
        buf.putInt(dataLen)
        for (s in samples) buf.putShort(s)
        return buf.array()
    }

    private companion object {
        const val SAMPLE_RATE = 22050
    }
}

/** Единая точка тактильной и звуковой обратной связи. */
@Stable
class Feedback(
    private val haptic: HapticFeedback,
    private val sound: SoundFx?,
) {
    fun tap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sound?.play(Sfx.TAP)
    }

    fun pick() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        sound?.play(Sfx.PICK)
    }

    fun drop() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sound?.play(Sfx.DROP)
    }

    fun switchRoom() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sound?.play(Sfx.SWITCH)
    }
}

@Composable
fun rememberFeedback(soundsEnabled: Boolean): Feedback {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sfx = remember { SoundFx(context) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    return remember(haptic, sfx, soundsEnabled) { Feedback(haptic, if (soundsEnabled) sfx else null) }
}
