package com.lqr.emoji;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

/**
 * ViewPager2 的适配器，用于管理不同类别的表情内容
 */
public class EmotionViewPager2Adapter extends RecyclerView.Adapter<EmotionViewPager2Adapter.ViewHolder> {

    private List<Integer> tabIndices = new ArrayList<>();
    private Context mContext;
    private IEmotionSelectedListener mListener;
    private boolean mStickerVisible;

    public EmotionViewPager2Adapter(Context context, boolean stickerVisible, IEmotionSelectedListener listener) {
        this.mContext = context;
        this.mStickerVisible = stickerVisible;
        this.mListener = listener;
        initTabIndices();
    }

    /**
     * 初始化标签索引
     */
    private void initTabIndices() {
        tabIndices.clear();

        // 添加 emoji 标签
        tabIndices.add(0);

        // 添加贴图标签
        if (mStickerVisible) {
            List<StickerCategory> categories = StickerManager.getInstance().getStickerCategories();
            for (int i = 0; i < categories.size(); i++) {
                tabIndices.add(i + 1);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.page_emotion_category, parent, false);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(layoutParams);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int tabIndex = tabIndices.get(position);

        // 设置 RecyclerView
        GridLayoutManager layoutManager = new GridLayoutManager(mContext,
            tabIndex == 0 ? EmotionLayout.EMOJI_COLUMNS : EmotionLayout.STICKER_COLUMNS);
        holder.recyclerView.setLayoutManager(layoutManager);
        OverScrollDecoratorHelper.setUpOverScroll(holder.recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        // 创建并设置表情适配器
        EmotionRecyclerAdapter adapter = new EmotionRecyclerAdapter(mContext, mStickerVisible, mListener);
        holder.recyclerView.setAdapter(adapter);

        // 加载内容
        adapter.loadTabContent(tabIndex);
    }

    @Override
    public int getItemCount() {
        return tabIndices.size();
    }

    /**
     * 获取位置对应的标签索引
     */
    public int getTabIndex(int position) {
        if (position >= 0 && position < tabIndices.size()) {
            return tabIndices.get(position);
        }
        return 0;
    }

    /**
     * 获取标签索引对应的位置
     */
    public int findPositionByTabIndex(int tabIndex) {
        for (int i = 0; i < tabIndices.size(); i++) {
            if (tabIndices.get(i) == tabIndex) {
                return i;
            }
        }
        return 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recyclerView_category);
        }
    }
}