package cn.wildfire.chat.kit.conversation.pick;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class SelectedConversationAdapter extends RecyclerView.Adapter<SelectedConversationAdapter.ViewHolder> {
    private List<ConversationInfo> conversations = new ArrayList<>();
    private Map<String, String> tempPortraitMap;
    private OnItemClickListener listener;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    private RequestOptions options;

    public SelectedConversationAdapter() {
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        options = new RequestOptions()
            .placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(4));
    }

    public void setConversations(List<ConversationInfo> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    public void setTempPortraitMap(Map<String, String> map) {
        this.tempPortraitMap = map;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.picked_conversation_item, parent, false);
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

    public interface OnItemClickListener {
        void onItemClick(ConversationInfo conversationInfo);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        ConversationInfo info;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(info);
                }
            });
        }

        void bind(ConversationInfo info) {
            this.info = info;
            String portrait = null;
            String key = info.conversation.type + "_" + info.conversation.target;

            if (tempPortraitMap != null && tempPortraitMap.containsKey(key)) {
                portrait = tempPortraitMap.get(key);
            }

            if (TextUtils.isEmpty(portrait)) {
                if (info.conversation.type == Conversation.ConversationType.Single) {
                    UserInfo userInfo = userViewModel.getUserInfo(info.conversation.target, false);
                    if (userInfo != null) {
                        portrait = userInfo.portrait;
                    }
                } else if (info.conversation.type == Conversation.ConversationType.Group) {
                    GroupInfo groupInfo = groupViewModel.getGroupInfo(info.conversation.target, false);
                    if (groupInfo != null) {
                        portrait = groupInfo.portrait;
                    }
                }
            }

            Glide.with(avatar).load(portrait).apply(options).into(avatar);
        }
    }
}
