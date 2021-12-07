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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kyleduo.switchbutton.SwitchButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
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
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class SingleConversationInfoFragment extends Fragment implements ConversationMemberAdapter.OnMemberClickListener, CompoundButton.OnCheckedChangeListener {

    // common
    @BindView(R2.id.memberRecyclerView)
    RecyclerView memberReclerView;
    @BindView(R2.id.stickTopSwitchButton)
    SwitchButton stickTopSwitchButton;
    @BindView(R2.id.silentSwitchButton)
    SwitchButton silentSwitchButton;

    @BindView(R2.id.fileRecordOptionItemView)
    OptionItemView fileRecordOptionItem;

    private ConversationInfo conversationInfo;
    private ConversationMemberAdapter conversationMemberAdapter;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;


    public static SingleConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        SingleConversationInfoFragment fragment = new SingleConversationInfoFragment();
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
        View view = inflater.inflate(R.layout.conversation_info_single_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        String userId = conversationInfo.conversation.target;
        conversationMemberAdapter = new ConversationMemberAdapter(conversationInfo, true, false);
        List<UserInfo> members = Collections.singletonList(userViewModel.getUserInfo(userId, true));
        conversationMemberAdapter.setMembers(members);
        conversationMemberAdapter.setOnMemberClickListener(this);

        memberReclerView.setAdapter(conversationMemberAdapter);
        memberReclerView.setLayoutManager(new GridLayoutManager(getActivity(), 5));
        stickTopSwitchButton.setChecked(conversationInfo.isTop);
        silentSwitchButton.setChecked(conversationInfo.isSilent);
        stickTopSwitchButton.setOnCheckedChangeListener(this);
        silentSwitchButton.setOnCheckedChangeListener(this);

        observerUserInfoUpdate();
        if (ChatManager.Instance().isCommercialServer()) {
            fileRecordOptionItem.setVisibility(View.VISIBLE);
        } else {
            fileRecordOptionItem.setVisibility(View.GONE);
        }
    }

    private void observerUserInfoUpdate() {
        userViewModel.userInfoLiveData().observe(this, userInfos -> {
            for (UserInfo userInfo : userInfos) {
                if (userInfo.uid.equals(this.conversationInfo.conversation.target)) {
                    List<UserInfo> members = Collections.singletonList(userInfo);
                    conversationMemberAdapter.setMembers(members);
                    conversationMemberAdapter.notifyDataSetChanged();
                    break;
                }
            }
        });
    }

    @OnClick(R2.id.clearMessagesOptionItemView)
    void clearMessage() {
        new MaterialDialog.Builder(getActivity())
            .items("清空本地会话", "清空远程会话")
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

    @OnClick(R2.id.searchMessageOptionItemView)
    void searchGroupMessage() {
        Intent intent = new Intent(getActivity(), SearchMessageActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    @OnClick(R2.id.fileRecordOptionItemView)
    void fileRecord() {
        Intent intent = new Intent(getActivity(), FileRecordActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
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
        conversationListViewModel.setConversationTop(conversationInfo, top);
        conversationInfo.isTop = top;
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
