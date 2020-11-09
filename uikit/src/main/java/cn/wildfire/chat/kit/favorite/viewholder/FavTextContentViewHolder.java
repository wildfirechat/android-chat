/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.favorite.FavoriteItem;

public class FavTextContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.favTextContentTextView)
    TextView favTextContentTextView;

    public FavTextContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        favTextContentTextView.setText(item.getTitle());
    }

    @OnClick(R2.id.favTextContentTextView)
    void showFavText() {
        WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), "收藏内容", favoriteItem.getTitle());
    }
}
