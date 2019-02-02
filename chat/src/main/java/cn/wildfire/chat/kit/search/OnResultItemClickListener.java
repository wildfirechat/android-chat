package cn.wildfire.chat.kit.search;

import android.view.View;

import androidx.fragment.app.Fragment;

public interface OnResultItemClickListener<R> {
    /**
     * @param fragment the fragment holds the search results
     * @param itemView the item view
     * @param view     the clicked view, now {@param view} and {@param itemView} is equal
     * @param r        the value bind to the item view
     */
    void onResultItemClick(Fragment fragment, View itemView, View view, R r);
}
