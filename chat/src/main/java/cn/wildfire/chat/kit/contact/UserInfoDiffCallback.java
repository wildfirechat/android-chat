package cn.wildfire.chat.kit.contact;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;

public class UserInfoDiffCallback extends DiffUtil.ItemCallback<Object> {

    @Override
    public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
        if (oldItem.getClass() != newItem.getClass()) {
            return false;
        }
        if (oldItem instanceof UIUserInfo) {
            UIUserInfo info1 = (UIUserInfo) oldItem;
            UIUserInfo info2 = (UIUserInfo) newItem;

            return info1.getUserInfo().uid.equals(info2.getUserInfo().uid);
        }

        if (oldItem instanceof UserListAdapter.HeaderValueWrapper) {
            UserListAdapter.HeaderValueWrapper wrapper1 = (UserListAdapter.HeaderValueWrapper) oldItem;
            UserListAdapter.HeaderValueWrapper wrapper2 = (UserListAdapter.HeaderValueWrapper) newItem;
            return wrapper1.headerViewHolderClazz == wrapper2.headerViewHolderClazz;
        }

        if (oldItem instanceof UserListAdapter.FooterValueWrapper) {
            UserListAdapter.FooterValueWrapper wrapper1 = (UserListAdapter.FooterValueWrapper) oldItem;
            UserListAdapter.FooterValueWrapper wrapper2 = (UserListAdapter.FooterValueWrapper) newItem;
            return wrapper1.footerViewHolderClazz == wrapper2.footerViewHolderClazz;
        }

        return false;
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
        if (oldItem instanceof UIUserInfo) {
            UIUserInfo info1 = (UIUserInfo) oldItem;
            UIUserInfo info2 = (UIUserInfo) newItem;
            return info1.getUserInfo().equals(info2.getUserInfo());
        }
        if (oldItem instanceof UserListAdapter.HeaderValueWrapper) {
            UserListAdapter.HeaderValueWrapper wrapper1 = (UserListAdapter.HeaderValueWrapper) oldItem;
            UserListAdapter.HeaderValueWrapper wrapper2 = (UserListAdapter.HeaderValueWrapper) newItem;
            return wrapper1.headerValue.equals(wrapper2.headerValue);
        }

        if (oldItem instanceof UserListAdapter.FooterValueWrapper) {
            UserListAdapter.FooterValueWrapper wrapper1 = (UserListAdapter.FooterValueWrapper) oldItem;
            UserListAdapter.FooterValueWrapper wrapper2 = (UserListAdapter.FooterValueWrapper) newItem;
            return wrapper1.footerValue.equals(wrapper2.footerValue);
        }
        return false;
    }
}
