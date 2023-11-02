/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceParticipantListFragment extends Fragment {
    RecyclerView recyclerView;
    TextView handupTextView;
    TextView applyingUnmuteTextView;

    Button muteAllButton;
    Button unmuteAllButton;
    private AVEngineKit.CallSession callSession;

    private List<AVEngineKit.ParticipantProfile> participantProfiles;
    private ParticipantListAdapter adapter;

    private ConferenceManager conferenceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference_participant_list, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.applyingUnmuteTextView).setOnClickListener(_v -> showApplyingUnmuteDialog());
        view.findViewById(R.id.handupTextView).setOnClickListener(_v -> showHandupDialog());
        view.findViewById(R.id.muteAllButton).setOnClickListener(_v -> muteAll());
        view.findViewById(R.id.unmuteAllButton).setOnClickListener(_v -> unmuteAll());
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        handupTextView = view.findViewById(R.id.handupTextView);
        applyingUnmuteTextView = view.findViewById(R.id.applyingUnmuteTextView);
        muteAllButton = view.findViewById(R.id.muteAllButton);
        unmuteAllButton = view.findViewById(R.id.unmuteAllButton);
    }

    private void init() {
        callSession = AVEngineKit.Instance().getCurrentSession();
        if (adapter == null) {
            adapter = new ParticipantListAdapter();
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        conferenceManager = ConferenceManager.getManager();

        LiveDataBus.subscribe("kConferenceMemberChanged", this, o -> loadAndShowConferenceParticipants());
        LiveDataBus.subscribe("kConferenceEnded", this, o -> loadAndShowConferenceParticipants());
        LiveDataBus.subscribe("kConferenceMutedStateChanged", this, o -> loadAndShowConferenceParticipants());
        LiveDataBus.subscribe("kConferenceCommandStateChanged", this, o -> loadAndShowConferenceParticipants());

        this.loadAndShowConferenceParticipants();
    }

    void showApplyingUnmuteDialog() {
        ConferenceApplyUnmuteListFragment fragment = new ConferenceApplyUnmuteListFragment();
        fragment.show(getChildFragmentManager(), "applyUnmuteFragment");
    }

    void showHandupDialog() {
        ConferenceHandUpListFragment fragment = new ConferenceHandUpListFragment();
        fragment.show(getChildFragmentManager(), "handUpFragment");
    }

    void muteAll() {
        new MaterialDialog.Builder(getActivity())
            .title("所有成员将被静音")
            .checkBoxPrompt("允许成员自主解除静音", false, null)
            .negativeText("取消")
            .positiveText("全体静音")
            .positiveColor(getResources().getColor(R.color.red0))
            .onPositive((dialog, which) -> conferenceManager.requestMuteAll(dialog.isPromptCheckBoxChecked()))
            .show();

    }

    void unmuteAll() {
        new MaterialDialog.Builder(getActivity())
            .title("允许全体成员开麦")
            .checkBoxPrompt("是否要求成员开麦", false, null)
            .negativeText("取消")
            .positiveText("取消全体静音")
            .positiveColor(getResources().getColor(R.color.red0))
            .onPositive((dialog, which) -> conferenceManager.requestUnmuteAll(dialog.isPromptCheckBoxChecked()))
            .show();
    }

    private void loadAndShowConferenceParticipants() {
        if (conferenceManager.getCurrentConferenceInfo() != null && ChatManager.Instance().getUserId().equals(conferenceManager.getCurrentConferenceInfo().getOwner())) {
            List<String> applyingUnmuteMembers = conferenceManager.getApplyingUnmuteMembers();
            if (applyingUnmuteMembers.isEmpty()) {
                applyingUnmuteTextView.setVisibility(View.GONE);
            } else {
                applyingUnmuteTextView.setVisibility(View.VISIBLE);
                String text = ChatManager.Instance().getUserDisplayName(applyingUnmuteMembers.get(0));
                if (applyingUnmuteMembers.size() > 1) {
                    text += " 等";
                }
                text += "正在申请解除静音";
                applyingUnmuteTextView.setText(text);
            }
            List<String> handupMembers = conferenceManager.getHandUpMembers();
            if (handupMembers.isEmpty()) {
                handupTextView.setVisibility(View.GONE);
            } else {
                handupTextView.setVisibility(View.VISIBLE);
                String text = ChatManager.Instance().getUserDisplayName(handupMembers.get(0));
                if (handupMembers.size() > 1) {
                    text += " 等";
                }
                text += "正在举手";
                handupTextView.setText(text);
            }

            if (conferenceManager.isMuteAll()) {
                muteAllButton.setEnabled(false);
                unmuteAllButton.setEnabled(true);
            } else {
                muteAllButton.setEnabled(true);
                unmuteAllButton.setEnabled(false);
            }
        } else {
            handupTextView.setVisibility(View.GONE);
            applyingUnmuteTextView.setVisibility(View.GONE);
            muteAllButton.setVisibility(View.GONE);
            unmuteAllButton.setVisibility(View.GONE);
        }

        this.participantProfiles = callSession.getParticipantProfiles();
        this.participantProfiles.add(callSession.getMyProfile());
        adapter.notifyDataSetChanged();
    }

    private void onClickParticipant(AVEngineKit.ParticipantProfile profile) {
        Callable<Void> unmuteVideoCall = () -> {
            conferenceManager.muteVideo(false);
            return null;
        };
        Callable<Void> muteVideoCall = () -> {
            conferenceManager.muteVideo(true);
            return null;
        };
        Callable<Void> unmuteAudioCall = () -> {
            conferenceManager.muteAudio(false);
            return null;
        };
        Callable<Void> muteAudioCall = () -> {
            conferenceManager.muteAudio(true);
            return null;
        };
        Callable<Void> requestFocus = () -> {
            conferenceManager.requestFocus(profile.getUserId(), null);
            return null;
        };
        Callable<Void> cancelFocus = () -> {
            conferenceManager.requestCancelFocus(null);
            return null;
        };
        Callable<Void> userInfoCall = () -> {
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            UserInfo userInfo = ChatManager.Instance().getUserInfo(profile.getUserId(), false);
            intent.putExtra("userInfo", userInfo);
            startActivity(intent);
            return null;
        };

        Map<String, Callable<Void>> items = new HashMap<>();
        String selfUid = ChatManager.Instance().getUserId();
        items.put("查看用户信息", userInfoCall);
        ConferenceInfo conferenceInfo = conferenceManager.getCurrentConferenceInfo();
        List<String> managers = conferenceInfo.getManagers();
        managers = managers != null ? managers : new ArrayList<>();
        if (selfUid.equals(conferenceInfo.getOwner()) || managers.contains(selfUid)) {
            if (selfUid.equals(profile.getUserId())) {
                // 主持人自己
                if (profile.isAudioMuted()) {
                    items.put("开启音频", unmuteAudioCall);
                } else {
                    items.put("关闭音频", muteAudioCall);
                }
                if (profile.isVideoMuted()) {
                    items.put("开启视频", unmuteVideoCall);
                } else {
                    items.put("关闭视频", muteVideoCall);
                }
                if (profile.isAudioMuted() && profile.isVideoMuted()) {
                    items.put("开启音视频", unmuteAudioCall);
                }
            } else {
                // 他人
                if (profile.isAudience()) {
                    items.put("邀请发言", () -> {
                        conferenceManager.requestMemberMute(profile.getUserId(), false);
                        return null;
                    });
                } else {
                    items.put("取消发言", () -> {
                        conferenceManager.requestMemberMute(profile.getUserId(), true);
                        return null;
                    });
                }
                items.put("移除成员", () -> {
                    callSession.kickoffParticipant(profile.getUserId(), null);
                    return null;
                });
            }
            if (profile.getUserId().equals(conferenceInfo.getFocus())) {
                items.put("取消焦点用户", cancelFocus);
            } else {
                items.put("设置为焦点用户", requestFocus);
            }
        } else {
            if (selfUid.equals(profile.getUserId())) {
                // 自己
                if (profile.isAudience()) {
                    items.put("举手", () -> {
                        conferenceManager.handUp(true);
                        return null;
                    });
                } else {
                    if (profile.isAudioMuted()) {
                        items.put("开启音频", unmuteAudioCall);
                    } else {
                        items.put("关闭音频", muteAudioCall);
                    }
                    if (profile.isVideoMuted()) {
                        items.put("开启视频", unmuteVideoCall);
                    } else {
                        items.put("关闭视频", muteVideoCall);
                    }
                }
            } else {
                // do nothing
            }

        }

        new MaterialDialog.Builder(getActivity())
            .cancelable(true)
            .items(items.keySet())
            .itemsCallback((dialog, itemView, position, text) -> {
                try {
                    items.get(text).call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })
            .show();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private View itemView;

        ImageView portraitImageView;
        TextView nameTextView;
        TextView descTextView;
        ImageView audioImageView;
        ImageView videoImageView;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            bindViews(itemView);
        }

        private void bindViews(View itemView) {
            portraitImageView = itemView.findViewById(R.id.portraitImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            audioImageView = itemView.findViewById(R.id.audioImageView);
            videoImageView = itemView.findViewById(R.id.videoImageView);
        }

        public void onBind(AVEngineKit.ParticipantProfile profile) {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(profile.getUserId(), false);
            String displayName = ChatManager.Instance().getUserDisplayName(userInfo);
            nameTextView.setText(displayName);
            Glide.with(this.itemView).load(userInfo.portrait).placeholder(R.mipmap.avatar_def)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                .into(portraitImageView);

            String desc = "";
            if (profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                if (profile.getUserId().equals(conferenceManager.getCurrentConferenceInfo().getOwner())) {
                    desc = "主持人，我";
                } else {
                    desc = "我";
                }
            } else {
                if (profile.getUserId().equals(conferenceManager.getCurrentConferenceInfo().getOwner())) {
                    desc = "主持人";
                    if (profile.isScreenSharing()) {
                        desc += "，屏幕共享";
                    }
                } else {
                    if (profile.isScreenSharing()) {
                        desc += "屏幕共享";
                    }
                }
            }
            if (TextUtils.isEmpty(desc)) {
                descTextView.setVisibility(View.GONE);
            } else {
                descTextView.setVisibility(View.VISIBLE);
                descTextView.setText(desc);
            }

            audioImageView.setSelected(profile.isAudience() || profile.isAudioMuted());
            videoImageView.setSelected(profile.isAudience() || profile.isVideoMuted());
            this.itemView.setOnClickListener(v -> {
                onClickParticipant(profile);
            });
        }
    }

    class ParticipantListAdapter extends RecyclerView.Adapter<ParticipantViewHolder> {

        @NonNull
        @Override
        public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.av_conference_participant_list_item, parent, false);
            return new ParticipantViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
            holder.onBind(participantProfiles.get(position));
        }

        @Override
        public int getItemCount() {
            return participantProfiles == null ? 0 : participantProfiles.size();
        }
    }

    class Action {
        String name;

    }

}
