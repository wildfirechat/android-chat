/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.third.location.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.NormalMessageContentViewHolder;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.third.location.ui.activity.ShowLocationActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.message.LocationMessageContent;

@MessageContentType(LocationMessageContent.class)
@EnableContextMenu
public class LocationMessageContentViewHolder extends NormalMessageContentViewHolder {

    TextView locationTitleTextView;
    ImageView locationImageView;

    public LocationMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
       itemView.findViewById(R.id.locationLinearLayout).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        locationTitleTextView =itemView.findViewById(R.id.locationTitleTextView);
        locationImageView =itemView.findViewById(R.id.locationImageView);
    }

    @Override
    public void onBind(UiMessage message) {
        LocationMessageContent locationMessage = (LocationMessageContent) message.message.content;
        locationTitleTextView.setText(locationMessage.getTitle());

        if (locationMessage.getThumbnail() != null && locationMessage.getThumbnail().getWidth() > 0) {
            int width = locationMessage.getThumbnail().getWidth();
            int height = locationMessage.getThumbnail().getHeight();
            locationImageView.getLayoutParams().width = UIUtils.dip2Px(width > 200 ? 200 : width);
            locationImageView.getLayoutParams().height = UIUtils.dip2Px(height > 200 ? 200 : height);
            locationImageView.setImageBitmap(locationMessage.getThumbnail());
        } else {
            Glide.with(fragment).load(R.mipmap.default_location)
                .apply(new RequestOptions().override(UIUtils.dip2Px(200), UIUtils.dip2Px(200)).centerCrop()).into(locationImageView);
        }
    }

    public void onClick(View view) {
        LocationMessageContent content = (LocationMessageContent) message.message.content;
        Intent intent = ShowLocationActivity.buildShowLocationIntent(fragment.requireContext(),
            content.getLocation().getLatitude(), content.getLocation().getLongitude(), content.getTitle());
        // 走 WfcPageCompat：平板上地图压到会话所在的那条右栏栈上，而不是整屏跳出去
        WfcPageCompat.startPage(fragment, intent);
    }
}
