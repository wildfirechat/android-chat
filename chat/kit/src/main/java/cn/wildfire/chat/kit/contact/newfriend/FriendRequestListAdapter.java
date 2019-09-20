package cn.wildfire.chat.kit.contact.newfriend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.UserInfo;

public class FriendRequestListAdapter extends RecyclerView.Adapter<FriendRequestViewHolder> {
    private List<FriendRequest> friendRequests;
    private FriendRequestListFragment fragment;

    public FriendRequestListAdapter(FriendRequestListFragment fragment) {
        this.fragment = fragment;
    }

    public void setFriendRequests(List<FriendRequest> friendRequests) {
        this.friendRequests = friendRequests;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.contact_item_new_friend, parent, false);
        return new FriendRequestViewHolder(fragment, this, view);
    }

    public void onUserInfosUpdate(List<UserInfo> userInfos) {
        if (friendRequests == null || friendRequests.isEmpty()) {
            return;
        }
        for (UserInfo info : userInfos) {
            for (int i = 0; i < friendRequests.size(); i++) {
                if (friendRequests.get(i).target.equals(info.uid)) {
                    notifyItemChanged(i);
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        holder.onBind(friendRequests.get(position));
    }

    @Override
    public int getItemCount() {
        return friendRequests == null ? 0 : friendRequests.size();
    }
}
