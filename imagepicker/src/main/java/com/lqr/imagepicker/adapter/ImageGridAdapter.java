package com.lqr.imagepicker.adapter;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lqr.imagepicker.ImagePickStore;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.R;
import com.lqr.imagepicker.Utils;
import com.lqr.imagepicker.bean.ImageItem;
import com.lqr.imagepicker.ui.ImageGridActivity;
import com.lqr.imagepicker.view.SuperCheckBox;

import java.util.ArrayList;


public class ImageGridAdapter extends BaseAdapter {

    private static final int ITEM_TYPE_CAMERA = 0;  //第一个条目是相机
    private static final int ITEM_TYPE_NORMAL = 1;  //第一个条目不是相机

    private ImagePicker imagePicker;
    private ImagePickStore store;
    private Activity mActivity;
    private ArrayList<ImageItem> images;       //当前需要显示的所有的图片数据
    private ArrayList<ImageItem> mSelectedImages; //全局保存的已经选中的图片数据
    private boolean showCamera;         //是否显示拍照按钮
    private int limit;
    private boolean multiMode;
    private int mImageSize;               //每个条目的大小
    private OnImageItemClickListener listener;   //图片被点击的监听

    public ImageGridAdapter(Activity activity, boolean showCamera, boolean multiMode, int limit) {
        this.mActivity = activity;
        this.images = new ArrayList<>();

        mImageSize = Utils.getImageItemWidth(mActivity);
        store = ImagePickStore.getInstance();
        this.showCamera = showCamera;
        this.multiMode = multiMode;
        this.limit = limit;
        mSelectedImages = store.getSelectedImages();
    }

    public void refreshData(ArrayList<ImageItem> images) {
        if (images == null || images.size() == 0) this.images = new ArrayList<>();
        else this.images = images;
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera) return position == 0 ? ITEM_TYPE_CAMERA : ITEM_TYPE_NORMAL;
        return ITEM_TYPE_NORMAL;
    }

    @Override
    public int getCount() {
        return showCamera ? images.size() + 1 : images.size();
    }

    @Override
    public ImageItem getItem(int position) {
        if (showCamera) {
            if (position == 0) return null;
            return images.get(position - 1);
        } else {
            return images.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        int itemViewType = getItemViewType(position);
        if (itemViewType == ITEM_TYPE_CAMERA) {
            convertView = LayoutInflater.from(mActivity).inflate(R.layout.adapter_camera_item, parent, false);
            convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mImageSize)); //让图片是个正方形
            convertView.setTag(null);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ImageGridActivity) mActivity).takePhoto();
                }
            });
        } else {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.adapter_image_list_item, parent, false);
                convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mImageSize)); //让图片是个正方形
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final ImageItem imageItem = getItem(position);

            holder.ivThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onImageItemClick(holder.rootView, imageItem, position);
                }
            });
            holder.cbCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.cbCheck.isChecked() && mSelectedImages.size() >= limit) {
                        Toast.makeText(mActivity.getApplicationContext(), mActivity.getString(R.string.select_limit, limit), Toast.LENGTH_SHORT).show();
                        holder.cbCheck.setChecked(false);
                        holder.mask.setVisibility(View.GONE);
                    } else {
                        store.addSelectedImageItem(position, imageItem, holder.cbCheck.isChecked());
                        holder.mask.setVisibility(View.VISIBLE);
                        ((ImageGridActivity) mActivity).updatePickStatus();
                    }
                }
            });
            //根据是否多选，显示或隐藏checkbox
            if (multiMode) {
                holder.cbCheck.setVisibility(View.VISIBLE);
                boolean checked = mSelectedImages.contains(imageItem);
                if (checked) {
                    holder.mask.setVisibility(View.VISIBLE);
                    holder.cbCheck.setChecked(true);
                } else {
                    holder.mask.setVisibility(View.GONE);
                    holder.cbCheck.setChecked(false);
                }
            } else {
                holder.cbCheck.setVisibility(View.GONE);
            }
            //imagePicker.getImageLoader().displayImage(mActivity, imageItem.path, holder.ivThumb, mImageSize, mImageSize); //显示图片
            Glide.with(mActivity).load(Uri.parse("file://" + imageItem.path).toString()).into(holder.ivThumb);
        }
        return convertView;
    }

    private class ViewHolder {
        public View rootView;
        public ImageView ivThumb;
        public View mask;
        public SuperCheckBox cbCheck;

        public ViewHolder(View view) {
            rootView = view;
            ivThumb = (ImageView) view.findViewById(R.id.iv_thumb);
            mask = view.findViewById(R.id.mask);
            cbCheck = (SuperCheckBox) view.findViewById(R.id.cb_check);
        }
    }

    public void setOnImageItemClickListener(OnImageItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnImageItemClickListener {
        void onImageItemClick(View view, ImageItem imageItem, int position);
    }
}