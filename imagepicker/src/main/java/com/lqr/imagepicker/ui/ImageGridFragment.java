package com.lqr.imagepicker.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.lqr.imagepicker.ImageDataSource;
import com.lqr.imagepicker.ImagePickStore;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.R;
import com.lqr.imagepicker.Utils;
import com.lqr.imagepicker.adapter.ImageFolderAdapter;
import com.lqr.imagepicker.adapter.ImageGridAdapter;
import com.lqr.imagepicker.bean.ImageFolder;
import com.lqr.imagepicker.bean.ImageItem;
import com.lqr.imagepicker.view.FolderPopUpWindow;

import java.util.List;

import cn.wildfirechat.uikit.permission.PermissionKit;

/**
 * 相册选择页的内容：文件夹切换 + 九宫格 + 拍照 + 预览入口。
 * <p>
 * 原来是 {@code ImageGridActivity} 独占一个 Activity 的全部内容，现在整体搬进本 Fragment，
 * {@link ImageGridActivity} 变成手机端的壳。平板双栏下由 uikit 里的一个子类
 * （{@code ImagePickerPanePageFragment}，implements 右栏页面契约）承载同一份实现——
 * 本模块不依赖 uikit，所以本类只暴露两个可覆写的收尾钩子（{@link #cancelPick()} /
 * {@link #finishPick(int, Intent)}），默认实现是手机端原来的 {@code setResult + finish}。
 */
public class ImageGridFragment extends Fragment implements ImageDataSource.OnImageLoadListener, ImageGridAdapter.OnImageItemClickListener, View.OnClickListener {

    public static final int REQUEST_PERMISSION_STORAGE = 0x01;
    public static final int REQUEST_PERMISSION_CAMERA = 0x02;

    private static final String ARG_MULTI_MODE = "multiMode";
    private static final String ARG_LIMIT = "limit";
    private static final String ARG_SHOW_CAMERA = "showCamera";
    private static final String ARG_SHOW_VIDEO = "showVideo";

    private ImagePickStore store;
    private boolean multiMode = false;
    private int limit;
    private boolean showCamera;
    private boolean showVideo;

    private GridView mGridView;  //图片展示控件
    private View mFooterBar;     //底部栏
    private TextView mBtnOk;       //确定按钮
    private Button mBtnDir;      //文件夹切换按钮
    private Button mBtnPre;      //预览按钮
    private LinearLayout partialAccessLayout;
    private ImageFolderAdapter mImageFolderAdapter;    //图片文件夹的适配器
    private FolderPopUpWindow mFolderPopupWindow;  //ImageSet的PopupWindow
    private List<ImageFolder> mImageFolders;   //所有的图片文件夹
    private ImageGridAdapter mImageGridAdapter;  //图片九宫格展示的适配器
    private int mGridViewScrollPosition = 0;

    private String takePhotoOutputPath;
    private ImageDataSource imageDataSource;
    private boolean isPartialAccessGranted = false;
    private boolean isFullAccessGranted = false;
    private final ViewTreeObserver.OnGlobalLayoutListener onGridViewLayoutListener = this::updateImageItemWidthFromGridView;

    public static ImageGridFragment fromIntent(Intent intent) {
        ImageGridFragment fragment = new ImageGridFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_MULTI_MODE, intent.getBooleanExtra("multiMode", false));
        args.putInt(ARG_LIMIT, intent.getIntExtra("limit", 9));
        args.putBoolean(ARG_SHOW_CAMERA, intent.getBooleanExtra("showCamera", false));
        args.putBoolean(ARG_SHOW_VIDEO, intent.getBooleanExtra("showVideo", false));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_image_grid, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        store = ImagePickStore.getInstance();
        store.clearSelectedImages();

        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        multiMode = args.getBoolean(ARG_MULTI_MODE, false);
        limit = args.getInt(ARG_LIMIT, 9);
        showCamera = args.getBoolean(ARG_SHOW_CAMERA, false);
        showVideo = args.getBoolean(ARG_SHOW_VIDEO, false);

