/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardAwareLinearLayout;

public class KeyboardDialogFragment extends DialogFragment implements KeyboardAwareLinearLayout.OnKeyboardHiddenListener,
    KeyboardAwareLinearLayout.OnKeyboardShownListener {
    private InputAwareLayout inputAwareLayout;
    private InputPanel commentInputPanel;
    private EditText editText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            // todo
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.keyboard_dialog_fragment, container, false);
        inputAwareLayout = view.findViewById(R.id.rootInputAwareLayout);
        inputAwareLayout.addOnKeyboardHiddenListener(this);
        inputAwareLayout.addOnKeyboardShownListener(this);
        commentInputPanel = view.findViewById(R.id.inputPanel);

        inputAwareLayout.setIsBubble(true);

        commentInputPanel.init(this, inputAwareLayout);
        editText = commentInputPanel.editText;

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setDimAmount(0);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        ((View) getView().getParent()).setBackgroundColor(Color.TRANSPARENT);

        inputAwareLayout.showSoftkey(editText);
    }

    @Override
    public void onResume() {
        // Get existing layout params for the window
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        // Assign window properties to fill the parent
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        // Call super onResume after sizing
        super.onResume();
    }

    @Override
    public void onKeyboardShown() {
        commentInputPanel.onKeyboardShown();
    }

    public void hideKeyboard() {
        inputAwareLayout.hideSoftkey(editText, null);
    }

    @Override
    public void onKeyboardHidden() {
        commentInputPanel.onKeyboardHidden();
    }
}
