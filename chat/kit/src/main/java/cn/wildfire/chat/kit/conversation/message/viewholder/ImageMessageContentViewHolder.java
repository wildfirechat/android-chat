package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.GlideRequest;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.ReceiveLayoutRes;
import cn.wildfire.chat.kit.annotation.SendLayoutRes;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.widget.BubbleImageView;
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

    @BindView(R.id.imageView)
    BubbleImageView imageView;

    public ImageMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        ImageMessageContent imageMessage = (ImageMessageContent) message.message.content;
        Bitmap thumbnail = imageMessage.getThumbnail();
        int width = thumbnail != null ? thumbnail.getWidth() : 200;
        int height = thumbnail != null ? thumbnail.getHeight() : 200;
        imageView.getLayoutParams().width = UIUtils.dip2Px(width > 200 ? 200 : width);
        imageView.getLayoutParams().height = UIUtils.dip2Px(height > 200 ? 200 : height);

        if (!TextUtils.isEmpty(imageMessage.localPath)) {
            GlideApp.with(fragment)
                .load(imageMessage.localPath)
                .centerCrop()
                .into(imageView);
        } else {
            GlideRequest<Drawable> request = GlideApp.with(fragment)
                .load(imageMessage.remoteUrl);
            if (thumbnail != null) {
                request = request.placeholder(new BitmapDrawable(fragment.getResources(), imageMessage.getThumbnail()));
            } else {
                request = request.placeholder(R.mipmap.img_error);
            }
            request.centerCrop()
                .into(imageView);
        }
    }

    @OnClick(R.id.imageView)
    void preview() {
        previewMM();
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
                    imageView.setPercent(message.progress);
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

    @Override
    public int sendLayoutResId() {
        return R.layout.conversation_item_image_send;
    }

    @Override
    public int receiveLayoutResId() {
        return R.layout.conversation_item_image_receive;
    }
}
