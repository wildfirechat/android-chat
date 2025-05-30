/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.model.Conversation;

@MessageContentType(CallStartMessageContent.class)
@EnableContextMenu
public class VoipMessageViewHolder extends NormalMessageContentViewHolder {
    TextView textView;

    ImageView callTypeImageView;

    public VoipMessageViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.contentTextView).setOnClickListener(this::call);
    }

    private void bindViews(View itemView) {
        textView = itemView.findViewById(R.id.contentTextView);
        callTypeImageView = itemView.findViewById(R.id.callTypeImageView);
    }

    @Override
    public void onBind(UiMessage message) {
        CallStartMessageContent content = (CallStartMessageContent) message.message.content;
        if (content.getConnectTime() > 0 && content.getEndTime() > 0) {
            String text;
            long duration = (content.getEndTime() - content.getConnectTime()) / 1000;
            if (duration > 3600) {
                text = fragment.getString(R.string.call_duration_hours, duration / 3600, (duration % 3600) / 60, (duration % 60));
            } else {
                text = fragment.getString(R.string.call_duration_minutes, duration / 60, (duration % 60));
            }
            textView.setText(text);
        } else {
            String text = fragment.getString(R.string.call_unknown);
            AVEngineKit.CallEndReason reason = AVEngineKit.CallEndReason.reason(content.getStatus());
            if (reason == AVEngineKit.CallEndReason.UnKnown) {
                text = fragment.getString(content.isAudioOnly() ? R.string.call_audio : R.string.call_video);
            } else if (reason == AVEngineKit.CallEndReason.Busy) {
                text = fragment.getString(R.string.call_busy);
            } else if (reason == AVEngineKit.CallEndReason.SignalError) {
                text = fragment.getString(R.string.call_error);
            } else if (reason == AVEngineKit.CallEndReason.Hangup) {
                text = fragment.getString(R.string.call_hangup);
            } else if (reason == AVEngineKit.CallEndReason.MediaError) {
                text = fragment.getString(R.string.call_error);
            } else if (reason == AVEngineKit.CallEndReason.RemoteHangup) {
                text = fragment.getString(R.string.call_remote_hangup);
            } else if (reason == AVEngineKit.CallEndReason.OpenCameraFailure) {
                text = fragment.getString(R.string.call_error);
            } else if (reason == AVEngineKit.CallEndReason.Timeout) {
                text = fragment.getString(R.string.call_no_answer);
            } else if (reason == AVEngineKit.CallEndReason.AcceptByOtherClient) {
                text = fragment.getString(R.string.call_accept_other);
            } else if (reason == AVEngineKit.CallEndReason.AllLeft) {
                text = fragment.getString(R.string.call_ended);
            } else if (reason == AVEngineKit.CallEndReason.RemoteBusy) {
                text = fragment.getString(R.string.call_remote_hangup);
            } else if (reason == AVEngineKit.CallEndReason.RemoteTimeout) {
                text = fragment.getString(R.string.call_remote_no_answer);
            } else if (reason == AVEngineKit.CallEndReason.RemoteNetworkError) {
                text = fragment.getString(R.string.call_remote_error);
            } else if (reason == AVEngineKit.CallEndReason.RoomDestroyed) {
                text = fragment.getString(R.string.call_ended);
            } else if (reason == AVEngineKit.CallEndReason.RoomNotExist) {
                text = fragment.getString(R.string.call_ended);
            } else if (reason == AVEngineKit.CallEndReason.RoomParticipantsFull) {
                text = fragment.getString(R.string.call_full);
            }
            textView.setText(text);
        }

        if (content.isAudioOnly()) {
            callTypeImageView.setImageResource(R.mipmap.ic_msg_cell_voice_call);
        } else {
            callTypeImageView.setImageResource(R.mipmap.ic_msg_cell_video_call);
        }
    }

    public void call(View view) {
        if (((CallStartMessageContent) message.message.content).getStatus() == 1) {
            return;
        }
        CallStartMessageContent callStartMessageContent = (CallStartMessageContent) message.message.content;
        if (message.message.conversation.type == Conversation.ConversationType.Single) {
            WfcUIKit.singleCall(fragment.getContext(), message.message.conversation.target, callStartMessageContent.isAudioOnly());
        } else {
            fragment.pickGroupMemberToVoipChat(callStartMessageContent.isAudioOnly());
        }
    }
}
