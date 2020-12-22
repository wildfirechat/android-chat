/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.conversation.message.CompositeMessageContentActivity;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfirechat.message.CompositeMessageContent;

public class FavCompositeContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.titleTextView)
    TextView titleTextView;
    @BindView(R2.id.contentTextView)
    TextView contentTextView;

    public FavCompositeContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        CompositeMessageContent compositeMessageContent = (CompositeMessageContent) item.toMessage().content;
        titleTextView.setText(compositeMessageContent.getTitle());
        contentTextView.setText(compositeMessageContent.compositeDigest());
    }

    @OnClick(R2.id.contentTextView)
    void showFavText() {
        Intent intent = new Intent(fragment.getContext(), CompositeMessageContentActivity.class);
        intent.putExtra("message", favoriteItem.toMessage());
        fragment.startActivity(intent);
    }
}
