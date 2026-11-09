package cn.wildfire.chat.moment.third.interfaces;

import android.view.View;

import cn.wildfire.chat.moment.third.beans.FriendCircleBean;

public interface OnTogglePraiseOrCommentPopupWindowListener {
    void togglePraiseOrCommentPopupWindow(View anchorView, FriendCircleBean friendCircleBean, int position);
}
