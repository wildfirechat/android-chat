/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;

public class FavVideoContentViewHolder extends FavContentViewHolder {
    ImageView imageView;

    public FavVideoContentViewHolder(@NonNull View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        imageView = itemView.findViewById(R.id.favImageContentImageView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        Glide.with(itemView)
            .load(item.getUrl()).into(imageView);
    }

    @Override
    protected void onClick() {
        MMPreviewActivity.previewVideo(fragment.getActivity(), favoriteItem.toMessage());
    }
}
