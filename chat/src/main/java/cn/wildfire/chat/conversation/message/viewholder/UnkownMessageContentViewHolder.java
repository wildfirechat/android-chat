package cn.wildfire.chat.conversation.message.viewholder;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import cn.wildfirechat.chat.R;

import butterknife.Bind;
import cn.wildfire.chat.annotation.EnableContextMenu;
import cn.wildfire.chat.annotation.MessageContentType;
import cn.wildfire.chat.annotation.ReceiveLayoutRes;
import cn.wildfire.chat.annotation.SendLayoutRes;
import cn.wildfire.chat.conversation.message.model.UiMessage;
import cn.wildfirechat.message.notification.NotificationMessageContent;

@MessageContentType(NotificationMessageContent.class)
@SendLayoutRes(resId = R.layout.conversation_item_unknown_send)
@ReceiveLayoutRes(resId = R.layout.conversation_item_unknown_receive)
@EnableContextMenu
public class UnkownMessageContentViewHolder extends NormalMessageContentViewHolder {
    @Bind(R.id.contentTextView)
    TextView contentTextView;

    public UnkownMessageContentViewHolder(FragmentActivity context, RecyclerView.Adapter adapter, View itemView) {
        super(context, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        contentTextView.setText("unknown");
    }
}
