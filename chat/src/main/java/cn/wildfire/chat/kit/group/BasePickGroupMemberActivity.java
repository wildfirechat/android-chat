package cn.wildfire.chat.kit.group;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickContactViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public abstract class BasePickGroupMemberActivity extends WfcBaseActivity {
    protected GroupInfo groupInfo;

    private PickContactViewModel pickContactViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickContactViewModel.getCheckedContacts();
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
        int maxPickCount = getIntent().getIntExtra("maxCount", Integer.MAX_VALUE);
        if (groupInfo == null) {
            finish();
            return;
        }

        pickContactViewModel = ViewModelProviders.of(this).get(PickContactViewModel.class);
        pickContactViewModel.contactCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        pickContactViewModel.setUncheckableIds(Collections.singletonList(userViewModel.getUserId()));
        pickContactViewModel.setMaxPickCount(maxPickCount);

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
        pickContactViewModel.contactCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }
}
