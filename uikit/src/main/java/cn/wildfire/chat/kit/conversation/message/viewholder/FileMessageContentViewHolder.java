/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.FileMessageContent;

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

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_SAVE_FILE.equals(tag)) {
            return context.getString(R.string.file_save_to_phone);
        }
        return super.contextMenuTitle(context, tag);
    }

}
