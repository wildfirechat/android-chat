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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Arrays;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.kit.conversation.file.FileRecordActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.search.SearchMessageActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.SecretChatInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class SecretConversationInfoFragment extends Fragment implements ConversationMemberAdapter.OnMemberClickListener, CompoundButton.OnCheckedChangeListener {

    SwitchMaterial stickTopSwitchButton;
    SwitchMaterial silentSwitchButton;

    OptionItemView fileRecordOptionItem;

    OptionItemView burnOptionItemView;

    String[] messageBurnTimeDesc;

    int[] messageBurnTime;


    private ConversationInfo conversationInfo;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;


    public static SecretConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        SecretConversationInfoFragment fragment = new SecretConversationInfoFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conversation_info_secret_fragment, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.clearMessagesOptionItemView).setOnClickListener(_v -> clearMessage());
        view.findViewById(R.id.searchMessageOptionItemView).setOnClickListener(_v -> searchGroupMessage());
        view.findViewById(R.id.fileRecordOptionItemView).setOnClickListener(_v -> fileRecord());
        view.findViewById(R.id.destroySecretChatButton).setOnClickListener(_v -> destroySecretChat());
        view.findViewById(R.id.burnOptionItemView).setOnClickListener(_v -> setSecretChatBurnTime());
    }

    private void bindViews(View view) {
        stickTopSwitchButton = view.findViewById(R.id.stickTopSwitchButton);
        silentSwitchButton = view.findViewById(R.id.silentSwitchButton);
        fileRecordOptionItem = view.findViewById(R.id.fileRecordOptionItemView);
        burnOptionItemView = view.findViewById(R.id.burnOptionItemView);

        messageBurnTimeDesc = getContext().getResources().getStringArray(R.array.secret_chat_message_burn_time_desc);
        messageBurnTime = getContext().getResources().getIntArray(R.array.secret_chat_message_burn_time);

    }

    private void init() {
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);

        stickTopSwitchButton.setChecked(conversationInfo.top > 0);
        silentSwitchButton.setChecked(conversationInfo.isSilent);
        stickTopSwitchButton.setOnCheckedChangeListener(this);
        silentSwitchButton.setOnCheckedChangeListener(this);

        if (ChatManager.Instance().isCommercialServer()) {
            fileRecordOptionItem.setVisibility(View.VISIBLE);
        } else {
            fileRecordOptionItem.setVisibility(View.GONE);
        }

        SecretChatInfo secretChatInfo = ChatManager.Instance().getSecretChatInfo(conversationInfo.conversation.target);
        int burnTime = secretChatInfo.getBurnTime();

        if (burnTime > 0) {
            int index = 0;
            for (int i = 0; i < messageBurnTime.length; i++) {
                if (messageBurnTime[i] == burnTime) {
                    index = i;
                    break;
                }
            }
            burnOptionItemView.setDesc(messageBurnTimeDesc[index]);
        } else {
            burnOptionItemView.setDesc(messageBurnTimeDesc[0]);
        }
    }

    void clearMessage() {
        new MaterialDialog.Builder(getActivity())
            .items(getString(R.string.clear_conversation))
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    if (position == 0) {
                        conversationViewModel.clearConversationMessage(conversationInfo.conversation);
                    }
                }
            })
            .show();
    }

    void searchGroupMessage() {
        Intent intent = new Intent(getActivity(), SearchMessageActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    void fileRecord() {
        Intent intent = new Intent(getActivity(), FileRecordActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    void destroySecretChat() {
        ChatManager.Instance().destroySecretChat(conversationInfo.conversation.target, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (getActivity().isFinishing()) {
                    return;
                }
                Intent intent = new Intent(getContext().getPackageName() + ".main");
                startActivity(intent);
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    void setSecretChatBurnTime() {
        new MaterialDialog.Builder(getActivity())
            .items(messageBurnTimeDesc)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    burnOptionItemView.setDesc(messageBurnTimeDesc[position]);
                    ChatManager.Instance().setSecretChatBurnTime(conversationInfo.conversation.target, messageBurnTime[position]);
                }
            })
            .show();
    }

    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @Override
    public void onAddMemberClick() {
        Intent intent = new Intent(getActivity(), CreateConversationActivity.class);
        ArrayList<String> participants = new ArrayList<>();
        participants.add(conversationInfo.conversation.target);
        intent.putExtra(PickConversationTargetActivity.CURRENT_PARTICIPANTS, participants);
        startActivity(intent);
    }

    @Override
    public void onRemoveMemberClick() {
        // do nothing
    }

    private void stickTop(boolean top) {
        ConversationListViewModel conversationListViewModel = ViewModelProviders
            .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
            .get(ConversationListViewModel.class);
        conversationListViewModel.setConversationTop(conversationInfo, top ? 1 : 0);
        conversationInfo.top = top ? 1 : 0;
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
