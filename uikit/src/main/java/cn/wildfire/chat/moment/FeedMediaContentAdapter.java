package cn.wildfire.chat.moment;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfire.chat.moment.third.widgets.NineGridView;
import cn.wildfire.chat.kit.R;

public class FeedMediaContentAdapter implements NineGridView.NineGridAdapter<String> {

    private List<String> mediaUrls = new ArrayList<>();
    private boolean isVideo;

    private Context mContext;

    private RequestOptions mRequestOptions;

    private DrawableTransitionOptions mDrawableTransitionOptions;


    public FeedMediaContentAdapter(Context context, RequestOptions requestOptions, DrawableTransitionOptions drawableTransitionOptions) {
        this.mContext = context;
        this.mDrawableTransitionOptions = drawableTransitionOptions;
        // 同 NineImageAdapter：解码尺寸按条目内容宽度算，不用整屏宽度。
        int itemSize = (Utils.getFeedContentWidth() - 2 * Utils.dp2px(4) - Utils.dp2px(54)) / 3;
        this.mRequestOptions = requestOptions.override(itemSize, itemSize);
    }

    public List<String> getImageUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mImageUrls, boolean isVideo) {
        this.mediaUrls = mImageUrls;
        this.isVideo = isVideo;
    }

    @Override
    public int getCount() {
        if (isVideo) {
            return 1;
        }
        return mediaUrls.size() < 9 ? mediaUrls.size() + 1 : 9;
    }

    @Override
    public String getItem(int position) {
        return mediaUrls == null ? null :
            position < mediaUrls.size() ? mediaUrls.get(position) : null;
    }

    @Override
    public View getView(int position, View itemView) {
        ImageView imageView;
        if (itemView == null) {
            imageView = new ImageView(mContext);
            imageView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.base_F2F2F2));
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            imageView = (ImageView) itemView;
        }

        if (position < mediaUrls.size()) {
            String url = mediaUrls.get(position);
            Glide.with(mContext).load(url).apply(mRequestOptions).transition(mDrawableTransitionOptions).into(imageView);
        } else {
            Glide.with(mContext).load(R.drawable.ic_tab_add).apply(mRequestOptions).transition(mDrawableTransitionOptions).into(imageView);
        }
        return imageView;
    }
}
