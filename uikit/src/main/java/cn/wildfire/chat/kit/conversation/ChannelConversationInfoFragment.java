/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Arrays;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.channel.ChannelViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.file.FileRecordActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.search.SearchMessageActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;

public class ChannelConversationInfoFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    // common
    ImageView portraitImageView;
    SwitchMaterial stickTopSwitchButton;
    SwitchMaterial silentSwitchButton;

    OptionItemView channelNameOptionItemView;
    OptionItemView channelDescOptionItemView;

    OptionItemView fileRecordOptionItem;

    private ConversationInfo conversationInfo;
    private ConversationViewModel conversationViewModel;
    private ChannelViewModel channelViewModel;
    private ChannelInfo channelInfo;

    public static ChannelConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        ChannelConversationInfoFragment fragment = new ChannelConversationInfoFragment();
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
        View view = inflater.inflate(R.layout.conversation_info_channel_fragment, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.searchMessageOptionItemView).setOnClickListener(_v -> searchGroupMessage());
        view.findViewById(R.id.clearMessagesOptionItemView).setOnClickListener(_v -> clearMessage());
        view.findViewById(R.id.channelQRCodeOptionItemView).setOnClickListener(_v -> showChannelQRCode());
        view.findViewById(R.id.fileRecordOptionItemView).setOnClickListener(_v -> fileRecord());
        view.findViewById(R.id.unsubscribeButton).setOnClickListener(_v -> unsubscribe());
    }

    private void bindViews(View view) {
        portraitImageView = view.findViewById(R.id.portraitImageView);
        stickTopSwitchButton = view.findViewById(R.id.stickTopSwitchButton);
        silentSwitchButton = view.findViewById(R.id.silentSwitchButton);
        channelNameOptionItemView = view.findViewById(R.id.channelNameOptionItemView);
        channelDescOptionItemView = view.findViewById(R.id.channelDescOptionItemView);
        fileRecordOptionItem = view.findViewById(R.id.fileRecordOptionItemView);
    }

    private void init() {
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        channelViewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);
        channelInfo = channelViewModel.getChannelInfo(conversationInfo.conversation.target, true);

        if (channelInfo != null) {
            initChannel(channelInfo);
        }

        channelViewModel.channelInfoLiveData().observe(this, channelInfos -> {
            if (channelInfos != null) {
                for (ChannelInfo info : channelInfos) {
                    if (conversationInfo.conversation.target.equals(info.channelId)) {
                        initChannel(info);
                    }
                }
            }

        });

        stickTopSwitchButton.setChecked(conversationInfo.top>0);
        silentSwitchButton.setChecked(conversationInfo.isSilent);
        stickTopSwitchButton.setOnCheckedChangeListener(this);
        silentSwitchButton.setOnCheckedChangeListener(this);

        if (ChatManager.Instance().isCommercialServer()) {
            fileRecordOptionItem.setVisibility(View.VISIBLE);
        } else {
            fileRecordOptionItem.setVisibility(View.GONE);
        }
    }

    private void initChannel(ChannelInfo channelInfo) {
        channelNameOptionItemView.setDesc(channelInfo.name);
        channelDescOptionItemView.setDesc(channelInfo.desc);
        Glide.with(this).load(channelInfo.portrait).into(portraitImageView);
    }

    void searchGroupMessage() {
        Intent intent = new Intent(getActivity(), SearchMessageActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    void clearMessage() {
        new MaterialDialog.Builder(getActivity())
            .items(getString(R.string.clear_local_conversation), getString(R.string.clear_remote_conversation))
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    if (position == 0) {
                        conversationViewModel.clearConversationMessage(conversationInfo.conversation);
                    } else {
                        conversationViewModel.clearRemoteConversationMessage(conversationInfo.conversation);
                    }
                }
            })
            .show();
    }

    void showChannelQRCode() {
        String qrCodeValue = WfcScheme.QR_CODE_PREFIX_CHANNEL + channelInfo.channelId;
        Intent intent = QRCodeActivity.buildQRCodeIntent(getActivity(), getString(R.string.channel_qr_code_title), channelInfo.portrait, qrCodeValue);
        startActivity(intent);
    }

    void fileRecord() {
        Intent intent = new Intent(getActivity(), FileRecordActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    void unsubscribe() {
        channelViewModel.listenChannel(this.conversationInfo.conversation.target, false).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(OperateResult<Boolean> booleanOperateResult) {
                if (booleanOperateResult.isSuccess()) {
                    Intent intent = new Intent(getContext().getPackageName() + ".main");
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.unsubscribe_failed, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void stickTop(boolean top) {
        ConversationListViewModel conversationListViewModel = ViewModelProviders
            .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
            .get(ConversationListViewModel.class);
        conversationListViewModel.setConversationTop(conversationInfo, top?1:0);
        conversationInfo.top = top?1:0;
    }

    private void silent(boolean silent) {
        conversationViewModel.setConversationSilent(conversationInfo.conversation, silent);
        conversationInfo.isSilent = silent;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.stickTopSwitchButton) {
            stickTop(isChecked);
        } else if (id == R.id.silentSwitchButton) {
            silent(isChecked);
        }

    }
}
