/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.content.Intent;

import com.lqr.imagepicker.ui.ImageGridActivity;
import com.lqr.imagepicker.ui.ImageGridFragment;

import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;

/**
 * 会话「+」→「相册」在平板双栏下的承载页：{@link ImageGridFragment} 原样复用，只覆写它的两个
 * 收尾钩子。手机端仍然是 {@link ImageGridActivity} 这个壳 Activity，本类只在双栏右栏出现，
 * 由 {@code PaneRegistry} 登记 {@code ImageGridActivity.class → 本类}。
 * <p>
 * 拍照、预览两个子流程未改动：{@link ImageGridFragment} 内部用的是 Fragment 自己的
 * {@code startActivityForResult}，跟 uikit 的右栏机制无关，落在哪个宿主里都会正确回调，
 * 因此这两步在平板上仍然全屏——与文件、地图等未登记的扩展页是同一类"本来就该全屏"。
 */
public class ImagePickerPanePageFragment extends ImageGridFragment implements WfcPage {

    public static ImagePickerPanePageFragment fromIntent(Intent intent) {
        ImagePickerPanePageFragment fragment = new ImagePickerPanePageFragment();
        fragment.setArguments(ImageGridFragment.fromIntent(intent).getArguments());
        return fragment;
    }

    @Override
    public boolean providesOwnToolbar() {
        // 顶部/底部栏是页面自己画的暗色沉浸式 UI，不需要宿主再给一条标题栏。
        return true;
    }

    @Override
    protected void cancelPick() {
        WfcPageCompat.finishPage(this);
    }

    @Override
    protected void finishPick(int resultCode, Intent data) {
        WfcPageCompat.setPageResult(this, resultCode, data);
        WfcPageCompat.finishPage(this);
    }
}
