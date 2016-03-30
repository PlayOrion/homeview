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

package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewLogoPresenter;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.plex.tasks.ToggleWatchedStateTask;
import com.monsterbutt.homeview.presenters.DetailsDescriptionPresenter;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.handler.MediaCardBackgroundHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.ui.activity.DetailsActivity;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment implements OnItemViewClickedListener, OnActionClickedListener, HomeViewActivity.OnPlayKeyListener, OnItemViewSelectedListener {

    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;
    private DetailsDescriptionPresenter mDetailPresenter;

    final static int ACTION_PLAY        = 1;
    final static int ACTION_VIEWSTATUS  = 2;

    private ArrayObjectAdapter mAdapter;

    private PlexServer mServer;
    private PlexLibraryItem mItem = null;
    private MediaCardBackgroundHandler mBackgroundHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        mServer = PlexServerManager.getInstance(activity.getApplicationContext()).getSelectedServer();
        mBackgroundHandler = new MediaCardBackgroundHandler(activity);

        mItem = activity.getIntent().getParcelableExtra(DetailsActivity.ITEM);
        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
        setupDetailsOverviewRowPresenter();
        if (mItem != null)
            setupDetailsOverviewRow();
    }


    public void startTheme() {

        Activity activity = getActivity();
        String theme = mItem != null ? mItem.getThemeKey() : "";
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed() &&
            SettingsManager.getInstance(activity.getApplicationContext()).getBoolean("preferences_navigation_thememusic")
                && !TextUtils.isEmpty(theme)) {

            Intent intent = new Intent(getActivity(), ThemeService.class);
            intent.setAction(ThemeService.ACTION_PLAY);
            intent.setData(Uri.parse(mServer.makeServerURL(theme)));
            activity.startService(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Intent intent = new Intent(getActivity(), ThemeService.class);
        intent.setAction(ThemeService.ACTION_STOP);
        getActivity().stopService(intent);
    }

    @Override
    public void onStop() {

        super.onStop();
        mBackgroundHandler.cancel();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        ((HomeViewActivity) getActivity()).setPlayKeyListener(this);

        String key = mItem != null  ? mItem.getKey()
                                    : getActivity().getIntent().getStringExtra(DetailsActivity.KEY);
        new LoadMetadataTask(mServer).execute(key);
    }

    private void setupDetailsOverviewRow() {

        mBackgroundHandler.updateBackground(mServer.makeServerURL(mItem.getBackgroundImageURL()));
        boolean usePoster = !(mItem instanceof Episode);
        final DetailsOverviewRow row = new DetailsOverviewRow(mItem);

        Context context = getActivity().getApplicationContext();
        Resources res = context.getResources();
        int width = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_WIDTH : R.dimen.DETAIL_THUMBNAIL_WIDTH);
        int height = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_HEIGHT : R.dimen.DETAIL_THUMBNAIL_HEIGHT);
        Glide.with(context)
                .load(mServer.makeServerURL(usePoster ? mItem.getCardImageURL() : mItem.getWideCardImageURL()))
                .asBitmap()
                .dontAnimate()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(final Bitmap resource,
                                                GlideAnimation glideAnimation) {
                        row.setImageBitmap(getActivity(), resource);
                        startEntranceTransition();
                    }
                });


        SparseArrayObjectAdapter actions = new SparseArrayObjectAdapter();
        setActions(actions, mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched);
        row.setActionsAdapter(actions);
        mAdapter.add(row);
        // setup codecs
        if (mItem instanceof PlexVideoItem) {

            ListRow codecs = ((PlexVideoItem)mItem).getCodecsRow(context, mServer);
            if (codecs != null)
                mAdapter.add(codecs);
        }
    }

    private void setActions(SparseArrayObjectAdapter adapter, boolean isWatched) {

        Context context = getActivity();
        adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, context.getString(R.string.action_play)));
        adapter.set(ACTION_VIEWSTATUS,
                new Action(ACTION_VIEWSTATUS, isWatched ? context.getString(R.string.action_watched)
                        : context.getString(R.string.action_unwatched)));
    }

    private void toggleWatched() {

        boolean isWatched = !(mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched);
        mDetailPresenter.setWatchedState(isWatched);
        setActions((SparseArrayObjectAdapter) ((DetailsOverviewRow) mAdapter.get(0)).getActionsAdapter(),
                isWatched);
        new ToggleWatchedStateTask(mItem).execute(mServer);
    }

    @Override
    public void onActionClicked(Action action) {

        if (action.getId() == ACTION_PLAY)
            mItem.onPlayPressed(this, null);
        else if (action.getId() == ACTION_VIEWSTATUS)
            toggleWatched();
    }

    static class MovieDetailsOverviewLogoPresenter extends DetailsOverviewLogoPresenter {

        private final boolean usePoster;
        public MovieDetailsOverviewLogoPresenter(boolean usePoster) {
            this.usePoster = usePoster;
        }

        static class ViewHolder extends DetailsOverviewLogoPresenter.ViewHolder {
            public ViewHolder(View view) {
                super(view);
            }

            public FullWidthDetailsOverviewRowPresenter getParentPresenter() {
                return mParentPresenter;
            }

            public FullWidthDetailsOverviewRowPresenter.ViewHolder getParentViewHolder() {
                return mParentViewHolder;
            }
        }

        @Override
        public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);

            Resources res = parent.getResources();
            int width = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_WIDTH : R.dimen.DETAIL_THUMBNAIL_WIDTH);
            int height = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_HEIGHT : R.dimen.DETAIL_THUMBNAIL_HEIGHT);
            imageView.setLayoutParams(new ViewGroup.MarginLayoutParams(width, height));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            DetailsOverviewRow row = (DetailsOverviewRow) item;
            ImageView imageView = ((ImageView) viewHolder.view);
            imageView.setImageDrawable(row.getImageDrawable());
            if (isBoundToImage((ViewHolder) viewHolder, row)) {

                MovieDetailsOverviewLogoPresenter.ViewHolder vh =
                        (MovieDetailsOverviewLogoPresenter.ViewHolder) viewHolder;
                vh.getParentPresenter().notifyOnBindLogo(vh.getParentViewHolder());
            }
        }
    }

    private class DetailPresenter extends FullWidthDetailsOverviewRowPresenter {

        @Override
        protected int getLayoutResourceId() {
            return R.layout.lb_fullwidth_details_overview;
        }

        public DetailPresenter(Presenter detailsPresenter, DetailsOverviewLogoPresenter logoPresenter) {
            super(detailsPresenter, logoPresenter);
        }


    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background and style.
        mDetailPresenter = new DetailsDescriptionPresenter(getActivity());
        DetailPresenter detailsPresenter =
                new DetailPresenter(mDetailPresenter,
                                    new MovieDetailsOverviewLogoPresenter(!(mItem instanceof Episode)));

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        theme.resolveAttribute(R.attr.card_translucent, typedValue, true);
        detailsPresenter.setBackgroundColor(typedValue.data);
        theme.resolveAttribute(R.attr.card_normal, typedValue, true);
        detailsPresenter.setActionsBackgroundColor(typedValue.data);
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_SMALL);
        FullWidthDetailsOverviewSharedElementHelper helper = new FullWidthDetailsOverviewSharedElementHelper();
        helper.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(helper);
        detailsPresenter.setParticipatingEntranceTransition(false);

        detailsPresenter.setOnActionClickedListener(this);

        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mAdapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(mAdapter);
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof PosterCard && !(item instanceof CodecCard))
            ((PosterCard)item).onClicked(this, ((ImageCardView) itemViewHolder.view).getMainImageView());
    }

    @Override
    public boolean playKeyPressed() {

        return mCurrentCard != null && mCurrentCard.onPlayPressed(this, mCurrentCardTransitionImage);
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject && !(item instanceof CodecCard)) {
            mCurrentCard = (CardObject) item;
            mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
        }
    }

    private class LoadMetadataTask extends AsyncTask<String, Void, PlexLibraryItem> {

        private final PlexServer server;


        public LoadMetadataTask(PlexServer server) {
            this.server = server;
        }

        @Override
        protected PlexLibraryItem doInBackground(String... params) {

            PlexLibraryItem ret = null;
            MediaContainer media = server.getVideoMetadata(params[0]);
            if (media != null) {

                if (media.getVideos() != null && 1 == media.getVideos().size())
                    ret = PlexVideoItem.getItem(media.getVideos().get(0));
                else
                    ret = PlexContainerItem.getItem(media);
            }

            return ret;
        }

        @Override
        protected void onPostExecute(PlexLibraryItem item) {

            boolean update =  (mItem == null);
            mItem = item;
            if (update)
                setupDetailsOverviewRow();
            //setupDetailsOverviewRow();
            ListRow children = item.getChildren(getActivity(), mServer);
            if (children != null)
                mAdapter.add(children);
            ListRow extras = item.getExtras(getActivity(), mServer);
            if (extras != null)
                mAdapter.add(extras);

            new LoadRelatedTask().execute(mItem.getRelated());
            startTheme();
        }
    }

    private class LoadRelatedTask extends AsyncTask<List<Hub>, Void, List<MediaContainer>> {

        @Override
        protected List<MediaContainer> doInBackground(List<Hub>... params) {

            List<MediaContainer> mcs = new ArrayList<>();
            if (params != null && params.length > 0 && params[0] != null) {

                for (Hub hub : params[0]) {

                    MediaContainer mc = mServer.getRelatedForKey(hub.getKey());
                    if (mc != null) {
                        mc.setTitle1(hub.getTitle());
                        mcs.add(mc);
                    }
                }
            }

            return mcs;
        }

        @Override
        protected void onPostExecute(List<MediaContainer> list) {

            Activity act = getActivity();
            for (MediaContainer mc : list) {

                if (mc.getVideos() != null && !mc.getVideos().isEmpty()) {
                    ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter(mServer));
                    ListRow row = new ListRow(new HeaderItem(0, mc.getTitle1()),
                            adapter);
                    for (Video video: mc.getVideos())
                        adapter.add(new PosterCard(act, PlexVideoItem.getItem(video)));
                    mAdapter.add(row);
                }
            }
        }
    }

}
