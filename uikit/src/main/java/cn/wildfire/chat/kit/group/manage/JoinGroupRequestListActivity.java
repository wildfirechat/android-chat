package cn.wildfire.chat.kit.group.manage;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.R;

public class JoinGroupRequestListActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        String groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, JoinGroupRequestListFragment.newInstance(groupId))
                .commit();
    }
}
