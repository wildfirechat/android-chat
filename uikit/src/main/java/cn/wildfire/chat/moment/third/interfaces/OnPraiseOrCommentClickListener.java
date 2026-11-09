package cn.wildfire.chat.moment.third.interfaces;

import android.view.View;

public interface OnPraiseOrCommentClickListener {
    void onPraiseClick(int position);


    void onCommentClick(View view, int position);
}
