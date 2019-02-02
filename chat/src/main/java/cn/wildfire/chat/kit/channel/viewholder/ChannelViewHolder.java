package cn.wildfire.chat.kit.channel.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ChannelInfo;

public class ChannelViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.portraitImageView)
    ImageView portraitImageView;
    @Bind(R.id.channelNameTextView)
    TextView channelNameTextView;

    public ChannelViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(ChannelInfo channelInfo) {
        channelNameTextView.setText(channelInfo.name == null ? "< " + channelInfo.channelId + "> " : channelInfo.name);
        Glide.with(itemView.getContext()).load(channelInfo.portrait).into(portraitImageView);
    }
}
