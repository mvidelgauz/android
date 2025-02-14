/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities;

import com.nextcloud.common.NextcloudClient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class RemoteActivitiesRepositoryTest {

    @Mock
    ActivitiesServiceApi serviceApi;

    @Mock
    ActivitiesRepository.LoadActivitiesCallback mockedLoadActivitiesCallback;

    @Mock
    NextcloudClient nextcloudClient;

    @Captor
    private ArgumentCaptor<ActivitiesServiceApi.ActivitiesServiceCallback> activitiesServiceCallbackCaptor;

    private ActivitiesRepository mActivitiesRepository;

    private List<Object> activitiesList;

    @Before
    public void setUpActivitiesRepository() {
        MockitoAnnotations.initMocks(this);
        mActivitiesRepository = new RemoteActivitiesRepository(serviceApi);
        activitiesList = new ArrayList<>();
    }

    @Test
    public void loadActivitiesReturnSuccess() {
        mActivitiesRepository.getActivities(-1, mockedLoadActivitiesCallback);
        verify(serviceApi).getAllActivities(eq(-1), activitiesServiceCallbackCaptor.capture());
        activitiesServiceCallbackCaptor.getValue().onLoaded(activitiesList, nextcloudClient, -1);
        verify(mockedLoadActivitiesCallback).onActivitiesLoaded(eq(activitiesList), eq(nextcloudClient), eq(-1));
    }

    @Test
    public void loadActivitiesReturnError() {
        mActivitiesRepository.getActivities(-1, mockedLoadActivitiesCallback);
        verify(serviceApi).getAllActivities(eq(-1), activitiesServiceCallbackCaptor.capture());
        activitiesServiceCallbackCaptor.getValue().onError("error");
        verify(mockedLoadActivitiesCallback).onActivitiesLoadedError(eq("error"));
    }

}
