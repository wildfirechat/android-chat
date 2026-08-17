/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetFragment;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 「发起群聊 / 新建会话」整页：选人列表 + 确定菜单 + 建会话逻辑。
 * <p>
 * 逐行搬自 {@code PickConversationTargetActivity}（确定菜单、PickUserViewModel 装配）与
 * {@link CreateConversationActivity}（{@code onContactPicked} 之后的建会话逻辑）。
 * 那两个类是「抽象 Activity + 子类 Activity」的形态，页面无法脱离 Activity 存在，
 * 因此永远进不了平板右栏；现在整页收敛到本 Fragment，手机端与右栏共用同一份。
 * <p>
 * 另一个兄弟页 {@code PickOrCreateConversationTargetActivity} 仍走老的 Activity 基类
 * ——它是「选一个会话并把结果返回给调用方」的选择器，用法不同，可按同一模板单独迁移。
 */
public class CreateConversationPageFragment extends PickConversationTargetFragment
    implements WfcPage, PickConversationTargetFragment.OnGroupPickListener {

    private TextView confirmTextView;
    private GroupViewModel groupViewModel;

    private final Observer<Object> checkStatusObserver = obj -> updateConfirmStatus();

    public static CreateConversationPageFragment newInstance(@Nullable ArrayList<String> initialParticipantIds) {
        Bundle args = new Bundle();
        // 与 PickConversationTargetFragment.newInstance(pickGroupForResult=false, multiGroupMode=false)
        // 一致：点群直接开群会话，不作为结果返回
        args.putBoolean("pickGroupForResult", false);
        args.putBoolean("multiGroupMode", false);
        args.putStringArrayList(CreateConversationActivity.CURRENT_PARTICIPANTS, initialParticipantIds);
        CreateConversationPageFragment fragment = new CreateConversationPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static CreateConversationPageFragment fromIntent(Intent intent) {
        return newInstance(intent.getStringArrayListExtra(CreateConversationActivity.CURRENT_PARTICIPANTS));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOnGroupPickListener(this);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        List<String> initialParticipantIds = getArguments() == null ? null
            : getArguments().getStringArrayList(CreateConversationActivity.CURRENT_PARTICIPANTS);
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

    // ==================== 建会话（原 CreateConversationActivity） ====================

    private void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos,
                                 List<Organization> organizations, List<Employee> employees) {
        List<String> initialCheckedIds = pickUserViewModel.getInitialCheckedIds();
        List<UserInfo> userInfos = null;
        if (initialCheckedIds != null && !initialCheckedIds.isEmpty()) {
            UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
            userInfos = userViewModel.getUserInfos(initialCheckedIds);
        }
        userInfos = userInfos == null ? new ArrayList<>() : userInfos;

        if (newlyCheckedUserInfos != null) {
            for (UIUserInfo uiUserinfo : newlyCheckedUserInfos) {
                userInfos.add(uiUserinfo.getUserInfo());
            }
        }
        if (employees != null) {
            for (Employee e : employees) {
                userInfos.add(e.toUserInfo());
            }
        }

        if (organizations != null && !organizations.isEmpty() && Config.ENABLE_SELECT_ORGANIZATION) {
            OrganizationServiceViewModel organizationServiceViewModel =
                new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
            List<Integer> orgIds = new ArrayList<>();
            for (Organization org : organizations) {
                orgIds.add(org.id);
            }
            List<UserInfo> finalUserInfos = userInfos;
            organizationServiceViewModel.getOrganizationEmployees(orgIds, true)
                .observe(getViewLifecycleOwner(), es -> {
                    if (es != null) {
                        for (Employee e : es) {
                            finalUserInfos.add(e.toUserInfo());
                        }
                    }
                    startConversation(finalUserInfos);
                });
        } else {
            startConversation(userInfos);
        }
    }

    private void startConversation(List<UserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            return;
        }
        if (userInfos.size() == 1) {
            Toast.makeText(getActivity(), R.string.create_conversation_multi_contact_hint, Toast.LENGTH_SHORT).show();
            openConversation(new Conversation(Conversation.ConversationType.Single, userInfos.get(0).uid));
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.creating_conversation)
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
                    Toast.makeText(getActivity(), getString(R.string.create_group_success), Toast.LENGTH_SHORT).show();
                    openConversation(new Conversation(Conversation.ConversationType.Group, result.getResult(), 0));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.create_group_fail), Toast.LENGTH_SHORT).show();
                    WfcPageCompat.finishPage(this);
                }
            });
    }

    @Override
    public void onGroupPicked(List<GroupInfo> groupInfos) {
        openConversation(new Conversation(Conversation.ConversationType.Group, groupInfos.get(0).target));
    }

    /**
     * 打开新建出来的会话，并把本页从导航栈里去掉 —— 选人页已经用完，返回时不该再回到它。
     * <p>
     * 右栏走 {@link WfcPageCompat#replaceSelfWithPage}：本页先出栈，会话页压到本页原来的位置，
     * 于是从会话返回就直接回到欢迎页（或本页下面那一层），与手机端
     * {@code startActivity(会话) + finish()} 的返回路径一致。
     * <p>
     * 不在右栏（手机端，或平板上本页被全屏打开）时原样走 {@code ConversationRouter} + finish，
     * 与改造前逐字节相同。
     */
    private void openConversation(Conversation conversation) {
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        intent.putExtra("conversation", conversation);
        if (WfcPageCompat.replaceSelfWithPage(this, intent)) {
            return;
        }
        ConversationRouter.open(this, intent);
        WfcPageCompat.finishAfterOpeningPage(this);
    }
}
