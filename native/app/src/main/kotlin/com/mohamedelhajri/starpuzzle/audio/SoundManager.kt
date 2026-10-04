package com.mohamedelhajri.starpuzzle.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.mohamedelhajri.starpuzzle.R

/** SoundPool wrapper: one play() per gameplay event, no spam. */
class SoundManager(private val context: Context) {

    @Volatile var enabled = true
    @Volatile var musicEnabled = true

    // BGM: one calm looping track, low volume, never competes with SFX
    private var bgm: MediaPlayer? = null
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var focusHeld = false

    /**
     * Audio focus: the OS tells us when a call or another app needs the
     * speakers (transient loss = pause our loop; permanent = stop it).
     * Without this the BGM would talk over a phone call.
     */
    private fun requestFocus() {
        if (focusHeld) return
        val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> { stopMusic(); focusHeld = false }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> stopMusic()
                }
            }
            .build()
            .also { focusRequest = it }
        focusHeld = audio.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (!focusHeld) return
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        focusHeld = false
    }

    /** Starts the background loop (no-op when already playing). */
    fun startMusic() {
        if (!musicEnabled) return
        if (bgm?.isPlaying == true) return
        requestFocus()
        if (!focusHeld) return
        runCatching {
            if (bgm == null) {
                bgm = MediaPlayer.create(context, R.raw.bgm_space)?.apply {
                    isLooping = true
                    setVolume(0.35f, 0.35f)
                }
            }
            bgm?.start()
        }
    }

    fun stopMusic() {
        runCatching {
            bgm?.let { if (it.isPlaying) it.pause() }
        }
        // only surrender focus on a deliberate user stop; a transient
        // loss (call) keeps our request so we can resume when it ends
    }

    /** Full silence + focus release: app going to the background. */
    fun pauseAll() {
        stopMusic()
        abandonFocus()
    }

    /** App back to the foreground. */
    fun resumeMusic() {
        if (musicEnabled) startMusic()
    }

    fun music(on: Boolean) {
        musicEnabled = on
        if (on) startMusic() else stopMusic()
    }

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

    fun release() {
        pool.release()
        runCatching { bgm?.release() }
        bgm = null
    }
}
