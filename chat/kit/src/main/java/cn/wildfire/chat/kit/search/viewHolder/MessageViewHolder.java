package cn.wildfire.chat.kit.search.viewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MessageViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.portraitImageView)
    protected ImageView portraitImageView;
    @BindView(R.id.nameTextView)
    protected TextView nameTextView;
    @BindView(R.id.contentTextView)
    protected TextView contentTextView;
    @BindView(R.id.timeTextView)
    protected TextView timeTextView;

    private Fragment fragment;
    private UserViewModel userViewModel;

    public MessageViewHolder(Fragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.userViewModel = ViewModelProviders.of(fragment).get(UserViewModel.class);
        ButterKnife.bind(this, itemView);
    }

    public void onBind(Message message) {
        UserInfo sender = userViewModel.getUserInfo(message.sender, false);
        if (sender != null) {
            String senderName;
            if (message.conversation.type == Conversation.ConversationType.Group) {
                senderName = ChatManager.Instance().getGroupMemberDisplayName(message.conversation.target, sender.uid);
            } else {
                senderName = ChatManager.Instance().getUserDisplayName(sender);
            }
            nameTextView.setText(senderName);
            GlideApp.with(portraitImageView).load(sender.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        }
        if (message.content instanceof NotificationMessageContent) {
            contentTextView.setText(((NotificationMessageContent) message.content).formatNotification(message));
        } else {
            contentTextView.setText(message.digest());
        }
        timeTextView.setText(TimeUtils.getMsgFormatTime(message.serverTime));
    }
}
