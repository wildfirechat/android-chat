package cn.wildfire.chat.kit.group;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public abstract class BasePickGroupMemberActivity extends WfcBaseActivity {
    protected GroupInfo groupInfo;
    protected List<String> unCheckableMemberIds;

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
        int maxPickCount = getIntent().getIntExtra("maxCount", Integer.MAX_VALUE);
        if (groupInfo == null) {
            finish();
            return;
        }

        pickUserViewModel = ViewModelProviders.of(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(userCheckStatusUpdateLiveDataObserver);
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
                .replace(R.id.containerFrameLayout, PickGroupMemberFragment.newInstance(groupInfo))
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(userCheckStatusUpdateLiveDataObserver);
    }
}
