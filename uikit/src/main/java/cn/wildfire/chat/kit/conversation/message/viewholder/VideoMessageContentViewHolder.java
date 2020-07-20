package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.widget.BubbleImageView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.message.VideoMessageContent;

@MessageContentType(VideoMessageContent.class)
@EnableContextMenu
public class VideoMessageContentViewHolder extends MediaMessageContentViewHolder {
    @BindView(R2.id.imageView)
    BubbleImageView imageView;
    @BindView(R2.id.playImageView)
    ImageView playImageView;

    public VideoMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        VideoMessageContent fileMessage = (VideoMessageContent) message.message.content;
        if (fileMessage.getThumbnail() != null && fileMessage.getThumbnail().getWidth() > 0) {
            imageView.setImageBitmap(fileMessage.getThumbnail());
            playImageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setImageResource(R.mipmap.img_video_default);
            playImageView.setVisibility(View.GONE);
        }
    }

    @OnClick(R2.id.videoContentLayout)
    void play() {
        previewMM();
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        if (MessageContextMenuItemTags.TAG_FORWARD.equals(tag)) {
            return true;
        } else {
            return super.contextMenuItemFilter(uiMessage, tag);
        }
    }

}