        view.findViewById(R.id.btn_back).setOnClickListener(this);
        mBtnOk = view.findViewById(R.id.btn_ok);
        mBtnOk.setOnClickListener(this);
        mBtnDir = view.findViewById(R.id.btn_dir);
        mBtnDir.setOnClickListener(this);
        mBtnPre = view.findViewById(R.id.btn_preview);
        mBtnPre.setOnClickListener(this);
        mGridView = view.findViewById(R.id.gridview);
        mFooterBar = view.findViewById(R.id.footer_bar);
        if (multiMode) {
            mBtnOk.setVisibility(View.VISIBLE);
            mBtnPre.setVisibility(View.VISIBLE);
        } else {
            mBtnOk.setVisibility(View.GONE);
            mBtnPre.setVisibility(View.GONE);
        }
        partialAccessLayout = view.findViewById(R.id.partialAccessLinearLayout);
        partialAccessLayout.setOnClickListener(v -> {
            Intent settingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
            settingIntent.setData(uri);
            startActivity(settingIntent);
        });

        mImageGridAdapter = new ImageGridAdapter(requireActivity(), showCamera, multiMode, limit);
        mImageFolderAdapter = new ImageFolderAdapter(requireActivity(), null);
        mImageGridAdapter.setOnImageItemClickListener(this);
        mGridView.setAdapter(mImageGridAdapter);
        // 构造时按整屏宽度给的初始值只在手机上是最终值；平板双栏下 GridView 的真实宽度是
        // 右栏宽度，等它真正测量完成后按真实宽度校正，见 updateImageItemWidthFromGridView()。
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(onGridViewLayoutListener);

