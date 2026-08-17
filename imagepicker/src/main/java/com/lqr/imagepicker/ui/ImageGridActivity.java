package com.lqr.imagepicker.ui;

import android.os.Bundle;

import com.lqr.imagepicker.R;

/**
 * 相册选择页的手机端壳。内容全部在 {@link ImageGridFragment} 里，平板双栏下由 uikit 的
 * {@code ImagePickerPanePageFragment}（{@link ImageGridFragment} 的子类）承载同一份实现，
 * 进右栏而不是这个 Activity——见该类与 {@code PaneRegistry} 里的登记项。
 */
public class ImageGridActivity extends ImageBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_grid_shell);
        if (getSupportFragmentManager().findFragmentById(R.id.content_container) == null) {
            getSupportFragmentManager().beginTransaction()
                .add(R.id.content_container, ImageGridFragment.fromIntent(getIntent()))
                .commit();
        }
    }
}
