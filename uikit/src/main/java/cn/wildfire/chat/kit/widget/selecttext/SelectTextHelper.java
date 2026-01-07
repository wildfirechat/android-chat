/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget.selecttext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.Magnifier;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.uikit.menu.PopupMenu;

/**
 * Created by hxg on 2021/9/13 929842234@qq.com
 * <p>
 * 仿照的例子：https://github.com/laobie
 * 放大镜 Magnifier：https://developer.android.google.cn/guide/topics/text/magnifier
 */
public class SelectTextHelper {
    private static final int DEFAULT_SELECTION_LENGTH = 2;
    private static final int DEFAULT_SHOW_DURATION = 100;
    private static volatile Map<String, Integer> emojiMap = new HashMap<>();

    private TextView mTextView;
    private CharSequence mOriginalContent;
    private CursorHandle mStartHandle;
    private CursorHandle mEndHandle;
    private PopupMenu mPopupMenu;
    private Magnifier mMagnifier;
    private SelectionInfo mSelectionInfo = new SelectionInfo();
    private OnSelectListener mSelectListener;
    private Context mContext;
    private Spannable mSpannable;
    private int mTouchX = 0;
    private int mTouchY = 0;
    private int mTextViewMarginStart = 0;
    private int mSelectedColor;
    private int mCursorHandleColor;
    private int mCursorHandleSize;
    private boolean mSelectAll;
    private boolean mSelectedAllNoPop;
    private int mSelectTextLength;
    private boolean mScrollShow;
    private boolean mMagnifierShow;
    private int mPopSpanCount;
    private int mPopBgResource;
    private int mPopDelay;
    private int mPopAnimationStyle;
    private int mPopArrowImg;
    private List<Pair<Integer, String>> itemTextList;
    private List<Builder.onSeparateItemClickListener> itemListenerList;
    private BackgroundColorSpan mSpan;
    private boolean isHideWhenScroll = false;
    private boolean isHide = true;
    // 标志是否正在拖动手柄
    private boolean isDraggingHandle = false;
    private boolean usedClickListener = false;
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;
    private View.OnTouchListener mRootTouchListener;

    public SelectTextHelper(Builder builder) {
        mTextView = builder.mTextView;
        mOriginalContent = mTextView.getText();
        mContext = mTextView.getContext();
        mSelectedColor = builder.mSelectedColor;
        mCursorHandleColor = builder.mCursorHandleColor;
        mSelectAll = builder.mSelectAll;
        mScrollShow = builder.mScrollShow;
        mMagnifierShow = builder.mMagnifierShow;
        mSelectedAllNoPop = builder.mSelectedAllNoPop;
        mSelectTextLength = builder.mSelectTextLength;
        mPopSpanCount = builder.mPopSpanCount;
        mPopBgResource = builder.mPopBgResource;
        mPopDelay = builder.mPopDelay;
        mPopAnimationStyle = builder.mPopAnimationStyle;
        mPopArrowImg = builder.mPopArrowImg;
        itemTextList = builder.itemTextList;
        itemListenerList = builder.itemListenerList;
        mCursorHandleSize = SelectUtils.dp2px(builder.mCursorHandleSizeInDp);
        init();
    }

    public static synchronized void putAllEmojiMap(Map<String, Integer> map) {
        if (map != null) {
            emojiMap.putAll(map);
        }
    }

    public static synchronized void putEmojiMap(String emojiKey, @DrawableRes int drawableRes) {
        emojiMap.put(emojiKey, drawableRes);
    }

    public void reset() {
        hideSelectView();
        resetSelectionInfo();
        if (mSelectListener != null) {
            mSelectListener.onReset();
        }
    }

    public boolean isPopShowing() {
        return mPopupMenu != null && mPopupMenu.isShowing();
    }

    public void setSelectListener(OnSelectListener selectListener) {
        mSelectListener = selectListener;
    }

    public void destroy() {
        if (mOnScrollChangedListener != null) {
            mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        }
        if (mOnPreDrawListener != null) {
            mTextView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        }
        if (mTextView.getRootView() != null) {
            mTextView.getRootView().setOnTouchListener(null);
        }
        reset();
        mStartHandle = null;
        mEndHandle = null;
        mPopupMenu = null;
    }

