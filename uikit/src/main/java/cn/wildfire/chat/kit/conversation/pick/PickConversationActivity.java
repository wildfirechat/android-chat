/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.app.Activity;
import android.content.Intent;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfire.chat.kit.conversationlist.OnClickConversationItemListener;
import cn.wildfirechat.model.ConversationInfo;

public class PickConversationActivity extends WfcBaseActivity implements OnClickConversationItemListener {
    @Override
    protected void afterViews() {
        ConversationListFragment conversationListFragment = new ConversationListFragment();
        conversationListFragment.setOnClickConversationItemListener(this);
        getSupportFragmentManager()
            .beginTransaction()
            .add(R.id.containerFrameLayout, conversationListFragment)
            .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    public void onClickConversationItem(ConversationInfo conversationInfo) {
        Intent intent = new Intent();
        intent.putExtra("conversationInfo", conversationInfo);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
