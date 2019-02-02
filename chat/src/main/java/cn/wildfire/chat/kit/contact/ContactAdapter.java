package cn.wildfire.chat.kit.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.viewholder.ContactViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.footer.FooterViewHolder;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfirechat.chat.R;

public class ContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CONTACT = 1024;
    private static final int TYPE_FOOTER_START_INDEX = 2048;
    protected List<UIUserInfo> contacts;
    protected Fragment fragment;
    private List<Class<? extends HeaderViewHolder>> headerViewHolders;
    private List<Class<? extends FooterViewHolder>> footerViewHolders;
    private List<HeaderValue> headerValues;
    private List<FooterValue> footerValues;
    protected OnContactClickListener onContactClickListener;
    protected OnHeaderClickListener onHeaderClickListener;
    protected OnFooterClickListener onFooterClickListener;

    public ContactAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public List<UIUserInfo> getContacts() {
        return contacts;
    }

    public int getContactCount() {
        return contacts == null ? 0 : contacts.size();
    }

    public void setContacts(List<UIUserInfo> contacts) {
        this.contacts = contacts;
    }

    public void updateContacts(List<UIUserInfo> userInfos) {
        for (UIUserInfo info : userInfos) {
            updateContact(info);
        }
    }

    public void updateContact(UIUserInfo userInfo) {
        int originalPosition = -1;
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getUserInfo().uid.equals(userInfo.getUserInfo().uid)) {
                contacts.set(i, userInfo);
                originalPosition = headerCount() + i;
                break;
            }
        }
        if (originalPosition == -1) {
            return;
        }

        Collections.sort(contacts, (o1, o2) -> o1.getSortName().compareToIgnoreCase(o2.getSortName()));

        int targetPosition = 0;
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getUserInfo().uid.equals(userInfo.getUserInfo().uid)) {
                targetPosition = headerCount() + i;
                break;
            }
        }

        if (targetPosition == headerCount()) {
            userInfo.setShowCategory(true);
        } else {
            UIUserInfo pre = contacts.get(targetPosition - headerCount() - 1);
            if (pre.getCategory() == null || !pre.getCategory().equals(userInfo.getCategory())) {
                userInfo.setShowCategory(true);
            }
        }

        if (originalPosition == targetPosition) {
            notifyItemChanged(originalPosition);
        } else {
            notifyItemMoved(originalPosition, targetPosition);
        }
    }

    public void setOnContactClickListener(OnContactClickListener onContactClickListener) {
        this.onContactClickListener = onContactClickListener;
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
                Constructor constructor = clazz.getConstructor(Fragment.class, ContactAdapter.class, View.class);
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
            Class<? extends FooterViewHolder> clazz = footerViewHolders.get(viewType - TYPE_FOOTER_START_INDEX);
            LayoutRes layoutRes = clazz.getAnnotation(LayoutRes.class);
            itemView = LayoutInflater.from(fragment.getActivity()).inflate(layoutRes.resId(), parent, false);
            try {
                Constructor constructor = clazz.getConstructor(Fragment.class, ContactAdapter.class, View.class);
                viewHolder = (FooterViewHolder) constructor.newInstance(fragment, this, itemView);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("create footer viewHolder failed");
            }
            itemView.setOnClickListener(v -> {
                if (onFooterClickListener != null) {
                    onFooterClickListener.onFooterClick(viewHolder.getAdapterPosition() - headerCount() - contactCount());
                }
            });

        }
        return viewHolder;
    }

    protected RecyclerView.ViewHolder onCreateContactViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.contact_item_contact, parent, false);
        ContactViewHolder viewHolder = new ContactViewHolder(fragment, this, itemView);
        itemView.setOnClickListener(v -> {
            if (onContactClickListener != null) {
                int position = viewHolder.getAdapterPosition();
                onContactClickListener.onContactClick(contacts.get(position - headerCount()));
            }
        });
        return viewHolder;
    }

    private void processOnLongClick(RecyclerView.ViewHolder viewHolder) {
        // not implement yet
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < headerCount()) {
            ((HeaderViewHolder) holder).onBind(headerValues.get(position));
        } else if (position < headerCount() + contactCount()) {
            ((ContactViewHolder) holder).onBind(contacts.get(position - headerCount()));
        } else {
            ((FooterViewHolder<FooterValue>) holder).onBind(footerValues.get(position - headerCount() - contactCount()));
        }
    }

    @Override
    public int getItemCount() {
        return contactCount() + headerCount() + footerCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < headerCount()) {
            return position;
        } else if (position < headerCount() + contactCount()) {
            return TYPE_CONTACT;
        } else {
            return TYPE_FOOTER_START_INDEX + (position - headerCount() - contactCount());
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
        notifyItemChanged(headerCount() + contactCount() + index);
    }

    public int headerCount() {
        return headerViewHolders == null ? 0 : headerViewHolders.size();
    }

    private int contactCount() {
        return contacts == null ? 0 : contacts.size();
    }

    public int footerCount() {
        return footerViewHolders == null ? 0 : footerViewHolders.size();
    }

    public interface OnContactClickListener {
        void onContactClick(UIUserInfo userInfo);
    }

    public interface OnHeaderClickListener {
        void onHeaderClick(int index);
    }

    public interface OnFooterClickListener {
        void onFooterClick(int index);
    }
}
