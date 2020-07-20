package cn.wildfire.chat.kit.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.UserViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.footer.FooterViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfirechat.chat.R;

public class UserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CONTACT = 1024;
    private static final int TYPE_FOOTER_START_INDEX = 2048;
    protected List<UIUserInfo> users;
    protected Fragment fragment;
    private List<HeaderValueWrapper> headerValues;
    private List<FooterValueWrapper> footerValues;
    protected OnUserClickListener onUserClickListener;
    protected OnHeaderClickListener onHeaderClickListener;
    protected OnFooterClickListener onFooterClickListener;

    public UserListAdapter(Fragment fragment) {
        super();
        this.fragment = fragment;
    }

    public List<UIUserInfo> getUsers() {
        return users;
    }

    public int getContactCount() {
        return userCount();
    }

    public void setUsers(List<UIUserInfo> users) {
        this.users = users;
        notifyDataSetChanged();
    }


    public void setOnUserClickListener(OnUserClickListener onUserClickListener) {
        this.onUserClickListener = onUserClickListener;
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
        if (viewType < TYPE_CONTACT) {
            HeaderValueWrapper wrapper = headerValues.get(viewType);
            itemView = LayoutInflater.from(fragment.getActivity()).inflate(wrapper.layoutResId, parent, false);
            try {
                Constructor constructor = wrapper.headerViewHolderClazz.getConstructor(Fragment.class, UserListAdapter.class, View.class);
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
        } else if (viewType == TYPE_CONTACT) {
            viewHolder = onCreateContactViewHolder(parent, viewType);
            // footer
        } else {
            FooterValueWrapper wrapper = footerValues.get(viewType - TYPE_FOOTER_START_INDEX);
            itemView = LayoutInflater.from(fragment.getActivity()).inflate(wrapper.layoutResId, parent, false);
            try {
                Constructor constructor = wrapper.footerViewHolderClazz.getConstructor(Fragment.class, UserListAdapter.class, View.class);
                viewHolder = (FooterViewHolder) constructor.newInstance(fragment, this, itemView);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("create footer viewHolder failed");
            }
            itemView.setOnClickListener(v -> {
                if (onFooterClickListener != null) {
                    onFooterClickListener.onFooterClick(viewHolder.getAdapterPosition() - headerCount() - userCount());
                }
            });

        }
        return viewHolder;
    }

    protected RecyclerView.ViewHolder onCreateContactViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.contact_item_contact, parent, false);
        UserViewHolder viewHolder = new UserViewHolder(fragment, this, itemView);
        itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                int position = viewHolder.getAdapterPosition();
                onUserClickListener.onUserClick(users.get(position - headerCount()));
            }
        });
        return viewHolder;
    }

    private void processOnLongClick(RecyclerView.ViewHolder viewHolder) {
        // not implement yet
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).onBind(users.get(position - headerCount()));
        } else if (holder instanceof HeaderViewHolder) {
            HeaderValueWrapper wrapper = headerValues.get(position);
            ((HeaderViewHolder) holder).onBind(wrapper.headerValue);
        } else if (holder instanceof FooterViewHolder) {
            int userCount = users == null ? 0 : users.size();
            FooterValueWrapper wrapper = footerValues.get(position - headerCount() - userCount);
            ((FooterViewHolder<FooterValue>) holder).onBind(wrapper.footerValue);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < headerCount()) {
            return position;
        } else if (position < headerCount() + userCount()) {
            return TYPE_CONTACT;
        } else {
            return TYPE_FOOTER_START_INDEX + (position - headerCount() - userCount());
        }
    }

    @Override
    public int getItemCount() {
        int userCount = users == null ? 0 : users.size();
        return headerCount() + footerCount() + userCount;
    }

    public void addHeaderViewHolder(Class<? extends HeaderViewHolder> clazz, int layoutResId, HeaderValue value) {

        if (headerValues == null) {
            headerValues = new ArrayList<>();
        }
        int position = headerCount();
        headerValues.add(new HeaderValueWrapper(clazz, layoutResId, value));

        notifyItemInserted(position);
    }

    public void updateHeader(int index, HeaderValue value) {
        HeaderValueWrapper wrapper = headerValues.get(index);
        headerValues.set(index, new HeaderValueWrapper(wrapper.headerViewHolderClazz, wrapper.layoutResId, value));
        notifyItemChanged(index);
    }

    public void addFooterViewHolder(Class<? extends FooterViewHolder> clazz, int layoutResId, FooterValue value) {
        if (footerValues == null) {
            footerValues = new ArrayList<>();
        }
        int footerCount = footerCount();
        footerValues.add(new FooterValueWrapper(clazz, layoutResId, value));
        notifyItemInserted(headerCount() + userCount() + footerCount);
    }

    public void updateFooter(int index, FooterValue value) {
        FooterValueWrapper wrapper = footerValues.get(index);
        footerValues.set(index, new FooterValueWrapper(wrapper.footerViewHolderClazz, wrapper.layoutResId, value));
        notifyItemChanged(headerCount() + userCount() + index);
    }

    public int headerCount() {
        return headerValues == null ? 0 : headerValues.size();
    }

    private int userCount() {
        return users == null ? 0 : users.size();
    }

    public int footerCount() {
        return footerValues == null ? 0 : footerValues.size();
    }

    public interface OnUserClickListener {
        void onUserClick(UIUserInfo userInfo);
    }

    public interface OnHeaderClickListener {
        void onHeaderClick(int index);
    }

    public interface OnFooterClickListener {
        void onFooterClick(int index);
    }

    static class HeaderValueWrapper {
        Class<? extends HeaderViewHolder> headerViewHolderClazz;
        int layoutResId;
        HeaderValue headerValue;

        public HeaderValueWrapper(Class<? extends HeaderViewHolder> headerViewHolderClazz, int layoutResId, HeaderValue headerValue) {
            this.headerViewHolderClazz = headerViewHolderClazz;
            this.layoutResId = layoutResId;
            this.headerValue = headerValue;
        }
    }

    static class FooterValueWrapper {
        Class<? extends FooterViewHolder> footerViewHolderClazz;
        int layoutResId;
        FooterValue footerValue;

        public FooterValueWrapper(Class<? extends FooterViewHolder> footerViewHolderClazz, int layoutResId, FooterValue footerValue) {
            this.footerViewHolderClazz = footerViewHolderClazz;
            this.layoutResId = layoutResId;
            this.footerValue = footerValue;
        }
    }
}
