package cn.wildfire.chat.moment.third.adapters;

import android.view.View;

import cn.wildfire.chat.moment.third.widgets.NineGridView;
import cn.wildfire.chat.kit.R;

class WordAndImagesViewHolder extends BaseFriendCircleViewHolder {

    NineGridView nineGridView;

    public WordAndImagesViewHolder(View itemView) {
        super(itemView);
        nineGridView = itemView.findViewById(R.id.nine_grid_view);
    }
}
