package cn.wildfire.chat.kit.channel;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class CategoryAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    @NonNull
    @Override
    final public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    final public void onBindViewHolder(@NonNull VH holder, int position) {

    }

    @Override
    final public int getItemCount() {
        int categoryCount = getCategoryCount();
        int contentItemCount = 0;
        for (int i = 0; i < categoryCount; i++) {
            contentItemCount += getCategoryContentItemCount(i);
        }
        return categoryCount + contentItemCount;
    }

    @Override
    final public int getItemViewType(int position) {
        return 0;
    }


    protected abstract int getCategoryCount();

    protected abstract int getCategoryViewType(int categoryPosition);

    protected abstract VH onCreateCategoryViewHolder(ViewGroup parent, int categoryViewType);

    protected abstract void onBindCategoryViewHolder(VH holder, int categoryPosition);


    protected abstract int getCategoryContentItemCount(int categoryPosition);

    protected abstract int getCategoryContentItemViewType(int categoryContentItemPosition);

    protected abstract VH onCreateCategoryContentItemViewHolder(ViewGroup parent, int categoryContentItemPosition);

    protected abstract void onBindCategoryContentItemViewHolder(VH holder, int categoryContentItemPosition);


    // TODO
    //public void notifyCategorxx();
}
