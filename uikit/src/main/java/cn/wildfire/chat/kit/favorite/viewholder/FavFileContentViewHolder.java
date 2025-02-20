/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.FileMessageContent;

public class FavFileContentViewHolder extends FavContentViewHolder {
    ImageView fileIconImageView;
    TextView fileNameTextView;
    TextView fileSizeTextView;

    public FavFileContentViewHolder(@NonNull View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        fileIconImageView = itemView.findViewById(R.id.fileIconImageView);
        fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
        fileSizeTextView = itemView.findViewById(R.id.fileSizeTextView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        FileMessageContent fileMessageContent = (FileMessageContent) item.toMessage().content;

        fileNameTextView.setText(fileMessageContent.getName());
        fileSizeTextView.setText(FileUtils.getReadableFileSize(fileMessageContent.getSize()));
        fileIconImageView.setImageResource(FileUtils.getFileTypeImageResId(fileMessageContent.getName()));
    }

    @Override
    protected void onClick() {
        Toast.makeText(fragment.getContext(), fragment.getString(R.string.preview_not_supported), Toast.LENGTH_SHORT).show();
    }
}
