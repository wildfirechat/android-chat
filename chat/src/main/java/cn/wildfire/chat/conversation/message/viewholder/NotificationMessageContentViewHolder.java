package cn.wildfire.chat.conversation.message.viewholder;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public abstract class NotificationMessageContentViewHolder extends MessageContentViewHolder {
    public NotificationMessageContentViewHolder(FragmentActivity activity, RecyclerView.Adapter adapter, View itemView) {
        super(activity, adapter, itemView);
    }
}
