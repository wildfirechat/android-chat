/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.live.message.LiveCoStreamContent;
import cn.wildfire.chat.kit.live.model.LiveInfo;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.live.message.LiveMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 观众观看直播页面（仅 HLS 播放，连麦跳转到 LiveCoStreamActivity）
 */
@androidx.media3.common.util.UnstableApi
public class LiveAudienceActivity extends FragmentActivity {
    private LowLatencyHlsPlayerView videoView;
    private ProgressBar loadingProgressBar;
    private ImageView hostAvatarImageView;
    private TextView hostNameTextView;
    private TextView liveTagTextView;
    private ImageButton shareButton;
    private ImageButton floatButton;
    private ImageButton closeButton;
    private ImageButton requestCoStreamButton;
    private TextView sayingSomethingView;

    private LiveInfo liveInfo;

    private Observer<Object> coStreamInviteObserver;
    private Observer<Object> coStreamAcceptedObserver;
    private Observer<Object> coStreamRejectedObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_live_audience);

        liveInfo = getIntent().getParcelableExtra("liveInfo");

        if (liveInfo == null) {
            finish();
            return;
        }

        bindViews();
        bindEvents();
        subscribeCoStreamEvents();
        startWatching();

        LiveKit.getInstance().setCurrentLiveInfo(liveInfo);
        LiveKit.getInstance().joinLiveChatRoom();
    }

    private void bindViews() {
        videoView = findViewById(R.id.videoView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        hostAvatarImageView = findViewById(R.id.hostAvatarImageView);
        hostNameTextView = findViewById(R.id.hostNameTextView);
        liveTagTextView = findViewById(R.id.liveTagTextView);
        shareButton = findViewById(R.id.shareButton);
        floatButton = findViewById(R.id.floatButton);
        closeButton = findViewById(R.id.closeButton);
        requestCoStreamButton = findViewById(R.id.requestCoStreamButton);
        sayingSomethingView = findViewById(R.id.sayingSomethingView);

        String hostUserId = liveInfo.getHost();
        if (!TextUtils.isEmpty(hostUserId)) {
            UserInfo hostInfo = ChatManager.Instance().getUserInfo(hostUserId, false);
            if (hostInfo != null) {
                if (hostInfo.displayName != null && !hostInfo.displayName.isEmpty()) {
                    hostNameTextView.setText(hostInfo.displayName);
                }
                if (hostInfo.portrait != null && !hostInfo.portrait.isEmpty()) {
                    Glide.with(this).load(hostInfo.portrait).circleCrop().into(hostAvatarImageView);
                }
            }
        }
    }

    private void bindEvents() {
        closeButton.setOnClickListener(v -> showExitConfirmDialog());
        requestCoStreamButton.setOnClickListener(v -> showCoStreamOptions());
        if (sayingSomethingView != null) {
            sayingSomethingView.setOnClickListener(v -> {
                String cid = liveInfo.getLiveId();
                if (cid != null) {
                    LiveMessageInputDialogFragment.newInstance(cid)
                            .show(getSupportFragmentManager(), "liveInput");
                }
            });
        }
        shareButton.setOnClickListener(v -> shareLive());
        floatButton.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.live_permission_float_required, Toast.LENGTH_SHORT).show();
                return;
            }
            LiveService.startForAudience(this, this.liveInfo, true);
            finish();
        });
    }

    private void subscribeCoStreamEvents() {
        coStreamInviteObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveInfo.getLiveId().equals(content.getCallId())) {
                    runOnUiThread(() -> showCoStreamInviteDialog(content));
                }
            }
        };
        coStreamAcceptedObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveInfo.getLiveId().equals(content.getCallId())) {
                    runOnUiThread(() -> launchCoStreamActivity(content));
                }
            }
        };
        coStreamRejectedObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveInfo.getLiveId().equals(content.getCallId())) {
                    runOnUiThread(() -> Toast.makeText(this,
                            R.string.live_co_stream_rejected, Toast.LENGTH_SHORT).show());
                }
            }
        };
        LiveDataBus.subscribeForever(LiveKit.EVENT_CO_STREAM_INVITE, coStreamInviteObserver);
        LiveDataBus.subscribeForever(LiveKit.EVENT_CO_STREAM_ACCEPTED, coStreamAcceptedObserver);
        LiveDataBus.subscribeForever(LiveKit.EVENT_CO_STREAM_REJECTED, coStreamRejectedObserver);
    }

    private void startWatching() {
        String callId = liveInfo.getLiveId();

        if (getSupportFragmentManager().findFragmentById(R.id.messageFragmentContainer) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.messageFragmentContainer, LiveMessageFragment.newInstance(callId, false))
                    .commitAllowingStateLoss();
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        liveTagTextView.setVisibility(View.GONE);

        videoView.setVideoURI(Uri.parse(liveInfo.getHlsUrl()));
        videoView.setOnPreparedListener(() -> {
            loadingProgressBar.setVisibility(View.GONE);
            liveTagTextView.setVisibility(View.VISIBLE);
            videoView.start();
        });
        videoView.setOnErrorListener((e) -> {
            loadingProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.live_streaming_load_failed, Toast.LENGTH_SHORT).show();
            finish();
        });
        videoView.setOnCompletionListener(() -> {
            Toast.makeText(this, R.string.live_streaming_ended, Toast.LENGTH_SHORT).show();
            finish();
        });
        videoView.start();
    }

    private void shareLive() {
        Message message = new Message();
        message.content = new LiveMessageContent(
                liveInfo.getLiveId(), liveInfo.getHost(), liveInfo.getTitle(), liveInfo.getDescription(),
                liveInfo.getStartTimestamp(), liveInfo.isAudioOnly(), liveInfo.isAudience());
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        Intent intent = new Intent(this, ForwardActivity.class);
        intent.putExtra("messages", messages);
        startActivity(intent);
    }

    private void showCoStreamOptions() {
        LiveCoStreamOptionsFragment.newInstance(liveInfo)
                .show(getSupportFragmentManager(), "coStreamOptions");
    }

    private void showCoStreamInviteDialog(LiveCoStreamContent content) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_live_co_stream_invite, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialogView.findViewById(R.id.dialogRejectButton).setOnClickListener(v -> {
            dialog.dismiss();
            LiveKit.getInstance().rejectCoStreamInvite(content);
        });
        dialogView.findViewById(R.id.dialogAcceptButton).setOnClickListener(v -> {
            dialog.dismiss();
            launchCoStreamActivity(content);
        });
        dialog.show();
    }

    private void showExitConfirmDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_live_confirm, null);
        ((TextView) dialogView.findViewById(R.id.confirmTitleView)).setText(R.string.live_exit_confirm_title);
        ((TextView) dialogView.findViewById(R.id.confirmMessageView)).setText(R.string.live_exit_confirm_message);
        ((TextView) dialogView.findViewById(R.id.confirmOkButton)).setText(R.string.live_confirm_ok);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);
            dialog.getWindow().getAttributes().width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        }
        dialogView.findViewById(R.id.confirmCancelButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.confirmOkButton).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }

    /** Stop HLS and open LiveCoStreamActivity to handle the WebRTC conference. */
    private void launchCoStreamActivity(LiveCoStreamContent content) {
        if (videoView != null) videoView.stopPlayback();
        Intent intent = new Intent(this, LiveCoStreamActivity.class);
        intent.putExtra("liveInfo", liveInfo);
        intent.putExtra("coStreamContent", content);
        startActivity(intent);
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WindowInsetsControllerCompat insetsController =
                    new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiveService.stop(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations()) {
            LiveService.startForAudience(this, this.liveInfo, true);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveDataBus.unsubscribe(LiveKit.EVENT_CO_STREAM_INVITE, coStreamInviteObserver);
        LiveDataBus.unsubscribe(LiveKit.EVENT_CO_STREAM_ACCEPTED, coStreamAcceptedObserver);
        LiveDataBus.unsubscribe(LiveKit.EVENT_CO_STREAM_REJECTED, coStreamRejectedObserver);
        LiveKit.getInstance().reset();
        if (videoView != null) {
            videoView.stopPlayback();
            videoView.release();
        }
    }
}
