package cn.wildfire.chat.kit.settings.blacklist;

import android.content.Intent;
import android.view.MenuItem;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

public class BlacklistListActivity extends WfcBaseActivity {


    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, new BlacklistListFragment())
                .commit();
    }
}
