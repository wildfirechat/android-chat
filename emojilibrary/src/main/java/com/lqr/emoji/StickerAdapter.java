package com.lqr.emoji;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * CSDN_LQR
 * 贴图适配器
 */

public class StickerAdapter extends BaseAdapter {

    private Context mContext;
    private StickerCategory mCategory;
    private int startIndex;

    private int mEmotionLayoutWidth;
    private int mEmotionLayoutHeight;
    private float mPerWidth;
    private float mPerHeight;
    private float mIvSize;

    public StickerAdapter(Context context, StickerCategory category, int emotionLayoutWidth, int emotionLayoutHeight, int startIndex) {
        mContext = context;
        mCategory = category;
        this.startIndex = startIndex;

        mEmotionLayoutWidth = emotionLayoutWidth;
        mEmotionLayoutHeight = emotionLayoutHeight - LQREmotionKit.dip2px(35 + 26 + 50);//减去底部的tab高度、小圆点的高度才是viewpager的高度，再减少30dp是让表情整体的顶部和底部有个外间距
        mPerWidth = mEmotionLayoutWidth * 1f / EmotionLayout.STICKER_COLUMNS;
        mPerHeight = mEmotionLayoutHeight * 1f / EmotionLayout.STICKER_ROWS;

        float ivWidth = mPerWidth * .8f;
        float ivHeight = mPerHeight * .8f;
        mIvSize = Math.min(ivWidth, ivHeight);
    }


    @Override
    public int getCount() {
        int count = mCategory.getStickers().size() - startIndex;
        count = Math.min(count, EmotionLayout.STICKER_PER_PAGE);
        return count;
    }

    @Override
    public Object getItem(int position) {
        return mCategory.getStickers().get(startIndex + position);
    }

    @Override
    public long getItemId(int position) {
        return startIndex + position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StickerViewHolder viewHolder;
        if (convertView == null) {
            RelativeLayout rl = new RelativeLayout(mContext);
            rl.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mPerHeight));

            ImageView imageView = new ImageView(mContext);
            imageView.setImageResource(R.drawable.ic_tab_emoji);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.width = (int) mIvSize;
            params.height = (int) mIvSize;
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            imageView.setLayoutParams(params);

            rl.addView(imageView);

            viewHolder = new StickerViewHolder();
            viewHolder.mImageView = imageView;

            convertView = rl;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (StickerViewHolder) convertView.getTag();
        }

        int index = startIndex + position;
        if (index >= mCategory.getStickers().size()) {
            return convertView;
        }

        StickerItem sticker = mCategory.getStickers().get(index);
        if (sticker == null) {
            return convertView;
        }

        String stickerBitmapUri = StickerManager.getInstance().getStickerBitmapUri(sticker.getCategory(), sticker.getName());
        LQREmotionKit.getImageLoader().displayImage(mContext, stickerBitmapUri, viewHolder.mImageView);

        return convertView;
    }

    class StickerViewHolder {
        public ImageView mImageView;
    }
}
