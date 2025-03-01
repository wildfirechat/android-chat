/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.app.Activity;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.file.FileRecordActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.group.AddGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.group.GroupMemberListActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.group.RemoveGroupMemberActivity;
import cn.wildfire.chat.kit.group.SetGroupAnnouncementActivity;
import cn.wildfire.chat.kit.group.SetGroupNameActivity;
import cn.wildfire.chat.kit.group.SetGroupRemarkActivity;
import cn.wildfire.chat.kit.group.manage.GroupManageActivity;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.search.SearchMessageActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

public class GroupConversationInfoFragment extends Fragment implements ConversationMemberAdapter.OnMemberClickListener, CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    ProgressBar progressBar;

    NestedScrollView contentNestedScrollView;

    // group
    LinearLayout groupLinearLayout_0;
    OptionItemView groupNameOptionItemView;
    OptionItemView groupPortraitOptionItemView;
    OptionItemView groupRemarkOptionItemView;
    OptionItemView groupQRCodeOptionItemView;
    LinearLayout noticeLinearLayout;
    TextView noticeTextView;
    OptionItemView groupManageOptionItemView;
    View groupManageDividerLine;
    Button showAllGroupMemberButton;

    LinearLayout groupLinearLayout_1;
    OptionItemView myGroupNickNameOptionItemView;
    SwitchMaterial showGroupMemberNickNameSwitchButton;

    TextView quitGroupButton;

    LinearLayout markGroupLinearLayout;
    SwitchMaterial markGroupSwitchButton;

    // common
    RecyclerView memberReclerView;
    SwitchMaterial stickTopSwitchButton;
    SwitchMaterial silentSwitchButton;

    OptionItemView fileRecordOptionItem;

    private ConversationInfo conversationInfo;
    private ConversationMemberAdapter conversationMemberAdapter;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;

    private GroupViewModel groupViewModel;
    private GroupInfo groupInfo;
    // me in group
    private GroupMember selfGroupMember;
    private static final int maxShowGroupMemberCount = 20;


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
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.groupNameOptionItemView).setOnClickListener(_v -> updateGroupName());
        view.findViewById(R.id.groupPortraitOptionItemView).setOnClickListener(_v -> updateGroupPortrait());
        view.findViewById(R.id.groupRemarkOptionItemView).setOnClickListener(_v -> updateGroupRemark());
        view.findViewById(R.id.groupNoticeLinearLayout).setOnClickListener(_v -> updateGroupNotice());
        view.findViewById(R.id.groupManageOptionItemView).setOnClickListener(_v -> manageGroup());
        view.findViewById(R.id.showAllMemberButton).setOnClickListener(_v -> showAllGroupMember());
        view.findViewById(R.id.myGroupNickNameOptionItemView).setOnClickListener(_v -> updateMyGroupAlias());
        view.findViewById(R.id.quitButton).setOnClickListener(_v -> quitGroup());
        view.findViewById(R.id.clearMessagesOptionItemView).setOnClickListener(_v -> clearMessage());
        view.findViewById(R.id.groupQRCodeOptionItemView).setOnClickListener(_v -> showGroupQRCode());
        view.findViewById(R.id.searchMessageOptionItemView).setOnClickListener(_v -> searchGroupMessage());
        view.findViewById(R.id.fileRecordOptionItemView).setOnClickListener(_v -> fileRecord());
    }

    private void bindViews(View view) {
        progressBar = view.findViewById(R.id.progressBar);
        contentNestedScrollView = view.findViewById(R.id.contentNestedScrollView);
        groupLinearLayout_0 = view.findViewById(R.id.groupLinearLayout_0);
        groupNameOptionItemView = view.findViewById(R.id.groupNameOptionItemView);
        groupPortraitOptionItemView = view.findViewById(R.id.groupPortraitOptionItemView);
        groupRemarkOptionItemView = view.findViewById(R.id.groupRemarkOptionItemView);
        groupQRCodeOptionItemView = view.findViewById(R.id.groupQRCodeOptionItemView);
        noticeLinearLayout = view.findViewById(R.id.groupNoticeLinearLayout);
        noticeTextView = view.findViewById(R.id.groupNoticeTextView);
        groupManageOptionItemView = view.findViewById(R.id.groupManageOptionItemView);
        groupManageDividerLine = view.findViewById(R.id.groupManageDividerLine);
        showAllGroupMemberButton = view.findViewById(R.id.showAllMemberButton);
        groupLinearLayout_1 = view.findViewById(R.id.groupLinearLayout_1);
        myGroupNickNameOptionItemView = view.findViewById(R.id.myGroupNickNameOptionItemView);
        showGroupMemberNickNameSwitchButton = view.findViewById(R.id.showGroupMemberAliasSwitchButton);
        quitGroupButton = view.findViewById(R.id.quitButton);
        markGroupLinearLayout = view.findViewById(R.id.markGroupLinearLayout);
        markGroupSwitchButton = view.findViewById(R.id.markGroupSwitchButton);
        memberReclerView = view.findViewById(R.id.memberRecyclerView);
        stickTopSwitchButton = view.findViewById(R.id.stickTopSwitchButton);
        silentSwitchButton = view.findViewById(R.id.silentSwitchButton);
        fileRecordOptionItem = view.findViewById(R.id.fileRecordOptionItemView);
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
        progressBar.setVisibility(View.VISIBLE);
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        groupInfo = groupViewModel.getGroupInfo(conversationInfo.conversation.target, true);
        if (groupInfo != null) {
            selfGroupMember = ChatManager.Instance().getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());

            if (groupInfo.type != GroupInfo.GroupType.Organization) {
                quitGroupButton.setVisibility(View.VISIBLE);
            }

            if (groupInfo.deleted == 1) {
                Toast.makeText(getActivity(), getString(R.string.group_dismissed_toast), Toast.LENGTH_SHORT).show();
                getActivity().finish();
                return;
            }
        }

        if (selfGroupMember == null || selfGroupMember.type == GroupMember.GroupMemberType.Removed) {
            Toast.makeText(getActivity(), getString(R.string.group_not_member_error), Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }
        loadAndShowGroupMembers(true);

        userViewModel.userInfoLiveData().observe(this, userInfos -> loadAndShowGroupMembers(false));
        observerFavGroupsUpdate();
        observerGroupInfoUpdate();
        observerGroupMembersUpdate();

        if (ChatManager.Instance().isCommercialServer()) {
            fileRecordOptionItem.setVisibility(View.VISIBLE);
        } else {
            fileRecordOptionItem.setVisibility(View.GONE);
        }
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
                    this.groupInfo = groupInfo;
                    if (groupInfo.deleted == 1) {
                        Toast.makeText(getActivity(), getString(R.string.group_dismissed_toast), Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                        return;
                    }
                    groupNameOptionItemView.setDesc(groupInfo.name);
                    groupRemarkOptionItemView.setDesc(groupInfo.remark);
                    Glide.with(this).load(groupInfo.portrait).placeholder(R.mipmap.ic_group_chat).into(groupPortraitOptionItemView.getEndImageView());
                    loadAndShowGroupMembers(false);
                    break;
                }
            }
        });
    }

    private void loadAndShowGroupMembers(boolean refresh) {
        groupViewModel.getGroupMembersLiveData(conversationInfo.conversation.target, maxShowGroupMemberCount)
            .observe(getViewLifecycleOwner(), groupMembers -> {
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
        if (selfGroupMember.type == GroupMember.GroupMemberType.Manager || selfGroupMember.type == GroupMember.GroupMemberType.Owner) {
            groupManageOptionItemView.setVisibility(View.VISIBLE);
        }

        showGroupMemberNickNameSwitchButton.setChecked(!"1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target)));
        showGroupMemberNickNameSwitchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userViewModel.setUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target, isChecked ? "0" : "1");
        });

        myGroupNickNameOptionItemView.setDesc(selfGroupMember.alias);
        groupNameOptionItemView.setDesc(groupInfo.name);
        Glide.with(this).load(groupInfo.portrait).transform(new RoundedCorners(5)).placeholder(R.mipmap.ic_group_chat).into(groupPortraitOptionItemView.getEndImageView());
        groupRemarkOptionItemView.setDesc(groupInfo.remark);

        stickTopSwitchButton.setChecked(conversationInfo.top > 0);
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
        int toShowMemberCount = maxShowGroupMemberCount;
        if (groupInfo.type != GroupInfo.GroupType.Organization) {
            if (groupInfo.joinType == 2) {
                if (selfGroupMember.type == GroupMember.GroupMemberType.Owner || selfGroupMember.type == GroupMember.GroupMemberType.Manager) {
                    enableAddMember = true;
                    enableRemoveMember = true;
                }
            } else {
                enableAddMember = true;
                if (selfGroupMember.type == GroupMember.GroupMemberType.Owner || selfGroupMember.type == GroupMember.GroupMemberType.Manager) {
                    enableRemoveMember = true;
                }
            }
            if (enableAddMember) {
                toShowMemberCount--;
            }
            if (enableRemoveMember) {
                toShowMemberCount--;
            }
        }
        if (memberIds.size() > toShowMemberCount) {
            showAllGroupMemberButton.setVisibility(View.VISIBLE);
            memberIds = memberIds.subList(0, toShowMemberCount);
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

    void updateGroupName() {
        if ((groupInfo.type != GroupInfo.GroupType.Restricted && groupInfo.type != GroupInfo.GroupType.Organization)
            || (selfGroupMember.type == GroupMember.GroupMemberType.Manager || selfGroupMember.type == GroupMember.GroupMemberType.Owner)) {
            Intent intent = new Intent(getActivity(), SetGroupNameActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        }
    }

    void updateGroupPortrait() {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    void updateGroupRemark() {
        Intent intent = new Intent(getActivity(), SetGroupRemarkActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    void updateGroupNotice() {
        if ((groupInfo.type != GroupInfo.GroupType.Restricted && groupInfo.type != GroupInfo.GroupType.Organization)
            || (selfGroupMember.type == GroupMember.GroupMemberType.Manager || selfGroupMember.type == GroupMember.GroupMemberType.Owner)) {
            Intent intent = new Intent(getActivity(), SetGroupAnnouncementActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        }
    }

    void manageGroup() {
        Intent intent = new Intent(getActivity(), GroupManageActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    void showAllGroupMember() {
        Intent intent = new Intent(getActivity(), GroupMemberListActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    void updateMyGroupAlias() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .input(getString(R.string.my_group_nickname_dialog_title), selfGroupMember.alias, true, (dialog1, input) -> {
                if (TextUtils.isEmpty(selfGroupMember.alias)) {
                    if (TextUtils.isEmpty(input.toString().trim())) {
                        return;
                    }
                } else if (selfGroupMember.alias.equals(input.toString().trim())) {
                    return;
                }

                groupViewModel.modifyMyGroupAlias(groupInfo.target, input.toString().trim(), null, Collections.singletonList(0))
                    .observe(GroupConversationInfoFragment.this, operateResult -> {
                        if (operateResult.isSuccess()) {
                            selfGroupMember.alias = input.toString().trim();
                            myGroupNickNameOptionItemView.setDesc(input.toString().trim());
                        } else {
                            Toast.makeText(getActivity(), "修改群昵称失败:" + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .negativeText(getString(R.string.action_cancel))
            .positiveText(getString(R.string.action_confirm))
            .onPositive((dialog12, which) -> {
                dialog12.dismiss();
            })
            .build();
        dialog.show();
    }

    void quitGroup() {
        if (groupInfo == null) {
            return;
        }

        String title;
        String content;
        if (userViewModel.getUserId().equals(groupInfo.owner)) {
            title = getString(R.string.dismiss_group_title);
            content = getString(R.string.dismiss_group_confirm_message);
        } else {
            title = getString(R.string.quit_group_title);
            content = getString(R.string.quit_group_confirm_message);
        }
        new AlertDialog.Builder(getActivity())
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton(getString(R.string.action_confirm), (dialog, which) -> {
                if (userViewModel.getUserId().equals(groupInfo.owner)) {
                    groupViewModel.dismissGroup(conversationInfo.conversation.target, Collections.singletonList(0), null).observe(this, aBoolean -> {
                        if (aBoolean != null && aBoolean) {
                            Intent intent = new Intent(getContext().getPackageName() + ".main");
                            startActivity(intent);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.dismiss_group_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    groupViewModel.quitGroup(conversationInfo.conversation.target, Collections.singletonList(0), null).observe(this, aBoolean -> {
                        if (aBoolean != null && aBoolean) {
                            Intent intent = new Intent(getContext().getPackageName() + ".main");
                            startActivity(intent);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.quit_group_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            })
            .setNegativeButton(getString(R.string.action_cancel), (dialog, which) -> {
                // do nothing
            })
            .show();
    }

    void clearMessage() {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.clear_local_chat));
        if (this.groupInfo.superGroup != 1) {
            items.add(getString(R.string.clear_remote_chat));
        }
        new MaterialDialog.Builder(getActivity())
            .items(items)
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

    void showGroupQRCode() {
        String qrCodeValue = WfcScheme.buildGroupScheme(groupInfo.target, ChatManager.Instance().getUserId());
        Intent intent = QRCodeActivity.buildQRCodeIntent(getActivity(), "群二维码", groupInfo.portrait, qrCodeValue);
        startActivity(intent);
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

    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        if (groupInfo != null && groupInfo.privateChat == 1 && selfGroupMember.type != GroupMember.GroupMemberType.Owner && selfGroupMember.type != GroupMember.GroupMemberType.Manager && !userInfo.uid.equals(groupInfo.owner)) {
            Toast.makeText(getActivity(), getString(R.string.group_member_disable_chat), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        intent.putExtra("groupId", groupInfo.target);
        GroupMember groupMember = ChatManager.Instance().getGroupMember(groupInfo.target, userInfo.uid);
        GroupMemberSource source = GroupMemberSource.getGroupMemberSource(groupMember.extra);
        intent.putExtra("groupMemberSource", source);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images == null || images.isEmpty()) {
                Toast.makeText(getActivity(), getString(R.string.update_portrait_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            File thumbImgFile = ImageUtils.genThumbImgFile(images.get(0).path);
            if (thumbImgFile == null) {
                Toast.makeText(getActivity(), getString(R.string.update_portrait_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            String imagePath = thumbImgFile.getAbsolutePath();
            MutableLiveData<OperateResult<Boolean>> result = groupViewModel.updateGroupPortrait(groupInfo.target, imagePath);
            result.observe(this, booleanOperateResult -> {
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(getActivity(), getString(R.string.update_portrait_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.update_portrait_failed, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void stickTop(boolean top) {
        ConversationListViewModel conversationListViewModel = ViewModelProviders
            .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
            .get(ConversationListViewModel.class);
        conversationListViewModel.setConversationTop(conversationInfo, top ? 1 : 0);
        conversationInfo.top = top ? 1 : 0;
    }

    private void markGroup(boolean mark) {
        groupViewModel.setFavGroup(groupInfo.target, mark);
    }

    private void silent(boolean silent) {
        conversationViewModel.setConversationSilent(conversationInfo.conversation, silent);
        conversationInfo.isSilent = silent;
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