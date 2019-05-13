package cn.wildfire.chat.kit.search.viewHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;

public class MessageViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.portraitImageView)
    protected ImageView portraitImageView;
    @Bind(R.id.nameTextView)
    protected TextView nameTextView;
    @Bind(R.id.contentTextView)
    protected TextView contentTextView;
    @Bind(R.id.timeTextView)
    protected TextView timeTextView;

    private Fragment fragment;
    private UserViewModel userViewModel;

    public MessageViewHolder(Fragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        ButterKnife.bind(this, itemView);
    }

    // fixme display name
    public void onBind(Message message) {
        UserInfo sender = userViewModel.getUserInfo(message.sender, false);
        if (sender != null) {
            if (!TextUtils.isEmpty(sender.displayName)) {
                nameTextView.setText(sender.displayName);
            } else {
                nameTextView.setText("<" + sender.uid + ">");
            }
            String name;
            if (message.conversation.type == Conversation.ConversationType.Group) {
                GroupViewModel groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);
                name = groupViewModel.getGroupMemberDisplayName(message.conversation.target, message.sender);
            }
            Glide.with(fragment).load(sender.portrait).apply(new RequestOptions().placeholder(R.mipmap.default_header)).into(portraitImageView);
        }
        if (message.content instanceof NotificationMessageContent) {
            contentTextView.setText(((NotificationMessageContent) message.content).formatNotification(message));
        } else {
            contentTextView.setText(message.digest());
        }
        timeTextView.setText(TimeUtils.getMsgFormatTime(message.serverTime));
    }
}
