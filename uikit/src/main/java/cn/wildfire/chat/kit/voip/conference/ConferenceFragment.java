/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.webrtc.StatsReport;

import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

// main view
// participantPreviewView

// 收到事件之后，判断是否影响当前页，不影响的话，就不做任何处理
// 切换页面时，初始化新切换到的页面
// 收到事件之后，可能需要增减页面

// 当前页面包含哪些人，只针对这些人的事件做处理

// 收到离开事件时，1. 当前页面处理，补位；2. 判断是否要减页面
// 收到加入事件时，1. 当前页面是否还能加位置；2. 判断是否需要增加页面

// 离开页面之后，反初始化

//  收到事件时，3 个页面(当前页，前后各一页)需根据实际情况处理

// 离开时，先更新 view，在删除

public class ConferenceFragment extends BaseConferenceFragment implements AVEngineKit.CallSessionCallback {

    private SparseArray<View> views;
    private ViewPager viewPager;
    private List<AVEngineKit.ParticipantProfile> profiles;
    private ConferenceViewAdapter adapter;
    private AVEngineKit.CallSession callSession;
    private int currentPosition = 0;
    private static final String TAG = "conferenceFragment";

    /**
     * 这个值需要和{@link ConferenceParticipantGridView} 的布局里面的  row * col 对应上
     */
    private static final int COUNT_PER_PAGE = 4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference, container, false);
