package cn.wildfire.chat.search;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public abstract class ResultItemViewHolder<T> extends RecyclerView.ViewHolder {
    public ResultItemViewHolder(View itemView) {
        super(itemView);
    }

    abstract void onBind(T t);
}
