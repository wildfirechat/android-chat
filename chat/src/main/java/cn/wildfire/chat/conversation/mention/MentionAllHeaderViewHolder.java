package cn.wildfire.chat.conversation.mention;

import androidx.fragment.app.Fragment;
import android.view.View;

import cn.wildfirechat.chat.R;
import cn.wildfire.chat.annotation.LayoutRes;
import cn.wildfire.chat.contact.ContactAdapter;
import cn.wildfire.chat.contact.model.HeaderValue;
import cn.wildfire.chat.contact.viewholder.header.HeaderViewHolder;

@LayoutRes(resId = R.layout.conversation_header_mention_all)
public class MentionAllHeaderViewHolder extends HeaderViewHolder<HeaderValue> {
    public MentionAllHeaderViewHolder(Fragment fragment, ContactAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(HeaderValue value) {

    }
}
