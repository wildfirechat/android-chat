package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.ProgressBar;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
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
}
