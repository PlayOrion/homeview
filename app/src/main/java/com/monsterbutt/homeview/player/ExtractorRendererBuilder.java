/*
 * Copyright (c) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.monsterbutt.homeview.player;

import android.app.Activity;
import android.media.MediaCodec;
import android.net.Uri;

import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.subrip.SubripParser;
import com.google.android.exoplayer.text.ttml.TtmlParser;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.monsterbutt.homeview.player.parser.PgsParser;
import com.monsterbutt.homeview.player.renderers.DeviceAudioTrackRenderer;
import com.monsterbutt.homeview.player.renderers.FfmpegAudioTrackRenderer;
import com.monsterbutt.homeview.player.ffmpeg.FfmpegTrackRenderer;

/**
 * A {@link VideoPlayer.RendererBuilder} for streams that can be read using an {@link Extractor}.
 * <p/>
 * This code was originally taken from the ExoPlayer demo application.
 */
public class ExtractorRendererBuilder implements VideoPlayer.RendererBuilder, FrameRateSwitcher.FrameRateSwitcherListener {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Activity activity;
    private final String userAgent;
    private final Uri uri;

    public ExtractorRendererBuilder(Activity activity, String userAgent, Uri uri) {
        this.activity = activity;
        this.userAgent = userAgent;
        this.uri = uri;
    }

    @Override
    public void buildRenderers(final VideoPlayer player) {

        FrameRateSwitcher.setDisplayRefreshRate(activity, player, this);
    }

    private void setupRenderers(VideoPlayer player) {
        // Build the video and audio renderers.
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        DataSource dataSource = new DefaultUriDataSource(activity, bandwidthMeter, userAgent, true);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(activity,
                sampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                player.getMainHandler(), player, 50);
        DeviceAudioTrackRenderer audioRenderer = DeviceAudioTrackRenderer.getRenderer(activity, player, sampleSource);
        FfmpegTrackRenderer ffmpegAudioRenderer = FfmpegAudioTrackRenderer.getRenderer(activity, player, sampleSource);
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper(), new PgsParser(), new SubripParser(), new TtmlParser());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[VideoPlayer.RENDERER_COUNT];
        renderers[VideoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[VideoPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[VideoPlayer.TYPE_TEXT] = textRenderer;
        renderers[VideoPlayer.TYPE_AUDIO_FFMPEG] = ffmpegAudioRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    @Override
    public void cancel() {
        // Do nothing.
    }

    @Override
    public void switchOccured(final VideoPlayer player, FrameRateSwitcher switcher) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setupRenderers(player);
            }
        });
    }
}