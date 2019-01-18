package cn.wildfire.chat.conversation.message.viewholder;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import cn.wildfirechat.chat.R;

import butterknife.Bind;
import cn.wildfire.chat.annotation.LayoutRes;
import cn.wildfire.chat.annotation.MessageContentType;
import cn.wildfire.chat.conversation.message.model.UiMessage;
import cn.wildfirechat.message.RecallMessageContent;

@MessageContentType(RecallMessageContent.class)
@LayoutRes(resId = R.layout.conversation_item_notification)
public class RecallMessageContentViewHolderSimple extends SimpleNotificationMessageContentViewHolder {
    @Bind(R.id.notificationTextView)
    TextView notificationTextView;

    public RecallMessageContentViewHolderSimple(FragmentActivity activity, RecyclerView.Adapter adapter, View itemView) {
        super(activity, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        RecallMessageContent content = (RecallMessageContent) message.message.content;
        notificationTextView.setText(content.digest());
    }
}
