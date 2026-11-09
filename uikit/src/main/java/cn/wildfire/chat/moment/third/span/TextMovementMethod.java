package cn.wildfire.chat.moment.third.span;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

public class TextMovementMethod extends LinkMovementMethod {

    private CommentUserSpan mCommentUserSpan;

    //记录开始按下的时间
    private long mStartTime;

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mStartTime = System.currentTimeMillis();
            mCommentUserSpan = getTextSpan(widget, buffer, event);
            if (mCommentUserSpan != null) {
                mCommentUserSpan.setPressed(true);
                Selection.setSelection(buffer, buffer.getSpanStart(mCommentUserSpan), buffer.getSpanEnd(mCommentUserSpan));
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            CommentUserSpan touchCommentUserSpan = getTextSpan(widget, buffer, event);
            if (mCommentUserSpan != null && touchCommentUserSpan != mCommentUserSpan) {
                mCommentUserSpan.setPressed(false);
                mCommentUserSpan = null;
                Selection.removeSelection(buffer);
            }
        } else {
            if (mCommentUserSpan != null) {
                mCommentUserSpan.setPressed(false);
                mCommentUserSpan = null;
                Selection.removeSelection(buffer);
                /**
                 *  当用户长按span时，不响应相应的点击事件。判断规则为从开始到结束的时间是否大于500ms
                 */
                if (System.currentTimeMillis() - mStartTime < 500) {
                    super.onTouchEvent(widget, buffer, event);
                }
            }
        }
        return true;
    }

    /**
     * 得到匹配的span
     *
     * @param widget
     * @param spannable
     * @param event
     * @return
     */
    private CommentUserSpan getTextSpan(TextView widget, Spannable spannable, MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= widget.getTotalPaddingLeft();
        y -= widget.getTotalPaddingTop();

        x += widget.getScrollX();
        y += widget.getScrollY();

        Layout layout = widget.getLayout();

        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        CommentUserSpan[] link = spannable.getSpans(off, off, CommentUserSpan.class);
        CommentUserSpan touchedSpan = null;
        if (link.length > 0) {
            touchedSpan = link[0];
        }
        return touchedSpan;
    }
}
