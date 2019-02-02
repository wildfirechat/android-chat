package cn.wildfire.chat.kit.user;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

public class UserInfoActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        UserInfo userInfo = getIntent().getParcelableExtra("userInfo");
        if (userInfo == null) {
            finish();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerFrameLayout, UserInfoFragment.newInstance(userInfo))
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
