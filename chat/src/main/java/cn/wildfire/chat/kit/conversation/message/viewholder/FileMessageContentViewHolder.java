package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.widget.BubbleImageView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.core.MessageStatus;

@MessageContentType(FileMessageContent.class)
@LayoutRes(resId = R.layout.conversation_item_file_send)
@EnableContextMenu
public class FileMessageContentViewHolder extends MediaMessageContentViewHolder {

    @Bind(R.id.imageView)
    BubbleImageView imageView;

    public FileMessageContentViewHolder(FragmentActivity context, RecyclerView.Adapter adapter, View itemView) {
        super(context, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        super.onBind(message);
        FileMessageContent fileMessageContent = (FileMessageContent) message.message.content;
        if (message.message.status != MessageStatus.Sent) {
            imageView.setPercent(0);
            imageView.setProgressVisible(true);
        } else {
            imageView.setProgressVisible(false);
            imageView.showShadow(false);
        }
        Glide.with(context).load(R.mipmap.ic_file)
                .apply(new RequestOptions().override(UIUtils.dip2Px(150), UIUtils.dip2Px(150)).centerCrop()).into(imageView);
    }

    public void onClick(View view) {
        //FileOpenUtils.openFile(context, fileMessage.localPath);
        Toast.makeText(context, "file message", Toast.LENGTH_SHORT).show();
    }
}
