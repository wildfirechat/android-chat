package cn.wildfire.chat.kit.contact.viewholder.header;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.contact.ContactAdapter;
import cn.wildfire.chat.kit.contact.model.HeaderValue;

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
