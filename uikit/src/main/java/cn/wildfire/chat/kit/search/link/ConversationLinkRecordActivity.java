/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.link;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;

public class ConversationLinkRecordActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        setTitle("链接记录");

        Conversation conversation = getIntent().getParcelableExtra("conversation");
        if (conversation == null) {
            finish();
            return;
        }

        Fragment fragment = ConversationLinkRecordFragment.newInstance(conversation);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }
}
