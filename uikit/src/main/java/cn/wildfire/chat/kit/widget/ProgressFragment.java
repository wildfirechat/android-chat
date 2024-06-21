/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;

public abstract class ProgressFragment extends Fragment {

    private View loadingView;
    private View emptyView;
    private View contentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.progress_fragment, container, false);

        ViewStub loadingViewStub = view.findViewById(R.id.loadingViewStub);
        ViewStub contentViewStub = view.findViewById(R.id.contentViewStub);
        ViewStub emtpyViewStub = view.findViewById(R.id.emptyViewStub);

        loadingViewStub.setLayoutResource(loadingLayout());
        loadingView = loadingViewStub.inflate();

        contentViewStub.setLayoutResource(contentLayout());
        contentView = contentViewStub.inflate();
        contentView.setVisibility(View.GONE);

        emtpyViewStub.setLayoutResource(emptyLayout());
        emptyView = emtpyViewStub.inflate();
        emptyView.setVisibility(View.GONE);

        afterViews(view);
        return view;
    }

    protected abstract int contentLayout();

    protected int loadingLayout() {
        return R.layout.loading_view;
    }

    protected int emptyLayout() {
        return R.layout.empty_view;
    }

    protected void showContent() {
        if (contentView.getVisibility() == View.VISIBLE) {
            return;
        }
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    protected void showLoading() {
        if (loadingView.getVisibility() == View.VISIBLE) {
            return;
        }
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    protected void showEmpty(String desc) {
        if (emptyView.getVisibility() == View.VISIBLE) {
            return;
        }
        TextView emptyTextView = emptyView.findViewById(R.id.emptyTextView);
        if (emptyTextView != null) {
            emptyTextView.setText(desc);
        }
        emptyView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
    }

    protected void afterViews(View view) {

    }
}
