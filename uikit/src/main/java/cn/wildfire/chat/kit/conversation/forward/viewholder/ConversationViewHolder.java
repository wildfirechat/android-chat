package cn.wildfire.chat.kit.conversation.forward.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class ConversationViewHolder extends RecyclerView.ViewHolder {
    private Fragment fragment;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;

    public ConversationViewHolder(Fragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;
        ButterKnife.bind(this, itemView);

        userViewModel = ViewModelProviders.of(fragment).get(UserViewModel.class);
        groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);
    }

    public void onBind(ConversationInfo conversationInfo) {
        Conversation conversation = conversationInfo.conversation;
        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo userInfo = userViewModel.getUserInfo(conversation.target, false);
            if (userInfo != null) {
                Glide.with(fragment).load(userInfo.portrait).apply(new RequestOptions().centerCrop().placeholder(R.mipmap.avatar_def)).into(portraitImageView);
                nameTextView.setText(userViewModel.getUserDisplayName(userInfo));
            }
        } else {
            GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
            if (groupInfo != null) {
                Glide.with(fragment).load(groupInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.ic_group_cheat).centerCrop()).into(portraitImageView);
                nameTextView.setText(groupInfo.name);
            }
        }
    }
}
