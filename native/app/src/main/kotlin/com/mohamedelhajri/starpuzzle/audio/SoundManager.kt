package com.mohamedelhajri.starpuzzle.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.mohamedelhajri.starpuzzle.R

/** SoundPool wrapper: one play() per gameplay event, no spam. */
class SoundManager(context: Context) {

    @Volatile var enabled = true

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = mutableMapOf<Sfx, Int>()
    // SoundPool's load callback fires on an audio thread while play()
    // reads from the UI thread — this set must be thread-safe.
    private val loaded: MutableSet<Int> =
        java.util.concurrent.ConcurrentHashMap.newKeySet()

    enum class Sfx { PLACE, INVALID, CLEAR, COMBO, BOMB, STAR, GAME_OVER, COIN }

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        ids[Sfx.PLACE] = pool.load(context, R.raw.piece_drop, 1)
        ids[Sfx.INVALID] = pool.load(context, R.raw.invalid_drop, 1)
        ids[Sfx.CLEAR] = pool.load(context, R.raw.line_clear, 1)
        ids[Sfx.COMBO] = pool.load(context, R.raw.combo, 1)
        ids[Sfx.BOMB] = pool.load(context, R.raw.bomb, 1)
        ids[Sfx.STAR] = pool.load(context, R.raw.star_piece, 1)
        ids[Sfx.GAME_OVER] = pool.load(context, R.raw.game_over, 1)
        ids[Sfx.COIN] = pool.load(context, R.raw.coin, 1)
    }

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (!enabled) return
        val id = ids[sfx] ?: return
        if (id in loaded) pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()
}
