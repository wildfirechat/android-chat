/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import butterknife.BindView;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.favorite.FavoriteItem;

public class FavTextContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.favTextContentTextView)
    TextView favTextContentTextView;

    public FavTextContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(FavoriteItem item) {
        super.bind(item);
        favTextContentTextView.setText(item.getTitle());
    }
}
