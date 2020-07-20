package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
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
import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.group.AddGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.group.GroupMemberListActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.group.RemoveGroupMemberActivity;
import cn.wildfire.chat.kit.group.SetGroupAnnouncementActivity;
import cn.wildfire.chat.kit.group.SetGroupNameActivity;
import cn.wildfire.chat.kit.group.manage.GroupManageActivity;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.search.SearchMessageActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

public class GroupConversationInfoFragment extends Fragment implements ConversationMemberAdapter.OnMemberClickListener, CompoundButton.OnCheckedChangeListener {

    @BindView(R2.id.progressBar)
    ProgressBar progressBar;

    @BindView(R2.id.contentNestedScrollView)
    NestedScrollView contentNestedScrollView;

    // group
    @BindView(R2.id.groupLinearLayout_0)
    LinearLayout groupLinearLayout_0;
    @BindView(R2.id.groupNameOptionItemView)
    OptionItemView groupNameOptionItemView;
    @BindView(R2.id.groupQRCodeOptionItemView)
    OptionItemView groupQRCodeOptionItemView;
    @BindView(R2.id.groupNoticeLinearLayout)
    LinearLayout noticeLinearLayout;
    @BindView(R2.id.groupNoticeTextView)
    TextView noticeTextView;
    @BindView(R2.id.groupManageOptionItemView)
    OptionItemView groupManageOptionItemView;
    @BindView(R2.id.groupManageDividerLine)
    View groupManageDividerLine;
    @BindView(R2.id.showAllMemberButton)
    Button showAllGroupMemberButton;

    @BindView(R2.id.groupLinearLayout_1)
    LinearLayout groupLinearLayout_1;
    @BindView(R2.id.myGroupNickNameOptionItemView)
    OptionItemView myGroupNickNameOptionItemView;
    @BindView(R2.id.showGroupMemberAliasSwitchButton)
    SwitchButton showGroupMemberNickNameSwitchButton;

    @BindView(R2.id.quitButton)
    TextView quitGroupButton;

    @BindView(R2.id.markGroupLinearLayout)
    LinearLayout markGroupLinearLayout;
    @BindView(R2.id.markGroupSwitchButton)
    SwitchButton markGroupSwitchButton;

    // common
    @BindView(R2.id.memberRecyclerView)
    RecyclerView memberReclerView;
    @BindView(R2.id.stickTopSwitchButton)
    SwitchButton stickTopSwitchButton;
    @BindView(R2.id.silentSwitchButton)
    SwitchButton silentSwitchButton;

    private ConversationInfo conversationInfo;
    private ConversationMemberAdapter conversationMemberAdapter;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;

    private GroupViewModel groupViewModel;
    private GroupInfo groupInfo;
    // me in group
    private GroupMember groupMember;


    public static GroupConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        GroupConversationInfoFragment fragment = new GroupConversationInfoFragment();
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
        View view = inflater.inflate(R.layout.conversation_info_group_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndShowGroupNotice();
    }

