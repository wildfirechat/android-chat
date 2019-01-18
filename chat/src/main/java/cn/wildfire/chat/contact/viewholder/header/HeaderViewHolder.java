package cn.wildfire.chat.contact.viewholder.header;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import cn.wildfire.chat.contact.ContactAdapter;
import cn.wildfire.chat.contact.model.HeaderValue;

public abstract class HeaderViewHolder<T extends HeaderValue> extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected ContactAdapter adapter;

    public HeaderViewHolder(Fragment fragment, ContactAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
    }

    public abstract void onBind(T t);

}
