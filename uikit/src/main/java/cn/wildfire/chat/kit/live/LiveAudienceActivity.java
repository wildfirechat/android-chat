/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
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
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfirechat.message.LiveStreamingStartMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 观众观看直播页面（仅 HLS 播放，连麦跳转到 LiveCoStreamActivity）
 */
public class LiveAudienceActivity extends FragmentActivity {
    private FullScreenVideoView videoView;
    private ProgressBar loadingProgressBar;
    private ImageView hostAvatarImageView;
    private TextView hostNameTextView;
    private TextView liveTagTextView;
    private ImageButton shareButton;
    private ImageButton floatButton;
    private ImageButton closeButton;
    private ImageButton requestCoStreamButton;

    private LiveStreamingStartMessageContent liveContent;

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

        liveContent = getIntent().getParcelableExtra("liveContent");
        if (liveContent == null) {
            finish();
            return;
        }

        bindViews();
        bindEvents();
        subscribeCoStreamEvents();
        startWatching();
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

        String hostUserId = liveContent.getHost();
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
        closeButton.setOnClickListener(v -> finish());
        requestCoStreamButton.setOnClickListener(v -> showCoStreamOptions());
        shareButton.setOnClickListener(v -> shareLive());
        floatButton.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.live_permission_float_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String title = liveContent.getTitle() != null ? liveContent.getTitle()
                    : getString(R.string.live_streaming);
            UserInfo hostInfo = ChatManager.Instance().getUserInfo(liveContent.getHost(), false);
            String hlsUrl = LiveStreamingKit.getHlsUrl(liveContent.getCallId());
            LiveStreamingFloatService.start(this, title, false, hostInfo != null ? hostInfo.portrait : null, hlsUrl, getIntent());
            finish();
        });
    }

    private void subscribeCoStreamEvents() {
        coStreamInviteObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveContent.getCallId().equals(content.getCallId())) {
                    runOnUiThread(() -> showCoStreamInviteDialog(content));
                }
            }
        };
        coStreamAcceptedObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveContent.getCallId().equals(content.getCallId())) {
                    runOnUiThread(() -> launchCoStreamActivity(content));
                }
            }
        };
        coStreamRejectedObserver = o -> {
            if (o instanceof LiveCoStreamContent) {
                LiveCoStreamContent content = (LiveCoStreamContent) o;
                if (liveContent.getCallId().equals(content.getCallId())) {
                    runOnUiThread(() -> Toast.makeText(this,
                            R.string.live_co_stream_rejected, Toast.LENGTH_SHORT).show());
                }
            }
        };
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_INVITE, coStreamInviteObserver);
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_ACCEPTED, coStreamAcceptedObserver);
        LiveDataBus.subscribeForever(LiveStreamingKit.EVENT_CO_STREAM_REJECTED, coStreamRejectedObserver);
    }

    private void startWatching() {
        String callId = liveContent.getCallId();
        String streamUrl = LiveStreamingKit.getHlsUrl(callId);

        if (getSupportFragmentManager().findFragmentById(R.id.messageFragmentContainer) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.messageFragmentContainer, LiveMessageFragment.newInstance(callId))
                    .commitAllowingStateLoss();
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        liveTagTextView.setVisibility(View.GONE);

        videoView.setVideoURI(Uri.parse(streamUrl));
        videoView.setOnPreparedListener(mp -> {
            loadingProgressBar.setVisibility(View.GONE);
            liveTagTextView.setVisibility(View.VISIBLE);
            mp.setLooping(false);
            videoView.setVideoSize(mp.getVideoWidth(), mp.getVideoHeight());
            videoView.start();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            loadingProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.live_streaming_load_failed, Toast.LENGTH_SHORT).show();
            return true;
        });
        videoView.setOnCompletionListener(mp -> {
            Toast.makeText(this, R.string.live_streaming_ended, Toast.LENGTH_SHORT).show();
            finish();
        });
        videoView.requestFocus();
        videoView.start();
    }

    private void shareLive() {
        Message message = new Message();
        message.content = liveContent;
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        Intent intent = new Intent(this, ForwardActivity.class);
        intent.putExtra("messages", messages);
        startActivity(intent);
    }

    private void showCoStreamOptions() {
        LiveCoStreamOptionsFragment.newInstance(liveContent)
                .show(getSupportFragmentManager(), "coStreamOptions");
    }

    private void showCoStreamInviteDialog(LiveCoStreamContent content) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.live_co_stream_invite_title)
                .setMessage(R.string.live_co_stream_invite_message)
                .setPositiveButton(R.string.live_co_stream_accept, (d, w) -> launchCoStreamActivity(content))
                .setNegativeButton(R.string.live_co_stream_reject, (d, w) ->
                        LiveStreamingKit.getInstance().rejectCoStreamInvite(content))
                .setCancelable(false)
                .show();
    }

    /** Stop HLS and open LiveCoStreamActivity to handle the WebRTC conference. */
    private void launchCoStreamActivity(LiveCoStreamContent content) {
        if (videoView != null) videoView.stopPlayback();
        Intent intent = new Intent(this, LiveCoStreamActivity.class);
        intent.putExtra("coStreamContent", content);
        intent.putExtra("liveContent", liveContent);
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
        LiveStreamingFloatService.stop(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations()) {
            String title = liveContent.getTitle() != null ? liveContent.getTitle()
                    : getString(R.string.live_streaming);
            UserInfo hostInfo = ChatManager.Instance().getUserInfo(liveContent.getHost(), false);
            String hlsUrl = LiveStreamingKit.getHlsUrl(liveContent.getCallId());
            LiveStreamingFloatService.start(this, title, false, hostInfo != null ? hostInfo.portrait : null, hlsUrl, getIntent());
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveDataBus.unsubscribe(LiveStreamingKit.EVENT_CO_STREAM_INVITE, coStreamInviteObserver);
        LiveDataBus.unsubscribe(LiveStreamingKit.EVENT_CO_STREAM_ACCEPTED, coStreamAcceptedObserver);
        LiveDataBus.unsubscribe(LiveStreamingKit.EVENT_CO_STREAM_REJECTED, coStreamRejectedObserver);
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