//        ButterKnife.bind(this, view);

        callSession = AVEngineKit.Instance().getCurrentSession();
        if (callSession == null || callSession.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            return null;
        }
        profiles = callSession.getParticipantProfiles();

        views = new SparseArray<>(3);
        viewPager = view.findViewById(R.id.viewPager);
        adapter = new ConferenceViewAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(pageChangeListener);


        // 禁用自动设置 surfaceView 层级关系
        AVEngineKit.DISABLE_SURFACE_VIEW_AUTO_OVERLAY = true;
        callSession.autoSwitchVideoType = false;

        return view;
    }

    private class ConferenceViewAdapter extends PagerAdapter {

        public ConferenceViewAdapter() {
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = null;
            if (position == 0) {
                view = new ConferenceMainView(container.getContext());
                AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
                AVEngineKit.ParticipantProfile myProfile = session.getMyProfile();
                ((ConferenceMainView) view).setup(session, myProfile, findFocusProfile(session));
            } else {
                view = new ConferenceParticipantGridView(container.getContext());
            }
            container.addView(view);
            views.put(position % 3, view);
            Log.d(TAG, "instantiateItem " + position);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            // 取消视频流订阅
            container.removeView((View) object);
            Log.d(TAG, "destroyItem " + position);
            View view = (View) object;
            if (view instanceof ConferenceMainView) {

            } else if (view instanceof ConferenceParticipantGridView) {
                ((ConferenceParticipantGridView) view).onDestroyView();
            }
        }

        @Override
        public int getCount() {
            Log.d(TAG, "getCount " + profiles.size());

            return 1 + (int) Math.ceil(profiles.size() / (double) COUNT_PER_PAGE);
//            return 1;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void finishUpdate(@NonNull ViewGroup container) {
            super.finishUpdate(container);
        }
    }

    final ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // TODO 可以在此控制透明度
        }

        @Override
        public void onPageSelected(int position) {
            Log.e(TAG, " onPageSelected " + position);
            View view = views.get(position % 3);
            if (view == null) {
                // pending layout
                return;
            }
            if (view instanceof ConferenceParticipantGridView) {
                ((ConferenceParticipantGridView) view).setParticipantProfiles(callSession, getGridPageParticipantProfiles(position));
            } else if (view instanceof ConferenceMainView) {
                ((ConferenceMainView) view).updateFocusProfile(findFocusProfile(callSession));
            }
            currentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private void onParticipantProfileUpdate(List<String> participants) {
        // 比如有人离开后
        if (adapter.getCount() <= currentPosition) {
            viewPager.setCurrentItem(currentPosition - 1);
        } else {
            View view = views.get(currentPosition % 3);
            if (view instanceof ConferenceMainView) {
                AVEngineKit.ParticipantProfile focusProfile = findFocusProfile(this.callSession);
                if (participants.contains(ChatManager.Instance().getUserId()) || focusProfile == null || participants.contains(focusProfile.getUserId())) {
                    ((ConferenceMainView) view).setup(this.callSession, callSession.getMyProfile(), focusProfile);
                }
            } else if (view instanceof ConferenceParticipantGridView) {
                List<AVEngineKit.ParticipantProfile> currentPageParticipantProfiles = getGridPageParticipantProfiles(currentPosition);
                boolean updateCurrentPage = false;
                for (String userId : participants) {
                    for (AVEngineKit.ParticipantProfile p : currentPageParticipantProfiles) {
                        if (userId.equals(p.getUserId())) {
                            updateCurrentPage = true;
                            break;
                        }
                    }
                }
                if (updateCurrentPage) {
                    ((ConferenceParticipantGridView) view).setParticipantProfiles(this.callSession, currentPageParticipantProfiles);
                }
            }
        }
    }

    private List<AVEngineKit.ParticipantProfile> getGridPageParticipantProfiles(int position) {
        int fromIndex = (position - 1) * COUNT_PER_PAGE;
        int endIndex = Math.min(fromIndex + COUNT_PER_PAGE, profiles.size());
        return profiles.subList(fromIndex, endIndex);
    }

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason reason) {

    }

    @Override
    public void didChangeState(AVEngineKit.CallState state) {

    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        this.profiles = callSession.getParticipantProfiles();
        this.adapter.notifyDataSetChanged();
        onParticipantProfileUpdate(Collections.singletonList(userId));
        Log.d(TAG, "didParticipantJoined " + userId);
        LiveDataBus.setValue("kConferenceMemberChanged", new Object());
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason reason, boolean screenSharing) {
        this.profiles = callSession.getParticipantProfiles();
        this.adapter.notifyDataSetChanged();
        onParticipantProfileUpdate(Collections.singletonList(userId));
        Log.d(TAG, "didParticipantLeft " + userId);
        LiveDataBus.setValue("kConferenceMemberChanged", new Object());
    }

    @Override
    public void didChangeType(String userId, boolean audience, boolean screenSharing) {
        this.profiles = callSession.getParticipantProfiles();
        this.adapter.notifyDataSetChanged();
        onParticipantProfileUpdate(Collections.singletonList(userId));
        Log.d(TAG, "didChangeType " + userId + " " + audience);
        LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {

    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {

    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {

    }

    @Override
    public void didError(String error) {

    }

    @Override
    public void didGetStats(StatsReport[] reports) {

    }

    @Override
    public void didVideoMuted(String userId, boolean videoMuted) {
    }

    @Override
    public void didMuteStateChanged(List<String> participants) {
        this.profiles = callSession.getParticipantProfiles();
        onParticipantProfileUpdate(participants);
        LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
    }

    @Override
    public void didMediaLostPacket(String media, int lostPacket, boolean screenSharing) {
    }

    @Override
    public void didMediaLostPacket(String userId, String media, int lostPacket, boolean uplink, boolean screenSharing) {
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
//        Log.d(TAG, "didReportAudioVolume " + userId + " " + volume);
        View view = views.get(currentPosition % 3);
        if (view instanceof ConferenceMainView) {
            ((ConferenceMainView) view).updateParticipantVolume(userId, volume);
        } else if (view instanceof ConferenceParticipantGridView) {
            ((ConferenceParticipantGridView) view).updateParticipantVolume(userId, volume);
        }
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {

    }

    private AVEngineKit getEngineKit() {
        return AVEngineKit.Instance();
    }

    private AVEngineKit.ParticipantProfile findFocusProfile(AVEngineKit.CallSession session) {
        AVEngineKit.ParticipantProfile focusProfile = null;
        for (AVEngineKit.ParticipantProfile profile : profiles) {
            if (!profile.isAudience()) {
                if (profile.isScreenSharing()) {
                    focusProfile = profile;
                    break;
                } else if (!profile.isVideoMuted() && (focusProfile == null || focusProfile.isVideoMuted())) {
                    focusProfile = profile;
                } else if (!profile.isAudioMuted() && focusProfile == null) {
                    focusProfile = profile;
                }
            }
        }
        return focusProfile;
    }
}
