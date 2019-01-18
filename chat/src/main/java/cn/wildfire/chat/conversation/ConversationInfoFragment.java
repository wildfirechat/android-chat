package cn.wildfire.chat.conversation;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kyleduo.switchbutton.SwitchButton;
import com.lqr.optionitemview.OptionItemView;
import cn.wildfirechat.chat.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.chatroom.ChatRoomViewModel;
import cn.wildfire.chat.common.OperateResult;
import cn.wildfire.chat.contact.ContactViewModel;
import cn.wildfire.chat.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.group.AddGroupMemberActivity;
import cn.wildfire.chat.group.GroupViewModel;
import cn.wildfire.chat.group.RemoveGroupMemberActivity;
import cn.wildfire.chat.group.SetGroupNameActivity;
import cn.wildfire.chat.user.UserInfoActivity;
import cn.wildfire.chat.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;

public class ConversationInfoFragment extends Fragment implements ConversationMemberAdapter.OnMemberClickListener, CompoundButton.OnCheckedChangeListener {

    // group
    @Bind(R.id.groupLinearLayout_0)
    LinearLayout groupLinearLayout_0;
    @Bind(R.id.groupNameOptionItemView)
    OptionItemView groupNameOptionItemView;
    @Bind(R.id.groupNoticeLinearLayout)
    LinearLayout noticeLinearLayout;
    @Bind(R.id.groupNoticeTextView)
    TextView noticeTextView;
    @Bind(R.id.groupManageOptionItemView)
    OptionItemView groupManageOptionItemView;
    @Bind(R.id.groupManageDividerLine)
    View groupManageDividerLine;

    @Bind(R.id.groupLinearLayout_1)
    LinearLayout groupLinearLayout_1;
    @Bind(R.id.myGroupNickNameOptionItemView)
    OptionItemView myGroupNickNameOptionItemView;
    @Bind(R.id.showGroupNickNameSwitchButton)
    SwitchButton showGroupMemberNickNameSwitchButton;

    @Bind(R.id.quitButton)
    Button quitGroupButton;

    @Bind(R.id.markGroupLinearLayout)
    LinearLayout markGroupLinearLayout;
    @Bind(R.id.markGroupSwitchButton)
    SwitchButton markGroupSwitchButton;

    // common
    @Bind(R.id.memberRecyclerView)
    RecyclerView memberReclerView;
    @Bind(R.id.stickTopSwitchButton)
    SwitchButton stickTopSwitchButton;
    @Bind(R.id.silentSwitchButton)
    SwitchButton silentSwitchButton;

    private ConversationInfo conversationInfo;
    private ConversationMemberAdapter conversationMemberAdapter;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;

    private GroupViewModel groupViewModel;
    private GroupInfo groupInfo;
    // me in group
    private GroupMember groupMember;
    private List<UserInfo> members;


    private static final int REQUEST_ADD_MEMBER = 100;
    private static final int REQUEST_REMOVE_MEMBER = 200;
    private static final int REQUEST_CODE_SET_GROUP_NAME = 300;

    public static ConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        ConversationInfoFragment fragment = new ConversationInfoFragment();
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
        View view = inflater.inflate(R.layout.conversation_info_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        conversationViewModel = ViewModelProviders.of(this, new ConversationViewModelFactory(conversationInfo.conversation)).get(ConversationViewModel.class);
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        ContactViewModel contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
        String userId = userViewModel.getUserId();
        if (conversationInfo.conversation.type == Conversation.ConversationType.Group) {
            groupLinearLayout_0.setVisibility(View.VISIBLE);
            groupLinearLayout_1.setVisibility(View.VISIBLE);
            markGroupLinearLayout.setVisibility(View.VISIBLE);
            markGroupSwitchButton.setOnCheckedChangeListener(this);
            quitGroupButton.setVisibility(View.VISIBLE);

            groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
            List<GroupMember> groupMembers = groupViewModel.getGroupMembers(conversationInfo.conversation.target, false);
            List<String> memberIds = new ArrayList<>();
            for (GroupMember member : groupMembers) {
                if (member.memberId.equals(userId)) {
                    groupMember = member;
                }
                memberIds.add(member.memberId);
            }
            groupInfo = groupViewModel.getGroupInfo(conversationInfo.conversation.target, false);

            boolean enableRemoveMember = false;
            if (groupMember.type != GroupMember.GroupMemberType.Normal || userId.equals(groupInfo.owner)) {
                enableRemoveMember = true;
            }
            conversationMemberAdapter = new ConversationMemberAdapter(true, enableRemoveMember);
            members = contactViewModel.getContacts(memberIds);
            myGroupNickNameOptionItemView.setRightText(groupMember.alias);
            groupNameOptionItemView.setRightText(groupInfo.name);

        } else {
            conversationMemberAdapter = new ConversationMemberAdapter(true, false);
            members = Collections.singletonList(userViewModel.getUserInfo(userId, false));
        }
        conversationMemberAdapter.setMembers(members);
        conversationMemberAdapter.setOnMemberClickListener(this);

        memberReclerView.setAdapter(conversationMemberAdapter);
        memberReclerView.setLayoutManager(new GridLayoutManager(getActivity(), 5));
        stickTopSwitchButton.setChecked(conversationInfo.isTop);
        silentSwitchButton.setChecked(conversationInfo.isSilent);
        stickTopSwitchButton.setOnCheckedChangeListener(this);
        silentSwitchButton.setOnCheckedChangeListener(this);
    }

