package cn.wildfire.chat.kit.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;

public abstract class ProgressFragment extends Fragment {

    private View loadingView;
    private View contentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.progress_fragment, container, false);

        ViewStub loadingViewStub = view.findViewById(R.id.loadingViewStub);
        ViewStub contentViewStub = view.findViewById(R.id.contentViewStub);

        loadingViewStub.setLayoutResource(loadingLayout());
        loadingView = loadingViewStub.inflate();

        contentViewStub.setLayoutResource(contentLayout());
        contentView = contentViewStub.inflate();
        contentView.setVisibility(View.GONE);

        afterViews(view);
        return view;
    }

    protected abstract int contentLayout();

    protected int loadingLayout() {
        return R.layout.loading_view;
    }

    protected void showContent() {
        if (contentView.getVisibility() == View.VISIBLE) {
            return;
        }
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    protected void showLoading() {
        if (loadingView.getVisibility() == View.VISIBLE) {
            return;
        }
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    protected void afterViews(View view) {

    }
}
