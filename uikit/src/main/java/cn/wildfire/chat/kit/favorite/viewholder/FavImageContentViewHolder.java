/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import butterknife.BindView;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.favorite.FavoriteItem;

public class FavImageContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.favImageContentImageView)
    ImageView imageView;

    public FavImageContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(FavoriteItem item) {
        super.bind(item);
        GlideApp.with(itemView)
            .load(item.getUrl()).into(imageView);
    }
}
