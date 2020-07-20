package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.message.Message;

public abstract class MessageContentViewHolder extends RecyclerView.ViewHolder {
    @NonNull
    protected ConversationFragment fragment;
    protected View itemView;
    protected UiMessage message;
    protected int position;
    protected RecyclerView.Adapter adapter;
    protected MessageViewModel messageViewModel;

    @BindView(R2.id.timeTextView)
    TextView timeTextView;


    public MessageContentViewHolder(@NonNull ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.itemView = itemView;
        this.adapter = adapter;
        messageViewModel = ViewModelProviders.of(fragment).get(MessageViewModel.class);
        ButterKnife.bind(this, itemView);
    }

    public void onBind(UiMessage message, int position) {
        setMessageTime(message.message, position);
    }

    /**
     * @param uiMessage
     * @param tag
     * @return 返回true，将从context menu中排除
     */

    public abstract boolean contextMenuItemFilter(UiMessage uiMessage, String tag);

    public void onViewRecycled() {
        // you can do some clean up here
    }

    protected void setMessageTime(Message item, int position) {
        long msgTime = item.serverTime;
        if (position > 0) {
            Message preMsg = ((ConversationMessageAdapter) adapter).getItem(position - 1).message;
            long preMsgTime = preMsg.serverTime;
            if (msgTime - preMsgTime > (5 * 60 * 1000)) {
                timeTextView.setVisibility(View.VISIBLE);
                timeTextView.setText(TimeUtils.getMsgFormatTime(msgTime));
            } else {
                timeTextView.setVisibility(View.GONE);
            }
        } else {
            timeTextView.setVisibility(View.VISIBLE);
            timeTextView.setText(TimeUtils.getMsgFormatTime(msgTime));
        }
    }

}
