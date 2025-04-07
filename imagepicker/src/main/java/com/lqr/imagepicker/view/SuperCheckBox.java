package com.lqr.imagepicker.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;

public class SuperCheckBox extends androidx.appcompat.widget.AppCompatCheckBox {

    public SuperCheckBox(Context context) {
        super(context);
    }

    public SuperCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SuperCheckBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean performClick() {
        final boolean handled = super.performClick();
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        return handled;
    }
}
