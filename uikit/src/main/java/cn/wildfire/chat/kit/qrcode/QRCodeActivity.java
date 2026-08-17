/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.qrcode;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 二维码展示页的空壳。
 * <p>
 * 页面本体是 {@link QRCodeFragment}：手机端由本壳装着，平板上同一份实现直接进右栏，
 * 标题栏、返回都由宿主提供。
 */
public class QRCodeActivity extends WfcBaseActivity {

    public static Intent buildQRCodeIntent(Context context, String title, String logoUrl, String qrCodeValue) {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("logoUrl", logoUrl);
        intent.putExtra("qrCodeValue", qrCodeValue);
        return intent;
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        // 配置变化后 FragmentManager 已经把页面恢复出来了，无条件 add 会再叠一层
        if (getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout) != null) {
            return;
        }
        Fragment fragment = QRCodeFragment.fromIntent(getIntent());
        if (fragment == null) {
            // 没有二维码内容，这一页显示不出东西
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
