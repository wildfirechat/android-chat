/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;

// 代码拷贝自，在此表示感谢 https://github.com/tinyvampirepudge/BreadCrumbsView

/**
 * @Description: 面包屑
 * @Author wangjianzhou
 * @Date 2020/5/26 3:05 PM
 * @Version
 */
public class BreadCrumbsView extends FrameLayout {
    private Context mContext;
    private LinkedList<Tab> tabList = new LinkedList<>();
    private RecyclerView recyclerView;
    private TabAdapter tabAdapter;
    private OnTabListener onTabListener;

    public BreadCrumbsView(Context context) {
        super(context);
        init(context);
    }

    public BreadCrumbsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BreadCrumbsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BreadCrumbsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        View view = View.inflate(context, R.layout.layout_breadcrumbs_container, null);
        recyclerView = view.findViewById(R.id.breadCrumbsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
//        float maxWidth = (ScreenUtil.getScreenWidth(mContext) - ScreenUtil.dip2px(mContext, 32)) / 3.0f - ScreenUtil.dip2px(mContext, 8);
        float maxWidth = 64;
        tabAdapter = new TabAdapter(mContext, tabList, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = (int) v.getTag();
                selectAt(index);
            }
        }, maxWidth);
        recyclerView.setAdapter(tabAdapter);
        addView(view);
    }

    /**
     * 添加新的面包屑
     *
     * @param tab
     */
    private void addTab(Tab tab) {
        // 修改tab的状态
        if (!tabList.isEmpty()) {
            tabList.getLast().setCurrent(false);
        }
        // 设置新增tab的数据及状态
        // 设置角标
        tab.setIndex(getCurrentIndex() + 1);
        // 设置状态
        tab.setCurrent(true);
        tabList.add(tab);
        if (onTabListener != null) {
            onTabListener.onAdded(tab);
        }
        tabAdapter.notifyDataSetChanged();
    }

    public void clearAllTab() {
        tabList.clear();
        tabAdapter.notifyDataSetChanged();
    }

    /**
     * 添加新的面包屑
     *
     * @param content
     */
    public void addTab(String content, Map<String, Object> value) {
        Tab tab = new Tab();
        tab.setContent(content);
        tab.setValue(value);
        addTab(tab);
        recyclerView.scrollToPosition(tabList.size() - 1);
    }

    /**
     * 非active状态的Tab支持选中事件
     *
     * @param index
     */
    public void selectAt(int index) {
        // 判断角标越界，当前的tab不可点击
        if (!tabList.isEmpty() && tabList.size() > index && !tabList.get(index).isCurrent()) {
            // 移除后面的tab数据
            int size = tabList.size();
            // 从尾部开始删除
            for (int i = size - 1; i > index; i--) {
                if (onTabListener != null) {
                    onTabListener.onRemoved(tabList.getLast());
                }
                tabList.removeLast();
            }
            // 修改当前选中的tab状态
            tabList.getLast().setCurrent(true);
            if (onTabListener != null) {
                onTabListener.onActivated(tabList.getLast());
            }
            tabAdapter.notifyDataSetChanged();
        }
    }

    public int getCurrentIndex() {
        return tabList.size() - 1;
    }

    public void setOnTabListener(OnTabListener onTabListener) {
        this.onTabListener = onTabListener;
    }

    public Tab getLastTab() {
        if (!tabList.isEmpty()) {
            return tabList.getLast();
        }
        return null;
    }

    public static class Tab {

        private Context mContext;
        /**
         * 是否当前tab，非当前tab才可点击
         */
        private boolean current = false;
        /**
         * 角标，从0开始
         */
        private int index;
        /**
         * 面包屑内容
         */
        private String content;

        /**
         * 业务携带的参数
         */
        private Map<String, Object> value;

        public boolean isCurrent() {
            return current;
        }

        public void setCurrent(boolean current) {
            this.current = current;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getValue() {
            return value;
        }

        public void setValue(Map<String, Object> value) {
            this.value = value;
        }
    }

    private static class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> {
        private Context mContext;
        private List<Tab> list;
        private OnClickListener onItemClickListener;
        private float maxItemWidth;

        public TabAdapter(Context mContext, List<Tab> list, OnClickListener onItemClickListener, float maxItemWidth) {
            this.mContext = mContext;
            this.list = list;
            this.onItemClickListener = onItemClickListener;
            this.maxItemWidth = maxItemWidth;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.layout_breadcrumbs_tab, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tab item = list.get(position);
            holder.tv.setText(item.getContent());
            if (item.isCurrent()) {
                holder.tv.setTextColor(Color.parseColor("#3B62E0"));
                holder.iv.setVisibility(GONE);
                holder.itemView.setOnClickListener(null);
            } else {
                holder.tv.setTextColor(Color.parseColor("#485666"));
                holder.iv.setVisibility(VISIBLE);
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onItemClickListener != null) {
                            v.setTag(position);
                            onItemClickListener.onClick(v);
                        }
                    }
                });
            }
            if (maxItemWidth > 0) {
                int width;
                if (item.isCurrent()) {
                    width = (int) maxItemWidth;
                } else {
                    //width = (int) maxItemWidth - ScreenUtil.dip2px(mContext, 24);
                    width = (int) maxItemWidth - 24;
                }
//                holder.tv.setMaxWidth(width);
            }
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv;
            private ImageView iv;

            public ViewHolder(View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tv_content);
                iv = itemView.findViewById(R.id.iv_arrow);
            }
        }
    }

    public interface OnTabListener {
        /**
         * 新添加进来的回调
         *
         * @param tab
         */
        void onAdded(Tab tab);

        /**
         * 再次显示到栈顶的回调
         *
         * @param tab
         */
        void onActivated(Tab tab);

        /**
         * 被移除的回调
         *
         * @param tab
         */
        void onRemoved(Tab tab);
    }
}
