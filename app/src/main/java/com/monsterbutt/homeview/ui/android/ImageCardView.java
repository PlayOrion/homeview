package com.monsterbutt.homeview.ui.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.BaseCardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;

public class ImageCardView extends BaseCardView {

    public static final int CARD_TYPE_FLAG_IMAGE_ONLY = 0;
    public static final int CARD_TYPE_FLAG_TITLE = 1;
    public static final int CARD_TYPE_FLAG_CONTENT = 2;
    public static final int CARD_TYPE_FLAG_ICON_RIGHT = 4;
    public static final int CARD_TYPE_FLAG_ICON_LEFT = 8;

    private ImageView mImageView;
    private ImageView mFlagView;
    private TextView mFlagText;
    private ProgressBar mProgressView;
    private ViewGroup mInfoArea;
    private TextView mTitleView;
    private TextView mContentView;
    private ImageView mBadgeImage;
    private boolean mAttachedToWindow;

    private boolean mAllowDoubleLine = false;

    private SimpleTarget<GlideDrawable> mTarget = null;
    public SimpleTarget<GlideDrawable> getTarget() { return mTarget; }
    public void  setTarget(SimpleTarget<GlideDrawable> target) { mTarget = target; }


    public ImageCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        buildImageCardView(attrs, defStyleAttr, android.support.v17.leanback.R.style.Widget_Leanback_ImageCardView);
    }

    private void buildImageCardView(AttributeSet attrs, int defStyleAttr, int defStyle) {
        // Make sure the ImagePlusCardView is focusable.
        setFocusable(true);
        setFocusableInTouchMode(true);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.lb_image_card_view, this);
        TypedArray cardAttrs = getContext().obtainStyledAttributes(attrs,
                android.support.v17.leanback.R.styleable.lbImageCardView, defStyleAttr, defStyle);
        int cardType = cardAttrs
                .getInt(android.support.v17.leanback.R.styleable.lbImageCardView_lbImageCardViewType, CARD_TYPE_FLAG_IMAGE_ONLY);

        boolean hasImageOnly = cardType == CARD_TYPE_FLAG_IMAGE_ONLY;
        boolean hasTitle = (cardType & CARD_TYPE_FLAG_TITLE) == CARD_TYPE_FLAG_TITLE;
        boolean hasContent = (cardType & CARD_TYPE_FLAG_CONTENT) == CARD_TYPE_FLAG_CONTENT;
        boolean hasIconRight = (cardType & CARD_TYPE_FLAG_ICON_RIGHT) == CARD_TYPE_FLAG_ICON_RIGHT;
        boolean hasIconLeft =
                !hasIconRight && (cardType & CARD_TYPE_FLAG_ICON_LEFT) == CARD_TYPE_FLAG_ICON_LEFT;

        mImageView = (ImageView) findViewById(R.id.main_image);

        mFlagView = (ImageView) findViewById(R.id.flag_image);
        if (mFlagView.getDrawable() == null) {
            mFlagView.setVisibility(View.INVISIBLE);
        }
        mFlagText = (TextView) findViewById(R.id.flag_text);
        if (mFlagText.getText() == null) {
            mFlagText.setVisibility(View.INVISIBLE);
        }
        mProgressView = (ProgressBar) findViewById(R.id.card_progress);
        if (mProgressView.getProgress() == 0) {
            mProgressView.setVisibility(View.GONE);
        }

        mInfoArea = (ViewGroup) findViewById(android.support.v17.leanback.R.id.info_field);
        if (hasImageOnly) {
            removeView(mInfoArea);
            cardAttrs.recycle();
            return;
        }
        // Create children
        if (hasTitle) {
            mTitleView = (TextView) inflater.inflate(android.support.v17.leanback.R.layout.lb_image_card_view_themed_title,
                    mInfoArea, false);
            mInfoArea.addView(mTitleView);
        }

        if (hasContent) {
            mContentView = (TextView) inflater.inflate(android.support.v17.leanback.R.layout.lb_image_card_view_themed_content,
                    mInfoArea, false);
            mContentView.setVisibility(View.GONE);
            mInfoArea.addView(mContentView);
        }

        if (hasIconRight || hasIconLeft) {
            int layoutId = android.support.v17.leanback.R.layout.lb_image_card_view_themed_badge_right;
            if (hasIconLeft) {
                layoutId = android.support.v17.leanback.R.layout.lb_image_card_view_themed_badge_left;
            }
            mBadgeImage = (ImageView) inflater.inflate(layoutId, mInfoArea, false);
            mInfoArea.addView(mBadgeImage);
        }

        // Set up LayoutParams for children
        if (hasTitle && !hasContent && mBadgeImage != null) {
            RelativeLayout.LayoutParams relativeLayoutParams =
                    (RelativeLayout.LayoutParams) mTitleView.getLayoutParams();
            // Adjust title TextView if there is an icon but no content
            if (hasIconLeft) {
                relativeLayoutParams.addRule(RelativeLayout.END_OF, mBadgeImage.getId());
            } else {
                relativeLayoutParams.addRule(RelativeLayout.START_OF, mBadgeImage.getId());
            }
            mTitleView.setLayoutParams(relativeLayoutParams);
        }

        // Set up LayoutParams for children
        if (hasContent) {
            RelativeLayout.LayoutParams relativeLayoutParams =
                    (RelativeLayout.LayoutParams) mContentView.getLayoutParams();
            if (!hasTitle) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            }
            // Adjust content TextView if icon is on the left
            if (hasIconLeft) {
                relativeLayoutParams.removeRule(RelativeLayout.START_OF);
                relativeLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START);
                relativeLayoutParams.addRule(RelativeLayout.END_OF, mBadgeImage.getId());
            }
            mContentView.setLayoutParams(relativeLayoutParams);
        }

        if (mBadgeImage != null) {
            RelativeLayout.LayoutParams relativeLayoutParams =
                    (RelativeLayout.LayoutParams) mBadgeImage.getLayoutParams();
            if (hasContent) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mContentView.getId());
            } else if (hasTitle) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mTitleView.getId());
            }
            mBadgeImage.setLayoutParams(relativeLayoutParams);
        }

        // Backward compatibility: Newly created ImagePlusCardViews should change
        // the InfoArea's background color in XML using the corresponding style.
        // However, since older implementations might make use of the
        // 'infoAreaBackground' attribute, we have to make sure to support it.
        // If the user has set a specific value here, it will differ from null.
        // In this case, we do want to override the value set in the style.
        Drawable background = cardAttrs.getDrawable(android.support.v17.leanback.R.styleable.lbImageCardView_infoAreaBackground);
        if (null != background) {
            setInfoAreaBackground(background);
        }
        // Backward compatibility: There has to be an icon in the default
        // version. If there is one, we have to set it's visibility to 'GONE'.
        // Disabling 'adjustIconVisibility' allows the user to set the icon's
        // visibility state in XML rather than code.
        if (mBadgeImage != null && mBadgeImage.getDrawable() == null) {
            mBadgeImage.setVisibility(View.GONE);
        }
        cardAttrs.recycle();
    }

    public ImageCardView(Context context) {
        this(context, null);
    }

    public ImageCardView(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v17.leanback.R.attr.imageCardViewStyle);
    }

    /**
     * Returns the main image view.
     */
    public final ImageView getMainImageView() {
        return mImageView;
    }

    public void allowDoubleLine(boolean val) { mAllowDoubleLine = val;}

    /**
     * Sets the image drawable with fade-in animation.
     */
    public void setMainImage(Drawable drawable) {
        setMainImage(drawable, true);
    }

    /**
     * Sets the image drawable with optional fade-in animation.
     */
    public void setMainImage(Drawable drawable, boolean fade) {
        if (mImageView == null) {
            return;
        }

        mImageView.setImageDrawable(drawable);
        if (drawable == null) {
            mImageView.animate().cancel();
            mImageView.setAlpha(1f);
            mImageView.setVisibility(View.INVISIBLE);
        } else {
            mImageView.setVisibility(View.VISIBLE);
            if (fade) {
                fadeIn();
            } else {
                mImageView.animate().cancel();
                mImageView.setAlpha(1f);
            }
        }
    }

    public void setFlag(Drawable drawable, String text) {

        if (drawable == null)
            mFlagView.setVisibility(View.INVISIBLE);
        else {
            mFlagView.setImageDrawable(drawable);
            mFlagView.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(text))
            mFlagText.setVisibility(View.INVISIBLE);
        else {
            mFlagText.setText(text);
            mFlagText.setVisibility(View.VISIBLE);
        }
    }

    public void setProgress(int progress) {
        if (progress == 0)
            mProgressView.setVisibility(View.GONE);
        else {
            mProgressView.setProgress(progress);
            mProgressView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the layout dimensions of the ImageView.
     */
    public void setMainImageDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mImageView.setLayoutParams(lp);

        lp = mProgressView.getLayoutParams();
        lp.width = width;
        mProgressView.setLayoutParams(lp);
    }


    /**
     * Sets the info area background drawable.
     */
    public void setInfoAreaBackground(Drawable drawable) {
        if (mInfoArea != null) {
            mInfoArea.setBackground(drawable);
        }
    }

    /**
     * Sets the title text.
     */
    public void setTitleText(CharSequence text) {
        if (mTitleView == null) {
            return;
        }
        mTitleView.setText(text);
        if (mAllowDoubleLine && TextUtils.isEmpty(mContentView.getText())) {

            mTitleView.setMinLines(2);
            mTitleView.setMaxLines(2);
        }
    }


    /**
     * Sets the content text.
     */
    public void setContentText(CharSequence text) {
        if (mContentView == null) {
            return;
        }
        mContentView.setText(text);
        mContentView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        if (mAllowDoubleLine && !TextUtils.isEmpty(text)) {

            mTitleView.setMinLines(1);
            mTitleView.setMaxLines(1);
        }
    }


    /**
     * Sets the badge image drawable.
     */
    public void setBadgeImage(Drawable drawable) {
        if (mBadgeImage == null) {
            return;
        }
        mBadgeImage.setImageDrawable(drawable);
        if (drawable != null) {
            mBadgeImage.setVisibility(View.VISIBLE);
        } else {
            mBadgeImage.setVisibility(View.GONE);
        }
    }


    private void fadeIn() {
        mImageView.setAlpha(0f);
        if (mAttachedToWindow) {
            mImageView.animate().alpha(1f).setDuration(
                    mImageView.getResources().getInteger(android.R.integer.config_shortAnimTime));
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        if (mImageView.getAlpha() == 0) {
            fadeIn();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mAttachedToWindow = false;
        mImageView.animate().cancel();
        mImageView.setAlpha(1f);
        super.onDetachedFromWindow();
    }
}
