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

package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.ui.asynctasks.PhotoSearchTask;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.SearchEvent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A Fragment that lists all files and folders in a given path. TODO refactor to get rid of direct dependency on
 * FileDisplayActivity
 */
public class PhotoFragment extends OCFileListFragment {
    private RecyclerView.OnScrollListener onScrollChangeListener;
    private boolean photoSearchQueryRunning = false;
    private boolean photoSearchNoNew = false;
    private SearchRemoteOperation searchRemoteOperation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        onScrollChangeListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
                if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                    GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

                    // scroll down
                    if (dy > 0 && !photoSearchQueryRunning) {
                        int visibleItemCount = gridLayoutManager.getChildCount();
                        int totalItemCount = gridLayoutManager.getItemCount();
                        int firstVisibleItem = gridLayoutManager.findFirstCompletelyVisibleItemPosition();

                        if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + 5)) {
                            // Almost reached the end, continue to load new photos
                            if ((totalItemCount - visibleItemCount) > 0) {
                                searchAndDisplay();
                            }
                        }
                    }
                }
            }
        };

        Log_OC.i(this, "onCreateView() in PhotoFragment end");
        return v;
    }

    @Override
    public void onItemClicked(OCFile file) {
        super.onItemClicked(file);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        currentSearchType = SearchType.PHOTO_SEARCH;

        switchToGridView();

        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT;
        requireActivity().invalidateOptionsMenu();

        onMessageEvent(searchEvent, false);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();

        onMessageEvent(searchEvent, true);
    }

    @Override
    public void onMessageEvent(ChangeMenuEvent changeMenuEvent) {
        super.onMessageEvent(changeMenuEvent);
    }

    public void onMessageEvent(final SearchEvent event, boolean refresh) {
        prepareCurrentSearch(event);
        searchFragment = true;
        setEmptyListLoadingMessage();

        if (refresh || preferences.getPhotoSearchTimestamp() == 0 ||
            System.currentTimeMillis() - preferences.getPhotoSearchTimestamp() >= 30 * 1000) {
            mAdapter.setData(
                new ArrayList<>(),
                SearchType.PHOTO_SEARCH,
                mContainerActivity.getStorageManager(),
                mFile,
                true);
        } else {
            mAdapter.showVirtuals(VirtualFolderType.PHOTOS, true, mContainerActivity.getStorageManager());
            preferences.setPhotoSearchTimestamp(System.currentTimeMillis());

            return;
        }

        setFabVisible(false);

        if (!currentSearchType.equals(SearchType.SHARED_FILTER)) {
            boolean searchOnlyFolders = false;
            if (getArguments() != null && getArguments().getBoolean(ARG_SEARCH_ONLY_FOLDER, false)) {
                searchOnlyFolders = true;
            }

            searchRemoteOperation = new SearchRemoteOperation(event.getSearchQuery(),
                                                              event.getSearchType(),
                                                              searchOnlyFolders);
        }

        searchAndDisplay();

        getRecyclerView().setOnScrollListener(onScrollChangeListener);
    }

    private void searchAndDisplay() {
        if (!photoSearchQueryRunning && !photoSearchNoNew) {
            new PhotoSearchTask(getColumnsCount(),
                                this,
                                accountManager.getCurrentAccount(),
                                searchRemoteOperation,
                                mContainerActivity.getStorageManager())
                .execute();
        }
    }

    public void setPhotoSearchQueryRunning(boolean bool) {
        photoSearchQueryRunning = bool;
    }

    public void setPhotoSearchNoNew(boolean bool) {
        photoSearchNoNew = bool;
    }

    @Override
    public boolean isLoading() {
        return !photoSearchNoNew;
    }
}
