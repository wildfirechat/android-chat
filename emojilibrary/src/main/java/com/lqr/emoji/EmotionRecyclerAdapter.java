package com.lqr.emoji;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.emoji2.widget.EmojiTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 表情控件的RecyclerView适配器，实现垂直滑动切换表情页
 */
public class EmotionRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements EmojiPreviewPopup.OnEmojiChangeListener {

    private static final String TAG = "EmotionRecyclerAdapter";
    private static final int VIEW_TYPE_EMOJI = 0;
    private static final int VIEW_TYPE_STICKER = 1;

    // 当前显示的表情项
    private List<EmotionItem> emotionItems = new ArrayList<>();
    private IEmotionSelectedListener listener;
    private Context mContext;
    private EmojiPreviewPopup mPreviewPopup;
    private boolean mIsPreviewMode = false; // 是否处于预览模式
    private RecyclerView mRecyclerView; // 保存 RecyclerView 引用
    private View mPressedView; // 当前处于 pressed 状态的视图

    /**
     * 表情项类，用于存储单个表情项的数据
     */
    public static class EmotionItem {
        public int type; // 0: emoji, 1: sticker
        public int index; // 表情索引
        public String category; // 贴图类别
        public String name; // 贴图名称
        public int pageIndex; // 页索引

        public EmotionItem(int type, int index, int pageIndex) {
            this.type = type;
            this.index = index;
            this.pageIndex = pageIndex;
        }

        public EmotionItem(int type, String category, String name, int pageIndex) {
            this.type = type;
            this.category = category;
            this.name = name;
            this.pageIndex = pageIndex;
        }
    }

    /**
     * 初始化适配器
     */
    public EmotionRecyclerAdapter(Context context, boolean stickerVisible, IEmotionSelectedListener listener) {
        this.mContext = context;
        this.listener = listener;
        this.mPreviewPopup = new EmojiPreviewPopup(context);
        this.mPreviewPopup.setOnEmojiChangeListener(this);

        // 初始化显示emoji表情页（默认选中第一个类别）
        loadTabContent(0);
    }

    /**
     * 设置 RecyclerView 引用（用于检测手指下的 item）
     */
    public void setRecyclerView(RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;
    }

    /**
     * 根据选择的标签索引加载对应类别的内容
     */
    public void loadTabContent(int tabIndex) {
        emotionItems.clear();

        if (tabIndex == 0) {
            // 加载emoji表情
            int emojiCount = EmojiManager.getDisplayCount();

            // 添加所有emoji，不添加删除按钮（删除按钮会在EmotionLayout中单独添加）
            for (int i = 0; i < emojiCount; i++) {
                emotionItems.add(new EmotionItem(VIEW_TYPE_EMOJI, i, 0));
            }

        } else {
            // 加载贴图页
            List<StickerCategory> categories = StickerManager.getInstance().getStickerCategories();
            if (tabIndex - 1 < categories.size()) {
                StickerCategory category = categories.get(tabIndex - 1);
                List<StickerItem> stickers = category.getStickers();

                // 添加所有贴图
                for (int i = 0; i < stickers.size(); i++) {
                    StickerItem sticker = stickers.get(i);
                    emotionItems.add(new EmotionItem(
                        VIEW_TYPE_STICKER,
                        sticker.getCategory(),
                        sticker.getName(),
                        0));
                }
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return emotionItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMOJI) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_emoji, parent, false);
            EmojiViewHolder viewHolder = new EmojiViewHolder(view);

            // 整个项目视图处理点击事件
            view.setOnClickListener(v -> {
                if (listener != null && !mIsPreviewMode) {
                    int position = viewHolder.getBindingAdapterPosition();
                    String hexCode = EmojiManager.getDisplayText(position);
                    if (!TextUtils.isEmpty(hexCode)) {
                        // 传入十六进制字符串以保持兼容性
                        // ConversationInputPanel 会将其转换为 Unicode 字符
                        listener.onEmojiSelected(hexCode);
                        Log.d(TAG, "Emoji selected: " + hexCode);
                    }
                }
            });

            // 长按显示预览
            view.setOnLongClickListener(v -> {
                int position = viewHolder.getBindingAdapterPosition();
                mIsPreviewMode = true;
                // 设置 pressed 状态
                setPressedView(v);
                // 使用 item 视图的位置，而不是跟随手指
                mPreviewPopup.show(v, position, true);
                // 长按后，父视图不要拦截触摸事件
                if (v.getParent() != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            });

            // 触摸事件处理
            view.setOnTouchListener(new EmotionTouchListener(viewHolder, true));

            return viewHolder;
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_sticker, parent, false);
            StickerViewHolder viewHolder = new StickerViewHolder(view);

            // 整个项目视图处理点击事件
            view.setOnClickListener(v -> {
                if (listener != null && !mIsPreviewMode) {
                    final EmotionItem item = emotionItems.get(viewHolder.getBindingAdapterPosition());
                    StickerCategory real = StickerManager.getInstance().getCategory(item.category);
                    if (real != null) {
                        listener.onStickerSelected(
                            item.category,
                            item.name,
                            StickerManager.getInstance().getStickerBitmapPath(item.category, item.name)
                        );
                        Log.d(TAG, "Sticker selected: " + item.category + "/" + item.name);
                    }
                }
            });

            // 长按显示预览
            view.setOnLongClickListener(v -> {
                int position = viewHolder.getBindingAdapterPosition();
                mIsPreviewMode = true;
                // 设置 pressed 状态
                setPressedView(v);
                // 使用 item 视图的位置，而不是跟随手指
                mPreviewPopup.show(view, position, false);
                // 长按后，父视图不要拦截触摸事件
                if (v.getParent() != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            });

            // 触摸事件处理
            view.setOnTouchListener(new EmotionTouchListener(viewHolder, false));

            return viewHolder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= emotionItems.size()) return;

        final EmotionItem item = emotionItems.get(position);

        if (holder instanceof EmojiViewHolder) {
            EmojiViewHolder emojiHolder = (EmojiViewHolder) holder;

            // 获取 emoji 的 Unicode 码点字符串（如 "0x1f603"）
            String hexCode = EmojiManager.getDisplayText(item.index);
            if (!TextUtils.isEmpty(hexCode)) {
                // 将十六进制字符串转换为 Unicode 字符
                String emojiChar = hexToEmoji(hexCode);
                emojiHolder.textView.setText(emojiChar);
            }

        } else if (holder instanceof StickerViewHolder) {
            StickerViewHolder stickerHolder = (StickerViewHolder) holder;

            // 设置贴图
            String stickerBitmapUri = StickerManager.getInstance().getStickerBitmapUri(item.category, item.name);
            LQREmotionKit.getImageLoader().displayImage(mContext, stickerBitmapUri, stickerHolder.imageView);

        }
    }

