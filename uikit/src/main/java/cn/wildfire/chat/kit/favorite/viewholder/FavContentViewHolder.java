/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnLongClick;
import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.favorite.FavoriteListFragment;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.third.utils.TimeUtils;

public abstract class FavContentViewHolder extends RecyclerView.ViewHolder {
    @BindView(R2.id.senderTextView)
    TextView senderTextView;
    @BindView(R2.id.timeTextView)
    TextView timeTextView;

    protected Fragment fragment;
    protected FavoriteItem favoriteItem;

    public FavContentViewHolder(@NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(Fragment fragment, FavoriteItem item) {
        this.fragment = fragment;
        this.favoriteItem = item;
        senderTextView.setText(item.getOrigin());
        timeTextView.setText((TimeUtils.getMsgFormatTime(item.getTimestamp())));
    }

    @OnLongClick(R2.id.favContentContainerLinearLayout)
    public void onLongClick() {
        new MaterialDialog.Builder(fragment.getActivity()).items("删除").itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                if (position == 0) {
                    ((FavoriteListFragment)fragment).delFav(favoriteItem);
                }
            }
        }).show();
    }

}
