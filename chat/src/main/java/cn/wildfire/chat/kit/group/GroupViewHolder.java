package cn.wildfire.chat.kit.group;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class GroupViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    private GroupListAdapter adapter;
    @Bind(R.id.portraitImageView)
    ImageView portraitImageView;
    @Bind(R.id.nameTextView)
    TextView nameTextView;
    @Bind(R.id.categoryTextView)
    TextView categoryTextView;
    @Bind(R.id.dividerLine)
    View dividerLine;

    protected GroupInfo groupInfo;

    public GroupViewHolder(Fragment fragment, GroupListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
    }

    // TODO hide the last diver line
    public void onBind(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
        categoryTextView.setVisibility(View.GONE);
        nameTextView.setText(this.groupInfo.name);
        Glide.with(fragment).load(this.groupInfo.portrait).into(portraitImageView);
    }

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }
}