        String[] permissions = null;
        checkAccessPermission();
        if (!isFullAccessGranted && !isPartialAccessGranted) {
            // Access denied or partial access granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                };
            } else {
                permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                };
            }
        }

        partialAccessLayout.setVisibility(!isFullAccessGranted && isPartialAccessGranted ? View.VISIBLE : View.GONE);

        if (!isFullAccessGranted && !isPartialAccessGranted) {
            PermissionKit.PermissionReqTuple[] permissionReqTuples = PermissionKit.buildRequestPermissionTuples(requireContext(), permissions);
            PermissionKit.checkThenRequestPermission(requireActivity(), getChildFragmentManager(), permissionReqTuples, allGranted -> {
                // do nothing
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAccessPermission();
        if (isFullAccessGranted || isPartialAccessGranted) {
            if (imageDataSource == null) {
                imageDataSource = new ImageDataSource(requireActivity(), null, showVideo, this);
            }
            imageDataSource.refresh();
            updatePickStatus();
        }
        if (!isFullAccessGranted && isPartialAccessGranted) {
            partialAccessLayout.setVisibility(View.VISIBLE);
        } else {
            partialAccessLayout.setVisibility(View.GONE);
        }
    }

    /**
     * GridView 真实测量完成（或宽度变化，比如平板分屏拖动、旋转）后重算每格的边长。
     * 手机上第一次算出来的值与构造时按 {@link Utils#getImageItemWidth} 给的初始值一致
     * （全屏下窗口宽度==屏幕宽度），{@link ImageGridAdapter#setImageItemWidth} 会因为
     * 数值没变而直接跳过，不会多刷一次。
     */
    private void updateImageItemWidthFromGridView() {
        // 布局回调是异步的，Fragment 可能已经在回调触发前被 detach（比如右栏里被换栈/弹出）。
        if (!isAdded() || mGridView == null || mImageGridAdapter == null || mGridView.getWidth() <= 0) {
            return;
        }
        int span = getResources().getInteger(R.integer.ip_image_grid_span);
        if (span <= 0) {
            return;
        }
        int columnSpace = (int) (2 * getResources().getDisplayMetrics().density);
        int usableWidth = mGridView.getWidth() - mGridView.getPaddingLeft() - mGridView.getPaddingRight();
        int size = (usableWidth - columnSpace * (span - 1)) / span;
        mImageGridAdapter.setImageItemWidth(size);
    }

    private void checkAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED)) {
            // Full access on Android 13 (API level 33) or higher
            isFullAccessGranted = true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED) {
            // Partial access on Android 14 (API level 34) or higher
            isPartialAccessGranted = true;
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Full access up to Android 12 (API level 32)
            isFullAccessGranted = true;
        }
    }

    private void takePhoto() {
        takePhotoOutputPath = Utils.genTakePhotoOutputPath(requireContext());
        String[] permissions = new String[]{Manifest.permission.CAMERA};
        PermissionKit.PermissionReqTuple[] permissionReqTuples = PermissionKit.buildRequestPermissionTuples(requireContext(), permissions);
        PermissionKit.checkThenRequestPermission(requireActivity(), getChildFragmentManager(), permissionReqTuples, allGranted -> {
            if (allGranted) {
                Utils.takePhoto(this, takePhotoOutputPath, ImagePicker.REQUEST_CODE_TAKE);
            } else {
                showToast("权限被禁止，无法打开相机");
            }
        });
    }

    private void showToast(String text) {
        if (isAdded()) {
            android.widget.Toast.makeText(requireContext().getApplicationContext(), text, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (mGridView != null) {
            mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(onGridViewLayoutListener);
        }
        super.onDestroyView();
        store.destroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ok) {
            finishImagePick();
        } else if (id == R.id.btn_dir) {
            if (mImageFolders == null) {
                Log.i("ImageGridFragment", "您的手机没有图片");
                return;
            }
            //点击文件夹按钮
            createPopupFolderList();
            mImageFolderAdapter.refreshData(mImageFolders);  //刷新数据
            if (mFolderPopupWindow.isShowing()) {
                mFolderPopupWindow.dismiss();
            } else {
                mFolderPopupWindow.showAtLocation(mFooterBar, Gravity.NO_GRAVITY, 0, 0);
                //默认选择当前选择的上一个，当目录很多时，直接定位到已选中的条目
                int index = mImageFolderAdapter.getSelectIndex();
                index = index == 0 ? index : index - 1;
                mFolderPopupWindow.setSelection(index);
            }
        } else if (id == R.id.btn_preview) {
            Intent intent = new Intent(requireContext(), ImagePreviewActivity.class);
            intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, 0);
            intent.putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, store.getSelectedImages());
            mGridViewScrollPosition = mGridView.getFirstVisiblePosition();
            startActivityForResult(intent, ImagePicker.REQUEST_CODE_PREVIEW);
        } else if (id == R.id.btn_back) {
            //点击返回按钮
            cancelPick();
        }
    }

    /**
     * 创建弹出的ListView
     */
    private void createPopupFolderList() {
        mFolderPopupWindow = new FolderPopUpWindow(requireContext(), mImageFolderAdapter);
        mFolderPopupWindow.setOnItemClickListener(new FolderPopUpWindow.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mImageFolderAdapter.setSelectIndex(position);
                store.setCurrentImageFolderPosition(position);
                mFolderPopupWindow.dismiss();
                ImageFolder imageFolder = (ImageFolder) adapterView.getAdapter().getItem(position);
                if (null != imageFolder) {
                    mImageGridAdapter.refreshData(imageFolder.images);
                    mBtnDir.setText(imageFolder.name);
                }
                mGridView.smoothScrollToPosition(0);//滑动到顶部
            }
        });
        mFolderPopupWindow.setMargin(mFooterBar.getHeight());
    }

    @Override
    public void onImageLoad(List<ImageFolder> imageFolders) {
        this.mImageFolders = imageFolders;
        store.setImageFolders(imageFolders);
        if (imageFolders.isEmpty()) {
            mImageGridAdapter.refreshData(null);
        } else {
            mImageGridAdapter.refreshData(imageFolders.get(0).images);
            mGridView.smoothScrollToPosition(mGridViewScrollPosition);
        }
        mImageFolderAdapter.refreshData(imageFolders);
    }

    @Override
    public void onImageItemClick(View view, ImageItem imageItem, int position) {
        //根据是否有相机按钮确定位置
        position = showCamera ? position - 1 : position;
        if (multiMode) {
            Intent intent = new Intent(requireContext(), ImagePreviewActivity.class);
            intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, position);
//            intent.putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, imagePicker.getCurrentImageFolderItems());//imagePicker.getCurrentImageFolderItems()的数据量太大，android5以后会OOM但不会报错
            mGridViewScrollPosition = mGridView.getFirstVisiblePosition();
            startActivityForResult(intent, ImagePicker.REQUEST_CODE_PREVIEW);  //如果是多选，点击图片进入预览界面
        } else {
            store.addSelectedImageItem(position, store.getCurrentImageFolderItems().get(position), true);
            finishImagePick();
        }
    }

    @Override
    public void onCameraClick() {
        takePhoto();
    }

    @Override
    public void onPickStatusChanged() {
        updatePickStatus();
    }

    private void updatePickStatus() {
        if (store.getSelectImageCount() > 0) {
            mBtnOk.setText(getString(R.string.select_complete, store.getSelectImageCount(), limit));
            mBtnOk.setEnabled(true);
            mBtnPre.setEnabled(true);
        } else {
            mBtnOk.setText(getString(R.string.complete));
            mBtnOk.setEnabled(false);
            mBtnPre.setEnabled(false);
        }
        mBtnPre.setText(getResources().getString(R.string.preview_count, store.getSelectImageCount()));
        mImageGridAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case ImagePicker.REQUEST_CODE_TAKE:
                    onTakePhoto();
                    break;
                case ImagePicker.REQUEST_CODE_PREVIEW:
                    onPreview();
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void finishImagePick() {
        Intent intent = new Intent();
        intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, store.getSelectedImages());
        intent.putExtra(ImagePicker.EXTRA_COMPRESS, store.isCompress());
        finishPick(Activity.RESULT_OK, intent);   //单选不需要裁剪，返回数据
    }

    private void onPreview() {
        if (store.getSelectImageCount() > 0) {
            finishImagePick();
        }
    }

    private void onTakePhoto() {
        //发送广播通知图片增加了
        Utils.notifyToScanMedia(requireContext(), takePhotoOutputPath);
        ImageItem imageItem = new ImageItem();
        imageItem.path = takePhotoOutputPath;
        store.clearSelectedImages();
        store.addSelectedImageItem(0, imageItem, true);
        Intent intent = new Intent();
        intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, store.getSelectedImages());
        intent.putExtra(ImagePicker.EXTRA_COMPRESS, store.isCompress());
        finishPick(Activity.RESULT_OK, intent);   //单选不需要裁剪，返回数据
    }

    /**
     * 取消选择（点返回按钮），不回传数据。手机端等价于原来的 {@code finish()}——不显式
     * {@code setResult}，宿主 Activity 默认结果就是 {@code RESULT_CANCELED}。
     * <p>
     * 平板双栏里的子类需要覆写：右栏的宿主不是这个 Activity，{@code getActivity()} 是双栏
     * 主界面，直接 {@code finish()} 会把整个界面关掉。
     */
    protected void cancelPick() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    /**
     * 选择完成，回传结果。手机端等价于原来的 {@code setResult(...) + finish()}。
     * 平板双栏里的子类需要覆写，理由同 {@link #cancelPick()}。
     */
    protected void finishPick(int resultCode, Intent data) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setResult(resultCode, data);
            activity.finish();
        }
    }
}
