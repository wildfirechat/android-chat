package cn.wildfire.chat.search.viewHolder;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public abstract class ResultItemViewHolder<R> extends RecyclerView.ViewHolder {
    protected Fragment fragment;

    public ResultItemViewHolder(Fragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;
    }

    public abstract void onBind(String keyword, R r);
}
