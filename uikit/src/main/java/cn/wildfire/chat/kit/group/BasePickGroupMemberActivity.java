/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.GroupInfo;

public abstract class BasePickGroupMemberActivity extends WfcBaseActivity {
    protected GroupInfo groupInfo;
    protected List<String> unCheckableMemberIds;
    protected List<String> checkedMemberIds;

    public static final String GROUP_INFO = "groupInfo";
    public static final String UNCHECKABLE_MEMBER_IDS = "unCheckableMemberIds";
    public static final String CHECKED_MEMBER_IDS = "checkedMemberIds";
    public static final String MAX_COUNT = "maxCount";

    protected PickUserViewModel pickUserViewModel;
    private Observer<UIUserInfo> userCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickUserViewModel.getCheckedUsers();
            onGroupMemberChecked(list);
        }
    };

    /**
     * 当群成员选择情况变化时调用
     *
     * @param checkedUserInfos
     */
    protected abstract void onGroupMemberChecked(List<UIUserInfo> checkedUserInfos);

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        unCheckableMemberIds = getIntent().getStringArrayListExtra("unCheckableMemberIds");
        checkedMemberIds = getIntent().getStringArrayListExtra(CHECKED_MEMBER_IDS);
        int maxPickCount = getIntent().getIntExtra("maxCount", Integer.MAX_VALUE);
        if (groupInfo == null) {
            finish();
            return;
        }

        pickUserViewModel = ViewModelProviders.of(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(userCheckStatusUpdateLiveDataObserver);
        if (checkedMemberIds != null && !checkedMemberIds.isEmpty()) {
            pickUserViewModel.setInitialCheckedIds(checkedMemberIds);
            pickUserViewModel.setUncheckableIds(checkedMemberIds);
        }

        if (unCheckableMemberIds != null && !unCheckableMemberIds.isEmpty()) {
            pickUserViewModel.setUncheckableIds(unCheckableMemberIds);
        } else {
            UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
            List<String> list = new ArrayList<>();
            list.add(userViewModel.getUserId());
            pickUserViewModel.setUncheckableIds(list);
        }
        pickUserViewModel.setMaxPickCount(maxPickCount);

        initView();
    }

    private void initView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, getFragment())
                .commit();
    }

    protected Fragment getFragment() {
        return PickGroupMemberFragment.newInstance(groupInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(userCheckStatusUpdateLiveDataObserver);
    }
}
