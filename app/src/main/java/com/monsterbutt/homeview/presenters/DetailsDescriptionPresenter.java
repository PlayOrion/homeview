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
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.android.AbstractDetailsDescriptionPresenter;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    private PlexServer server;
    private Context context;
    public DetailsDescriptionPresenter(Context context, PlexServer server) {
        this.context = context;
        this.server = server;
    }

    private boolean setImage(ImageView image, String path) {

        boolean ret = !TextUtils.isEmpty(path);
        if (ret)
            Glide.with(context).load(path).into(image);
        else
            image.setImageDrawable(null);
        return ret;
    }
    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {

        if (item != null && item instanceof PlexLibraryItem) {

            PlexLibraryItem video = (PlexLibraryItem) item;
            viewHolder.getTitle().setText(video.getDetailTitle(context));
            viewHolder.getSubtitle().setText(video.getDetailSubtitle(context));
            viewHolder.getBody().setText(video.getSummary());
            viewHolder.getGenre().setText(video.getDetailGenre(context));
            viewHolder.getDuration().setText(video.getDetailDuration(context));
            viewHolder.hasStudio(setImage(viewHolder.getStudio(), video.getDetailStudioPath(server)));
            viewHolder.hasRating(setImage(viewHolder.getRating(), video.getDetailRatingPath(server)));
            viewHolder.getContent().setText(video.getDetailContent(context));
            boolean watched = video.getWatchedState() == PlexLibraryItem.WatchedState.Watched;
            viewHolder.getWatched().setVisibility(watched ? View.INVISIBLE : View.VISIBLE);
            int progress = watched ? 0 : video.getViewedProgress();
            viewHolder.getProgress().setProgress(progress);
        }
    }
}
