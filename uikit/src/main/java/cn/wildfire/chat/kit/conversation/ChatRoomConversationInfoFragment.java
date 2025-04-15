/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.chatroom.ChatRoomViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

public class ChatRoomConversationInfoFragment extends Fragment {

    // common
    ImageView portraitImageView;

    OptionItemView chatroomNameOptionItemView;
    OptionItemView chatroomDescOptionItemView;

    private ConversationInfo conversationInfo;
    private ConversationViewModel conversationViewModel;
    private ChatRoomViewModel chatroomViewModel;
    private ChatRoomInfo chatroomInfo;

    public static ChatRoomConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        ChatRoomConversationInfoFragment fragment = new ChatRoomConversationInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("conversationInfo", conversationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        conversationInfo = args.getParcelable("conversationInfo");
        assert conversationInfo != null;
        getActivity().setTitle(getString(R.string.channel_details));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conversation_info_chatroom_fragment, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.reportOptionItemView).setOnClickListener(_v -> {
            new MaterialDialog.Builder(getActivity())
                .title(R.string.report)
                .content(R.string.report_tip)
                .positiveText(R.string.report)
                .positiveColor(Color.RED)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    Intent intent = ConversationActivity.buildConversationIntent(getContext(), Conversation.ConversationType.Single, "uiuJuJcc", 0);
                    startActivity(intent);
                })
                .build()
                .show();
        });
    }

    private void bindViews(View view) {
        portraitImageView = view.findViewById(R.id.portraitImageView);
        chatroomNameOptionItemView = view.findViewById(R.id.chatroomNameOptionItemView);
        chatroomDescOptionItemView = view.findViewById(R.id.chatroomDescOptionItemView);
    }

    private void init() {
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        chatroomViewModel = new ViewModelProvider(this).get(ChatRoomViewModel.class);
        chatroomViewModel.getChatRoomInfo(conversationInfo.conversation.target, 0)
            .observe(getViewLifecycleOwner(), new Observer<OperateResult<ChatRoomInfo>>() {
                @Override
                public void onChanged(OperateResult<ChatRoomInfo> chatRoomInfoOperateResult) {
                    ChatRoomInfo chatRoomInfo = chatRoomInfoOperateResult.getResult();
                    if (chatroomInfo != null) {
                        initChatroom(chatRoomInfo);
                    }
                }
            });
    }

    private void initChatroom(ChatRoomInfo chatroomInfo) {
        chatroomNameOptionItemView.setDesc(chatroomInfo.title);
        chatroomDescOptionItemView.setDesc(chatroomInfo.desc);
        Glide.with(this).load(chatroomInfo.portrait).into(portraitImageView);
    }
}
