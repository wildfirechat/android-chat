/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.webrtc.RendererCommon;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

/**
 * 直播悬浮窗 Service
 * <p>用户点击"悬浮"按钮时启动此 Service，显示一个可拖拽的小悬浮窗。
 * 点击悬浮窗时带回原直播 Activity。</p>
 */
public class LiveStreamingFloatService extends Service {

    private static final String CHANNEL_ID = "live_float_channel";
    private static final int NOTIFICATION_ID = 9001;

    public static final String EXTRA_TITLE = "live_title";
    public static final String EXTRA_IS_HOST = "live_is_host";
    public static final String EXTRA_IS_CO_STREAM = "live_is_co_stream";
    public static final String EXTRA_HOST_USER_ID = "live_host_user_id";
    public static final String EXTRA_PORTRAIT = "live_portrait";
    public static final String EXTRA_HLS_URL = "live_hls_url";
    public static final String EXTRA_ORIGINAL_INTENT = "live_original_intent";
    public static final String ACTION_STOP = "cn.wildfire.live.STOP_FLOAT";

    private WindowManager wm;
    private View floatView;
    private WindowManager.LayoutParams params;
    private Intent mOriginalIntent;
    private String mHlsUrl;
    private boolean mIsCoStream;
    private boolean mIsHost;
    private String mHostUserId;

    // Touch drag
    private float touchStartX, touchStartY;
    private int layoutStartX, layoutStartY;

    // ── Public static helpers ────────────────────────────────────────────────

    /** Start floating window (called from Activity). */
    public static void start(Context ctx, String title, boolean isHost, String portrait, String hlsUrl, Intent originalIntent) {
        start(ctx, title, isHost, false, "", portrait, hlsUrl, originalIntent);
    }

    /** Start floating window for a co-stream session (audience side). */
    public static void start(Context ctx, String title, boolean isHost, boolean isCoStream,
                             String hostUserId, String portrait, String hlsUrl, Intent originalIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
            return;
        }
        Intent intent = new Intent(ctx, LiveStreamingFloatService.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_IS_HOST, isHost);
        intent.putExtra(EXTRA_IS_CO_STREAM, isCoStream);
        intent.putExtra(EXTRA_HOST_USER_ID, hostUserId);
        intent.putExtra(EXTRA_PORTRAIT, portrait);
        intent.putExtra(EXTRA_HLS_URL, hlsUrl);
        intent.putExtra(EXTRA_ORIGINAL_INTENT, originalIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }
    }

    /** Stop floating window (called when Activity comes back to foreground). */
    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, LiveStreamingFloatService.class));
    }

    // ── Service lifecycle ────────────────────────────────────────────────────

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String title = intent != null
                ? intent.getStringExtra(EXTRA_TITLE) : getString(R.string.live_streaming);
        mIsHost = intent != null && intent.getBooleanExtra(EXTRA_IS_HOST, false);
        mIsCoStream = intent != null && intent.getBooleanExtra(EXTRA_IS_CO_STREAM, false);
        mHostUserId = intent != null ? intent.getStringExtra(EXTRA_HOST_USER_ID) : null;
        String portrait = intent != null ? intent.getStringExtra(EXTRA_PORTRAIT) : null;
        mHlsUrl = intent != null ? intent.getStringExtra(EXTRA_HLS_URL) : null;
        mOriginalIntent = intent != null ? intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT) : null;

        startForegroundWithNotification(title, mIsHost);
        showFloatView(title, mIsHost, portrait);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        hideFloatView();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ── Foreground notification ───────────────────────────────────────────────

    private void startForegroundWithNotification(String title, boolean isHost) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.live_streaming), NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Intent resumeIntent = new Intent(this, mIsCoStream ? LiveCoStreamActivity.class
                : (isHost ? LiveHostActivity.class : LiveAudienceActivity.class));
        if (mOriginalIntent != null) {
            resumeIntent.putExtras(mOriginalIntent);
        }
        resumeIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, resumeIntent, piFlags);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.live_streaming))
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_live_badge)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    // ── Float view ───────────────────────────────────────────────────────────

    private void showFloatView(String title, boolean isHost, String portrait) {
        if (wm != null) return;

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params.type = type;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        params.format = PixelFormat.TRANSLUCENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 24;
        params.y = 200;

        floatView = LayoutInflater.from(this).inflate(R.layout.view_live_float, null);
        TextView titleView = floatView.findViewById(R.id.floatTitleTextView);
        if (titleView != null && title != null) titleView.setText(title);

        FrameLayout videoContainer = floatView.findViewById(R.id.videoContainer);

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            videoContainer.setVisibility(View.VISIBLE);
            if (isHost) {
                // Host: show local camera
                session.setupLocalVideoView(videoContainer, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            } else if (mIsCoStream && !TextUtils.isEmpty(mHostUserId)) {
                // Co-stream audience: always show the host's video
                session.setupRemoteVideoView(mHostUserId, videoContainer, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            } else {
                // Fallback: first non-self, non-robot participant
                for (String userId : session.getParticipantIds()) {
                    if (!userId.equals(ChatManager.Instance().getUserId())
                            && !Config.LIVE_STREAMING_ROBOT.equals(userId)) {
                        session.setupRemoteVideoView(userId, videoContainer, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                        break;
                    }
                }
            }
        } else if (!isHost && !TextUtils.isEmpty(mHlsUrl)) {
            videoContainer.setVisibility(View.VISIBLE);
            android.widget.VideoView videoView = new android.widget.VideoView(this);
            videoContainer.addView(videoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            videoView.setVideoPath(mHlsUrl);
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(false);
                videoView.start();
            });
            videoView.setOnErrorListener((mp, what, extra) -> true);
        } else if (!TextUtils.isEmpty(portrait)) {
            videoContainer.setVisibility(View.VISIBLE);
            ImageView portraitImageView = new ImageView(this);
            portraitImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            videoContainer.addView(portraitImageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            com.bumptech.glide.Glide.with(this).load(portrait).placeholder(R.drawable.live_avatar_placeholder).into(portraitImageView);
        }

        // Drag + click
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        moved = false;
                        touchStartX = event.getRawX();
                        touchStartY = event.getRawY();
                        layoutStartX = params.x;
                        layoutStartY = params.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - touchStartX;
                        float dy = event.getRawY() - touchStartY;
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true;
                        params.x = (int) (layoutStartX - dx); // gravity=END, so invert x
                        params.y = (int) (layoutStartY + dy);
                        if (wm != null && floatView != null) {
                            wm.updateViewLayout(floatView, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            // Tap → resume activity
                            resumeActivity(isHost);
                        }
                        return true;
                }
                return false;
            }
        });

        wm.addView(floatView, params);
    }

    private void hideFloatView() {
        if (wm != null && floatView != null) {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null) {
                // Detach whichever renderer was attached to the float container
                if (mIsHost) {
                    try {
                        session.setupLocalVideoView(null, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    } catch (Exception ignored) {
                    }
                } else if (!TextUtils.isEmpty(mHostUserId)) {
                    try {
                        session.setupRemoteVideoView(mHostUserId, null, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    } catch (Exception ignored) {
                    }
                }
            }
            wm.removeView(floatView);
            wm = null;
            floatView = null;
        }
    }

    private void resumeActivity(boolean isHost) {
        Class<?> cls = mIsCoStream ? LiveCoStreamActivity.class
                : (isHost ? LiveHostActivity.class : LiveAudienceActivity.class);
        Intent intent = new Intent(this, cls);
        if (mOriginalIntent != null) {
            intent.putExtras(mOriginalIntent);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        stopSelf();
    }
}
