/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;

public class FavImageContentViewHolder extends FavContentViewHolder {
    ImageView imageView;

    public FavImageContentViewHolder(@NonNull View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        imageView = itemView.findViewById(R.id.favImageContentImageView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        GlideApp.with(itemView)
            .load(item.getUrl()).into(imageView);
    }

    @Override
    protected void onClick() {
        MMPreviewActivity.previewImage(fragment.getActivity(), favoriteItem.getUrl());
    }
}
