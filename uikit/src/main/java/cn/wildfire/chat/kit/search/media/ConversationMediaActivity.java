/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.media;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;

public class ConversationMediaActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        setTitle("图片与视频");

        Conversation conversation = getIntent().getParcelableExtra("conversation");
        if (conversation == null) {
            finish();
            return;
        }

        Fragment fragment = ConversationMediaFragment.newInstance(conversation);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }
}
