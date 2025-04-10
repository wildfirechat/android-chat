package com.lqr.emoji;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 表情控件的RecyclerView适配器，实现垂直滑动切换表情页
 */
public class EmotionRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "EmotionRecyclerAdapter";
    private static final int VIEW_TYPE_EMOJI = 0;
    private static final int VIEW_TYPE_STICKER = 1;

    // 当前显示的表情项
    private List<EmotionItem> emotionItems = new ArrayList<>();
    private IEmotionSelectedListener listener;
    private Context mContext;

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

        // 初始化显示emoji表情页（默认选中第一个类别）
        loadTabContent(0);
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
            RecyclerView.ViewHolder viewHolder = new EmojiViewHolder(view);

            // 整个项目视图处理点击事件
            view.setOnClickListener(v -> {
                if (listener != null) {
                    String text = EmojiManager.getDisplayText(viewHolder.getBindingAdapterPosition());
                    if (!TextUtils.isEmpty(text)) {
                        listener.onEmojiSelected(text);
                        Log.d(TAG, "Emoji selected: " + text);
                    }
                }
            });
            return viewHolder;
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_sticker, parent, false);
            StickerViewHolder viewHolder = new StickerViewHolder(view);
            // 整个项目视图处理点击事件
            view.setOnClickListener(v -> {
                if (listener != null) {
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
            return viewHolder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= emotionItems.size()) return;

        final EmotionItem item = emotionItems.get(position);

        if (holder instanceof EmojiViewHolder) {
            EmojiViewHolder emojiHolder = (EmojiViewHolder) holder;

            // 设置emoji表情
            emojiHolder.imageView.setBackground(EmojiManager.getDisplayDrawable(mContext, item.index));

        } else if (holder instanceof StickerViewHolder) {
            StickerViewHolder stickerHolder = (StickerViewHolder) holder;

            // 设置贴图
            String stickerBitmapUri = StickerManager.getInstance().getStickerBitmapUri(item.category, item.name);
            LQREmotionKit.getImageLoader().displayImage(mContext, stickerBitmapUri, stickerHolder.imageView);

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
        ImageView imageView;

        public EmojiViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_emoji);
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
}