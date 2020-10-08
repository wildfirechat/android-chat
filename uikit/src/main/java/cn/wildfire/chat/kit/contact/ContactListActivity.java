/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class ContactListActivity extends WfcBaseActivity {
    public static String FILTER_USER_LIST = "filterUserList";

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        Intent intent = getIntent();
        ArrayList<String> filterUserList = intent.getStringArrayListExtra(FILTER_USER_LIST);
        ContactListFragment fragment = new ContactListFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("pick", true);
        bundle.putStringArrayList(FILTER_USER_LIST, filterUserList);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
