package cn.wildfire.chat.kit.contact;

import android.os.Bundle;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class ContactListActivity extends WfcBaseActivity {
    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        ContactListFragment fragment = new ContactListFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("pick", true);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
