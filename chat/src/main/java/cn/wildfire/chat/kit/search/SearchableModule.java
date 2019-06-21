package cn.wildfire.chat.kit.search;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class SearchableModule<R, V extends RecyclerView.ViewHolder> {
    protected String keyword;
    private OnResultItemClickListener<R> listener;

    public abstract V onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int type);

    public abstract void onBind(Fragment fragment, V holder, R r);

    /**
     * 目前是作为一个整体来处理onClick事件的
     *
     * @param fragment
     * @param holder
     * @param view
     * @param r
     */
    public final void onClickInternal(Fragment fragment, V holder, View view, R r) {

        InputMethodManager inputManager = (InputMethodManager) fragment.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (listener != null) {
            listener.onResultItemClick(fragment, holder.itemView, view, r);
        } else {
            onClick(fragment, holder, view, r);
        }
    }

    public void onClick(Fragment fragment, V holder, View view, R r) {
        // do nothing
    }

    /**
     * -1, 0, 1 内部保留使用，所有的{@link SearchableModule} 之间，{@code viewType} 不能重复
     *
     * @param r one of the search results
     * @return
     */
    public abstract int getViewType(R r);

    public abstract int priority();

    public abstract String category();

    public final List<R> searchInternal(String keyword) {
        this.keyword = keyword;
        return search(keyword);
    }

    /**
     * 具体的搜索逻辑
     * <p>
     * 本方法在工作线程执行
     *
     * @param keyword
     * @return
     */
    public abstract List<R> search(String keyword);

    public void setOnResultItemListener(OnResultItemClickListener<R> listener) {
        this.listener = listener;
    }

    /**
     * @return true, 只展示4项，其他收起；false，不可展开, 全部直接展开
     * <p>
     * 可展示时，默认展示4项，其他的需要点击显示更多，才展示
     */
    public boolean expandable() {
        return true;
    }

    public static final int DEFAULT_SHOW_RESULT_ITEM_COUNT = 4;
}
