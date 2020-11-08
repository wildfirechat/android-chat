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

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.SoundMessageContent;

public class FavAudioContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.audioImageView)
    ImageView audioImageView;
    @BindView(R2.id.audioContentLayout)
    RelativeLayout contentLayout;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;

    private UiMessage uiMessage;

    public FavAudioContentViewHolder(@NonNull View itemView) {
        super(itemView);
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

    @OnClick(R2.id.audioContentLayout)
    void playAudio(){
        Message message = favoriteItem.toMessage();
        uiMessage =new UiMessage(message);

        MessageViewModel messageViewModel = ViewModelProviders.of(fragment).get(MessageViewModel.class);
        File file = messageViewModel.mediaMessageContentFile(message);
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
