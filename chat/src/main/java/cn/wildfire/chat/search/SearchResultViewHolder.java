package cn.wildfire.chat.search;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public abstract class SearchResultViewHolder<R> extends RecyclerView.ViewHolder {
    protected Fragment fragment;

    public SearchResultViewHolder(Fragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;
    }

    public abstract void onBind(Fragment fragment, R r);
}
