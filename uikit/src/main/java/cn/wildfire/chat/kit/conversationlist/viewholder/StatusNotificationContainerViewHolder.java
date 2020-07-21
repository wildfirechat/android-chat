package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotificationManager;
import cn.wildfire.chat.kit.conversationlist.notification.viewholder.StatusNotificationViewHolder;
import cn.wildfire.chat.kit.R2;

public class StatusNotificationContainerViewHolder extends RecyclerView.ViewHolder {
    @BindView(R2.id.notificationContainerLayout)
    LinearLayout containerLayout;

    public StatusNotificationContainerViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void onBind(Fragment fragment, View itemView, List<StatusNotification> statusNotifications) {
        LayoutInflater layoutInflater = LayoutInflater.from(fragment.getContext());

        containerLayout.removeAllViews();
        StatusNotificationViewHolder statusNotificationViewHolder;
        View view;
        for (StatusNotification notification : statusNotifications) {
            try {
                Class<? extends StatusNotificationViewHolder> holderClass = StatusNotificationManager.getInstance().getNotificationViewHolder(notification);
                Constructor constructor = holderClass.getConstructor(Fragment.class);
                statusNotificationViewHolder = (StatusNotificationViewHolder) constructor.newInstance(fragment);
                view = layoutInflater.inflate(StatusNotificationManager.getInstance().getNotificationViewHolderLayoutResId(notification), (ViewGroup) itemView, false);
                ButterKnife.bind(statusNotificationViewHolder, view);

                statusNotificationViewHolder.onBind(view, notification);
                containerLayout.addView(view);
                // TODO add divider
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
