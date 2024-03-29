package com.monsterbutt.homeview.plex.media;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.Utils;

import java.util.List;

import us.nineworlds.plex.rest.model.impl.Collection;
import us.nineworlds.plex.rest.model.impl.Video;


public class Movie extends PlexVideoItem implements Parcelable {

    public static String TYPE = "movie";

    public Movie(Video video) {
        super(video);
    }

    protected Movie(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    public String getStudio() {
        return mVideo.getStudio();
    }
    public String getTagline() {
        return mVideo.getTagLine();
    }
    public List<Collection> getCollection() {
        return mVideo.getCollections();
    }

    @Override
    public String getCardContent(Context context) {
        return  String.format("%s %s %s %s",
                getYear(),
                context.getString(R.string.mid_dot),
                Long.toString(getDurationInMin()),
                context.getString(R.string.minutes_abbrev));
    }

    @Override
    public String getPlaybackTitle(Context context) {
        return getTitle();
    }

    @Override
    public String getPlaybackSubtitle(Context context) {

        String tag = getTagline();
        if (!TextUtils.isEmpty(tag))
            return String.format("%s %s %s", getYear(), context.getString(R.string.mid_dot), tag);
        return getYear();
    }

    public String getPlaybackImageURL() {
        return getCardImageURL();
    }

    @Override
    public String getWideCardImageURL() {

        String ret = mVideo.getBackgroundImageKey();
        if (TextUtils.isEmpty(ret))
            ret = getCardImageURL();
        return ret;
    }

    @Override
    public String getDetailSubtitle(Context context) { return getTagline(); }

    @Override
    public String getDetailContent(Context context) {

        String date = Utils.convertDateToText(context, mVideo.getOriginallyAvailableDate());
        if (TextUtils.isEmpty(date))
            date = TextUtils.isEmpty(getYear()) ? "" : getYear();
        return date;
    }
}
