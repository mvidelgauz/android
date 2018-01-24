/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.os.AsyncTask;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.PhotoFragment;

import java.lang.ref.WeakReference;

public class PhotoSearchTask extends AsyncTask<Void, Void, Boolean> {

    private int columnCount;
    private Account account;
    private WeakReference<PhotoFragment> photoFragmentWeakReference;
    private SearchRemoteOperation searchRemoteOperation;
    private FileDataStorageManager storageManager;

    public PhotoSearchTask(int columnsCount,
                           PhotoFragment photoFragment,
                           Account account,
                           SearchRemoteOperation searchRemoteOperation,
                           FileDataStorageManager storageManager) {
        this.columnCount = columnsCount;
        this.account = account;
        this.photoFragmentWeakReference = new WeakReference<>(photoFragment);
        this.searchRemoteOperation = searchRemoteOperation;
        this.storageManager = storageManager;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (photoFragmentWeakReference.get() == null) {
            return false;
        }
        PhotoFragment photoFragment = photoFragmentWeakReference.get();
        OCFileListAdapter adapter = photoFragment.getAdapter();

        photoFragment.setPhotoSearchQueryRunning(true);

        if (!isCancelled()) {
            int limit = -1;

            limit = 15 * columnCount;

            long timestamp = -1;
            if (adapter.getLastTimestamp() > 0) {
                timestamp = adapter.getLastTimestamp();
            }

            searchRemoteOperation.setLimit(limit);
            searchRemoteOperation.setTimestamp(timestamp);

            RemoteOperationResult remoteOperationResult = searchRemoteOperation.execute(account,
                                                                                        photoFragment.requireContext());

            if (remoteOperationResult.isSuccess() && remoteOperationResult.getData() != null
                && !isCancelled()) {
                if (remoteOperationResult.getData() == null || remoteOperationResult.getData().size() == 0) {
                    photoFragment.setPhotoSearchNoNew(true);
                } else {
                    adapter.setData(remoteOperationResult.getData(),
                                    ExtendedListFragment.SearchType.PHOTO_SEARCH,
                                    storageManager,
                                    null,
                                    false);
                    Log_OC.d(this, "Search: count: " + remoteOperationResult.getData().size());
                }
            }

            return remoteOperationResult.isSuccess();
        } else {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (photoFragmentWeakReference.get() != null) {
            PhotoFragment photoFragment = photoFragmentWeakReference.get();

            final ToolbarActivity fileDisplayActivity = (ToolbarActivity) photoFragment.getActivity();
            if (fileDisplayActivity != null) {
                fileDisplayActivity.runOnUiThread(() -> fileDisplayActivity.setIndeterminate(false));
            }

            if (result && !isCancelled()) {
                photoFragment.setPhotoSearchQueryRunning(false);
                photoFragment.getAdapter().notifyDataSetChanged();
            } else {
                photoFragment.setPhotoSearchQueryRunning(false);
                photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.PHOTO_SEARCH);
            }
        }
    }
}
