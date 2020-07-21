package cn.wildfire.chat.kit.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.viewholder.footer.FooterViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;

public abstract class HeaderFooterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CONTENT = 1024;
    private static final int TYPE_FOOTER_START_INDEX = 2048;
    protected Fragment fragment;
    private List<Class<? extends HeaderViewHolder>> headerViewHolders;
    private List<Class<? extends FooterViewHolder>> footerViewHolders;
    private List<HeaderValue> headerValues;
    private List<FooterValue> footerValues;
    protected OnHeaderClickListener onHeaderClickListener;
    protected OnFooterClickListener onFooterClickListener;

    public HeaderFooterAdapter(Fragment fragment) {
        this.fragment = fragment;
    }


    public void setOnHeaderClickListener(OnHeaderClickListener onHeaderClickListener) {
        this.onHeaderClickListener = onHeaderClickListener;
    }

    public void setOnFooterClickListener(OnFooterClickListener onFooterClickListener) {
        this.onFooterClickListener = onFooterClickListener;
    }

    @NonNull
    @Override
    final public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        View itemView;
        // header
        if (viewType < headerCount()) {
            Class<? extends HeaderViewHolder> clazz = headerViewHolders.get(viewType);
            LayoutRes layoutRes = clazz.getAnnotation(LayoutRes.class);
            itemView = LayoutInflater.from(fragment.getActivity()).inflate(layoutRes.resId(), parent, false);
            try {
                Constructor constructor = clazz.getConstructor(Fragment.class, HeaderFooterAdapter.class, View.class);
                viewHolder = (HeaderViewHolder) constructor.newInstance(fragment, this, itemView);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("create header viewHolder failed");
            }
            itemView.setOnClickListener(v -> {
                if (onHeaderClickListener != null) {
                    onHeaderClickListener.onHeaderClick(viewHolder.getAdapterPosition());
                }
            });
            // contact
        } else if (viewType == TYPE_CONTENT) {
            viewHolder = onCreateContentViewHolder(parent, viewType);
            // footer
        } else {
            Class<? extends FooterViewHolder> clazz = footerViewHolders.get(viewType - TYPE_FOOTER_START_INDEX);
            LayoutRes layoutRes = clazz.getAnnotation(LayoutRes.class);
            itemView = LayoutInflater.from(fragment.getActivity()).inflate(layoutRes.resId(), parent, false);
            try {
                Constructor constructor = clazz.getConstructor(Fragment.class, HeaderFooterAdapter.class, View.class);
                viewHolder = (FooterViewHolder) constructor.newInstance(fragment, this, itemView);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("create footer viewHolder failed");
            }
            itemView.setOnClickListener(v -> {
                if (onFooterClickListener != null) {
                    onFooterClickListener.onFooterClick(viewHolder.getAdapterPosition() - headerCount() - getContentItemCount());
                }
            });

        }
        return viewHolder;
    }

    protected abstract RecyclerView.ViewHolder onCreateContentViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    final public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < headerCount()) {
            ((HeaderViewHolder) holder).onBind(headerValues.get(position));
        } else if (position < headerCount() + getContentItemCount()) {
            onBindContentViewHolder(holder, position - headerCount());
        } else {
            ((FooterViewHolder<FooterValue>) holder).onBind(footerValues.get(position - headerCount() - getContentItemCount()));
        }
    }

    public abstract void onBindContentViewHolder(RecyclerView.ViewHolder holder, int position);


    @Override
    final public int getItemCount() {
        return getContentItemCount() + headerCount() + footerCount();
    }

    public abstract int getContentItemCount();

    @Override
    public int getItemViewType(int position) {
        if (position < headerCount()) {
            return position;
        } else if (position < headerCount() + getContentItemCount()) {
            return TYPE_CONTENT;
        } else {
            return TYPE_FOOTER_START_INDEX + (position - headerCount() - getContentItemCount());
        }
    }

    public void addHeaderViewHolder(Class<? extends HeaderViewHolder> clazz, HeaderValue value) {

        if (headerViewHolders == null) {
            headerViewHolders = new ArrayList<>();
            headerValues = new ArrayList<>();
        }
        headerViewHolders.add(clazz);
        headerValues.add(value);
    }

    public void updateHeader(int index, HeaderValue value) {
        headerValues.set(index, value);
        notifyItemChanged(index);
    }

    public void addFooterViewHolder(Class<? extends FooterViewHolder> clazz, FooterValue value) {
        if (footerViewHolders == null) {
            footerViewHolders = new ArrayList<>();
            footerValues = new ArrayList<>();
        }

        footerViewHolders.add(clazz);
        footerValues.add(value);
    }

    public void updateFooter(int index, FooterValue value) {
        footerValues.set(index, value);
        notifyItemChanged(headerCount() + getContentItemCount() + index);
    }

    public int headerCount() {
        return headerViewHolders == null ? 0 : headerViewHolders.size();
    }

    public int footerCount() {
        return footerViewHolders == null ? 0 : footerViewHolders.size();
    }

    public interface OnHeaderClickListener {
        void onHeaderClick(int index);
    }

    public interface OnFooterClickListener {
        void onFooterClick(int index);
    }
}
