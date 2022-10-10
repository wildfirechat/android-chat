/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.webrtc.RendererCommon;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.voip.ZoomableFrameLayout;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceParticipantItemVideoView extends ConferenceParticipantItemView {
    public ZoomableFrameLayout videoContainer;
    private boolean enableVideoZoom = false;

    private static final RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

    public ConferenceParticipantItemVideoView(@NonNull Context context) {
        super(context);
    }

    public ConferenceParticipantItemVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ConferenceParticipantItemVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConferenceParticipantItemVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    protected void initView(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.av_conference_participant_grid_item_video, this);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        statusTextView = view.findViewById(R.id.statusTextView);
        videoContainer = view.findViewById(R.id.videoContainer);
        micImageView = view.findViewById(R.id.micImageView);
        videoStateImageView = view.findViewById(R.id.videoStateImageView);
        nameTextView = view.findViewById(R.id.userNameTextView);
        videoContainer.setEnableZoom(enableVideoZoom);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        if (l != null) {
            videoContainer.setOnClickListener(v -> l.onClick(ConferenceParticipantItemVideoView.this));
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

    @Override
    public void setup(AVEngineKit.CallSession session, AVEngineKit.ParticipantProfile profile) {
        super.setup(session, profile);
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
    }

//    public void updateParticipantProfile(AVEngineKit.ParticipantProfile profile) {
//
//    }

//    public void updateVolume(int volume) {
//        int padding = 0;
//        if (volume > 500) {
//            padding = 2;
//            videoContainer.setBackgroundResource(R.drawable.av_conference_participant_highlight_boarder);
//        } else {
//            videoContainer.setBackground(null);
//        }
//        videoContainer.setPadding(padding, padding, padding, padding);
//        micImageView.setVolume(volume);
//    }
}
