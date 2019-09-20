package cn.wildfire.chat.kit.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class GroupListAdapter extends RecyclerView.Adapter<GroupViewHolder> {
    private List<GroupInfo> groupInfos;
    private Fragment fragment;
    private OnGroupItemClickListener onGroupItemClickListener;

    public GroupListAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public void setGroupInfos(List<GroupInfo> groupInfos) {
        this.groupInfos = groupInfos;
    }

    public List<GroupInfo> getGroupInfos() {
        return groupInfos;
    }

    public void setOnGroupItemClickListener(OnGroupItemClickListener onGroupItemClickListener) {
        this.onGroupItemClickListener = onGroupItemClickListener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item_contact, parent, false);
        GroupViewHolder viewHolder = new GroupViewHolder(fragment, this, view);
        view.findViewById(R.id.contactLinearLayout).setOnClickListener(v -> {
            if (onGroupItemClickListener != null) {
                onGroupItemClickListener.onGroupClick(viewHolder.getGroupInfo());
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.onBind(groupInfos.get(position));
    }

    @Override
    public int getItemCount() {
        return groupInfos == null ? 0 : groupInfos.size();
    }
}
