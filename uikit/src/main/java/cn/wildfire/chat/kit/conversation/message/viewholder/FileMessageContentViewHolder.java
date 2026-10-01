/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.pan.PanSaveHelper;
import cn.wildfire.chat.kit.pan.PanService;
import cn.wildfire.chat.kit.pan.PanServiceProvider;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.FileMessageContent;

import java.util.Arrays;

@MessageContentType(FileMessageContent.class)
@EnableContextMenu
public class FileMessageContentViewHolder extends MediaMessageContentViewHolder {
    ImageView fileIconImageView;
    TextView nameTextView;
    TextView sizeTextView;

    private FileMessageContent fileMessageContent;

    public FileMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
       itemView.findViewById(R.id.fileMessageContentItemView).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        fileIconImageView =itemView.findViewById(R.id.fileIconImageView);
        nameTextView =itemView.findViewById(R.id.fileNameTextView);
        sizeTextView =itemView.findViewById(R.id.fileSizeTextView);
    }

    @Override
    public void onBind(UiMessage message) {
        super.onBind(message);
        fileMessageContent = (FileMessageContent) message.message.content;
        nameTextView.setText(fileMessageContent.getName());
        sizeTextView.setText(FileUtils.getReadableFileSize(fileMessageContent.getSize()));
        fileIconImageView.setImageResource(FileUtils.getFileTypeImageResId(fileMessageContent.getName()));
    }

    public void onClick(View view) {
        if (message.isDownloading) {
            return;
        }
        String ext = fileMessageContent.getName().substring(fileMessageContent.getName().lastIndexOf(".") + 1).toLowerCase();
        if (Arrays.asList(Config.DISABLED_RECEIVE_FILE_TYPES).contains(ext)) {
            Toast.makeText(fragment.getContext(), R.string.open_file_forbidden, Toast.LENGTH_SHORT).show();
            return;
        }
        FileUtils.openFile(fragment.getContext(), message.message);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_SAVE_FILE, confirm = false, priority = 14)
    public void saveFile(View itemView, UiMessage message) {
        File file = DownloadManager.mediaMessageContentFile(message.message);
        if (file == null || !file.exists()) {
            Toast.makeText(fragment.getContext(), R.string.file_need_download, Toast.LENGTH_SHORT).show();
            return;
        }

        File dstFile = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dstFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + file.getName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            dstFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + file.getName());
        } else {
            dstFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + file.getName());
        }

        boolean result = FileUtils.copyFile(file, dstFile);
        if (result) {
            Toast.makeText(fragment.getContext(), R.string.file_save_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(fragment.getContext(), R.string.file_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 在线预览：把消息里的文件按只读链接交给在线文档页（服务端代理取内容，不回写）。
     */
    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_OPEN_ONLINE_DOC, confirm = false, priority = 15)
    public void openOnlineDoc(View itemView, UiMessage message) {
        FileMessageContent content = (FileMessageContent) message.message.content;
        String url = PanService.docViewUrl(content.remoteUrl, content.getName());
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(fragment.getContext(), R.string.pan_operation_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        WfcWebViewActivity.loadUrl(fragment, content.getName(), url);
    }

    /**
     * 存到网盘：副本进「我的个人空间」根目录。
     */
    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_SAVE_TO_PAN, confirm = false, priority = 16)
    public void saveToMyPan(View itemView, UiMessage message) {
        saveMessageToPan(message, false);
    }

    /**
     * 存到网盘并打开。
     */
    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_SAVE_TO_PAN_AND_OPEN, confirm = false, priority = 17)
    public void saveToMyPanAndOpen(View itemView, UiMessage message) {
        saveMessageToPan(message, true);
    }

    private void saveMessageToPan(UiMessage message, boolean openAfterSave) {
        FileMessageContent content = (FileMessageContent) message.message.content;
        PanSaveHelper.saveFileMessageToMyPan(fragment.getContext(), content.getName(), content.getSize(),
            content.remoteUrl, null, null, openAfterSave);
    }

    /**
     * 下载到本地文件目录。
     */
    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_DOWNLOAD, confirm = false, priority = 18)
    public void downloadFile(View itemView, UiMessage message) {
        FileMessageContent content = (FileMessageContent) message.message.content;
        if (TextUtils.isEmpty(content.remoteUrl)) {
            Toast.makeText(fragment.getContext(), R.string.pan_download_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadManager.download(content.remoteUrl, Config.FILE_SAVE_DIR, content.getName(),
            new DownloadManager.SimpleOnDownloadListener() {
                @Override
                public void onUiSuccess(File file) {
                    Toast.makeText(fragment.getContext(), R.string.file_save_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUiFail() {
                    Toast.makeText(fragment.getContext(), R.string.pan_download_failed, Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_SAVE_FILE.equals(tag)) {
            return context.getString(R.string.file_save_to_phone);
        } else if (MessageContextMenuItemTags.TAG_OPEN_ONLINE_DOC.equals(tag)) {
            return context.getString(R.string.doc_online_preview);
        } else if (MessageContextMenuItemTags.TAG_SAVE_TO_PAN.equals(tag)) {
            return context.getString(R.string.pan_save_to_my_pan);
        } else if (MessageContextMenuItemTags.TAG_SAVE_TO_PAN_AND_OPEN.equals(tag)) {
            return context.getString(R.string.pan_save_to_my_pan_and_open);
        } else if (MessageContextMenuItemTags.TAG_DOWNLOAD.equals(tag)) {
            return context.getString(R.string.pan_download);
        }
        return super.contextMenuTitle(context, tag);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        // 返回 true 表示从菜单里移除。网盘未配置时不渲染任何网盘/文档动作。
        boolean panAvailable = PanServiceProvider.getInstance().isAvailable();
        if (MessageContextMenuItemTags.TAG_OPEN_ONLINE_DOC.equals(tag)) {
            if (!panAvailable || !(uiMessage.message.content instanceof FileMessageContent)) {
                return true;
            }
            FileMessageContent content = (FileMessageContent) uiMessage.message.content;
            return TextUtils.isEmpty(content.remoteUrl) || !PanService.isOnlineDocName(content.getName());
        }
        if (MessageContextMenuItemTags.TAG_SAVE_TO_PAN.equals(tag)
            || MessageContextMenuItemTags.TAG_SAVE_TO_PAN_AND_OPEN.equals(tag)) {
            if (!panAvailable || !(uiMessage.message.content instanceof FileMessageContent)) {
                return true;
            }
            return TextUtils.isEmpty(((FileMessageContent) uiMessage.message.content).remoteUrl);
        }
        return super.contextMenuItemFilter(uiMessage, tag);
    }

}
