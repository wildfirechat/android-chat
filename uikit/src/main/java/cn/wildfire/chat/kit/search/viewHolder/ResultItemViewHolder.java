package cn.wildfire.chat.kit.search.viewHolder;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ResultItemViewHolder<R> extends RecyclerView.ViewHolder {
    protected Fragment fragment;

    public ResultItemViewHolder(Fragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;
    }

    public abstract void onBind(String keyword, R r);
}
