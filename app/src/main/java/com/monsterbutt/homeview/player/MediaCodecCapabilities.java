package com.monsterbutt.homeview.player;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.text.TextUtils;

import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.util.MimeTypes;
import com.monsterbutt.homeview.settings.SettingsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaCodecCapabilities {

    public enum DecodeType {
        Hardware,
        Passthrough,
        LegacyPassthrough,
        Software,
        Unsupported
    }

    private static final int NO_ENCODING = -1;

    private static MediaCodecCapabilities gInstance = null;
    public static MediaCodecCapabilities getInstance(Context context) {

        if (gInstance == null)
            gInstance = new MediaCodecCapabilities(context);
        return gInstance;
    }

    private final Context context;
    private final AudioCapabilities audioCapabilities;
    private final MediaCodecInfo[] regularCodecs;
    private final Map<String, List<MediaCodecInfo>> videoDecoderCodecs = new HashMap<>();
    private final Map<String, List<MediaCodecInfo>> audioDecoderCodecs = new HashMap<>();
    private final Map<String, DecodeType>           subtitleDecoderCodecs = new HashMap<>();

    private static final Map<String,String> codecTranslations;
    static
    {
        codecTranslations = new HashMap<>();
        codecTranslations.put("video/h263",     MimeTypes.VIDEO_MPEG2);
        codecTranslations.put("video/mpeg2v",   MimeTypes.VIDEO_MPEG2);
        codecTranslations.put("video/mpeg2video",MimeTypes.VIDEO_MPEG2);
        codecTranslations.put("video/h264",     MimeTypes.VIDEO_H264);
        codecTranslations.put("video/h265",     MimeTypes.VIDEO_H265);
        codecTranslations.put("video/vc1",      MimeTypes.VIDEO_VC1);

        codecTranslations.put("audio/pcm",      MimeTypes.AUDIO_RAW);
        codecTranslations.put("audio/truehd",   MimeTypes.AUDIO_TRUEHD);
        codecTranslations.put("audio/dca",      MimeTypes.AUDIO_DTS);
        codecTranslations.put("audio/dca-hra",  MimeTypes.AUDIO_DTS_HD);
        codecTranslations.put("audio/dca-ma",   MimeTypes.AUDIO_DTS_HD);

        codecTranslations.put("text/pgs",       MimeTypes.APPLICATION_PGS);
    }

    private MediaCodecCapabilities(Context context) {

        this.context = context;
        audioCapabilities = AudioCapabilities.getCapabilities(context);
        regularCodecs = (new MediaCodecList(MediaCodecList.REGULAR_CODECS)).getCodecInfos();
        fillCodecs();
    }

    private boolean isVideoCodec(String mimeType) {
        return mimeType.startsWith(MimeTypes.BASE_TYPE_VIDEO);
    }

    private boolean isAudioCodec(String mimeType) {
        return mimeType.startsWith(MimeTypes.BASE_TYPE_AUDIO);
    }

    private boolean isSubtitleCodec(String mimeType) {
        return mimeType.startsWith(MimeTypes.BASE_TYPE_TEXT)
                || mimeType.startsWith(MimeTypes.BASE_TYPE_APPLICATION);
    }

    private void fillCodecs() {

        for (MediaCodecInfo codec : regularCodecs) {

            if (codec.isEncoder())
                continue;
            for (String mimeType : codec.getSupportedTypes()) {

                if (isVideoCodec(mimeType))
                    fillCodec(videoDecoderCodecs, mimeType, codec);
                else if (isAudioCodec(mimeType))
                    fillCodec(audioDecoderCodecs, mimeType, codec);
            }
        }

        // use hardware for subs to keep the label from displaying, it's really software
        subtitleDecoderCodecs.put(MimeTypes.APPLICATION_PGS, DecodeType.Hardware);
    }

    private void fillCodec(Map<String, List<MediaCodecInfo>> map, String mimeType, MediaCodecInfo codec) {

        List<MediaCodecInfo> list = map.get(mimeType);
        if (list == null) {

            list = new ArrayList<>();
            map.put(mimeType, list);
        }
        list.add(codec);
    }

    private static int hasPassthroughForAudio(String mimeType, long bitDepth) {

        switch (mimeType) {
            case MimeTypes.AUDIO_AC3:
                return AudioFormat.ENCODING_AC3;
            case MimeTypes.AUDIO_E_AC3:
                return AudioFormat.ENCODING_E_AC3;
            case MimeTypes.AUDIO_DTS:
                return AudioFormat.ENCODING_DTS;
            case MimeTypes.AUDIO_DTS_HD:
                return AudioFormat.ENCODING_DTS_HD;
            case MimeTypes.AUDIO_RAW:
                if (bitDepth == 16)
                    return AudioFormat.ENCODING_PCM_16BIT;
            default:
                return NO_ENCODING;
        }
    }

    public boolean usePassthroughAudioIfAvailable() {

        return SettingsManager.getInstance(context).getBoolean("preferences_device_passthrough");
    }

    public DecodeType determineDecoderType(String trackType, String codec, String profile, long bitDepth) {

        String mimeType = !TextUtils.isEmpty(trackType) ? String.format("%s/%s", trackType, codec) : codec;

        if (isVideoCodec(mimeType))
            return determineVideoDecoderType(mimeType);
        if (isAudioCodec(mimeType))
            return determineAudioDecoderType(mimeType, profile, bitDepth);
        if (isSubtitleCodec(mimeType))
            return determinSubtitleDecoderType(mimeType);

        return DecodeType.Unsupported;
    }

    private DecodeType determineVideoDecoderType(String mimeType) {

        DecodeType ret = DecodeType.Unsupported;
        if (videoDecoderCodecs.get(mimeType) != null)
            ret = DecodeType.Hardware;
        else {

            String alt = codecTranslations.get(mimeType);
            if (!TextUtils.isEmpty(alt) && videoDecoderCodecs.get(alt) != null)
                ret = DecodeType.Hardware;
        }

        return ret;
    }

    private DecodeType determineAudioDecoderType(String mimeType, String profile, long bitDepth) {

        String mimeTypeProfile = String.format("%s-%s", mimeType, profile);

        DecodeType ret = !TextUtils.isEmpty(profile)
                        ?   determineAudioDecoderType(mimeTypeProfile, bitDepth)
                        :   DecodeType.Unsupported;
        if (ret != DecodeType.Passthrough && ret != DecodeType.Hardware) {

            // this can be changed when we can decode HD formats in software
            // we need to do legacy passthrough
            DecodeType retBase = determineAudioDecoderType(mimeType, bitDepth);
            ret = retBase;
        }
        return ret;
    }

    private DecodeType determineAudioDecoderType(String mimeType, long bitDepth) {

        DecodeType ret = DecodeType.Unsupported;
        if (determineAudioPassthrough(mimeType, bitDepth))
            ret = DecodeType.Passthrough;
        else {

            String alt = codecTranslations.get(mimeType);
            if (!TextUtils.isEmpty(alt) && determineAudioPassthrough(alt, bitDepth))
                ret = DecodeType.Passthrough;
            else if (audioDecoderCodecs.get(mimeType) != null)
                ret = DecodeType.Hardware;
            else if (!TextUtils.isEmpty(alt) && audioDecoderCodecs.get(alt) != null)
                ret = DecodeType.Hardware;
        }
        return ret;
    }

    private boolean determineAudioPassthrough(String mimeType, long bitDepth) {

        boolean ret = false;
        int encoding = hasPassthroughForAudio(mimeType, bitDepth);
        if (encoding != NO_ENCODING)
            ret = audioCapabilities.supportsEncoding(encoding);
        return ret;
    }

    private DecodeType determinSubtitleDecoderType(String mimeType) {

        DecodeType ret = subtitleDecoderCodecs.get(mimeType);
        if (ret != DecodeType.Hardware) {

            String alt = codecTranslations.get(mimeType);
            if (!TextUtils.isEmpty(alt))
                ret = subtitleDecoderCodecs.get(alt);
        }
        return ret;
    }

    public AudioCapabilities getSystemAudioCapabilities() {

        return audioCapabilities;
    }
}