package cn.wildfire.chat.contact.viewholder.header;

import androidx.fragment.app.Fragment;
import android.view.View;

import cn.wildfirechat.chat.R;

import cn.wildfire.chat.annotation.LayoutRes;
import cn.wildfire.chat.contact.ContactAdapter;
import cn.wildfire.chat.contact.model.HeaderValue;

@SuppressWarnings("unused")
@LayoutRes(resId = R.layout.contact_header_channel)
public class ChannelViewHolder extends HeaderViewHolder<HeaderValue> {

    public ChannelViewHolder(Fragment fragment, ContactAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(HeaderValue headerValue) {

    }
}
