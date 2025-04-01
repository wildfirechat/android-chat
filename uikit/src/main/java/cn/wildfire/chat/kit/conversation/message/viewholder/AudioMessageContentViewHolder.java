/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleEventSourceListener;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfirechat.message.PTTSoundMessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.remote.ChatManager;
import okhttp3.Response;
import okhttp3.sse.EventSource;

@MessageContentType(value = {SoundMessageContent.class, PTTSoundMessageContent.class})

@EnableContextMenu
public class AudioMessageContentViewHolder extends MediaMessageContentViewHolder {
    ImageView ivAudio;
    TextView durationTextView;
    RelativeLayout contentLayout;
    @Nullable
    View playStatusIndicator;
    LinearLayout translateLayout;
    TextView translateTextView;
    ProgressBar translateProgressBar;
    private StringBuilder speechToTextSB;

    public AudioMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.audioContentLayout).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        ivAudio = itemView.findViewById(R.id.audioImageView);
        durationTextView = itemView.findViewById(R.id.durationTextView);
        contentLayout = itemView.findViewById(R.id.audioContentLayout);
        playStatusIndicator = itemView.findViewById(R.id.playStatusIndicator);
        translateLayout = itemView.findViewById(R.id.translationLinearLayout);
        translateTextView = itemView.findViewById(R.id.translationTextView);
        translateProgressBar = itemView.findViewById(R.id.translationProgressBar);
    }

    @Override
    public void onBind(UiMessage message) {
        super.onBind(message);
        SoundMessageContent voiceMessage = (SoundMessageContent) message.message.content;
        int increment = UIUtils.getDisplayWidth(fragment.getContext()) / 3 / Config.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND * voiceMessage.getDuration();

        durationTextView.setText(voiceMessage.getDuration() + "''");
        translateLayout.setVisibility(View.GONE);
        speechToTextSB = null;
        ViewGroup.LayoutParams params = contentLayout.getLayoutParams();
        params.width = UIUtils.dip2Px(65) + increment;
        contentLayout.setLayoutParams(params);
        if (message.message.direction == MessageDirection.Receive) {
            if (message.message.status != MessageStatus.Played) {
                playStatusIndicator.setVisibility(View.VISIBLE);
            } else {
                playStatusIndicator.setVisibility(View.GONE);
            }
        }

        AnimationDrawable animation;
        if (message.isPlaying) {
            animation = (AnimationDrawable) ivAudio.getBackground();
            if (!animation.isRunning()) {
                animation.start();
            }
        } else {
            // TODO 不知道怎么回事，动画开始了，就停不下来, 所以采用这种方式
            ivAudio.setBackground(null);
            if (message.message.direction == MessageDirection.Send) {
                ivAudio.setBackgroundResource(R.drawable.audio_animation_right_list);
            } else {
                ivAudio.setBackgroundResource(R.drawable.audio_animation_left_list);
            }
        }

        if (!TextUtils.isEmpty(message.audioMessageSpeechToText)) {
            translateLayout.setVisibility(View.VISIBLE);
            translateTextView.setText(message.audioMessageSpeechToText);
        }
    }

    @Override
    public void onViewRecycled() {
        // TODO 可实现语音是否持续播放、中断登录逻辑
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_TRANSLATE, confirm = false, priority = 13)
    public void translate(View itemView, UiMessage message) {
        Map<String, Object> object = new HashMap<>();
        translateLayout.setVisibility(View.VISIBLE);
        translateProgressBar.setVisibility(View.VISIBLE);
        SoundMessageContent voiceMessage = (SoundMessageContent) message.message.content;
        final String currentAudioUrl = voiceMessage.remoteUrl;

        object.put("url", currentAudioUrl);
        object.put("noReuse", true);
        object.put("noLlm", false);
        this.speechToTextSB = new StringBuilder();

        fragment.scrollToKeepItemVisible(getAdapterPosition());

        ChatManager.Instance().setMediaMessagePlayed(message.message.messageId);
        message.message.status = MessageStatus.Played;
        playStatusIndicator.setVisibility(View.GONE);

        OKHttpHelper.sse(Config.ASR_SERVER_URL, object, new SimpleEventSourceListener() {

            @Override
            public void onUiEvent(@NonNull EventSource eventSource, @Nullable String id, @Nullable String type, @NonNull String data) {
                translateProgressBar.setVisibility(View.GONE);
                FragmentActivity activity = fragment.getActivity();
                if (activity == null || activity.isFinishing()) {
                    eventSource.cancel();
                    return;
                }
                if (TextUtils.equals(currentAudioUrl, ((SoundMessageContent) message.message.content).remoteUrl)) {
                    speechToTextSB.append(data.replaceAll("\n", ""));
                    String text = speechToTextSB.toString();
                    translateTextView.setText(text);
                    message.audioMessageSpeechToText = text;
                    fragment.scrollToKeepItemVisible(getAdapterPosition());
                }
            }

            @Override
            public void onUiFailure(@NonNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                translateLayout.setVisibility(View.GONE);
                speechToTextSB = null;
            }

            @Override
            public void onUiClosed(@NonNull EventSource eventSource) {
                translateProgressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_TRANSLATE.equals(tag)) {
            //return context.getString(R.string.file_save_to_phone);
            return context.getString(R.string.speech_to_text);
        }
        return super.contextMenuTitle(context, tag);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        if (TextUtils.equals(tag, MessageContextMenuItemTags.TAG_TRANSLATE)) {
            return TextUtils.isEmpty(Config.ASR_SERVER_URL) || !TextUtils.isEmpty(message.audioMessageSpeechToText);
        }
        return super.contextMenuItemFilter(uiMessage, tag);
    }

    public void onClick(View view) {
        File file = DownloadManager.mediaMessageContentFile(message.message);
        if (file == null) {
            return;
        }
        if (file.exists()) {
            messageViewModel.playAudioMessage(message);
        } else {
            if (message.isDownloading) {
                return;
            }
            messageViewModel.downloadMedia(message, file);
        }
    }

}
