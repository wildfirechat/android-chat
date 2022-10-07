/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.bumptech.glide.request.RequestOptions;

import org.webrtc.RendererCommon;

import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfire.chat.kit.voip.ZoomableFrameLayout;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceParticipantItemView extends FrameLayout {
    public ImageView portraitImageView;
    public TextView statusTextView;
    public ZoomableFrameLayout videoContainer;
    private boolean enableVideoZoom = true;
    private MicImageView micImageView;
    private ImageView videoStateImageView;
    private TextView nameTextView;

    private static final RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

    public ConferenceParticipantItemView(@NonNull Context context) {
        super(context);
        initView(context, null);
    }

    public ConferenceParticipantItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ConferenceParticipantItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConferenceParticipantItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void initView(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.av_conference_participant_grid_item, this);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        statusTextView = view.findViewById(R.id.statusTextView);
        videoContainer = view.findViewById(R.id.videoContainer);
        micImageView = view.findViewById(R.id.micImageView);
        videoStateImageView = view.findViewById(R.id.videoStateImageView);
        nameTextView = view.findViewById(R.id.userNameTextView);
        videoContainer.setEnableZoom(enableVideoZoom);
    }

    public ImageView getPortraitImageView() {
        return portraitImageView;
    }

    public TextView getStatusTextView() {
        return statusTextView;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        if (l != null) {
            videoContainer.setOnClickListener(v -> l.onClick(ConferenceParticipantItemView.this));
        } else {
            videoContainer.setOnClickListener(null);
        }
    }

    /**
     * 是否开启视频缩放
     */
    public void setEnableVideoZoom(boolean enable) {
        this.enableVideoZoom = enable;
        videoContainer.setEnableZoom(enable);
    }

    public void setup(AVEngineKit.CallSession session, AVEngineKit.ParticipantProfile profile) {
        String participantKey = VoipBaseActivity.participantKey(profile.getUserId(), profile.isScreenSharing());
        UserInfo userInfo = ChatManager.Instance().getUserInfo(profile.getUserId(), false);
        this.setTag(participantKey);

        GlideApp.with(this).load(userInfo.portrait).apply(new RequestOptions().circleCrop()).placeholder(R.mipmap.avatar_def).into(portraitImageView);
//        if (!profile.isVideoMuted()) {
            videoContainer.setVisibility(VISIBLE);
            if (profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                session.setupLocalVideoView(videoContainer, scalingType);
            } else {
                session.setupRemoteVideoView(profile.getUserId(), profile.isScreenSharing(), videoContainer, scalingType);
            }
            //statusTextView.setText(R.string.connecting);
//        } else {
//            videoContainer.setVisibility(GONE);
//        }
        videoStateImageView.setSelected(profile.isAudience() || profile.isVideoMuted());
        micImageView.setMuted(profile.isAudience() || profile.isAudioMuted());
        nameTextView.setText(ChatManager.Instance().getUserDisplayName(profile.getUserId()));
    }

    public void updateParticipantProfile(AVEngineKit.ParticipantProfile profile) {

    }

    public void updateVolume(int volume) {
        int padding = 0;
        if (volume > 500) {
            padding = 2;
            videoContainer.setBackgroundResource(R.drawable.av_conference_participant_highlight_boarder);
        } else {
            videoContainer.setBackground(null);
        }
        videoContainer.setPadding(padding, padding, padding, padding);
        micImageView.setVolume(volume);
    }
}
