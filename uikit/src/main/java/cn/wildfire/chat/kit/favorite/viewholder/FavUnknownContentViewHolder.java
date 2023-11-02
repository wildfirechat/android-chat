/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.favorite.FavoriteItem;

public class FavUnknownContentViewHolder extends FavContentViewHolder {
    TextView textView;

    public FavUnknownContentViewHolder(@NonNull View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        textView = itemView.findViewById(R.id.favUnkownContentTextView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        textView.setText("当前版本不支持，请升级查看");
    }
}
