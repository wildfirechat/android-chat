/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.gridlayout.widget.GridLayout;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

class VideoConferenceParticipantGridView extends RelativeLayout {
    private AVEngineKit.CallSession callSession;
    private List<AVEngineKit.ParticipantProfile> profiles;
    private GridLayout participantGridView;
    private static final String TAG = "ParticipantGridView";

    public VideoConferenceParticipantGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public VideoConferenceParticipantGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public VideoConferenceParticipantGridView(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.av_conference_video_participant_grid, this);
        participantGridView = view.findViewById(R.id.participantGridView);
    }

    public void setParticipantProfiles(AVEngineKit.CallSession session, List<AVEngineKit.ParticipantProfile> profiles) {
//        if (profiles == null || profiles.isEmpty()) {
//            Log.d(TAG, "setParticipantProfiles profiles is empty, page has been removed");
//            ((ViewGroup) getParent()).removeView(this);
//            return;
//        }
        this.callSession = session;
        this.profiles = profiles;

        this.participantGridView.removeAllViews();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = Math.min(dm.widthPixels, dm.heightPixels);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        for (AVEngineKit.ParticipantProfile profile : profiles) {
            ConferenceParticipantItemView conferenceItem = new ConferenceParticipantItemView(getContext());
            conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(width / 2, height / 2));
            this.participantGridView.addView(conferenceItem);
            conferenceItem.setup(session, profile);
            session.setParticipantVideoType(profile.getUserId(), profile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_SMALL_STREAM);
        }
    }

    public void addParticipantProfile(AVEngineKit.ParticipantProfile profile) {
        this.profiles.add(profile);
        // TODO
    }

    public void removeParticipantProfile(AVEngineKit.ParticipantProfile profile) {
        boolean result = this.profiles.removeIf((p -> p.getUserId().equals(profile.getUserId())));
        if (result) {
            // TODO
        }
    }

    public void onParticipantProfileUpdate(AVEngineKit.ParticipantProfile profile) {

    }

    public void updateParticipantVolume(String userId, int volume) {
        View view = this.participantGridView.findViewWithTag(VoipBaseActivity.participantKey(userId, false));
        if (view != null) {
            ((ConferenceParticipantItemView) view).updateVolume(volume);
        }
    }

    public void onDestroyView() {
        if (this.profiles != null) {
            for (AVEngineKit.ParticipantProfile profile : profiles) {
                this.callSession.setParticipantVideoType(profile.getUserId(), profile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
            }
        }
    }

    public void onPageUnselected() {
        if (profiles == null) {
            return;
        }
        for (AVEngineKit.ParticipantProfile p : profiles) {
            if (!p.isAudience() && !p.isVideoMuted() && !p.getUserId().equals(ChatManager.Instance().getUserId())) {
                callSession.setParticipantVideoType(p.getUserId(), p.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
            }
        }
    }
}
