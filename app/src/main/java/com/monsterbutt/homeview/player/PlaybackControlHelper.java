package com.monsterbutt.homeview.player;


import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.os.Handler;
import android.support.v17.leanback.app.PlaybackControlGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.view.View;

import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.presenters.PlaybackDetailsPresenter;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;


public class PlaybackControlHelper extends PlaybackControlGlue {
    private static final int[] SEEK_SPEEDS = {2}; // A single seek speed for fast-forward / rewind.
    private static final int DEFAULT_UPDATE_PERIOD = 500;
    private static final int UPDATE_PERIOD = 16;
    Drawable mMediaArt;
    private boolean mIsPlaying;
    private int mSpeed;
    private PlaybackFragment mFragment;
    private MediaController.TransportControls mTransportControls;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;

    private Handler mHandler = new Handler();
    private Runnable mUpdateProgressRunnable;
    private Video mVideo = null;
    private ProgressUpdateCallback mProgressCallback;

    public interface ProgressUpdateCallback {
        void progressUpdate(long timeInMs);
    }

    public PlaybackControlHelper(Context context, PlaybackFragment fragment, Video video, ProgressUpdateCallback callback) {

        super(context, fragment, SEEK_SPEEDS);
        mFragment = fragment;
        mVideo = video;
        mProgressCallback = callback;

        mTransportControls = mFragment.getActivity().getMediaController().getTransportControls();
        mIsPlaying = true;
        mSpeed = PLAYBACK_SPEED_NORMAL;
    }

    private PlaybackControlsRowPresenter makeControlsRowAndPresenter() {
        PlaybackControlsRow controlsRow = new PlaybackControlsRow(this);
        setControlsRow(controlsRow);

        return new PlaybackControlsRowPresenter(new PlaybackDetailsPresenter()) {
            @Override
            protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                super.onBindRowViewHolder(vh, item);
                vh.setOnKeyListener(PlaybackControlHelper.this);
            }

            @Override
            protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                super.onUnbindRowViewHolder(vh);
                vh.setOnKeyListener(null);
            }
        };
    }

    @Override
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        PlaybackControlsRowPresenter presenter = makeControlsRowAndPresenter();

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ControlButtonPresenterSelector());
        getControlsRow().setSecondaryActionsAdapter(adapter);

        mFastForwardAction = (PlaybackControlsRow.FastForwardAction) getPrimaryActionsAdapter()
                .lookup(ACTION_FAST_FORWARD);

        mRewindAction = (PlaybackControlsRow.RewindAction) getPrimaryActionsAdapter()
                .lookup(ACTION_REWIND);

        presenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                dispatchAction(action);
            }
        });

        return presenter;
    }

    @Override
    public void enableProgressUpdating(boolean enable) {
        mHandler.removeCallbacks(mUpdateProgressRunnable);
        if (enable) {
            mHandler.post(mUpdateProgressRunnable);
        }
    }

    @Override
    public int getUpdatePeriod() {
        View view = mFragment.getView();
        int totalTime = getControlsRow().getTotalTime();
        if (view == null || totalTime <= 0 || view.getWidth() == 0) {
            return DEFAULT_UPDATE_PERIOD;
        }
        return Math.max(UPDATE_PERIOD, totalTime / view.getWidth());
    }

    @Override
    public void updateProgress() {
        if (mUpdateProgressRunnable == null) {
            mUpdateProgressRunnable = new Runnable() {
                @Override
                public void run() {
                    int totalTime = getControlsRow().getTotalTime();
                    int currentTime = getCurrentPosition();
                    getControlsRow().setCurrentTime(currentTime);

                    int progress = (int) mFragment.getBufferedPosition();
                    getControlsRow().setBufferedProgress(progress);

                    if (mProgressCallback != null)
                        mProgressCallback.progressUpdate(currentTime);

                    if (totalTime > 0 && totalTime <= currentTime) {
                        stopProgressAnimation();
                    } else {
                        updateProgress();
                    }
                }
            };
        }

        mHandler.postDelayed(mUpdateProgressRunnable, getUpdatePeriod());
    }

    @Override
    public boolean hasValidMedia() {
        return mVideo != null && !TextUtils.isEmpty(mVideo.title);
    }

    @Override
    public boolean isMediaPlaying() {
        return mIsPlaying;
    }

    @Override
    public CharSequence getMediaTitle() {
        return mVideo != null ? mVideo.title : "";
    }

    @Override
    public CharSequence getMediaSubtitle() {
        return mVideo != null ? mVideo.description : "";
    }

    @Override
    public int getMediaDuration() {
        return (int) mFragment.getDuration();
    }

    @Override
    public Drawable getMediaArt() {
        return mMediaArt;
    }

    @Override
    public long getSupportedActions() {
        return ACTION_PLAY_PAUSE | ACTION_FAST_FORWARD | ACTION_REWIND | ACTION_SKIP_TO_PREVIOUS |
                ACTION_SKIP_TO_NEXT;
    }

    @Override
    public int getCurrentSpeedId() {
        return mSpeed;
    }

    @Override
    public int getCurrentPosition() {
        return (int) mFragment.getCurrentPosition();
    }

    @Override
    protected void startPlayback(int speed) {
        if (mSpeed == speed) {
            return;
        }

        mSpeed = speed;
        mIsPlaying = true;
        mTransportControls.play();
    }

    @Override
    protected void pausePlayback() {
        mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_PAUSED;
        mIsPlaying = false;
        mTransportControls.pause();
    }

    @Override
    protected void skipToNext() {
        mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_NORMAL;
        mIsPlaying = true;
        mTransportControls.skipToNext();
    }

    @Override
    protected void skipToPrevious() {
        mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_NORMAL;
        mIsPlaying = true;
        mTransportControls.skipToPrevious();
    }

    @Override
    protected void onRowChanged(PlaybackControlsRow row) {
        // Do nothing.
    }

    @Override
    public void onMetadataChanged() {
        MediaMetadata metadata = mFragment.getActivity().getMediaController().getMetadata();
        if (metadata != null) {
            mVideo = new Video.VideoBuilder().buildFromMediaDesc(metadata.getDescription());
            int duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            getControlsRow().setTotalTime(duration);
            mMediaArt = new BitmapDrawable(mFragment.getResources(),
                    metadata.getBitmap(MediaMetadata.METADATA_KEY_ART));
        }
        super.onMetadataChanged();
    }

    public void dispatchAction(Action action) {
        if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            notifyActionChanged(multiAction);
        }

        if (action == mFastForwardAction) {
            mTransportControls.fastForward();
        } else if (action == mRewindAction) {
            mTransportControls.rewind();
        } else {
            super.onActionClicked(action);
        }
    }

    private void notifyActionChanged(PlaybackControlsRow.MultiAction action) {
        int index;
        index = getPrimaryActionsAdapter().indexOf(action);
        if (index >= 0) {
            getPrimaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
        } else {
            index = getSecondaryActionsAdapter().indexOf(action);
            if (index >= 0) {
                getSecondaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    private void stopProgressAnimation() {
        if (mHandler != null && mUpdateProgressRunnable != null) {
            mHandler.removeCallbacks(mUpdateProgressRunnable);
            mUpdateProgressRunnable = null;
        }
    }

    private SparseArrayObjectAdapter getPrimaryActionsAdapter() {
        return (SparseArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
    }

    private ArrayObjectAdapter getSecondaryActionsAdapter() {
        return (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter();
    }
}
