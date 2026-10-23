package app.smriti.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import app.smriti.domain.RadioTrack
import java.io.File

/** Callbacks from the audio layer back into the radio state machine. */
interface RadioPlayerListener {
    fun onTrackCompleted(trackId: String)
    fun onTrackFailed(trackId: String)
    /** Another app (a call, a navigation prompt…) took audio focus. */
    fun onInterrupted()
}

interface RadioPlayer {
    var listener: RadioPlayerListener?
    fun play(track: RadioTrack)
    fun pause()
    fun resume()
    fun stop()
    fun setVolume(volume: Float)
    fun release()
}

/**
 * Plays local radio tracks with the platform MediaPlayer (no extra media dependency). A missing or
 * unreadable file reports [RadioPlayerListener.onTrackFailed]; it never throws to the caller.
 */
class MediaPlayerRadioPlayer(
    context: Context,
    private val localFile: (String) -> File,
) : RadioPlayer {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null
    private var currentId: String? = null
    private var volume = 0.7f
    override var listener: RadioPlayerListener? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            ) listener?.onInterrupted()
        }
        .build()

    override fun play(track: RadioTrack) {
        stop()
        currentId = track.id
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(attributes)
            when (val src = track.source) {
                is RadioTrack.Source.Asset -> appContext.assets.openFd(src.path).use { fd ->
                    mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
                is RadioTrack.Source.LocalFile -> {
                    val f = localFile(src.fileName)
                    require(f.exists()) { "missing file" }
                    mp.setDataSource(f.absolutePath)
                }
            }
            mp.setOnCompletionListener { listener?.onTrackCompleted(track.id) }
            mp.setOnErrorListener { _, _, _ -> listener?.onTrackFailed(track.id); true }
            mp.prepare()
            mp.setVolume(volume, volume)
            if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mp.release()
                listener?.onInterrupted()
                return
            }
            mp.start()
            player = mp
        } catch (e: Exception) {
            mp.release()
            player = null
            listener?.onTrackFailed(track.id)
        }
    }

    override fun pause() {
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    override fun resume() {
        val p = player ?: return
        if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) runCatching { p.start() }
    }

    override fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        currentId = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        runCatching { player?.setVolume(this.volume, this.volume) }
    }

    override fun release() = stop()
}
