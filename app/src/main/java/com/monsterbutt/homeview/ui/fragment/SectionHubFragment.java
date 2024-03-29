package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.MediaRowCreator;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.activity.SectionHubActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.handler.MediaCardBackgroundHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class SectionHubFragment extends BrowseFragment implements HomeViewActivity.OnPlayKeyListener, OnItemViewClickedListener, OnItemViewSelectedListener {

    private PlexServer mServer;
    private MediaCardBackgroundHandler mBackgroundHandler;

    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;
    private String mBackgroundURL = "";

    private Map<String, MediaRowCreator.RowData> mRows = new HashMap<>();
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        ((HomeViewActivity) getActivity()).setPlayKeyListener(this);

        Activity activity = getActivity();
        mServer = PlexServerManager.getInstance(activity.getApplicationContext()).getSelectedServer();
        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
        ((HomeViewActivity)activity).setPlayKeyListener(this);

        TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        setTitle(activity.getIntent().getStringExtra(SectionHubActivity.TITLE));

        setHeadersTransitionOnBackEnabled(false);
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }

    @Override
    public void onResume() {

        super.onResume();

        for (MediaRowCreator.RowData row : mRows.values())
            ((PlexItemRow)row.data).resume();

        mBackgroundHandler = new MediaCardBackgroundHandler(getActivity());
        if (!TextUtils.isEmpty(mBackgroundURL))
            mBackgroundHandler.updateBackground(mBackgroundURL, false);

        new LoadMetadataTask().execute(getActivity().getIntent().getStringExtra(SectionHubActivity.SECTIONID));
    }

    @Override
    public void onPause() {

        super.onPause();

        for (MediaRowCreator.RowData row : mRows.values())
            ((PlexItemRow)row.data).pause();
        mBackgroundHandler.cancel();
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject) {
            mCurrentCard = (CardObject) item;
            mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
            mBackgroundURL = mBackgroundHandler.updateBackgroundTimed(mServer, mCurrentCard);
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject)
            ((CardObject) item).onClicked(this, null, mCurrentCardTransitionImage);
    }

    @Override
    public boolean playKeyPressed() {

        return  mCurrentCard != null && mCurrentCard.onPlayPressed(this, null, mCurrentCardTransitionImage);
    }

    private class LoadMetadataTask extends AsyncTask<String, Void, MediaContainer> implements CardPresenter.CardPresenterLongClickListener {

        @Override
        protected MediaContainer doInBackground(String... params) {

            return  mServer.getHubForSection(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer item) {

            if (item != null && item.getHubs() != null) {

                List<MediaRowCreator.MediaRow> newRows = MediaRowCreator.buildRowList(null, item);

                List<MediaRowCreator.RowData> currentRows = new ArrayList<>();
                currentRows.addAll(mRows.values());
                Collections.sort(currentRows);
                // remove old rows that aren't there anymore
                for (MediaRowCreator.RowData row : currentRows) {
                    // we are reversing through the list
                    boolean found = false;
                    for (MediaRowCreator.MediaRow newRow : newRows) {

                        if (newRow.key.equals(row.id)) {

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        mRows.remove(row.id);
                        mRowsAdapter.removeItems(row.currentIndex, 1);
                    }
                }

                int index = 0;
                for (MediaRowCreator.MediaRow row : newRows) {

                    PlexItemRow rowUpdate = MediaRowCreator.fillAdapterForWatchedRow(getActivity(), mServer, row, false, this);
                    if (mRows.containsKey(row.title)) {

                        MediaRowCreator.RowData current = mRows.get(row.title);
                        ((PlexItemRow)current.data).updateRow(rowUpdate);
                        current.currentIndex = index;
                    }
                    else {

                        mRows.put(row.title, new MediaRowCreator.RowData(row.title ,index, rowUpdate));
                        mRowsAdapter.add(index, rowUpdate);
                    }
                    ++index;
                }
            }
        }

        @Override
        public boolean longClickOccured() {
            return playKeyPressed();
        }
    }
}
