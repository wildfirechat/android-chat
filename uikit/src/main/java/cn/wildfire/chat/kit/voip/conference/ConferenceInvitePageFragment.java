/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.forward.ForwardPromptView;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationPageFragment;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.message.ConferenceInviteMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

/**
 * 「把会议邀请发给某个会话」整页。逐行搬自 {@link ConferenceInviteActivity}，
 * 与转发页共用 {@link PickOrCreateConversationPageFragment}。
 * <p>
 * 这一页的入口是会议界面（全屏），因此<strong>不注册进 {@code PaneRegistry}</strong>——
 * 它不会出现在右栏里。搬过来只为一件事：选会话那套 UI 全仓只留一份实现，
 * 不再有一个 Activity 版和一个 Fragment 版各自漂移。
 */
public class ConferenceInvitePageFragment extends PickOrCreateConversationPageFragment {

    private static final String ARG_INVITE_MESSAGE = "inviteMessage";

    private ConferenceInviteMessageContent inviteMessage;
    private MessageViewModel messageViewModel;

    @Nullable
    public static ConferenceInvitePageFragment fromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        ConferenceInviteMessageContent inviteMessage = intent.getParcelableExtra(ARG_INVITE_MESSAGE);
        if (inviteMessage == null) {
            return null;
        }
        ConferenceInvitePageFragment fragment = new ConferenceInvitePageFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_INVITE_MESSAGE, inviteMessage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inviteMessage = getArguments() == null ? null : getArguments().getParcelable(ARG_INVITE_MESSAGE);
        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
    }

    @Override
    protected void onPickOrCreateConversation(Conversation conversation) {
        switch (conversation.type) {
            case Single:
                UserInfo userInfo = userViewModel.getUserInfo(conversation.target, false);
                invite(userInfo.displayName, userInfo.portrait, conversation);
                break;
            case Group:
                GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
                invite(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name,
                    groupInfo.portrait, conversation);
                break;
            default:
                break;
        }
    }

    private void invite(String targetName, String targetPortrait, Conversation targetConversation) {
        ForwardPromptView view = new ForwardPromptView(requireContext());
        view.bind(targetName, targetPortrait, getString(R.string.conf_invite_title));
        new MaterialDialog.Builder(requireContext())
            .customView(view, false)
            .negativeText(R.string.cancel)
            .positiveText(R.string.send)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    messageViewModel.sendMessage(targetConversation, inviteMessage);
                    Toast.makeText(getActivity(), getString(R.string.conf_invite_success), Toast.LENGTH_SHORT).show();
                    WfcPageCompat.finishPage(ConferenceInvitePageFragment.this);
                }
            })
            .build()
            .show();
    }
}
