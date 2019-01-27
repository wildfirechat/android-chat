package cn.wildfire.chat.conversation.message.viewholder;

import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.OnClick;
import cn.wildfire.chat.GlideApp;
import cn.wildfire.chat.annotation.EnableContextMenu;
import cn.wildfire.chat.annotation.MessageContentType;
import cn.wildfire.chat.annotation.ReceiveLayoutRes;
import cn.wildfire.chat.annotation.SendLayoutRes;
import cn.wildfire.chat.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.conversation.message.model.UiMessage;
import cn.wildfire.chat.preview.MMPreviewActivity;
import cn.wildfire.chat.third.utils.UIUtils;
import cn.wildfire.chat.widget.BubbleImageView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;

@MessageContentType(ImageMessageContent.class)
@SendLayoutRes(resId = R.layout.conversation_item_image_send)
@ReceiveLayoutRes(resId = R.layout.conversation_item_image_receive)
@EnableContextMenu
public class ImageMessageContentViewHolder extends MediaMessageContentViewHolder {

    @Bind(R.id.imageView)
    BubbleImageView imageView;

    public ImageMessageContentViewHolder(FragmentActivity context, RecyclerView.Adapter adapter, View itemView) {
        super(context, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        ImageMessageContent imageMessage = (ImageMessageContent) message.message.content;
        int width = imageMessage.getThumbnail().getWidth();
        int height = imageMessage.getThumbnail().getHeight();
        imageView.getLayoutParams().width = UIUtils.dip2Px(width > 200 ? 200 : width);
        imageView.getLayoutParams().height = UIUtils.dip2Px(height > 200 ? 200 : height);

        if (!TextUtils.isEmpty(imageMessage.localPath)) {
            GlideApp.with(context)
                    .load(imageMessage.localPath)
                    .centerCrop()
                    .into(imageView);
        } else {
            GlideApp.with(context)
                    .load(imageMessage.remoteUrl)
                    .placeholder(new BitmapDrawable(context.getResources(), imageMessage.getThumbnail()))
                    .centerCrop()
                    .into(imageView);
        }
    }

    @OnClick(R.id.imageView)
    void preview() {
        // FIXME: 2018/10/3
        List<UiMessage> messages = ((ConversationMessageAdapter) adapter).getMessages();
        List<UiMessage> mmMessages = new ArrayList<>();
        for (UiMessage msg : messages) {
            if (msg.message.content.getType() == cn.wildfirechat.message.core.MessageContentType.ContentType_Image
                    || msg.message.content.getType() == cn.wildfirechat.message.core.MessageContentType.ContentType_Video) {
                mmMessages.add(msg);
            }
        }
        if (mmMessages.isEmpty()) {
            return;
        }

        int current = 0;
        for (int i = 0; i < mmMessages.size(); i++) {
            if (message.message.messageId == mmMessages.get(i).message.messageId) {
                current = i;
                break;
            }
        }
        MMPreviewActivity.startActivity(context, mmMessages, current);
    }

    @Override
    protected void setSendStatus(Message item) {
        super.setSendStatus(item);
        MessageContent msgContent = item.content;
        if (msgContent instanceof ImageMessageContent) {
            boolean isSend = item.direction == MessageDirection.Send;
            if (isSend) {
                MessageStatus sentStatus = item.status;
                if (sentStatus == MessageStatus.Sending) {
                    imageView.setProgressVisible(true);
                    imageView.showShadow(true);
                } else if (sentStatus == MessageStatus.Send_Failure) {
                    imageView.setProgressVisible(false);
                    imageView.showShadow(false);
                } else if (sentStatus == MessageStatus.Sent) {
                    imageView.setProgressVisible(false);
                    imageView.showShadow(false);
                }
            } else {
                imageView.setProgressVisible(false);
                imageView.showShadow(false);
            }
        }
    }
}
