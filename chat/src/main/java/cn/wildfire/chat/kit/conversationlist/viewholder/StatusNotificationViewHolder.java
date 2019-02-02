package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfirechat.chat.R;

public class StatusNotificationViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.notificationContainerLayout)
    LinearLayout containerLayout;

    public StatusNotificationViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void onBind(Fragment fragment, View itemView, Map<Class<? extends StatusNotification>, Object> statusNotifications) {
        LayoutInflater layoutInflater = LayoutInflater.from(fragment.getContext());

        containerLayout.removeAllViews();
        StatusNotification statusNotification;
        View view;
        for (Map.Entry<Class<? extends StatusNotification>, Object> entry : statusNotifications.entrySet()) {
            try {
                Constructor constructor = entry.getKey().getConstructor(Fragment.class);
                statusNotification = (StatusNotification) constructor.newInstance(fragment);
                view = layoutInflater.inflate(statusNotification.layoutRes(), (ViewGroup) itemView, false);
                view.setTag(entry.getKey().getSimpleName());
                ButterKnife.bind(statusNotification, view);

                statusNotification.onBind(view, entry.getValue());
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
