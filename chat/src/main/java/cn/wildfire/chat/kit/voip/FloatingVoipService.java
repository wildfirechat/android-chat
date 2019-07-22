package cn.wildfire.chat.kit.voip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.webrtc.StatsReport;

import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;

import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

public class FloatingVoipService extends Service {
    private static boolean isStarted = false;
    private static final int NOTIFICATION_ID = 1;

    private WindowManager wm;
    private View view;
    private WindowManager.LayoutParams params;
    private AVEngineKit.CallSession session;
    private Intent resumeActivityIntent;

    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStarted) {
            return START_NOT_STICKY;
        }

        isStarted = true;
        session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || AVEngineKit.CallState.Idle == session.getState()) {
            stopSelf();
        }

        resumeActivityIntent = new Intent(this, SingleVoipCallActivity.class);
        resumeActivityIntent.putExtra(SingleVoipCallActivity.EXTRA_FROM_FLOATING_VIEW, true);
        resumeActivityIntent.putExtra(SingleVoipCallActivity.EXTRA_MO, intent.getBooleanExtra(SingleVoipCallActivity.EXTRA_MO, false));
        resumeActivityIntent.putExtra(SingleVoipCallActivity.EXTRA_AUDIO_ONLY, intent.getBooleanExtra(SingleVoipCallActivity.EXTRA_AUDIO_ONLY, false));
        resumeActivityIntent.putExtra(SingleVoipCallActivity.EXTRA_TARGET, intent.getStringExtra(SingleVoipCallActivity.EXTRA_TARGET));
        resumeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resumeActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channelId = BuildConfig.APPLICATION_ID + ".voip";
            String channelName = "voip";
            NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("通话中...")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, builder.build());
        try {
            showFloatingWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wm.removeView(view);
        isStarted = false;
    }

    private void showFloatingWindow() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.type = type;
        params.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        params.format = PixelFormat.TRANSLUCENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        params.x = getResources().getDisplayMetrics().widthPixels;
        params.y = 0;

        view = LayoutInflater.from(this).inflate(R.layout.av_voip_float_view, null);
        view.setOnTouchListener(onTouchListener);
        wm.addView(view, params);
        if (session.isAudioOnly()) {
            showAudioInfo();
        } else {
            showVideoInfo();
        }
        session.setCallback(new AVEngineKit.CallSessionCallback() {
            @Override
            public void didCallEndWithReason(AVEngineKit.CallEndReason reason) {
                hideFloatBox();
            }

            @Override
            public void didChangeState(AVEngineKit.CallState state) {

            }

            @Override
            public void didChangeMode(boolean audioOnly) {
                handler.post(() -> showAudioInfo());
            }

            @Override
            public void didCreateLocalVideoTrack() {

            }

            @Override
            public void didReceiveRemoteVideoTrack() {

            }

            @Override
            public void didError(String error) {
                hideFloatBox();
            }

            @Override
            public void didGetStats(StatsReport[] reports) {

            }
        });
    }

    public void hideFloatBox() {
        stopSelf();
    }

    private void showAudioInfo() {
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        if (remoteVideoFrameLayout.getVisibility() == View.VISIBLE) {
            session.setupRemoteVideo(null, SCALE_ASPECT_BALANCED);
            remoteVideoFrameLayout.setVisibility(View.GONE);
            wm.removeView(view);
            wm.addView(view, params);
//            wm.updateViewLayout(view, params);
        }

        view.findViewById(R.id.audioLinearLayout).setVisibility(View.VISIBLE);
        TextView timeV = view.findViewById(R.id.durationTextView);
        ImageView mediaIconV = view.findViewById(R.id.av_media_type);
        mediaIconV.setImageResource(R.drawable.av_float_audio);
        refreshCallDurationInfo(timeV);
    }

    private void showVideoInfo() {
        view.findViewById(R.id.audioLinearLayout).setVisibility(View.GONE);
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        remoteVideoFrameLayout.setVisibility(View.VISIBLE);
        SurfaceView surfaceView = session.createRendererView();
        if (surfaceView != null) {
            remoteVideoFrameLayout.addView(surfaceView);
            session.setupRemoteVideo(surfaceView, SCALE_ASPECT_BALANCED);
        }
    }

    private void clickToResume() {
        startActivity(resumeActivityIntent);
    }

    private void refreshCallDurationInfo(TextView timeView) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || !session.isAudioOnly()) {
            return;
        }

        long duration = (System.currentTimeMillis() - session.getStartTime()) / 1000;
        if (duration >= 3600) {
            timeView.setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
        } else {
            timeView.setText(String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60)));
        }
        handler.postDelayed(() -> refreshCallDurationInfo(timeView), 1000);
    }

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        float lastX, lastY;
        int oldOffsetX, oldOffsetY;
        int tag = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            if (tag == 0) {
                oldOffsetX = params.x;
                oldOffsetY = params.y;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                lastX = x;
                lastY = y;
            } else if (action == MotionEvent.ACTION_MOVE) {
                // 减小偏移量,防止过度抖动
                params.x += (int) (x - lastX) / 3;
                params.y += (int) (y - lastY) / 3;
                tag = 1;
                wm.updateViewLayout(v, params);
            } else if (action == MotionEvent.ACTION_UP) {
                int newOffsetX = params.x;
                int newOffsetY = params.y;
                if (Math.abs(oldOffsetX - newOffsetX) <= 20 && Math.abs(oldOffsetY - newOffsetY) <= 20) {
                    clickToResume();
                    hideFloatBox();
                } else {
                    tag = 0;
                }
            }
            return true;
        }
    };
}
