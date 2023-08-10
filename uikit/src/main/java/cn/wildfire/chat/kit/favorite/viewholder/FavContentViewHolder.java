/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.favorite.FavoriteListFragment;
import cn.wildfire.chat.kit.third.utils.TimeUtils;

public abstract class FavContentViewHolder extends RecyclerView.ViewHolder {
    TextView senderTextView;
    TextView timeTextView;

    protected Fragment fragment;
    protected FavoriteItem favoriteItem;

    public FavContentViewHolder(@NonNull View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        senderTextView = itemView.findViewById(R.id.senderTextView);
        timeTextView = itemView.findViewById(R.id.timeTextView);
        itemView.findViewById(R.id.favContentContainerLinearLayout).setOnLongClickListener(v -> this.onLongClick());
        itemView.findViewById(R.id.favContentContainerLinearLayout).setOnClickListener(v -> this.onClick());
    }

    public void bind(Fragment fragment, FavoriteItem item) {
        this.fragment = fragment;
        this.favoriteItem = item;
        senderTextView.setText(item.getOrigin());
        timeTextView.setText((TimeUtils.getMsgFormatTime(item.getTimestamp())));
    }

    public boolean onLongClick() {
        new MaterialDialog.Builder(fragment.getActivity()).items("删除").itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                if (position == 0) {
                    ((FavoriteListFragment) fragment).delFav(favoriteItem);
                }
            }
        }).show();
        return true;
    }

    protected void onClick() {

    }
}
