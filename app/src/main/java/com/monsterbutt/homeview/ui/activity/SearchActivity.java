package com.monsterbutt.homeview.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.provider.MediaContentProvider;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.fragment.SearchFragment;


public class SearchActivity extends HomeViewActivity {
    private SearchFragment mFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent != null ? intent.getData() : null;
        if (data != null) {

            String key = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            String path = data.getLastPathSegment();
            if (path.equals(MediaContentProvider.ID_PLAYBACK)) {

                Intent suggestion = new Intent(getApplicationContext(), PlaybackActivity.class);
                suggestion.putExtra(PlaybackActivity.KEY, key);
                PlexVideoItem vid = intent.getParcelableExtra(PlaybackActivity.VIDEO);
                if (vid != null)
                    suggestion.putExtra(PlaybackActivity.VIDEO, vid);
                startActivity(suggestion);
                finish();
            }
            else if (path.equals(MediaContentProvider.ID_DETAIL)) {

                Intent suggestion = new Intent(getApplicationContext(), DetailsActivity.class);
                suggestion.putExtra(DetailsActivity.KEY, key);
                startActivity(suggestion);
                finish();
            }
        }
        else {
            setContentView(R.layout.activity_search);
            mFragment = (SearchFragment) getFragmentManager().findFragmentById(R.id.search_fragment);
        }
    }

    @Override
    public boolean onSearchRequested() {
        if (mFragment.hasResults()) {
            startActivity(new Intent(this, SearchActivity.class));
        } else {
            mFragment.startRecognition();
        }
        return true;
    }
}