    public void selectAll() {
        hideSelectView();
        selectText(0, mTextView.getText().length());
        isHide = false;
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
//        showOperateWindow();
        showPopupMenu();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        SpannableStringBuilder spanStr = new SpannableStringBuilder(mOriginalContent);
        SelectUtils.replaceContent(spanStr, mOriginalContent, "\u00A0\u00A0", "\u3000\u3000");
        SelectUtils.replaceContent(spanStr, mOriginalContent, "\u0020\u0020", "\u3000\u3000");
        SelectUtils.replaceText2Emoji(mContext, emojiMap, spanStr, mOriginalContent);

        mTextView.setHighlightColor(Color.TRANSPARENT);
        mTextView.setText(spanStr, TextView.BufferType.SPANNABLE);
        mTextView.setOnTouchListener((v, event) -> {
            mTouchX = (int) event.getX();
            mTouchY = (int) event.getY();
            return false;
        });
        mTextView.setOnClickListener(v -> {
            if (usedClickListener) {
                usedClickListener = false;
                return;
            }
            if (mPopupMenu == null || !mPopupMenu.isShowing()) {
                if (mSelectListener != null) {
                    mSelectListener.onDismiss();
                }
            }
            reset();
            if (mSelectListener != null) {
                mSelectListener.onClick(mTextView, mOriginalContent);
            }
        });
        mTextView.setOnLongClickListener(v -> {
            mTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    destroy();
                }
            });
            mOnPreDrawListener = () -> {
                if (isHideWhenScroll) {
                    isHideWhenScroll = false;
                    postShowSelectView(mPopDelay);
                }
                if (mTextViewMarginStart == 0) {
                    int[] location = new int[2];
                    mTextView.getLocationInWindow(location);
                    mTextViewMarginStart = location[0];
                }
                return true;
            };
            mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);

            mRootTouchListener = (view, event) -> {
                reset();
                mTextView.getRootView().setOnTouchListener(null);
                return false;
            };
            mTextView.getRootView().setOnTouchListener(mRootTouchListener);

            mOnScrollChangedListener = () -> {
                if (mScrollShow) {
                    if (!isHideWhenScroll && !isHide) {
                        isHideWhenScroll = true;
                        if (mPopupMenu != null) {
                            mPopupMenu.dismiss();
                        }
                        if (mStartHandle != null) {
                            mStartHandle.dismiss();
                        }
                        if (mEndHandle != null) {
                            mEndHandle.dismiss();
                        }
                    }
                    if (mSelectListener != null) {
                        mSelectListener.onScrolling();
                    }
                } else {
                    reset();
                }
            };
            mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);

            if (mPopupMenu == null) {
                mPopupMenu = mSelectListener.newPopupMenu();
            }
            if (mSelectAll) {
                showAllView();
            } else {
                showSelectView(mTouchX, mTouchY);
            }
            if (mSelectListener != null) {
                mSelectListener.onLongClick(mTextView);
            }
            return true;
        });

        mTextView.setMovementMethod(new LinkMovementMethodInterceptor());
    }

    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    private Runnable mShowSelectViewRunnable = () -> {
        if (isHide) return;
        if (mPopupMenu != null) {
//            showOperateWindow();
            showPopupMenu();
        }
        if (mStartHandle != null) {
            showCursorHandle(mStartHandle);
        }
        if (mEndHandle != null) {
            showCursorHandle(mEndHandle);
        }
    };

    private void hideSelectView() {
        isHide = true;
        usedClickListener = false;
        if (mStartHandle != null) {
            mStartHandle.dismiss();
        }
        if (mEndHandle != null) {
            mEndHandle.dismiss();
        }
        if (mPopupMenu!= null) {
            mPopupMenu.dismiss();
        }
    }

    private void resetSelectionInfo() {
        resetEmojiBackground();
        mSelectionInfo.mSelectionContent = null;
        if (mSpannable != null && mSpan != null) {
            mSpannable.removeSpan(mSpan);
            mSpan = null;
        }
    }

    private void showSelectView(int x, int y) {
        reset();
        isHide = false;
        if (mStartHandle == null) mStartHandle = new CursorHandle(true);
        if (mEndHandle == null) mEndHandle = new CursorHandle(false);
        int startOffset = SelectUtils.getPreciseOffset(mTextView, x, y);
        int endOffset = startOffset + mSelectTextLength;
        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        if (mSpannable == null || endOffset - 1 >= mTextView.getText().length()) {
            endOffset = mTextView.getText().length();
        }
        endOffset = changeEndOffset(startOffset, endOffset);
        selectText(startOffset, endOffset);
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
//        showOperateWindow();
        showPopupMenu();
    }

    private int changeEndOffset(int startOffset, int endOffset) {
        Spannable selectText = (Spannable) mSpannable.subSequence(startOffset, endOffset);
        if (SelectUtils.isImageSpanText(selectText)) {
            while (!SelectUtils.matchImageSpan(emojiMap, selectText.toString())) {
                endOffset++;
                selectText = (Spannable) mSpannable.subSequence(startOffset, endOffset);
            }
        }
        String selectTextString = selectText.toString();
        if (selectTextString.length() > 1) {
            if (!SelectUtils.isEmojiText(selectTextString.charAt(selectTextString.length() - 2)) &&
                SelectUtils.isEmojiText(selectTextString.charAt(selectTextString.length() - 1))) {
                endOffset--;
            }
        }
        return endOffset;
    }

    private void showPopupMenu() {
        isDraggingHandle = false;
        mPopupMenu = mSelectListener.newPopupMenu();
        if (mPopupMenu != null) {
            mPopupMenu.setOnDismissListener(() -> {
                // 如果不是拖动导致的关闭，则取消文本选择
                if (!isDraggingHandle) {
                    mTextView.post(() -> reset());
                }
            });
            mPopupMenu.showAsGridMenu(mTextView, 5);
        }
    }


    private void showAllView() {
        reset();
        isHide = false;
        if (mStartHandle == null) mStartHandle = new CursorHandle(true);
        if (mEndHandle == null) mEndHandle = new CursorHandle(false);
        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        if (mSpannable == null) {
            return;
        }
        selectText(0, mTextView.getText().length());
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
        //showOperateWindow();
        showPopupMenu();
    }

    private void showCursorHandle(CursorHandle cursorHandle) {
        Layout layout = mTextView.getLayout();
        int offset = cursorHandle.isLeft ? mSelectionInfo.mStart : mSelectionInfo.mEnd;
        int x = (int) layout.getPrimaryHorizontal(offset);
        int y = layout.getLineBottom(layout.getLineForOffset(offset));

        if (!cursorHandle.isLeft && mSelectionInfo.mEnd != 0 && x == 0) {
            x = (int) layout.getLineRight(layout.getLineForOffset(mSelectionInfo.mEnd - 1));
            y = layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mEnd - 1));
        }
        cursorHandle.show(x, y);
    }

    private void selectText(int startPos, int endPos) {
        if (startPos != -1) {
            mSelectionInfo.mStart = startPos;
        }
        if (endPos != -1) {
            mSelectionInfo.mEnd = endPos;
        }
        if (mSelectionInfo.mStart > mSelectionInfo.mEnd) {
            int temp = mSelectionInfo.mStart;
            mSelectionInfo.mStart = mSelectionInfo.mEnd;
            mSelectionInfo.mEnd = temp;
        }
        if (mSpannable != null) {
            if (mSpan == null) {
                mSpan = new BackgroundColorSpan(mSelectedColor);
            }
            mSelectionInfo.mSelectionContent =
                mSpannable.subSequence(mSelectionInfo.mStart, mSelectionInfo.mEnd);
            mSpannable.setSpan(
                mSpan, mSelectionInfo.mStart, mSelectionInfo.mEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            );
            if (mSelectListener != null) {
                mSelectListener.onTextSelected(mSelectionInfo.mSelectionContent);
            }
            setEmojiBackground();
        }
    }

    private void setEmojiBackground() {
        if (emojiMap.isEmpty()) {
            return;
        }
        setEmojiBackground(
            (Spannable) mSpannable.subSequence(0, mSelectionInfo.mStart),
            Color.TRANSPARENT
        );
        setEmojiBackground(
            (Spannable) mSpannable.subSequence(mSelectionInfo.mStart, mSelectionInfo.mEnd),
            mSelectedColor
        );
        setEmojiBackground(
            (Spannable) mSpannable.subSequence(mSelectionInfo.mEnd, mSpannable.length()),
            Color.TRANSPARENT
        );
    }

    private void setEmojiBackground(Spannable spannable, @ColorInt int bgColor) {
        if (TextUtils.isEmpty(spannable)) {
            return;
        }
        Object mSpansObj = SelectUtils.getFieldValue(spannable, "mSpans");
        if (mSpansObj != null && mSpansObj instanceof Object[]) {
            Object[] mSpans = (Object[]) mSpansObj;
            for (Object mSpan : mSpans) {
                if (mSpan instanceof SelectImageSpan) {
                    if (((SelectImageSpan) mSpan).bgColor != bgColor) {
                        ((SelectImageSpan) mSpan).bgColor = bgColor;
                    }
                }
            }
        }
    }

    private void resetEmojiBackground() {
        if (mSpannable != null) {
            setEmojiBackground(mSpannable, Color.TRANSPARENT);
        }
    }

    public abstract static class OnSelectListenerImpl implements OnSelectListener {
        @Override
        public void onClick(View v, CharSequence originalContent) {
        }

        @Override
        public void onLongClick(View v) {
        }

        @Override
        public void onTextSelected(CharSequence content) {
        }

        @Override
        public void onDismiss() {
        }

        @Override
        public void onClickUrl(String url) {
        }

        @Override
        public void onSelectAllShowCustomPop() {
        }

        @Override
        public void onReset() {
        }

        @Override
        public void onDismissCustomPop() {
        }

        @Override
        public void onScrolling() {
        }


    }

    public interface OnSelectListener {
        void onClick(View v, CharSequence originalContent);

        void onLongClick(View v);

        void onTextSelected(CharSequence content);

        void onDismiss();

        void onClickUrl(String url);

        void onSelectAllShowCustomPop();

        void onReset();

        void onDismissCustomPop();

        void onScrolling();

        PopupMenu newPopupMenu();
    }

    public static class Builder {
        public TextView mTextView;
        public int mCursorHandleColor = 0xffffff - 0xec862a;
        public int mSelectedColor = 0xffffff - 0x501e0c;
        public float mCursorHandleSizeInDp = 24f;
        public boolean mSelectAll = true;
        public boolean mSelectedAllNoPop = false;
        public int mSelectTextLength = DEFAULT_SELECTION_LENGTH;
        public boolean mScrollShow = true;
        public boolean mMagnifierShow = true;
        public int mPopSpanCount = 5;
        public int mPopBgResource = 0;
        public int mPopDelay = DEFAULT_SHOW_DURATION;
        public int mPopAnimationStyle = 0;
        public int mPopArrowImg = 0;
        public List<Pair<Integer, String>> itemTextList = new LinkedList<>();
        public List<onSeparateItemClickListener> itemListenerList = new LinkedList<>();

        public Builder(TextView mTextView) {
            this.mTextView = mTextView;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public Builder setSelectAll(boolean selectAll) {
            mSelectAll = selectAll;
            return this;
        }

        public Builder setSelectedAllNoPop(boolean selectedAllNoPop) {
            mSelectedAllNoPop = selectedAllNoPop;
            return this;
        }

        public Builder setSelectTextLength(int selectTextLength) {
            mSelectTextLength = selectTextLength;
            return this;
        }

        public Builder setScrollShow(boolean scrollShow) {
            mScrollShow = scrollShow;
            return this;
        }

        public Builder setMagnifierShow(boolean magnifierShow) {
            mMagnifierShow = magnifierShow;
            return this;
        }

        public Builder setPopSpanCount(int popSpanCount) {
            mPopSpanCount = popSpanCount;
            return this;
        }

        public Builder setPopStyle(int popBgResource, int popArrowImg) {
            mPopBgResource = popBgResource;
            mPopArrowImg = popArrowImg;
            return this;
        }

        public Builder setPopDelay(int popDelay) {
            mPopDelay = popDelay;
            return this;
        }

        public Builder setPopAnimationStyle(int popAnimationStyle) {
            mPopAnimationStyle = popAnimationStyle;
            return this;
        }

        public Builder addItem(
            @DrawableRes int drawableId,
            @StringRes int textResId,
            onSeparateItemClickListener listener
        ) {
            itemTextList.add(new Pair<>(drawableId, mTextView.getContext().getResources().getString(textResId)));
            itemListenerList.add(listener);
            return this;
        }

        public Builder addItem(
            @DrawableRes int drawableId,
            String itemText,
            onSeparateItemClickListener listener
        ) {
            itemTextList.add(new Pair<>(drawableId, itemText));
            itemListenerList.add(listener);
            return this;
        }

        public Builder addItem(
            @StringRes int textResId,
            onSeparateItemClickListener listener
        ) {
            itemTextList.add(new Pair<>(0, mTextView.getContext().getResources().getString(textResId)));
            itemListenerList.add(listener);
            return this;
        }

        public Builder addItem(String itemText, onSeparateItemClickListener listener) {
            itemTextList.add(new Pair<>(0, itemText));
            itemListenerList.add(listener);
            return this;
        }

        public SelectTextHelper build() {
            return new SelectTextHelper(this);
        }

        public interface onSeparateItemClickListener {
            void onClick();
        }
    }

    private class CursorHandle extends View {
        public boolean isLeft;
        private Paint mPaint;
        private PopupWindow mPopupWindow;
        private int mCircleRadius;
        private int mWidth;
        private int mHeight;
        private int mPadding = 32;

        private int mAdjustX = 0;
        private int mAdjustY = 0;
        private int mBeforeDragStart = 0;
        private int mBeforeDragEnd = 0;
        private int[] mTempCoors = new int[2];

        CursorHandle(boolean isLeft) {
            super(mContext);
            this.isLeft = isLeft;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCircleRadius = mCursorHandleSize / 2;
            mWidth = mCursorHandleSize;
            mHeight = mCursorHandleSize;
            mPaint.setColor(mCursorHandleColor);
            mPopupWindow = new PopupWindow(this);
            mPopupWindow.setClippingEnabled(false);
            mPopupWindow.setWidth(mWidth + mPadding * 2);
            mPopupWindow.setHeight(mHeight + mPadding / 2);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(
                mCircleRadius + mPadding,
                mCircleRadius,
                mCircleRadius,
                mPaint
            );
            if (isLeft) {
                canvas.drawRect(
                    mCircleRadius + mPadding,
                    0,
                    mCircleRadius * 2 + mPadding,
                    mCircleRadius,
                    mPaint
                );
            } else {
                canvas.drawRect(
                    mPadding,
                    0,
                    mCircleRadius + mPadding,
                    mCircleRadius,
                    mPaint
                );
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 触摸手柄时设置拖动标志，防止 dismiss listener 中调用 reset
                    isDraggingHandle = true;
                    mBeforeDragStart = mSelectionInfo.mStart;
                    mBeforeDragEnd = mSelectionInfo.mEnd;
                    mAdjustX = (int) event.getX();
                    mAdjustY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
//                    showOperateWindow();
                    showPopupMenu();
                    if (mMagnifierShow) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && mMagnifier != null) {
                            mMagnifier.dismiss();
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mPopupMenu != null) {
                        mPopupMenu.dismiss();
                    }
                    if (mSelectListener != null) {
                        mSelectListener.onDismissCustomPop();
                    }
                    int rawX = (int) event.getRawX();
                    int rawY = (int) event.getRawY();
                    update(
                        rawX + mAdjustX - mWidth - mTextViewMarginStart,
                        rawY + mAdjustY - mHeight - (int) mTextView.getTextSize()
                    );
                    if (mMagnifierShow) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            if (mMagnifier == null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    mMagnifier = new Magnifier.Builder(mTextView).build();
                                } else {
                                    mMagnifier = new Magnifier(mTextView);
                                }
                            }
                            int[] viewPosition = new int[2];
                            mTextView.getLocationOnScreen(viewPosition);
                            int magnifierX = rawX - viewPosition[0];
                            int magnifierY = rawY - viewPosition[1] - SelectUtils.dp2px(32f);
                            mMagnifier.show(
                                magnifierX,
                                Math.max(0, magnifierY)
                            );
                        }
                    }
                    break;
            }
            return true;
        }

        private void changeDirection() {
            isLeft = !isLeft;
            invalidate();
        }

        void dismiss() {
            mPopupWindow.dismiss();
        }

        void update(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int oldOffset = isLeft ? mSelectionInfo.mStart : mSelectionInfo.mEnd;
            y -= mTempCoors[1];
            int offset = SelectUtils.getHysteresisOffset(mTextView, x, y, oldOffset);
            if (offset != oldOffset) {
                resetSelectionInfo();
                if (isLeft) {
                    if (offset > mBeforeDragEnd) {
                        CursorHandle handle = getCursorHandle(false);
                        changeDirection();
                        if (handle != null) {
                            handle.changeDirection();
                        }
                        mBeforeDragStart = mBeforeDragEnd;
                        selectText(mBeforeDragEnd, offset);
                        if (handle != null) {
                            handle.updateCursorHandle();
                        }
                    } else {
                        selectText(offset, -1);
                    }
                    updateCursorHandle();
                } else {
                    if (offset < mBeforeDragStart) {
                        CursorHandle handle = getCursorHandle(true);
                        if (handle != null) {
                            handle.changeDirection();
                        }
                        changeDirection();
                        mBeforeDragEnd = mBeforeDragStart;
                        selectText(offset, mBeforeDragStart);
                        if (handle != null) {
                            handle.updateCursorHandle();
                        }
                    } else {
                        selectText(mBeforeDragStart, offset);
                    }
                    updateCursorHandle();
                }
            }
        }

        private void updateCursorHandle() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            if (isLeft) {
                mPopupWindow.update(
                    (int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) - mWidth + getExtraX(),
                    layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mStart)) + getExtraY(),
                    -1,
                    -1
                );
            } else {
                int horizontalEnd = (int) layout.getPrimaryHorizontal(mSelectionInfo.mEnd);
                int lineBottomEnd = layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mEnd));
                if (mSelectionInfo.mEnd != 0 && horizontalEnd == 0) {
                    horizontalEnd = (int) layout.getLineRight(layout.getLineForOffset(mSelectionInfo.mEnd - 1));
                    lineBottomEnd = layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mEnd - 1));
                }
                mPopupWindow.update(horizontalEnd + getExtraX(), lineBottomEnd + getExtraY(), -1, -1);
            }
        }

        void show(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int offset = isLeft ? mWidth : 0;
            mPopupWindow.showAtLocation(
                mTextView, Gravity.NO_GRAVITY, x - offset + getExtraX(), y + getExtraY()
            );
        }

        private int getExtraX() {
            return mTempCoors[0] - mPadding + mTextView.getPaddingLeft();
        }

        private int getExtraY() {
            return mTempCoors[1] + mTextView.getPaddingTop();
        }
    }

    private CursorHandle getCursorHandle(boolean isLeft) {
        if (mStartHandle != null && mStartHandle.isLeft == isLeft) {
            return mStartHandle;
        } else {
            return mEndHandle;
        }
    }

    private static class SelectionInfo {
        int mStart = 0;
        int mEnd = 0;
        CharSequence mSelectionContent;
    }

    private class LinkMovementMethodInterceptor extends LinkMovementMethod {
        private long downLinkTime = 0;

        @Override
        public boolean onTouchEvent(
            TextView widget,
            Spannable buffer,
            MotionEvent event
        ) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();
                x += widget.getScrollX();
                y += widget.getScrollY();
                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);
                if (links.length > 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        if (downLinkTime + ViewConfiguration.getLongPressTimeout() < System.currentTimeMillis()) {
                            return false;
                        }
                        if (links[0] instanceof URLSpan) {
                            String url = ((URLSpan) links[0]).getURL();
                            if (!TextUtils.isEmpty(url)) {
                                if (mSelectListener != null) {
                                    usedClickListener = true;
                                    mSelectListener.onClickUrl(url);
                                }
                                return true;
                            } else {
                                links[0].onClick(widget);
                            }
                        }
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        downLinkTime = System.currentTimeMillis();
                        Selection.setSelection(
                            buffer, buffer.getSpanStart(links[0]), buffer.getSpanEnd(links[0])
                        );
                    }
                    return true;
                } else {
                    Selection.removeSelection(buffer);
                }
            }
            return super.onTouchEvent(widget, buffer, event);
        }
    }
}
