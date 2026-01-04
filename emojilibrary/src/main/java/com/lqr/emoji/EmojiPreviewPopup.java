package com.lqr.emoji;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.emoji2.widget.EmojiTextView;

/**
 * 表情预览弹窗，支持滑动预览相邻表情
 */
public class EmojiPreviewPopup {

    public interface OnEmojiChangeListener {
        /**
         * 获取指定位置的 emoji 字符
         * @param position 位置索引
         * @return emoji 字符（Unicode 字符串）或贴图路径
         */
        String getEmojiAt(int position);

        /**
         * 获取当前位置
         * @return 位置索引
         */
        int getCurrentPosition();

        /**
         * 检查是否为 emoji（false 表示为贴图）
         * @param position 位置索引
         */
        boolean isEmoji(int position);
    }

    private PopupWindow mPopupWindow;
    private EmojiTextView mEmojiTextView;
    private ImageView mStickerView;
    private Context mContext;
    private View mAnchorView;
    private OnEmojiChangeListener mChangeListener;
    private int mCurrentPosition = -1;

    // 滑动相关
    private float mDownX;
    private float mDownY;
    private static final float SWIPE_THRESHOLD = 50; // 滑动阈值（像素）
    private boolean mIsSwipeMode = false;

    public EmojiPreviewPopup(Context context) {
        this.mContext = context;
        initPopup();
    }

    private void initPopup() {
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.popup_emoji_preview, null);
        mEmojiTextView = contentView.findViewById(R.id.tv_emoji_preview);
        mStickerView = contentView.findViewById(R.id.iv_sticker_preview);

        mPopupWindow = new PopupWindow(contentView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true); // focusable = true，获取焦点以拦截触摸事件

        mPopupWindow.setOutsideTouchable(false); // 不允许点击外部关闭，防止误触
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.setElevation(16f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mPopupWindow.setTouchModal(true); // 拦截所有触摸事件
        }

