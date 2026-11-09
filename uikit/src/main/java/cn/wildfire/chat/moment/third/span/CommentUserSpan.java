package cn.wildfire.chat.moment.third.span;

import android.content.Context;
import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.core.content.ContextCompat;

import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.kit.R;

/**
 * @author KCrason
 * @date 2018/4/28
 */
public class CommentUserSpan extends ClickableSpan {

    private Context mContext;

    private String userId;
    private String mUserName;
    private boolean mPressed;
    private OnCommentUserClickListener listener;

    public CommentUserSpan(Context context, String userId, String userName, OnCommentUserClickListener listener) {
        this.mContext = context;
        this.userId = userId;
        this.mUserName = userName;
        this.listener = listener;
    }

    public void setPressed(boolean isPressed) {
        this.mPressed = isPressed;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.bgColor = mPressed ? ContextCompat.getColor(mContext, R.color.base_B5B5B5) : Color.TRANSPARENT;
        ds.setColor(ContextCompat.getColor(mContext, R.color.blue3));
        ds.setUnderlineText(false);
    }

    @Override
    public void onClick(View widget) {
        if (listener != null) {
            listener.onCommentUserClick(userId);
        }
    }
}
