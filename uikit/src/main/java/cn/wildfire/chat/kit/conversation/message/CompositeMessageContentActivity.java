/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.Message;

public class CompositeMessageContentActivity extends WfcBaseActivity {
    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;

    @Override
    protected int contentLayout() {
        return R.layout.composite_message_activity;
    }

    @Override
    protected void afterViews() {
        Message message = getIntent().getParcelableExtra("message");
        if (message == null || !(message.content instanceof CompositeMessageContent)) {
            finish();
            return;
        }
        CompositeMessageContent content = (CompositeMessageContent) message.content;
        setTitle(content.getTitle());
        CompositeMessageContentAdapter adapter = new CompositeMessageContentAdapter(message);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
}
