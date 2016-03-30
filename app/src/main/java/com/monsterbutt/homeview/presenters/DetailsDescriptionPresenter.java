/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.monsterbutt.homeview.presenters;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.android.AbstractDetailsDescriptionPresenter;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    private Context context;
    private ImageView mHACK;
    private ProgressBar mHACK2;
    public DetailsDescriptionPresenter(Context context) {
        this.context = context;
    }

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {

        if (item != null && item instanceof PlexLibraryItem) {

            PlexLibraryItem video = (PlexLibraryItem) item;
            viewHolder.getTitle().setText(video.getDetailTitle(context));
            viewHolder.getSubtitle().setText(video.getDetailSubtitle(context));
            viewHolder.getBody().setText(video.getSummary());
            viewHolder.getYear().setText(video.getDetailYear(context));
            viewHolder.getDuration().setText(video.getDetailDuration(context));
            viewHolder.getStudio().setText(video.getDetailStudio(context));
            viewHolder.getContent().setText(video.getDetailContent(context));
            mHACK = viewHolder.getWatched();
            mHACK2 = viewHolder.getProgress();
            setWatchedState(video.getWatchedState() == PlexLibraryItem.WatchedState.Watched);
            mHACK2.setProgress(video.getViewedProgress());
        }
    }

    public void setWatchedState(boolean watched) {
        mHACK.setVisibility(watched ? View.GONE : View.VISIBLE);
        mHACK2.setProgress(0);
    }
}
