package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.ProgressBar;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.mm.MediaEntry;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;

public class MediaMessageContentViewHolder extends NormalMessageContentViewHolder {
    @Bind(R.id.progressBar)
    ProgressBar progressBar;

    public MediaMessageContentViewHolder(FragmentActivity activity, RecyclerView.Adapter adapter, View itemView) {
        super(activity, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        if (message.message.direction == MessageDirection.Receive) {
            if (message.isDownloading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        } else {
            // todo
        }
    }

    @Override
    protected void setSendStatus(Message item) {
        super.setSendStatus(item);
        MessageContent msgContent = item.content;
        if (msgContent instanceof MediaMessageContent) {
            //只需要设置自己发送的状态
            MessageStatus sentStatus = item.status;
            if (sentStatus == MessageStatus.Sending) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (sentStatus == MessageStatus.Send_Failure) {
                progressBar.setVisibility(View.GONE);
            } else if (sentStatus == MessageStatus.Sent) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    protected void previewMM() {
        List<UiMessage> messages = ((ConversationMessageAdapter) adapter).getMessages();
        List<MediaEntry> entries = new ArrayList<>();
        UiMessage msg;

        int current = 0;
        int index = 0;
        for (int i = 0; i < messages.size(); i++) {
            msg = messages.get(i);
            if (msg.message.content.getType() != MessageContentType.ContentType_Image
                    && msg.message.content.getType() != MessageContentType.ContentType_Video) {
                continue;
            }
            MediaEntry entry = new MediaEntry();
            if (msg.message.content.getType() == cn.wildfirechat.message.core.MessageContentType.ContentType_Image) {
                entry.setType(MediaEntry.TYPE_IMAGE);
                entry.setThumbnail(((ImageMessageContent) msg.message.content).getThumbnail());

            } else {
                entry.setType(MediaEntry.TYPE_VIDEO);
                entry.setThumbnail(((VideoMessageContent) msg.message.content).getThumbnail());
            }
            entry.setMediaUrl(((MediaMessageContent) msg.message.content).remoteUrl);
            entry.setMediaLocalPath(((MediaMessageContent) msg.message.content).localPath);
            entries.add(entry);

            if (message.message.messageId == msg.message.messageId) {
                current = index;
            }
            index++;
        }
        if (entries.isEmpty()) {
            return;
        }
        MMPreviewActivity.startActivity(context, entries, current);
    }
}
