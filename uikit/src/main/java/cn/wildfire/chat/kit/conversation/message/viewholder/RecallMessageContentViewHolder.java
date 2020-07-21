package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.RecallMessageContent;

@MessageContentType(RecallMessageContent.class)
@EnableContextMenu
public class RecallMessageContentViewHolder extends NotificationMessageContentViewHolder {
    @BindView(R2.id.notificationTextView)
    TextView notificationTextView;
    @BindView(R2.id.reeditTextView)
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

    @OnClick(R2.id.reeditTextView)
    public void onClick(View view) {
        fragment.setInputText(content.getOriginalSearchableContent());
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String itemTitle) {
        return false;
    }

}
