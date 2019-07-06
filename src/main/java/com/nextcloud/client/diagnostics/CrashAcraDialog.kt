/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 *
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

package com.nextcloud.client.diagnostics

import android.os.Bundle
import android.widget.Button
import com.owncloud.android.R
import kotlinx.android.synthetic.main.acra_crash_report_dialog.*
import org.acra.dialog.BaseCrashReportDialog

class CrashAcraDialog : BaseCrashReportDialog() {

    private lateinit var ok: Button
    private lateinit var cancel: Button

    override fun init(savedInstanceState: Bundle?) {
        super.init(savedInstanceState)
        setContentView(R.layout.acra_crash_report_dialog)

        ok = acra_crash_report_dialog_ok
        cancel = acra_crash_report_dialog_cancel

        ok.setOnClickListener {
            sendCrash(null, null)
            finish()
        }
        cancel.setOnClickListener {
            cancelReports()
            finish()
        }
    }
}