    /**
     * 将十六进制码点字符串转换为 Unicode emoji 字符
     * @param hexCode 十六进制字符串，如 "0x1f603"
     * @return Unicode emoji 字符
     */
    private String hexToEmoji(String hexCode) {
        try {
            // 移除 "0x" 前缀（如果存在）
            String hex = hexCode.replace("0x", "");
            // 解析为整数，然后转换为 Unicode 字符
            int codePoint = Integer.parseInt(hex, 16);
            return new String(Character.toChars(codePoint));
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert hex to emoji: " + hexCode, e);
            return "";
        }
    }

    private void setupItemClickListener(View view, Runnable action) {
        view.setOnClickListener(v -> {
            action.run();
        });
    }

    @Override
    public int getItemCount() {
        return emotionItems.size();
    }

    /**
     * Emoji表情的ViewHolder
     */
    static class EmojiViewHolder extends RecyclerView.ViewHolder {
        EmojiTextView textView;

        public EmojiViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.iv_emoji);
        }
    }

    /**
     * 贴图的ViewHolder
     */
    static class StickerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public StickerViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_sticker);
        }
    }

    // ==================== EmojiPreviewPopup.OnEmojiChangeListener 实现 ====================

    @Override
    public String getEmojiAt(int position) {
        if (position < 0 || position >= emotionItems.size()) {
            return null;
        }

        EmotionItem item = emotionItems.get(position);

        if (item.type == VIEW_TYPE_EMOJI) {
            // 返回 Unicode emoji 字符
            String hexCode = EmojiManager.getDisplayText(item.index);
            return hexToEmoji(hexCode);
        } else {
            // 返回贴图路径
            return StickerManager.getInstance().getStickerBitmapUri(item.category, item.name);
        }
    }

    @Override
    public int getCurrentPosition() {
        return 0; // 未使用，保留接口兼容性
    }

    @Override
    public boolean isEmoji(int position) {
        if (position < 0 || position >= emotionItems.size()) {
            return false;
        }
        return emotionItems.get(position).type == VIEW_TYPE_EMOJI;
    }

    /**
     * 表情触摸监听器，处理长按后滑动切换预览
     */
    private class EmotionTouchListener implements View.OnTouchListener {
        private RecyclerView.ViewHolder mInitialHolder;
        private boolean mIsEmoji;
        private int mCurrentPosition = -1;

        EmotionTouchListener(RecyclerView.ViewHolder holder, boolean isEmoji) {
            this.mInitialHolder = holder;
            this.mIsEmoji = isEmoji;
            this.mCurrentPosition = holder.getBindingAdapterPosition();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!mIsPreviewMode) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    // 检测手指下的 item
                    if (mRecyclerView != null) {
                        // 将触摸坐标转换为相对于 RecyclerView 的坐标
                        int[] location = new int[2];
                        mRecyclerView.getLocationOnScreen(location);
                        float x = event.getRawX() - location[0];
                        float y = event.getRawY() - location[1];

                        View childUnder = mRecyclerView.findChildViewUnder(x, y);
                        if (childUnder != null) {
                            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(childUnder);
                            if (holder != null) {
                                int position = holder.getBindingAdapterPosition();
                                if (position != RecyclerView.NO_POSITION && position != mCurrentPosition) {
                                    // 手指移动到了另一个 item 上，更新预览
                                    mCurrentPosition = position;
                                    // 清除旧的 pressed 状态
                                    clearPressedView();
                                    // 设置新的 pressed 状态
                                    setPressedView(childUnder);
                                    // 使用新 item 的视图位置，而不是手指位置
                                    mPreviewPopup.show(childUnder, position, mIsEmoji);
                                }
                            }
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsPreviewMode = false;
                    mPreviewPopup.dismiss();
                    // 清除 pressed 状态
                    clearPressedView();
                    // 恢复父视图拦截
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    mCurrentPosition = mInitialHolder.getBindingAdapterPosition();
                    return true;
            }
            return true;
        }
    }

    /**
     * 设置视图的 pressed 状态
     */
    private void setPressedView(View view) {
        if (view != null) {
            view.setPressed(true);
            mPressedView = view;
        }
    }

    /**
     * 清除当前 pressed 状态
     */
    private void clearPressedView() {
        if (mPressedView != null) {
            mPressedView.setPressed(false);
            mPressedView = null;
        }
    }
}