    private void initGroupConversation(GroupInfo groupInfo) {

    }

    private void initPrivateCovnersation() {

    }

    @OnClick(R.id.groupNameOptionItemView)
    void updateGroupName() {
        Intent intent = new Intent(getActivity(), SetGroupNameActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivityForResult(intent, REQUEST_CODE_SET_GROUP_NAME);
    }

    @OnClick(R.id.groupNoticeLinearLayout)
    void updateGroupNotice() {
        // TODO
    }

    @OnClick(R.id.groupManageOptionItemView)
    void manageGroup() {
        // TODO
    }

    @OnClick(R.id.myGroupNickNameOptionItemView)
    void updateMyGroupAlias() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .input("请输入你的群昵称", groupMember.alias, false, (dialog1, input) -> {
                    groupViewModel.modifyMyGroupAlias(groupInfo.target, input.toString().trim())
                            .observe(ConversationInfoFragment.this, new Observer<OperateResult>() {
                                @Override
                                public void onChanged(@Nullable OperateResult operateResult) {
                                    if (operateResult.isSuccess()) {
                                        myGroupNickNameOptionItemView.setRightText(input.toString().trim());
                                    } else {
                                        Toast.makeText(getActivity(), "修改群昵称失败:" + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });
                })
                .negativeText("取消")
                .positiveText("确定")
                .onPositive((dialog12, which) -> {
                    dialog12.dismiss();
                })
                .build();
        dialog.show();
    }

    @OnClick(R.id.quitButton)
    void quit() {
        switch (conversationInfo.conversation.type) {
            case Group:
                break;
            case ChatRoom:
                break;
            default:
                break;
        }
    }

    private void quitGroup() {
        groupViewModel.quitGroup(conversationInfo.conversation.target, Collections.singletonList(0)).observe(this, aBoolean -> {
            if (aBoolean != null && aBoolean) {
                getActivity().finish();
            } else {
                Toast.makeText(getActivity(), "退出群组失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void quitChatRoom() {
        ChatRoomViewModel chatRoomViewModel = ViewModelProviders.of(this).get(ChatRoomViewModel.class);
        chatRoomViewModel.quitChatRoom(conversationInfo.conversation.target).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult<Boolean> booleanOperateResult) {
                if (booleanOperateResult.isSuccess()) {
                    getActivity().finish();
                } else {
                    Toast.makeText(getActivity(), "退出聊天室失败", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @OnClick(R.id.clearMessagesOptionItemView)
    void clearMessage() {
        conversationViewModel.clearConversationMessage(conversationInfo.conversation);
    }


    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @Override
    public void onAddMemberClick() {
        if (groupInfo != null) {
            Intent intent = new Intent(getActivity(), AddGroupMemberActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivityForResult(intent, REQUEST_ADD_MEMBER);
        } else {
            Intent intent = new Intent(getActivity(), CreateConversationActivity.class);
            ArrayList<String> participants = new ArrayList<>();
            participants.add(conversationInfo.conversation.target);
            intent.putExtra(PickConversationTargetActivity.CURRENT_PARTICIPANTS, participants);
            startActivity(intent);
        }
    }

    @Override
    public void onRemoveMemberClick() {
        if (groupInfo != null) {
            Intent intent = new Intent(getActivity(), RemoveGroupMemberActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivityForResult(intent, REQUEST_REMOVE_MEMBER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_MEMBER:
                if (resultCode == AddGroupMemberActivity.RESULT_ADD_SUCCESS) {
                    List<String> memberIds = data.getStringArrayListExtra("memberIds");
                    addGroupMember(memberIds);
                }
                break;
            case REQUEST_REMOVE_MEMBER:
                if (resultCode == RemoveGroupMemberActivity.RESULT_REMOVE_SUCCESS) {
                    List<String> memberIds = data.getStringArrayListExtra("memberIds");
                    removeGroupMember(memberIds);
                }
                break;
            case REQUEST_CODE_SET_GROUP_NAME:
                if (resultCode == SetGroupNameActivity.RESULT_SET_GROUP_NAME_SUCCESS) {
                    groupNameOptionItemView.setRightText(data.getStringExtra("groupName"));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void addGroupMember(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        List<UserInfo> userInfos = userViewModel.getUserInfos(memberIds);
        if (userInfos == null) {
            return;
        }
        conversationMemberAdapter.addMembers(userInfos);
    }

    private void removeGroupMember(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        conversationMemberAdapter.removeMembers(memberIds);
    }

    private void stickTop(boolean top) {
        ConversationListViewModel conversationListViewModel = ViewModelProviders
                .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
                .get(ConversationListViewModel.class);
        conversationListViewModel.setConversationTop(conversationInfo, top);
    }

    private void markGroup(boolean mark) {
        groupViewModel.setFavGroup(groupInfo.target, mark);
    }

    private void silent(boolean silent) {
        conversationViewModel.setConversationSilent(conversationInfo.conversation, silent);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.markGroupSwitchButton:
                markGroup(isChecked);
                break;
            case R.id.stickTopSwitchButton:
                stickTop(isChecked);
                break;
            case R.id.silentSwitchButton:
                silent(isChecked);
                break;
            default:
                break;
        }

    }
}
