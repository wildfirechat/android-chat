/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.conversation.message.CompositeMessageContentActivity;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfirechat.message.CompositeMessageContent;

public class FavCompositeContentViewHolder extends FavContentViewHolder {
    TextView titleTextView;
    TextView contentTextView;

    public FavCompositeContentViewHolder(@NonNull View itemView) {
        super(itemView);
        bindViews(itemView);
    }


    private void bindViews(View itemView) {
        titleTextView = itemView.findViewById(R.id.titleTextView);
        contentTextView = itemView.findViewById(R.id.contentTextView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        CompositeMessageContent compositeMessageContent = (CompositeMessageContent) item.toMessage().content;
        titleTextView.setText(compositeMessageContent.getTitle());
        contentTextView.setText(compositeMessageContent.compositeDigest());
    }

    @Override
    protected void onClick() {
        Intent intent = new Intent(fragment.getContext(), CompositeMessageContentActivity.class);
        intent.putExtra("message", favoriteItem.toMessage());
        fragment.startActivity(intent);
    }
}
