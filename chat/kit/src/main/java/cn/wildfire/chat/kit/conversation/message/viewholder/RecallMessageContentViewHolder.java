package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.RecallMessageContent;

@MessageContentType(RecallMessageContent.class)
@LayoutRes(resId = R.layout.conversation_item_recall_notification)
@EnableContextMenu
public class RecallMessageContentViewHolder extends NotificationMessageContentViewHolder {
    @BindView(R.id.notificationTextView)
    TextView notificationTextView;
    @BindView(R.id.reeditTextView)
    TextView reeditTextView;

    private RecallMessageContent content;

    public RecallMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        content = (RecallMessageContent) message.message.content;
        notificationTextView.setText(message.message.digest());
        if (content.getOriginalContentType() == cn.wildfirechat.message.core.MessageContentType.ContentType_Text && ((NotificationMessageContent) message.message.content).fromSelf) {
            reeditTextView.setVisibility(View.VISIBLE);
        } else {
            reeditTextView.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.reeditTextView)
    public void onClick(View view) {
        fragment.setInputText(content.getOriginalSearchableContent());
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String itemTitle) {
        return false;
    }
}
