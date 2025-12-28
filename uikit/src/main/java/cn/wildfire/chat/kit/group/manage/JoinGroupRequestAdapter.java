package cn.wildfire.chat.kit.group.manage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.JoinGroupRequest;

public class JoinGroupRequestAdapter extends RecyclerView.Adapter<JoinGroupRequestViewHolder> {
    private List<JoinGroupRequest> requests = new ArrayList<>();
    private JoinGroupRequestListFragment fragment;

    public JoinGroupRequestAdapter(JoinGroupRequestListFragment fragment) {
        this.fragment = fragment;
    }

    public void setJoinGroupRequests(List<JoinGroupRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JoinGroupRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_join_request_item, parent, false);
        return new JoinGroupRequestViewHolder(fragment, view);
    }

    @Override
    public void onBindViewHolder(@NonNull JoinGroupRequestViewHolder holder, int position) {
        holder.onBind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests == null ? 0 : requests.size();
    }
}