    private void init() {
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        groupLinearLayout_0.setVisibility(View.VISIBLE);
        groupLinearLayout_1.setVisibility(View.VISIBLE);
        markGroupLinearLayout.setVisibility(View.VISIBLE);
        markGroupSwitchButton.setOnCheckedChangeListener(this);
        quitGroupButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        groupInfo = groupViewModel.getGroupInfo(conversationInfo.conversation.target, true);
        if (groupInfo != null) {
            groupMember = ChatManager.Instance().getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        }

        if (groupMember == null || groupMember.type == GroupMember.GroupMemberType.Removed) {
            Toast.makeText(getActivity(), "你不在群组或发生错误, 请稍后再试", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }
        loadAndShowGroupMembers(true);

        userViewModel.userInfoLiveData().observe(this, userInfos -> loadAndShowGroupMembers(false));
        observerFavGroupsUpdate();
        observerGroupInfoUpdate();
        observerGroupMembersUpdate();
    }

    private void observerFavGroupsUpdate() {
        groupViewModel.getFavGroups().observe(this, listOperateResult -> {
            if (listOperateResult.isSuccess()) {
                for (GroupInfo info : listOperateResult.getResult()) {
                    if (groupInfo.target.equals(info.target)) {
                        markGroupSwitchButton.setChecked(true);
                        break;
                    }
                }
            }
        });
    }

    private void observerGroupMembersUpdate() {
        groupViewModel.groupMembersUpdateLiveData().observe(this, groupMembers -> {
            loadAndShowGroupMembers(false);
        });
    }

    private void observerGroupInfoUpdate() {
        groupViewModel.groupInfoUpdateLiveData().observe(this, groupInfos -> {
            for (GroupInfo groupInfo : groupInfos) {
                if (groupInfo.target.equals(this.groupInfo.target)) {
                    groupNameOptionItemView.setDesc(groupInfo.name);
                    loadAndShowGroupMembers(false);
                    break;
                }
            }

        });
    }


    private void loadAndShowGroupMembers(boolean refresh) {
        groupViewModel.getGroupMembersLiveData(conversationInfo.conversation.target, refresh)
            .observe(this, groupMembers -> {
                progressBar.setVisibility(View.GONE);
                showGroupMembers(groupMembers);
                showGroupManageViews();
                contentNestedScrollView.setVisibility(View.VISIBLE);
            });
    }

    private void loadAndShowGroupNotice() {

        WfcUIKit.getWfcUIKit().getAppServiceProvider().getGroupAnnouncement(groupInfo.target, new AppServiceProvider.GetGroupAnnouncementCallback() {
            @Override
            public void onUiSuccess(GroupAnnouncement announcement) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (TextUtils.isEmpty(announcement.text)) {
                    noticeTextView.setVisibility(View.GONE);
                } else {
                    noticeTextView.setText(announcement.text);
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                noticeTextView.setVisibility(View.GONE);
            }
        });
    }

    private void showGroupManageViews() {
        if (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner) {
            groupManageOptionItemView.setVisibility(View.VISIBLE);
        }

        showGroupMemberNickNameSwitchButton.setChecked("1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target)));
        showGroupMemberNickNameSwitchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userViewModel.setUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target, isChecked ? "1" : "0");
        });

        myGroupNickNameOptionItemView.setDesc(groupMember.alias);
        groupNameOptionItemView.setDesc(groupInfo.name);

        stickTopSwitchButton.setChecked(conversationInfo.isTop);
        silentSwitchButton.setChecked(conversationInfo.isSilent);
        stickTopSwitchButton.setOnCheckedChangeListener(this);
        silentSwitchButton.setOnCheckedChangeListener(this);

