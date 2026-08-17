/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pane;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;

/**
 * 右栏欢迎页。每个 tab 的导航栈都以它<strong>打底</strong>：切到还没进过的 tab、
 * 或者把该 tab 的栈全部返回完之后，看到的就是它。
 * <p>
 * 它不带 toolbar（不是 {@link PanePageFragment} 包起来的页面）：欢迎页没有标题也没有返回，
 * 多一条空标题栏只会让右栏顶部凭空多出一截灰条。
 */
public class PaneWelcomeFragment extends Fragment {

    private static final String ARG_HINT = "paneWelcomeHint";

    public static PaneWelcomeFragment newInstance(CharSequence hint) {
        PaneWelcomeFragment fragment = new PaneWelcomeFragment();
        Bundle args = new Bundle();
        args.putCharSequence(ARG_HINT, hint);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pane_welcome_fragment, container, false);
        TextView hintTextView = view.findViewById(R.id.paneWelcomeTextView);
        CharSequence hint = getArguments() == null ? null : getArguments().getCharSequence(ARG_HINT);
        if (TextUtils.isEmpty(hint)) {
            hintTextView.setVisibility(View.GONE);
        } else {
            hintTextView.setText(hint);
        }
        return view;
    }
}
