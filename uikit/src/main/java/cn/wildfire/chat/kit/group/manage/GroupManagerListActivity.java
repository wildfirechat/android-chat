/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.group.BasePickGroupMemberActivity;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;

public class GroupManagerListActivity extends WfcBaseActivity {

    @Override
    protected void afterViews() {
        GroupInfo groupInfo = getIntent().getParcelableExtra("groupInfo");
        int line = getIntent().getIntExtra(BasePickGroupMemberActivity.LINE, Conversation.LINE_DEFAULT);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, GroupManagerListFragment.newInstance(groupInfo, line))
                .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }
}
