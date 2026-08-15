/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetFragment;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 「选一个会话对象」整页：选中一个人就把这个人回传，选中多个人则先建群再把群回传。
 * 转发页里的「新建会话」走的就是这一页。
 * <p>
 * 逐行搬自 {@link PickOrCreateConversationTargetActivity}（回传结果那部分）与
 * {@code PickConversationTargetActivity}（确定菜单、{@code PickUserViewModel} 装配）。
 * 与兄弟页 {@link cn.wildfire.chat.kit.conversation.CreateConversationPageFragment}
 * 的唯一区别就是收尾：那个直接打开会话，这个把结果回传给调用方。
 * <p>
 * 结果通过 {@link WfcPageCompat#setPageResult} 回传 —— 手机端就是 {@code Activity.setResult}，
 * 右栏则在本页出栈时投递回发起方的 {@code onActivityResult}，调用方两端写法一致。
 */
public class PickOrCreateConversationTargetPageFragment extends PickConversationTargetFragment
    implements WfcPage, PickConversationTargetFragment.OnGroupPickListener {

    private TextView confirmTextView;
    private GroupViewModel groupViewModel;

    private final Observer<Object> checkStatusObserver = obj -> updateConfirmStatus();

    public static PickOrCreateConversationTargetPageFragment fromIntent(@Nullable Intent intent) {
        Bundle args = new Bundle();
        // 与 PickConversationTargetActivity 的默认值一致：点群不直接开群会话，而是当作结果返回
        args.putBoolean("pickGroupForResult", true);
        args.putBoolean("multiGroupMode", false);
        args.putStringArrayList(PickConversationTargetActivity.CURRENT_PARTICIPANTS,
            intent == null ? null
                : intent.getStringArrayListExtra(PickConversationTargetActivity.CURRENT_PARTICIPANTS));
        PickOrCreateConversationTargetPageFragment fragment = new PickOrCreateConversationTargetPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOnGroupPickListener(this);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        List<String> initialParticipantIds = getArguments() == null ? null
            : getArguments().getStringArrayList(PickConversationTargetActivity.CURRENT_PARTICIPANTS);
        pickUserViewModel.setInitialCheckedIds(initialParticipantIds);
        pickUserViewModel.setUncheckableIds(initialParticipantIds);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(checkStatusObserver);
    }

    @Override
    public void onDestroy() {
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(checkStatusObserver);
        super.onDestroy();
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.contact_pick;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        if (item == null) {
            return;
        }
        View actionView = item.getActionView();
        confirmTextView = actionView == null ? null : actionView.findViewById(R.id.confirm_tv);
        if (confirmTextView != null) {
            confirmTextView.setOnClickListener(v -> onConfirmClick());
        }
        updateConfirmStatus();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onConfirmClick();
            return true;
        }
        return false;
    }

    private void updateConfirmStatus() {
        if (confirmTextView == null || pickUserViewModel == null) {
            return;
        }
        int count = pickUserViewModel.getCheckedEmployees().size()
            + pickUserViewModel.getCheckedUsers().size()
            + pickUserViewModel.getCheckedOrganizations().size();
        if (count == 0) {
            confirmTextView.setText(R.string.pick_conversation_done);
            confirmTextView.setEnabled(false);
        } else {
            confirmTextView.setText(getString(R.string.pick_conversation_done_with_count, count));
            confirmTextView.setEnabled(true);
        }
    }

    private void onConfirmClick() {
        List<UIUserInfo> newlyChecked = getCheckedUserInfos().stream()
            .filter(UIUserInfo::isCheckable).collect(Collectors.toList());
        onContactPicked(newlyChecked, getCheckedOrganizations(), getCheckedEmployees());
    }

    // ==================== 回传结果（原 PickOrCreateConversationTargetActivity） ====================

    private void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos,
                                 List<Organization> organizations, List<Employee> employees) {
        Set<UserInfo> userInfos = new HashSet<>();
        for (UIUserInfo uiUserinfo : newlyCheckedUserInfos) {
            userInfos.add(uiUserinfo.getUserInfo());
        }
        if (employees != null) {
            for (Employee e : employees) {
                userInfos.add(e.toUserInfo());
            }
        }

        if (organizations != null && !organizations.isEmpty()) {
            OrganizationServiceViewModel organizationServiceViewModel =
                new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
            List<Integer> orgIds = new ArrayList<>();
            for (Organization org : organizations) {
                orgIds.add(org.id);
            }
            organizationServiceViewModel.getOrganizationEmployees(orgIds, true)
                .observe(getViewLifecycleOwner(), es -> {
                    if (es != null) {
                        for (Employee e : es) {
                            userInfos.add(e.toUserInfo());
                        }
                    }
                    pickTarget(new ArrayList<>(userInfos));
                });
        } else {
            pickTarget(new ArrayList<>(userInfos));
        }
    }

    private void pickTarget(List<UserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            return;
        }
        if (userInfos.size() == 1) {
            Intent data = new Intent();
            data.putExtra("userInfo", userInfos.get(0));
            finishWithResult(data);
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.creating)
            .progress(true, 100)
            .build();
        dialog.show();

        Map<String, UserInfo> userMap = new HashMap<>();
        for (UserInfo info : userInfos) {
            userMap.put(info.uid, info);
        }

        String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(
            GroupMemberSource.Type_Invite, ChatManager.Instance().getUserId());
        groupViewModel.createGroup(getActivity(), new ArrayList<>(userMap.values()), null,
                Collections.singletonList(0), null, memberExtra)
            .observe(getViewLifecycleOwner(), result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    GroupInfo groupInfo = groupViewModel.getGroupInfo(result.getResult(), false);
                    Intent data = new Intent();
                    data.putExtra("groupInfo", groupInfo);
                    WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.create_group_fail), Toast.LENGTH_SHORT).show();
                }
                // 建群失败也要关掉本页，与改造前一致（那里 finish() 在 if/else 之外）
                WfcPageCompat.finishPage(this);
            });
    }

    @Override
    public void onGroupPicked(List<GroupInfo> groupInfos) {
        Intent data = new Intent();
        data.putExtra("groupInfo", groupInfos.get(0));
        finishWithResult(data);
    }

    private void finishWithResult(Intent data) {
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
        WfcPageCompat.finishPage(this);
    }
}
