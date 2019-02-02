package cn.wildfire.chat.kit.contact.newfriend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.FriendRequest;

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

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        holder.onBind(friendRequests.get(position));
    }

    @Override
    public int getItemCount() {
        return friendRequests == null ? 0 : friendRequests.size();
    }
}
