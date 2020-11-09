/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.favorite.FavoriteItem;

public class FavUnknownContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.favUnkownContentTextView)
    TextView textView;

    public FavUnknownContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        textView.setText("当前版本不支持，请升级查看");
    }
}
