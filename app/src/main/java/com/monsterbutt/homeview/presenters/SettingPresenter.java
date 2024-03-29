package com.monsterbutt.homeview.presenters;

import android.content.res.Resources;
import android.support.v17.leanback.widget.Presenter;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.ImageCardView;


public class SettingPresenter extends Presenter {

    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = parent.getContext().getTheme();
        theme.resolveAttribute(R.attr.card_normal, typedValue, true);
        mDefaultBackgroundColor = typedValue.data;
        theme.resolveAttribute(R.attr.card_selected, typedValue, true);
        mSelectedBackgroundColor = typedValue.data;

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
        view.findViewById(R.id.card_progress).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

        CardObject obj = (CardObject) item;

        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(obj.getTitle());
        cardView.setContentText(obj.getContent());

        // Set card size from dimension resources.
        Resources res = cardView.getResources();
        int width = res.getDimensionPixelSize(obj.getWidth());
        int height = res.getDimensionPixelSize(obj.getHeight());
        cardView.setMainImageDimensions(width, height);
        cardView.setMainImage(obj.getImage(cardView.getContext()), true);
        cardView.getMainImageView().setScaleType(ImageView.ScaleType.CENTER);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}

