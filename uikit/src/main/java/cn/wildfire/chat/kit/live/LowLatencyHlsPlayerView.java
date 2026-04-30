/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

/**
 * A low-latency HLS player using Media3 ExoPlayer.
 * It optimizes buffering and target offset to reduce latency to ~3-5 seconds.
 */
@UnstableApi
public class LowLatencyHlsPlayerView extends FrameLayout {

    private PlayerView playerView;
    private ExoPlayer player;
    private OnPreparedListener onPreparedListener;
    private OnErrorListener onErrorListener;
    private OnCompletionListener onCompletionListener;

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnErrorListener {
        void onError(Exception e);
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

    public LowLatencyHlsPlayerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public LowLatencyHlsPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        playerView = new PlayerView(context);
        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM); // Equivalent to center-crop
        addView(playerView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Configure LoadControl for low latency
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        1500,  // minBufferMs (Wait only 1.5s before starting)
                        5000,  // maxBufferMs
                        500,   // bufferForPlaybackMs
                        500    // bufferForPlaybackAfterRebufferMs
                )
                .build();

        player = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(context))
                .build();

        // Configure Live configuration for low latency
        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    if (onPreparedListener != null) {
                        onPreparedListener.onPrepared();
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    if (onCompletionListener != null) {
                        onCompletionListener.onCompletion();
                    }
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                if (onErrorListener != null) {
                    onErrorListener.onError(error);
                }
            }
        });

        playerView.setPlayer(player);
    }

    public void setVideoURI(Uri uri) {
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(3000) // Aim for 3 seconds latency
                        .build())
                .build();
        player.setMediaItem(mediaItem);
        player.prepare();
    }

    public void start() {
        player.setPlayWhenReady(true);
    }

    public void stopPlayback() {
        player.stop();
    }

    public void release() {
        player.release();
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }
}
