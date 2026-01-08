package cn.wildfire.chat.kit.conversation.forward;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class ForwardTargetAdapter extends RecyclerView.Adapter<ForwardTargetAdapter.ViewHolder> {
    private List<Conversation> conversations;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    private RequestOptions options;

    public ForwardTargetAdapter(List<Conversation> conversations) {
        this.conversations = conversations;
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        options = new RequestOptions()
            .placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(4));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forward_target, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(conversations.get(position));
    }

    @Override
    public int getItemCount() {
        return conversations == null ? 0 : conversations.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView nameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
        }

        void bind(Conversation conversation) {
            String name = "";
            String portrait = "";

            if (conversation.type == Conversation.ConversationType.Single) {
                UserInfo userInfo = userViewModel.getUserInfo(conversation.target, false);
                if (userInfo != null) {
                    name = userInfo.displayName;
                    portrait = userInfo.portrait;
                }
            } else if (conversation.type == Conversation.ConversationType.Group) {
                GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
                if (groupInfo != null) {
                    name = !TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name;
                    portrait = groupInfo.portrait;
                }
            }

            nameTextView.setText(name);
            Glide.with(avatarImageView).load(portrait).apply(options).into(avatarImageView);
        }
    }
}