        // 设置触摸监听，处理滑动
        contentView.setOnTouchListener(this::onPopupTouch);
    }

    /**
     * 获取弹窗尺寸（dp）
     */
    private int[] getPopupSize(boolean isEmoji) {
        if (isEmoji) {
            // emoji 预览较小
            return new int[]{120, 120}; // 宽度, 高度
        } else {
            // sticker 预览较大
            return new int[]{200, 200}; // 宽度, 高度
        }
    }

    /**
     * 获取文本大小（sp）
     */
    private int getTextSize(boolean isEmoji) {
        return isEmoji ? 60 : 140; // emoji 较小，sticker 较大
    }

    public void setOnEmojiChangeListener(OnEmojiChangeListener listener) {
        this.mChangeListener = listener;
    }

    /**
     * 显示 emoji 预览
     * @param anchor 锚点视图
     * @param position 位置索引
     * @param isEmoji 是否为 emoji（false 为贴图）
     */
    public void show(View anchor, int position, boolean isEmoji) {
        show(anchor, position, isEmoji, -1, -1);
    }

    /**
     * 显示 emoji 预览，支持指定位置
     * @param anchor 锚点视图
     * @param position 位置索引
     * @param isEmoji 是否为 emoji（false 为贴图）
     * @param rawX 手指在屏幕上的 X 坐标（-1 表示使用 anchor 位置）
     * @param rawY 手指在屏幕上的 Y 坐标（-1 表示使用 anchor 位置）
     */
    public void show(View anchor, int position, boolean isEmoji, float rawX, float rawY) {
        this.mAnchorView = anchor;
        this.mCurrentPosition = position;
        this.mIsSwipeMode = true;

        updateContent(position, isEmoji);

        // 根据类型调整文本大小
        mEmojiTextView.setTextSize(getTextSize(isEmoji));

        if (!mPopupWindow.isShowing()) {
            mPopupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);
        }

        // 更新位置
        if (rawX >= 0 && rawY >= 0) {
            // 使用手指坐标
            updatePositionAt(rawX, rawY, isEmoji);
        } else {
            // 使用 anchor 位置
            updatePosition(anchor, isEmoji);
        }
    }

    /**
     * 更新显示内容
     */
    private void updateContent(int position, boolean isEmoji) {
        if (mChangeListener == null) {
            return;
        }

        String content = mChangeListener.getEmojiAt(position);
        if (content == null || content.isEmpty()) {
            return;
        }

        if (isEmoji) {
            mEmojiTextView.setVisibility(View.VISIBLE);
            mStickerView.setVisibility(View.GONE);
            mEmojiTextView.setText(content);
        } else {
            mEmojiTextView.setVisibility(View.GONE);
            mStickerView.setVisibility(View.VISIBLE);
            LQREmotionKit.getImageLoader().displayImage(mContext, content, mStickerView);
        }
    }

    /**
     * 处理弹窗触摸事件，实现滑动切换
     */
    private boolean onPopupTouch(View v, MotionEvent event) {
        if (!mIsSwipeMode || mChangeListener == null) {
            // 即使不在滑动模式，也消费所有触摸事件，防止传递到下层
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getRawX();
                mDownY = event.getRawY();
                return true; // 消费事件

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - mDownX;
                float deltaY = event.getRawY() - mDownY;

                // 判断是否为水平滑动
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                        // 左滑：显示下一个
                        if (deltaX < 0) {
                            showNextEmoji();
                        }
                        // 右滑：显示上一个
                        else {
                            showPreviousEmoji();
                        }
                        // 更新起始点，防止连续触发
                        mDownX = event.getRawX();
                        mDownY = event.getRawY();
                    }
                }
                return true; // 消费事件

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 抬起手指时关闭预览
                dismiss();
                return true; // 消费事件
        }
        return true; // 消费所有事件
    }

    /**
     * 显示下一个表情
     */
    private void showNextEmoji() {
        int nextPosition = mCurrentPosition + 1;
        boolean isEmoji = mChangeListener.isEmoji(nextPosition);

        if (mChangeListener.getEmojiAt(nextPosition) != null) {
            mCurrentPosition = nextPosition;
            updateContent(nextPosition, isEmoji);
        }
    }

    /**
     * 显示上一个表情
     */
    private void showPreviousEmoji() {
        int prevPosition = mCurrentPosition - 1;
        boolean isEmoji = mChangeListener.isEmoji(prevPosition);

        if (prevPosition >= 0 && mChangeListener.getEmojiAt(prevPosition) != null) {
            mCurrentPosition = prevPosition;
            updateContent(prevPosition, isEmoji);
        }
    }

    /**
     * 更新弹窗位置到指定坐标（手指位置）
     */
    private void updatePositionAt(float rawX, float rawY, boolean isEmoji) {
        if (!mPopupWindow.isShowing()) {
            return;
        }

        // 获取屏幕尺寸
        int screenWidth = mAnchorView.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = mAnchorView.getResources().getDisplayMetrics().heightPixels;

        // 弹窗尺寸（dp 转 px）
        int[] sizeDp = getPopupSize(isEmoji);
        float density = mAnchorView.getResources().getDisplayMetrics().density;
        int popupWidth = (int) (sizeDp[0] * density);
        int popupHeight = (int) (sizeDp[1] * density);

        // 计算位置：在手指上方居中
        int x = (int) rawX - popupWidth / 2;
        int y = (int) rawY - popupHeight - (int)(20 * density); // 手指上方 20dp

        // 确保不超出屏幕边界
        if (x < 0) x = 0;
        if (x + popupWidth > screenWidth) x = screenWidth - popupWidth;
        if (y < 0) {
            // 如果上方空间不足，显示在手指下方
            y = (int) rawY + (int)(20 * density);
        }
        if (y + popupHeight > screenHeight) y = screenHeight - popupHeight;

        mPopupWindow.update(x, y, popupWidth, popupHeight);
    }

    /**
     * 更新弹窗位置
     */
    private void updatePosition(View anchor, boolean isEmoji) {
        if (anchor == null || !mPopupWindow.isShowing()) {
            return;
        }

        // 获取锚点在屏幕上的位置
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);

        int anchorX = location[0];
        int anchorY = location[1];
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        // 获取屏幕尺寸
        int screenWidth = anchor.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = anchor.getResources().getDisplayMetrics().heightPixels;

        // 弹窗尺寸
        int[] sizeDp = getPopupSize(isEmoji);

        // 转换为像素
        float density = anchor.getResources().getDisplayMetrics().density;
        int popupWidth = (int) (sizeDp[0] * density);
        int popupHeight = (int) (sizeDp[1] * density);

        // 计算位置：在锚点上方居中，留出一定间距
        int x = anchorX + (anchorWidth - popupWidth) / 2;
        int y = anchorY - popupHeight - (int)(20 * density); // 20dp 间距

        // 确保不超出屏幕边界
        if (x < 0) x = 0;
        if (x + popupWidth > screenWidth) x = screenWidth - popupWidth;
        if (y < 0) {
            // 如果上方空间不足，显示在下方
            y = anchorY + anchorHeight + (int)(20 * density);
        }

        mPopupWindow.update(x, y, popupWidth, popupHeight);
    }

    /**
     * 隐藏弹窗
     */
    public void dismiss() {
        mIsSwipeMode = false;
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    /**
     * 检查是否正在显示
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }
}
