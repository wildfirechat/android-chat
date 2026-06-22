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
import android.widget.Toast;

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
import cn.wildfire.chat.kit.audio.AudioPlayModeUtils;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleEventSourceListener;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.ToastUtils;
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
    LinearLayout speechToTextLayout;
    TextView speechToTextTextView;
    ProgressBar speechToTextProgressBar;
    private StringBuilder speechToTextSB;
    private boolean speechToTextInProgress = false;

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
        speechToTextLayout = itemView.findViewById(R.id.speechToTextLinearLayout);
        speechToTextTextView = itemView.findViewById(R.id.speechToTextTextView);
        speechToTextProgressBar = itemView.findViewById(R.id.speechToTextProgressBar);
    }

    @Override
    public void onBind(UiMessage message) {
        super.onBind(message);
        SoundMessageContent voiceMessage = (SoundMessageContent) message.message.content;
        // 整体允许站到屏幕宽度的 55/100
        int increment = (UIUtils.getDisplayWidth(fragment.getContext()) * 55 / 100 - UIUtils.dip2Px(65)) / Config.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND * voiceMessage.getDuration();

        durationTextView.setText(voiceMessage.getDuration() + "''");
        speechToTextLayout.setVisibility(View.GONE);
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
            speechToTextLayout.setVisibility(View.VISIBLE);
            speechToTextTextView.setText(message.audioMessageSpeechToText);
        }
    }

    @Override
    public void onViewRecycled() {
        // TODO 可实现语音是否持续播放、中断登录逻辑
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_SPEECH_TO_TEXT, confirm = false, priority = 13)
    public void speechToText(View itemView, UiMessage message) {
        if (!TextUtils.isEmpty(message.audioMessageSpeechToText)) {
            speechToTextLayout.setVisibility(View.VISIBLE);
            speechToTextTextView.setText(message.audioMessageSpeechToText);
            return;
        }
        Map<String, Object> object = new HashMap<>();
        speechToTextLayout.setVisibility(View.VISIBLE);
        speechToTextProgressBar.setVisibility(View.VISIBLE);
        speechToTextTextView.setText("");
        SoundMessageContent voiceMessage = (SoundMessageContent) message.message.content;
        final String currentAudioUrl = voiceMessage.remoteUrl;

        object.put("url", currentAudioUrl);
        object.put("noReuse", true);
        object.put("noLlm", false);
        this.speechToTextSB = new StringBuilder();
        this.speechToTextInProgress = true;

        fragment.scrollToKeepItemVisible(getAdapterPosition());

        if (message.message.direction == MessageDirection.Receive) {
            ChatManager.Instance().setMediaMessagePlayed(message.message.messageId);
            message.message.status = MessageStatus.Played;
            playStatusIndicator.setVisibility(View.GONE);
        }

        OKHttpHelper.sse(Config.ASR_SERVER_URL, object, new SimpleEventSourceListener() {

            @Override
            public void onUiEvent(@NonNull EventSource eventSource, @Nullable String id, @Nullable String type, @NonNull String data) {
                speechToTextProgressBar.setVisibility(View.GONE);
                FragmentActivity activity = fragment.getActivity();
                if (activity == null || activity.isFinishing()) {
                    eventSource.cancel();
                    return;
                }
                if (TextUtils.equals(currentAudioUrl, ((SoundMessageContent) message.message.content).remoteUrl)) {
                    if (TextUtils.isEmpty(data)) {
                        return;
                    }
                    char firstChar = data.charAt(0);
                    String curText = speechToTextSB.toString();
                    if (!curText.isEmpty()
                        && ((firstChar >= 'a' && firstChar <= 'z') || (firstChar >= 'A' && firstChar <= 'Z'))) {
                        speechToTextSB.append(" ");
                        speechToTextSB.append(data);

                        char lastChar = curText.charAt(curText.length() - 1);
                        if (!((lastChar >= 'a' && lastChar <= 'z') || (lastChar >= 'A' && lastChar <= 'Z'))) {
                            speechToTextSB.append(" ");
                        }
                    } else {
                        speechToTextSB.append(data);
                    }
                    String text = speechToTextSB.toString();
                    speechToTextTextView.setText(text);
                    message.audioMessageSpeechToText = text;
                    itemView.post(() -> fragment.scrollToKeepItemVisible(getAdapterPosition()));
                }
            }

            @Override
            public void onUiFailure(@NonNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                speechToTextLayout.setVisibility(View.GONE);
                speechToTextInProgress = false;
                speechToTextSB = null;
            }

            @Override
            public void onUiClosed(@NonNull EventSource eventSource) {
                speechToTextProgressBar.setVisibility(View.GONE);
                speechToTextInProgress = false;
            }
        });
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CANCEL_SPEECH_TO_TEXT, confirm = false, priority = 13)
    public void cancelSpeechToText(View itemView, UiMessage message) {
        message.audioMessageSpeechToText = null;
        speechToTextLayout.setVisibility(View.GONE);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_AUDIO_PLAY_MODE, confirm = false, priority = 14)
    public void switchAudioPlayMode(View itemView, UiMessage message) {
        Context context = fragment.getContext();
        boolean earpiece = AudioPlayModeUtils.isEarpieceMode(context);
        AudioPlayModeUtils.setEarpieceMode(context, !earpiece);
        // 切换播放方式后停止当前播放，下次播放即按新方式生效
        messageViewModel.stopPlayAudio();
        Toast.makeText(context, !earpiece ? R.string.audio_play_mode_earpiece_toast : R.string.audio_play_mode_speaker_toast, Toast.LENGTH_SHORT).show();
        fragment.onAudioPlayModeChanged();
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_SPEECH_TO_TEXT.equals(tag)) {
            //return context.getString(R.string.file_save_to_phone);
            return context.getString(R.string.speech_to_text);
        } else if (MessageContextMenuItemTags.TAG_CANCEL_SPEECH_TO_TEXT.equals(tag)) {
            //return context.getString(R.string.file_save_to_phone);
            return context.getString(R.string.cancel_speech_to_text);
        } else if (MessageContextMenuItemTags.TAG_AUDIO_PLAY_MODE.equals(tag)) {
            // 菜单项显示的是“切换到”的目标方式：当前听筒则提供“扬声器播放”，反之亦然
            return AudioPlayModeUtils.isEarpieceMode(context)
                ? context.getString(R.string.audio_play_mode_speaker)
                : context.getString(R.string.audio_play_mode_earpiece);
        }
        return super.contextMenuTitle(context, tag);
    }

    @Override
    public int contextMenuIcon(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_AUDIO_PLAY_MODE.equals(tag)) {
            return AudioPlayModeUtils.isEarpieceMode(context)
                ? R.drawable.ic_msg_speaker
                : R.drawable.ic_msg_earpiece;
        }
        return super.contextMenuIcon(context, tag);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        if (TextUtils.equals(tag, MessageContextMenuItemTags.TAG_SPEECH_TO_TEXT)) {
            if (speechToTextInProgress) {
                return true;
            } else {
                return TextUtils.isEmpty(Config.ASR_SERVER_URL) || !TextUtils.isEmpty(message.audioMessageSpeechToText);
            }
        } else if (TextUtils.equals(tag, MessageContextMenuItemTags.TAG_CANCEL_SPEECH_TO_TEXT)) {
            return TextUtils.isEmpty(message.audioMessageSpeechToText);
        }
        return super.contextMenuItemFilter(uiMessage, tag);
    }

    public void onClick(View view) {
        File file = DownloadManager.mediaMessageContentFile(message.message);
        if (file == null) {
            return;
        }
        if (file.exists()) {
            // 听筒播放模式下，开始播放（而非停止当前播放）时展示听筒播放提示；已连接耳机时无需提示
            Context context = fragment.getContext();
            if (!message.isPlaying
                && AudioPlayModeUtils.isEarpieceMode(context)
                && !AudioPlayModeUtils.isHeadsetOn(context)) {
                ToastUtils.showTipToast(context, R.drawable.ic_conversation_earpiece, fragment.getString(R.string.audio_earpiece_playing_hint));
            }
            messageViewModel.playAudioMessage(message);
        } else {
            if (message.isDownloading) {
                return;
            }
            messageViewModel.downloadMedia(message, file);
        }
    }

}
