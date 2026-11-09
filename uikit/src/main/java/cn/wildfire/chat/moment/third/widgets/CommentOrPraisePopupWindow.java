package cn.wildfire.chat.moment.third.widgets;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import cn.wildfire.chat.moment.third.interfaces.OnPraiseOrCommentClickListener;
import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfire.chat.kit.R;

public class CommentOrPraisePopupWindow extends PopupWindow implements View.OnClickListener {

    private Context mContext;

    private OnPraiseOrCommentClickListener mOnPraiseOrCommentClickListener;

    private int mPopupWindowHeight;
    private int mPopupWindowWidth;
    private int mCurrentPosition;
    private TextView praiseTextView;

    public CommentOrPraisePopupWindow(Context context) {
        View contentView = LayoutInflater.from(context).inflate(R.layout.moment_popup_window_praise_or_comment_view, null);
        this.setContentView(contentView);
        praiseTextView = contentView.findViewById(R.id.praiseTextView);
        contentView.findViewById(R.id.layout_praise).setOnClickListener(this);
        contentView.findViewById(R.id.layout_comment).setOnClickListener(this);
        //不设置宽高将无法显示popupWindow
        this.mPopupWindowHeight = Utils.dp2px(38);
        this.mPopupWindowWidth = Utils.dp2px(190);
        this.setHeight(mPopupWindowHeight);
        this.setWidth(mPopupWindowWidth);
        // 设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.setAnimationStyle(R.style.PraiseOrCommentAnimationStyle);
        // 刷新状态
        this.update();
        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        this.setBackgroundDrawable(dw);
    }

    public void setCurrentPosition(int currentPosition) {
        mCurrentPosition = currentPosition;
    }

    public void setLiked(boolean liked) {
        if (liked) {
            praiseTextView.setText("取消");
        } else {
            praiseTextView.setText("赞");
        }
    }

    public CommentOrPraisePopupWindow setOnPraiseOrCommentClickListener(OnPraiseOrCommentClickListener onPraiseOrCommentClickListener) {
        mOnPraiseOrCommentClickListener = onPraiseOrCommentClickListener;
        return this;
    }

    public void showPopupWindow(View anchor) {
        if (anchor == null) {
            return;
        }
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int xOffset = location[0] - mPopupWindowWidth - Utils.dp2px(10f);
        int yOffset = location[1] + (anchor.getHeight() - mPopupWindowHeight) / 2;
        showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset);
    }

    @Override
    public void onClick(View v) {
        dismiss();
        int id = v.getId();
        if (id == R.id.layout_praise) {
            if (mOnPraiseOrCommentClickListener != null) {
                mOnPraiseOrCommentClickListener.onPraiseClick(mCurrentPosition);
            }
        } else if (id == R.id.layout_comment) {
            if (mOnPraiseOrCommentClickListener != null) {
                mOnPraiseOrCommentClickListener.onCommentClick(v, mCurrentPosition);
            }
        }
    }
}
