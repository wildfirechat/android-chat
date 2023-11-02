/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class CompositeMessageContentActivity extends WfcBaseActivity implements CompositeMessageContentAdapter.OnMessageClickListener {
    RecyclerView recyclerView;
    private CompositeMessageContentAdapter adapter;

    protected void bindViews() {
        super.bindViews();
        recyclerView = findViewById(R.id.recyclerView);
    }

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
        if (!((CompositeMessageContent) message.content).isLoaded()) {
            File file = DownloadManager.mediaMessageContentFile(message);
            if (!TextUtils.isEmpty(content.remoteUrl) && !file.exists()) {
                String fileUrl = content.remoteUrl;
                if (message.conversation.type == Conversation.ConversationType.SecretChat) {
                    fileUrl = DownloadManager.buildSecretChatMediaUrl(message);
                }
                Toast.makeText(this, "消息加载中，请稍后", Toast.LENGTH_SHORT).show();
                DownloadManager.download(fileUrl, Config.FILE_SAVE_DIR, new DownloadManager.OnDownloadListener() {
                    @Override
                    public void onSuccess(File file) {
                        content.localPath = file.getAbsolutePath();
                        ChatManager.Instance().updateMessage(message.messageId, content);
                        UIUtils.postTaskSafely(() -> {
                            adapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onProgress(int progress) {

                    }

                    @Override
                    public void onFail() {
                        UIUtils.postTaskSafely(() -> {
                            Toast.makeText(CompositeMessageContentActivity.this, "加载消息失败", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        }
        adapter = new CompositeMessageContentAdapter(message, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClickMessage(Message message) {
        MessageContent content = message.content;
        if (content instanceof FileMessageContent) {
            FileUtils.openFile(this, message);
        } else if (content instanceof VideoMessageContent) {
            MMPreviewActivity.previewVideo(this, message);
        } else if (content instanceof ImageMessageContent) {
            MMPreviewActivity.previewImage(this, message);
        } else if (content instanceof CompositeMessageContent) {
            Intent intent = new Intent(this, CompositeMessageContentActivity.class);
            intent.putExtra("message", message);
            startActivity(intent);
        }
    }
}
