package cn.wildfire.chat.moment.third.widgets;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

import cn.wildfire.chat.moment.third.beans.CommentBean;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.moment.third.others.SimpleWeakObjectPool;
import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfire.chat.kit.R;

/**
 * @author KCrason
 * @date 2018/4/27
 */
public class VerticalCommentWidget extends LinearLayout implements ViewGroup.OnHierarchyChangeListener {


    private int feedPosition;
    private List<CommentBean> mCommentBeans;

    private LayoutParams mLayoutParams;
    private SimpleWeakObjectPool<View> COMMENT_TEXT_POOL;
    private int mCommentVerticalSpace;
    private OnCommentItemClickListener onCommentItemClickListener;
    private OnCommentItemLongClickListener onCommentItemLongClickListener;
    private OnCommentUserClickListener onCommentUserClickListener;

    private boolean showDetail;

    public VerticalCommentWidget(Context context) {
        super(context);
        init();
    }

    public VerticalCommentWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VerticalCommentWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mCommentVerticalSpace = Utils.dp2px(3f);
        COMMENT_TEXT_POOL = new SimpleWeakObjectPool<>();
        setOnHierarchyChangeListener(this);
    }

    public void setOnCommentItemClickListener(OnCommentItemClickListener onCommentItemClickListener) {
        this.onCommentItemClickListener = onCommentItemClickListener;
    }

    public void setOnCommentItemLongClickListener(OnCommentItemLongClickListener onCommentItemLongClickListener) {
        this.onCommentItemLongClickListener = onCommentItemLongClickListener;
    }

    public void setOnCommentUserClickListener(OnCommentUserClickListener onCommentUserClickListener) {
        this.onCommentUserClickListener = onCommentUserClickListener;
    }

    public boolean isShowDetail() {
        return showDetail;
    }

    public void setShowDetail(boolean showDetail) {
        this.showDetail = showDetail;
    }

    public void addComments(int feedPosition, List<CommentBean> commentBeans, boolean isStartAnimation) {
        this.feedPosition = feedPosition;
        this.mCommentBeans = commentBeans;
        if (commentBeans != null) {
            int oldCount = getChildCount();
            int newCount = commentBeans.size();
            if (oldCount > newCount) {
                removeViewsInLayout(newCount, oldCount - newCount);
            }
            for (int i = 0; i < newCount; i++) {
                boolean hasChild = i < oldCount;
                View childView = hasChild ? getChildAt(i) : null;
                CommentBean commentBean = commentBeans.get(i);
                commentBean.build(getContext(), onCommentUserClickListener);
                SpannableStringBuilder commentSpan = commentBean.getCommentContentSpan();
                if (childView == null) {
                    childView = COMMENT_TEXT_POOL.get();
                    if (childView == null) {
                        addViewInLayout(makeCommentItemView(commentBean, commentSpan, i, isStartAnimation), i, generateMarginLayoutParams(i), true);
                    } else {
                        addCommentItemView(childView, commentSpan, i, isStartAnimation);
                    }
                } else {
                    updateCommentData(childView, commentSpan, i, isStartAnimation);
                }
            }
            requestLayout();
        }
    }


    /**
     * 更新指定的position的comment
     */
    public void updateTargetComment(int position, List<CommentBean> commentBeans) {
        int oldCount = getChildCount();
        for (int i = 0; i < oldCount; i++) {
            if (i == position) {
                View childView = getChildAt(i);
                if (childView != null) {
                    CommentBean commentBean = commentBeans.get(i);
                    SpannableStringBuilder commentSpan = commentBean.getCommentContentSpan();
                    updateCommentData(childView, commentSpan, i, true);
                }
                break;
            }
        }
        requestLayout();
    }


    /**
     * 創建Comment item view
     */
    protected View makeCommentItemView(CommentBean commentBean, SpannableStringBuilder content, int index, boolean isStartAnimation) {
        // TODO showDetail
        return makeContentTextView(content, index);
    }


    /**
     * 添加需要的Comment View
     */
    private void addCommentItemView(View view, SpannableStringBuilder builder, int index, boolean isStartAnimation) {
        if (view instanceof TextView) {
            ((TextView) view).setText(builder);
            addOnItemLongClickListener(view, index);
            addOnItemClickListener(view, index);
            addViewInLayout(view, index, generateMarginLayoutParams(index), true);
        }
    }


    private void addOnItemLongClickListener(View view, int index) {
        view.setOnLongClickListener(v -> {
            if (onCommentItemLongClickListener != null) {
                TextView textView = (TextView) view;
                if (textView.getSelectionStart() == -1 && textView.getSelectionEnd() == -1) {
                    onCommentItemLongClickListener.onCommentItemLongClick(view, feedPosition, index);
                }
            }
            return true;
        });
    }

    private void addOnItemClickListener(View view, int index) {
        view.setOnClickListener(v -> {
            if (onCommentItemClickListener != null) {
                TextView textView = (TextView) view;
                if (textView.getSelectionStart() == -1 && textView.getSelectionEnd() == -1) {
                    onCommentItemClickListener.onCommentItemClick(view, feedPosition, index);
                }
            }
        });
    }

    /**
     * 更新comment list content
     */
    private void updateCommentData(View view, SpannableStringBuilder builder, int index, boolean isStartAnimation) {
        if (view instanceof TextView) {
            ((TextView) view).setText(builder);
        }
    }

    private TextView makeContentTextView(SpannableStringBuilder content, int index) {
        TextView textView = new TextView(getContext());
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.black3));
        textView.setBackgroundResource(R.drawable.selector_view_name_state);
        textView.setTextSize(14f);
        textView.setLineSpacing(mCommentVerticalSpace, 1f);
        textView.setText(content);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        addOnItemLongClickListener(textView, index);
        addOnItemClickListener(textView, index);
        return textView;
    }


    private LayoutParams generateMarginLayoutParams(int index) {
        if (mLayoutParams == null) {
            mLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (mCommentBeans != null && index > 0) {
            mLayoutParams.bottomMargin = index == mCommentBeans.size() - 1 ? 0 : mCommentVerticalSpace;
        }
        return mLayoutParams;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {

    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        COMMENT_TEXT_POOL.put(child);
    }
}
