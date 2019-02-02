package cn.wildfire.chat.kit.conversationlist.notification;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import butterknife.Bind;
import butterknife.OnClick;
import cn.wildfirechat.chat.R;

public class ConnectionNotification extends StatusNotification {
    public ConnectionNotification(Fragment fragment) {
        super(fragment);
    }

    @Bind(R.id.statusTextView)
    TextView statusTextView;

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public int layoutRes() {
        return R.layout.conversationlist_item_notification_connection_status;
    }

    @Override
    public void onBind(View view, Object value) {
        String status = (String) value;
        statusTextView.setText(status);
    }

    @OnClick(R.id.statusTextView)
    public void onClick() {
        Toast.makeText(fragment.getContext(), "status on Click", Toast.LENGTH_SHORT).show();

    }
}