        if (groupInfo != null && ChatManagerHolder.gChatManager.getUserId().equals(groupInfo.owner)) {
            quitGroupButton.setText(R.string.delete_and_dismiss);
        } else {
            quitGroupButton.setText(R.string.delete_and_exit);
        }
    }

    private void showGroupMembers(List<GroupMember> groupMembers) {
        if (groupMembers == null || groupMembers.isEmpty()) {
            return;
        }
        String userId = ChatManager.Instance().getUserId();
        List<String> memberIds = new ArrayList<>();
        for (GroupMember member : groupMembers) {
            memberIds.add(member.memberId);
        }

        boolean enableRemoveMember = false;
        boolean enableAddMember = false;
        if (groupInfo.joinType == 2) {
            if (groupMember.type == GroupMember.GroupMemberType.Owner || groupMember.type == GroupMember.GroupMemberType.Manager) {
                enableAddMember = true;
                enableRemoveMember = true;
            }
        } else {
            enableAddMember = true;
            if (groupMember.type != GroupMember.GroupMemberType.Normal || userId.equals(groupInfo.owner)) {
                enableRemoveMember = true;
            }
        }
        int maxShowMemberCount = 45;
        if (enableAddMember) {
            maxShowMemberCount--;
        }
        if (enableRemoveMember) {
            maxShowMemberCount--;
        }
        if (memberIds.size() > maxShowMemberCount) {
            showAllGroupMemberButton.setVisibility(View.VISIBLE);
            memberIds = memberIds.subList(0, maxShowMemberCount);
        }

        conversationMemberAdapter = new ConversationMemberAdapter(conversationInfo, enableAddMember, enableRemoveMember);
        List<UserInfo> members = UserViewModel.getUsers(memberIds, groupInfo.target);

        conversationMemberAdapter.setMembers(members);
        conversationMemberAdapter.setOnMemberClickListener(this);
        memberReclerView.setAdapter(conversationMemberAdapter);
        memberReclerView.setLayoutManager(new GridLayoutManager(getActivity(), 5));
        memberReclerView.setNestedScrollingEnabled(false);
        memberReclerView.setHasFixedSize(true);
        memberReclerView.setFocusable(false);

    }

    @OnClick(R2.id.groupNameOptionItemView)
    void updateGroupName() {
        if (groupInfo.type != GroupInfo.GroupType.Restricted
            || (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner)) {
            Intent intent = new Intent(getActivity(), SetGroupNameActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        }
    }

    @OnClick(R2.id.groupNoticeLinearLayout)
    void updateGroupNotice() {
        if (groupInfo.type != GroupInfo.GroupType.Restricted
            || (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner)) {
            Intent intent = new Intent(getActivity(), SetGroupAnnouncementActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        }
    }

    @OnClick(R2.id.groupManageOptionItemView)
    void manageGroup() {
        Intent intent = new Intent(getActivity(), GroupManageActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.showAllMemberButton)
    void showAllGroupMember() {
        Intent intent = new Intent(getActivity(), GroupMemberListActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.myGroupNickNameOptionItemView)
    void updateMyGroupAlias() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .input("请输入你的群昵称", groupMember.alias, true, (dialog1, input) -> {
                if (TextUtils.isEmpty(groupMember.alias)) {
                    if (TextUtils.isEmpty(input.toString().trim())) {
                        return;
                    }
                } else if (groupMember.alias.equals(input.toString().trim())) {
                    return;
                }

                groupViewModel.modifyMyGroupAlias(groupInfo.target, input.toString().trim(), null, Collections.singletonList(0))
                    .observe(GroupConversationInfoFragment.this, operateResult -> {
                        if (operateResult.isSuccess()) {
                            groupMember.alias = input.toString().trim();
                            myGroupNickNameOptionItemView.setDesc(input.toString().trim());
                        } else {
                            Toast.makeText(getActivity(), "修改群昵称失败:" + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
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

    @OnClick(R2.id.quitButton)
    void quitGroup() {
        if (groupInfo != null && userViewModel.getUserId().equals(groupInfo.owner)) {
            groupViewModel.dismissGroup(conversationInfo.conversation.target, Collections.singletonList(0), null).observe(this, aBoolean -> {
                if (aBoolean != null && aBoolean) {
                    // FIXME: 2020/7/20 jyj
//                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(null);
                } else {
                    Toast.makeText(getActivity(), "解散群组失败", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            groupViewModel.quitGroup(conversationInfo.conversation.target, Collections.singletonList(0), null).observe(this, aBoolean -> {
                if (aBoolean != null && aBoolean) {
                    // FIXME: 2020/7/20 jyj
//                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(null);
                } else {
                    Toast.makeText(getActivity(), "退出群组失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @OnClick(R2.id.clearMessagesOptionItemView)
    void clearMessage() {
        conversationViewModel.clearConversationMessage(conversationInfo.conversation);
    }

    @OnClick(R2.id.groupQRCodeOptionItemView)
    void showGroupQRCode() {
        String qrCodeValue = WfcScheme.QR_CODE_PREFIX_GROUP + groupInfo.target;
        Intent intent = QRCodeActivity.buildQRCodeIntent(getActivity(), "群二维码", groupInfo.portrait, qrCodeValue);
        startActivity(intent);
    }

    @OnClick(R2.id.searchMessageOptionItemView)
    void searchGroupMessage() {
        Intent intent = new Intent(getActivity(), SearchMessageActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        if (groupInfo != null && groupInfo.privateChat == 1 && groupMember.type != GroupMember.GroupMemberType.Owner && groupMember.type != GroupMember.GroupMemberType.Manager && !userInfo.uid.equals(groupInfo.owner)) {
            Toast.makeText(getActivity(), "禁止群成员私聊", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @Override
    public void onAddMemberClick() {
        Intent intent = new Intent(getActivity(), AddGroupMemberActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @Override
    public void onRemoveMemberClick() {
        if (groupInfo != null) {
            Intent intent = new Intent(getActivity(), RemoveGroupMemberActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        }
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
        int id = buttonView.getId();
        if (id == R.id.markGroupSwitchButton) {
            markGroup(isChecked);
        } else if (id == R.id.stickTopSwitchButton) {
            stickTop(isChecked);
        } else if (id == R.id.silentSwitchButton) {
            silent(isChecked);
        }

    }
}
