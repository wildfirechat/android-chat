/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.stream.Collectors;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.organization.model.Employee;
import cn.wildfire.chat.kit.organization.model.Organization;

/**
 * 「选人（可跨群、跨组织）」页的旧基类。
 * <p>
 * <strong>本仓库内已无子类</strong>：两个子类分别下沉成了
 * {@code CreateConversationPageFragment}（选完直接开会话）与
 * {@code PickOrCreateConversationTargetPageFragment}（选完把结果回传），
 * 它们都继承 {@link PickConversationTargetFragment} 并实现 {@code WfcPage}，手机端与右栏共用。
 * <p>
 * <strong>新增这类页面请继承 {@link PickConversationTargetFragment}</strong>，别再从这里派生。
 * 本类仅为已经继承它的外部集成方保留。{@link #CURRENT_PARTICIPANTS} 仍是两端通用的 extra 键名。
 *
 * @deprecated 用 {@link PickConversationTargetFragment} + {@code WfcPage}
 */
@Deprecated
public abstract class PickConversationTargetActivity extends WfcBaseActivity implements PickConversationTargetFragment.OnGroupPickListener {
    public static final String CURRENT_PARTICIPANTS = "currentParticipants";

    private boolean pickGroupForResult = true;
    private boolean multiGroupMode = false;
    private TextView confirmTv;

    protected PickUserViewModel pickUserViewModel;
    private PickConversationTargetFragment fragment;
    private Observer<Object> contactCheckStatusUpdateLiveDataObserver = new Observer<Object>() {
        @Override
        public void onChanged(@Nullable Object obj) {
            updateConfirmMenuItemStatus();
        }
    };

    protected void updateConfirmMenuItemStatus() {
        int count = pickUserViewModel.getCheckedEmployees().size() + pickUserViewModel.getCheckedUsers().size() + pickUserViewModel.getCheckedOrganizations().size();
        if (count == 0) {
            confirmTv.setText(R.string.pick_conversation_done);
            confirmTv.setEnabled(false);
        } else {
            confirmTv.setText(getString(R.string.pick_conversation_done_with_count, count));
            confirmTv.setEnabled(true);
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        Intent intent = getIntent();
        List<String> initialParticipantsIds = intent.getStringArrayListExtra(CURRENT_PARTICIPANTS);

        pickUserViewModel = new ViewModelProvider(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        pickUserViewModel.setInitialCheckedIds(initialParticipantsIds);
        pickUserViewModel.setUncheckableIds(initialParticipantsIds);

        initView();
    }

    @Override
    protected int menu() {
        return R.menu.contact_pick;
    }

    @Override
    protected void afterMenus(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        View view = item.getActionView();
        confirmTv = view.findViewById(R.id.confirm_tv);
        confirmTv.setOnClickListener(v -> onConfirmClick());

        updateConfirmMenuItemStatus();
    }

    private void initView() {
        fragment = PickConversationTargetFragment.newInstance(pickGroupForResult, multiGroupMode);
        fragment.setOnGroupPickListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    protected abstract void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos, List<Organization> organizations, List<Employee> employees);

    protected void onConfirmClick() {
        List<UIUserInfo> newlyCheckedUserInfos = fragment.getCheckedUserInfos().stream().filter(UIUserInfo::isCheckable).collect(Collectors.toList());
        onContactPicked(newlyCheckedUserInfos, fragment.getCheckedOrganizations(), fragment.getCheckedEmployees());
    }
}
