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
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.MediaController
import android.widget.Toast
import com.owncloud.android.datamodel.OCFile

class PlayerService : Service() {

    companion object {
        const val EXTRA_ACCOUNT = "ACCOUNT"
        const val EXTRA_FILE = "FILE"
        const val EXTRA_AUTO_PLAY = "EXTRA_AUTO_PLAY"
        const val EXTRA_START_POSITION_MS = "START_POSITION_MS"

        const val ACTION_PLAY = "PLAY"
        const val ACTION_STOP = "STOP"
    }

    class Binder(val service: PlayerService, val player: MediaController.MediaPlayerControl) : android.os.Binder()

    private val playerListener = object : Player.Listener {

        override fun onStart() {

        }

        override fun onStop() {

        }

        override fun onError(error: Player.Error) {
            Toast.makeText(this@PlayerService, error.message, Toast.LENGTH_SHORT).show()
        }
    }

    private val player = Player(this, playerListener)

    override fun onBind(intent: Intent?): IBinder? {
        return Binder(this, player)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_PLAY -> onActionPlay(intent)
            ACTION_STOP -> onActionStop()
        }
        return START_NOT_STICKY
    }

    private fun onActionPlay(intent: Intent) {
        val account: Account = intent.getParcelableExtra(EXTRA_ACCOUNT)
        val file: OCFile = intent.getParcelableExtra(EXTRA_FILE)
        val startPos = intent.getIntExtra(EXTRA_START_POSITION_MS, 0)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, true)
        player.play(file = file, startPositionMs = startPos, autoPlay = autoPlay, account = account)
    }

    private fun onActionStop() {
        player.stop()
    }
}
