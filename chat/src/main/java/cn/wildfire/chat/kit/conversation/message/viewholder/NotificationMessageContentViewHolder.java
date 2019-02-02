package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

public abstract class NotificationMessageContentViewHolder extends MessageContentViewHolder {
    public NotificationMessageContentViewHolder(FragmentActivity activity, RecyclerView.Adapter adapter, View itemView) {
        super(activity, adapter, itemView);
    }
}
