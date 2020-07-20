package cn.wildfire.chat.app.third.location.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.app.third.location.ui.activity.ShowLocationActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.NormalMessageContentViewHolder;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.LocationMessageContent;

@MessageContentType(LocationMessageContent.class)
@LayoutRes(resId = R.layout.conversation_item_location_send)
@EnableContextMenu
public class LocationMessageContentViewHolder extends NormalMessageContentViewHolder {

    @BindView(R.id.locationTitleTextView)
    TextView locationTitleTextView;
    @BindView(R.id.locationImageView)
    ImageView locationImageView;

    public LocationMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        LocationMessageContent locationMessage = (LocationMessageContent) message.message.content;
        locationTitleTextView.setText(locationMessage.getTitle());

        if (locationMessage.getThumbnail() != null && locationMessage.getThumbnail().getWidth() > 0) {
            int width = locationMessage.getThumbnail().getWidth();
            int height = locationMessage.getThumbnail().getHeight();
            locationImageView.getLayoutParams().width = UIUtils.dip2Px(width > 200 ? 200 : width);
            locationImageView.getLayoutParams().height = UIUtils.dip2Px(height > 200 ? 200 : height);
            locationImageView.setImageBitmap(locationMessage.getThumbnail());
        } else {
            Glide.with(fragment).load(R.mipmap.default_location)
                    .apply(new RequestOptions().override(UIUtils.dip2Px(200), UIUtils.dip2Px(200)).centerCrop()).into(locationImageView);
        }
    }

    @OnClick(R.id.locationLinearLayout)
    public void onClick(View view) {
        Intent intent = new Intent(fragment.getContext(), ShowLocationActivity.class);
        LocationMessageContent content = (LocationMessageContent) message.message.content;
        intent.putExtra("Lat", content.getLocation().getLatitude());
        intent.putExtra("Long", content.getLocation().getLongitude());
        intent.putExtra("title", content.getTitle());
        fragment.startActivity(intent);
    }

    @Override
    public int sendLayoutResId() {
        return R.layout.conversation_item_location_send;
    }

    @Override
    public int receiveLayoutResId() {
        return R.layout.conversation_item_location_send;
    }
}
