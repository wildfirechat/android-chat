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

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

class ConferenceParticipantGridView extends RelativeLayout {
    private AVEngineKit.CallSession callSession;
    private List<AVEngineKit.ParticipantProfile> profiles;
    private GridLayout participantGridView;
    private static final String TAG = "ParticipantGridView";

    private boolean isAudioConference = false;

    public ConferenceParticipantGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public ConferenceParticipantGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ConferenceParticipantGridView(Context context) {
        super(context);
        initView(context);
    }

    public ConferenceParticipantGridView(Context context, boolean isAudioConference) {
        super(context);
        this.isAudioConference = isAudioConference;
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.av_conference_video_participant_grid, this);
        participantGridView = view.findViewById(R.id.participantGridView);
        if (this.isAudioConference) {
            participantGridView.setColumnCount(3);
            participantGridView.setRowCount(4);
        }
    }

    public void setParticipantProfiles(AVEngineKit.CallSession session, List<AVEngineKit.ParticipantProfile> profiles) {
        this.callSession = session;
        List<AVEngineKit.ParticipantProfile> removedProfiles = new ArrayList<>();
        if (this.profiles != null) {
            for (AVEngineKit.ParticipantProfile op : this.profiles) {
                boolean removed = true;
                for (AVEngineKit.ParticipantProfile profile : profiles) {
                    if (profile.getUserId().equals(op.getUserId())) {
                        removed = false;
                        break;
                    }
                }
                if (removed) {
                    removedProfiles.add(op);
                }
            }
        }

        for (AVEngineKit.ParticipantProfile rp : removedProfiles) {
            if (!rp.getUserId().equals(ChatManager.Instance().getUserId()) && !rp.isAudience() && !rp.isVideoMuted()) {
                callSession.setParticipantVideoType(rp.getUserId(), rp.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
            }
        }

        this.profiles = profiles;

        // TODO diff，全删，然后重新添加，UI 会稍微闪一下
        this.participantGridView.removeAllViews();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = Math.min(dm.widthPixels, dm.heightPixels);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int itemWidth, itemHeight;
        if (isAudioConference) {
            itemWidth = width / 3;
            itemHeight = (height - UIUtils.dip2Px(120)) / 4;
        } else {
            itemWidth = width / 2;
            itemHeight = height / 2;
        }
        for (AVEngineKit.ParticipantProfile profile : profiles) {
            if (profile.isAudience() || profile.isVideoMuted()) {
                ConferenceParticipantItemView conferenceItem = new ConferenceParticipantItemView(getContext());
                conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(itemWidth, itemHeight));
                this.participantGridView.addView(conferenceItem);
                conferenceItem.setup(session, profile);
                if (!this.isAudioConference) {
                    conferenceItem.setBackgroundResource(R.color.gray0);
                }
            } else {
                ConferenceParticipantItemVideoView conferenceVideoItem = new ConferenceParticipantItemVideoView(getContext());
                conferenceVideoItem.setLayoutParams(new ViewGroup.LayoutParams(itemWidth, itemHeight));
                this.participantGridView.addView(conferenceVideoItem);
                conferenceVideoItem.setup(session, profile);
                if (!profile.isAudience() && !profile.isVideoMuted()) {
                    session.setParticipantVideoType(profile.getUserId(), profile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_SMALL_STREAM);
                }
            }
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
                if (!profile.isAudience() && !profile.isVideoMuted() && !profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                    this.callSession.setParticipantVideoType(profile.getUserId(), profile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
                }
            }
        }
    }

    public void onPageUnselected(String keepSubscribeUserId) {
        if (profiles == null) {
            return;
        }
        for (AVEngineKit.ParticipantProfile p : profiles) {
            if (p.getUserId().equals(keepSubscribeUserId)) {
                continue;
            }
            if (!p.isAudience() && !p.isVideoMuted() && !p.getUserId().equals(ChatManager.Instance().getUserId())) {
                callSession.setParticipantVideoType(p.getUserId(), p.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
            }
        }
    }
}
