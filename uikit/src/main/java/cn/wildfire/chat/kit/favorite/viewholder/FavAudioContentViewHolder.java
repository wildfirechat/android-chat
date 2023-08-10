/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import java.io.File;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.SoundMessageContent;

public class FavAudioContentViewHolder extends FavContentViewHolder {
    ImageView audioImageView;
    RelativeLayout contentLayout;
    TextView durationTextView;

    private UiMessage uiMessage;

    public FavAudioContentViewHolder(@NonNull View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        audioImageView = itemView.findViewById(R.id.audioImageView);
        contentLayout = itemView.findViewById(R.id.audioContentLayout);
        durationTextView = itemView.findViewById(R.id.durationTextView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        SoundMessageContent soundMessageContent = (SoundMessageContent) item.toMessage().content;

        durationTextView.setText(soundMessageContent.getDuration() + "''");
        ViewGroup.LayoutParams params = contentLayout.getLayoutParams();
        int increment = UIUtils.getDisplayWidth(fragment.getContext()) / 3 / Config.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND * soundMessageContent.getDuration();
        params.width = UIUtils.dip2Px(65) + increment;
        contentLayout.setLayoutParams(params);
    }

    @Override
    protected void onClick() {
        Message message = favoriteItem.toMessage();
        uiMessage = new UiMessage(message);

        MessageViewModel messageViewModel = ViewModelProviders.of(fragment).get(MessageViewModel.class);
        File file = DownloadManager.mediaMessageContentFile(message);
        if (file == null) {
            return;
        }
        if (file.exists()) {
            messageViewModel.playAudioMessage(uiMessage);
        } else {
            if (uiMessage.isDownloading) {
                return;
            }
            messageViewModel.downloadMedia(uiMessage, file);
        }
    }
}
