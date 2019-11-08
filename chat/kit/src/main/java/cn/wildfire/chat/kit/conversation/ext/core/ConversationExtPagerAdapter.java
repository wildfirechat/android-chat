package cn.wildfire.chat.kit.conversation.ext.core;

import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class ConversationExtPagerAdapter extends PagerAdapter {
    private SparseArray<ConversationExtPageView> pagers = new SparseArray<>();
    private List<ConversationExt> exts;
    private ConversationExtPageView.OnExtViewClickListener listener;

    public ConversationExtPagerAdapter(List<ConversationExt> exts, ConversationExtPageView.OnExtViewClickListener listener) {
        this.exts = exts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ConversationExtPageView view = pagers.get(position);
        if (view == null) {
            view = new ConversationExtPageView(container.getContext());
            view.setPageIndex(position);
            view.setOnExtViewClickListener(listener);
            int startIndex = ConversationExtPageView.EXT_PER_PAGE * position;
            int end = startIndex + ConversationExtPageView.EXT_PER_PAGE > exts.size() ? exts.size() : startIndex + ConversationExtPageView.EXT_PER_PAGE;
            view.updateExtViews(exts.subList(startIndex, end));

            container.addView(view);
            pagers.put(position, view);
        }
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return exts == null ? 0 : (exts.size() + 7) / 8;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
