/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.favorite.FavoriteItem;

public class FavFileContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.fileIconImageView)
    ImageView fileIconImageView;
    @BindView(R2.id.fileNameTextView)
    TextView fileNameTextView;
    @BindView(R2.id.fileSizeTextView)
    TextView fileSizeTextView;

    public FavFileContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);

//        fileMessageContent = (FileMessageContent) message.message.content;
//        nameTextView.setText(fileMessageContent.getName());
//        sizeTextView.setText(FileUtils.getReadableFileSize(fileMessageContent.getSize()));
    }
}
