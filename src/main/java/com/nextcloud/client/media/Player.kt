/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.media

import android.accounts.Account
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.widget.MediaController
import com.nextcloud.client.media.PlayerStateMachine.Event
import com.nextcloud.client.media.PlayerStateMachine.State
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * @startuml
 *
 * [*] --> STOPPED
 * STOPPED --> RUNNING: PLAY
 * RUNNING --> STOPPED: STOP
 * RUNNING --> STOPPED: ERROR
 * state RUNNING {
 *      [*] --> DOWNLOADING: [!isDownloaded]
 *      [*] --> PREPARING: [isDownloaded]
 *      DOWNLOADING --> PREPARING: DOWNLOADED
 *      PREPARING --> PLAYING: START\n[autoPlay]
 *      PREPARING --> PAUSED: START\n[!autoPlay]
 *      PLAYING -l-> PAUSED: PAUSE
 *      PAUSED -r-> PLAYING: RESUME
 * }
 *
 * @enduml
 */
@Suppress("TooManyFunctions")
class Player(
    private val context: Context,
    private val listener: Player.Listener? = null
) : MediaController.MediaPlayerControl {

    data class Error(val message: String)
    interface Listener {
        fun onStart()
        fun onStop()
        fun onError(error: Player.Error)
    }

    private val fsm: PlayerStateMachine
    private var mediaPlayer: MediaPlayer? = null
    private var loadUrlTask: LoadUrlTask? = null

    private var file: OCFile? = null
    private var startPositionMs: Int = 0
    private var autoPlay = true
    private var account: Account? = null
    private var dataSource: String? = null
    private var lastError: Error? = null

    private val delegate = object : PlayerStateMachine.Delegate {
        override val isDownloaded: Boolean get() = file?.isDown ?: false
        override val isAutoplayEnabled: Boolean get() = autoPlay

        override fun onStartDownloading() {
            trace("onStartDownloading()")
            if (file == null) {
                throw IllegalStateException("File not set.")
            }
            file?.let {
                val client = buildClient()
                val task = LoadUrlTask(client, it.remoteId, this@Player::onDownloaded)
                task.execute()
                loadUrlTask = task
            }
        }

        override fun onPrepare() {
            trace("onPrepare()")
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnErrorListener(this@Player::onMediaPlayerError)
            mediaPlayer?.setOnPreparedListener(this@Player::onMediaPlayerPrepared)
            mediaPlayer?.setOnCompletionListener(this@Player::onMediaPlayerCompleted)
            mediaPlayer?.setOnBufferingUpdateListener(this@Player::onMediaPlayerBufferingUpdate)
            mediaPlayer?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mediaPlayer?.setDataSource(dataSource)
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer?.prepareAsync()
        }

        override fun onStopped() {
            trace("onStoppped()")
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null

            file = null
            startPositionMs = 0
            account = null
            autoPlay = true
            dataSource = null
            loadUrlTask?.cancel(true)
            loadUrlTask = null
        }

        override fun onError() {
            trace("onError()")
            this.onStopped()
            lastError?.let {
                this@Player.listener?.onError(it)
            }
            if (lastError == null) {
                this@Player.listener?.onError(Error("Unknown"))
            }
        }

        override fun onStart() {
            trace("onStart()")
            mediaPlayer?.start()
        }

        override fun onPause() {
            trace("onPause()")
            mediaPlayer?.pause()
        }

        override fun onResume() {
            trace("onResume()")
            mediaPlayer?.start()
        }
    }

    init {
        fsm = PlayerStateMachine(delegate)
    }

    fun play(file: OCFile, startPositionMs: Int, autoPlay: Boolean, account: Account) {
        this.file = file
        this.startPositionMs = startPositionMs
        this.autoPlay = autoPlay
        this.account = account
        if (file.isDown) {
            dataSource = file.storagePath
        }
        fsm.post(Event.PLAY)
    }

    fun stop() {
        fsm.post(Event.STOP)
    }

    private fun onMediaPlayerError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        lastError = Error(PlayerError.toString(context, what, extra))
        fsm.post(Event.ERROR)
        return true
    }

    private fun onMediaPlayerPrepared(mp: MediaPlayer) {
        trace( "onMediaPlayerPrepared()")
        fsm.post(Event.PREPARED)
    }

    private fun onMediaPlayerCompleted(mp: MediaPlayer) {
        fsm.post(Event.STOP)
    }

    private fun onMediaPlayerBufferingUpdate(mp: MediaPlayer, percent: Int) {
        trace("onMediaPlayerBufferingUpdate(): $percent")
    }

    private fun onDownloaded(url: String?) {
        if (url != null) {
            dataSource = url
            fsm.post(Event.DOWNLOADED)
        } else {
            lastError = Error(context.getString(R.string.media_err_io))
            fsm.post(Event.ERROR)
        }
    }

    // this should be refactored into a proper, injectable factory
    private fun buildClient(): OwnCloudClient {
        val account = this.account
        if (account != null) {
            val ocAccount = OwnCloudAccount(account, context)
            return OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)
        } else {
            throw IllegalArgumentException("Account not set")
        }
    }

    private fun trace(fmt: String, vararg args: Any?) {
        Log_OC.d(this, fmt.format(args))
    }

    // region Media player controls

    override fun isPlaying(): Boolean {
        return fsm.state == State.PLAYING
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        if (fsm.state in setOf(State.PLAYING, State.PAUSED)) {
            return mediaPlayer?.duration ?: 0
        } else {
            return 0
        }
    }

    override fun pause() {
        fsm.post(Event.PAUSE)
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun seekTo(pos: Int) {
        if (fsm.state == State.PLAYING) {
            mediaPlayer?.seekTo(pos)
        }
    }

    override fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {
        fsm.post(Event.START)
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        return fsm.state == State.PLAYING
    }

    // endregion
}
