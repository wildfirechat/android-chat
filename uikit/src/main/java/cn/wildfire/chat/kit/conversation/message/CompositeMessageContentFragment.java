/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
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

/**
 * 合并转发消息的详情页（「群聊的聊天记录」点开之后那一屏）。
 * <p>
 * 手机端装在 {@link CompositeMessageContentActivity} 这个空壳里，平板上同一份实现进右栏。
 * 合并消息可以层层嵌套，所以本页里再点一条合并消息会再压一层同样的页面。
 */
public class CompositeMessageContentFragment extends Fragment
    implements WfcPage, CompositeMessageContentAdapter.OnMessageClickListener {

    private RecyclerView recyclerView;
    private CompositeMessageContentAdapter adapter;
    private Message message;

    /**
     * 只有内容确实是合并消息才是一个能显示的页面，否则返回 null 让调用方放弃。
     */
    @Nullable
    public static CompositeMessageContentFragment fromIntent(@Nullable Intent intent) {
        Message message = intent == null ? null : intent.getParcelableExtra("message");
        if (message == null || !(message.content instanceof CompositeMessageContent)) {
            return null;
        }
        CompositeMessageContentFragment fragment = new CompositeMessageContentFragment();
        Bundle args = new Bundle();
        args.putParcelable("message", message);
        fragment.setArguments(args);
        return fragment;
    }

    private Message message() {
        if (message == null && getArguments() != null) {
            message = getArguments().getParcelable("message");
        }
        return message;
    }

    /**
     * 标题是合并消息自带的（「某某和某某的聊天记录」），不是 manifest 里的固定 label。
     */
    @Nullable
    @Override
    public CharSequence pageTitle() {
        Message msg = message();
        if (msg == null || !(msg.content instanceof CompositeMessageContent)) {
            return null;
        }
        return ((CompositeMessageContent) msg.content).getTitle();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.composite_message_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);

        Message msg = message();
        if (msg == null || !(msg.content instanceof CompositeMessageContent)) {
            WfcPageCompat.finishPage(this);
            return;
        }
        CompositeMessageContent content = (CompositeMessageContent) msg.content;
        if (!content.isLoaded()) {
            downloadContent(msg, content);
        }

        adapter = new CompositeMessageContentAdapter(msg, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * 合并消息的正文是一个远端文件，本地没有时先下下来再刷新列表。
     */
    private void downloadContent(Message msg, CompositeMessageContent content) {
        File file = DownloadManager.mediaMessageContentFile(msg);
        if (TextUtils.isEmpty(content.remoteUrl) || file.exists()) {
            return;
        }
        String fileUrl = content.remoteUrl;
        if (msg.conversation.type == Conversation.ConversationType.SecretChat) {
            fileUrl = DownloadManager.buildSecretChatMediaUrl(msg);
        }
        Toast.makeText(getActivity(), R.string.message_loading, Toast.LENGTH_SHORT).show();
        DownloadManager.download(fileUrl, Config.FILE_SAVE_DIR, new DownloadManager.OnDownloadListener() {
            @Override
            public void onSuccess(File file) {
                content.localPath = file.getAbsolutePath();
                ChatManager.Instance().updateMessage(msg.messageId, content);
                UIUtils.postTaskSafely(() -> {
                    // 下载是后台线程，回来时页面可能已经关掉了
                    if (isAdded() && adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onProgress(int progress) {

            }

            @Override
            public void onFail() {
                UIUtils.postTaskSafely(() -> {
                    if (isAdded()) {
                        Toast.makeText(getActivity(), R.string.message_load_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onClickMessage(Message message) {
        MessageContent content = message.content;
        if (content instanceof FileMessageContent) {
            FileUtils.openFile(getActivity(), message);
        } else if (content instanceof VideoMessageContent) {
            MMPreviewActivity.previewVideo(getActivity(), message);
        } else if (content instanceof ImageMessageContent) {
            MMPreviewActivity.previewImage(getActivity(), message);
        } else if (content instanceof CompositeMessageContent) {
            // 合并消息里再套合并消息，再压一层
            Intent intent = new Intent(getActivity(), CompositeMessageContentActivity.class);
            intent.putExtra("message", message);
            WfcPageCompat.startPage(this, intent);
        }
    }
}
