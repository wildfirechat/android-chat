package cn.wildfire.chat.moment.third.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.moment.model.Profile;
import cn.wildfirechat.remote.ChatManager;

public class HostViewHolder extends RecyclerView.ViewHolder {
    private View rootView;
    public ImageView friend_wall_pic;
    public ImageView friend_avatar;
    private ImageView message_avatar;
    private TextView message_detail;
    private TextView hostid;
    private RequestOptions requestOptions = new RequestOptions()
        .placeholder(R.mipmap.avatar_def)
        .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(10)));

    public HostViewHolder(View itemView) {
        super(itemView);
        this.rootView = itemView;
        this.hostid = (TextView) rootView.findViewById(R.id.host_id);
        this.friend_wall_pic = (ImageView) rootView.findViewById(R.id.friend_wall_pic);
        this.friend_avatar = (ImageView) rootView.findViewById(R.id.friend_avatar);
        this.message_avatar = (ImageView) rootView.findViewById(R.id.message_avatar);
        this.message_detail = (TextView) rootView.findViewById(R.id.unreadFeedMessageCountTextView);
    }

    public View getView() {
        return rootView;
    }

    public void bind(Context context, UserInfo userInfo, Profile profile) {
        if (userInfo == null) {
            userInfo = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
        }
        if (userInfo != null) {
            hostid.setText(userInfo.displayName);
            Glide.with(context)
                .load(userInfo.portrait)
                .apply(requestOptions)
                .into(friend_avatar);
        }
        if (profile != null && !TextUtils.isEmpty(profile.backgroundUrl)) {
            Glide.with(context).load(profile.backgroundUrl).placeholder(R.drawable.test_wallpic)
                .into(friend_wall_pic);
        }
    }
}
