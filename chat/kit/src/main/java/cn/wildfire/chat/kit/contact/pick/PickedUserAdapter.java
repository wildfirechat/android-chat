package cn.wildfire.chat.kit.contact.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.chat.R;

public class PickedUserAdapter extends RecyclerView.Adapter {

    List<UIUserInfo> mData;
    RequestOptions mOptions;

    public PickedUserAdapter() {
        this.mData = new ArrayList<>();
        mOptions = new RequestOptions()
                .placeholder(UIUtils.getRoundedDrawable(R.mipmap.default_header, 4))
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(4)));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.picked_user, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((Holder) holder).bind(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void addUser(UIUserInfo uiUserInfo) {
        mData.add(uiUserInfo);
        notifyDataSetChanged();
    }

    public void removeUser(UIUserInfo uiUserInfo) {
        mData.remove(uiUserInfo);
        notifyDataSetChanged();
    }

    private class Holder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        Holder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.avatar);
        }

        void bind(UIUserInfo uiUserInfo) {
            Glide.with(imageView)
                    .load(uiUserInfo.getUserInfo().portrait)
                    .apply(mOptions)
                    .into(imageView);
        }
    }
}
