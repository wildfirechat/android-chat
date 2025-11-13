package com.lqr.imagepicker.adapter;

import android.app.Activity;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.R;
import com.lqr.imagepicker.Utils;
import com.lqr.imagepicker.bean.ImageItem;

import java.util.ArrayList;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImagePageAdapter extends PagerAdapter {

    private int screenWidth;
    private int screenHeight;
    private ImagePicker imagePicker;
    private ArrayList<ImageItem> images = new ArrayList<>();
    private Activity mActivity;
    private View playingView;
    private int playingViewIndex = 0;
    public PhotoViewClickListener listener;

    public ImagePageAdapter(Activity activity, ArrayList<ImageItem> images) {
        this.mActivity = activity;
        this.images = images;

        DisplayMetrics dm = Utils.getScreenPix(activity);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }

    public void setData(ArrayList<ImageItem> images) {
        this.images = images;
    }

    public void setPhotoViewClickListener(PhotoViewClickListener listener) {
        this.listener = listener;
    }

    public void updateCurrentPagePosition(int position) {
        if (position != this.playingViewIndex && this.playingView != null) {
            VideoView videoView = this.playingView.findViewById(R.id.videoView);
            if (videoView != null && videoView.isPlaying()) {
                videoView.pause();
            }
            ImageView playButtonView = this.playingView.findViewById(R.id.playButton);
            if (playButtonView != null) {
                playButtonView.setVisibility(View.VISIBLE);
            }
            this.playingView = null;
            this.playingViewIndex = -1;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageItem imageItem = this.images.get(position);
        if (imageItem.mimeType.startsWith("video")) {
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.preview_adapter_video_item, container, false);
            VideoView videoView = view.findViewById(R.id.videoView);
            videoView.setVideoPath(imageItem.path);
            ImageView imageView = view.findViewById(R.id.playButton);
            imageView.setOnClickListener(v -> {
                videoView.start();
                imageView.setVisibility(View.GONE);
                playingView = view;
                playingViewIndex = position;
            });
            videoView.setOnClickListener(v -> {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    playingView = null;
                    playingViewIndex = -1;
                    imageView.setVisibility(View.VISIBLE);
                }
                if (listener != null) listener.OnPhotoTapListener(view, v.getX(), v.getY());
            });
            videoView.setOnCompletionListener(mp -> {
                this.playingView = null;
                this.playingViewIndex = -1;
            });
            videoView.seekTo(1);
            container.addView(view);
            return view;
        } else {
            PhotoView photoView = new PhotoView(mActivity);
            Glide.with(mActivity).load(Uri.parse("file://" + imageItem.path).toString()).into(photoView);
            photoView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float x, float y) {
                    if (listener != null) listener.OnPhotoTapListener(view, x, y);
                }
            });
            container.addView(photoView);
            return photoView;
        }
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        VideoView videoView = ((View) object).findViewById(R.id.videoView);
        if (videoView != null) {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
        }
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public interface PhotoViewClickListener {
        void OnPhotoTapListener(View view, float v, float v1);
    }
}
