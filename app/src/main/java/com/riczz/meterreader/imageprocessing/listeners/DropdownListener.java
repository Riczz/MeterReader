package com.riczz.meterreader.imageprocessing.listeners;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import net.cachapa.expandablelayout.ExpandableLayout;

public final class DropdownListener implements View.OnClickListener {

    private static final LinearInterpolator INTERPOLATOR = new LinearInterpolator();

    private static final RotateAnimation DROPDOWN_OPEN_ANIM =
            new RotateAnimation(0, 90,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );

    private static final RotateAnimation DROPDOWN_CLOSE_ANIM =
            new RotateAnimation(90, 0,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );

    static {
        DROPDOWN_OPEN_ANIM.setDuration(100);
        DROPDOWN_OPEN_ANIM.setInterpolator(INTERPOLATOR);
        DROPDOWN_OPEN_ANIM.setFillAfter(true);

        DROPDOWN_CLOSE_ANIM.setDuration(100);
        DROPDOWN_CLOSE_ANIM.setInterpolator(INTERPOLATOR);
        DROPDOWN_CLOSE_ANIM.setFillAfter(true);
    }

    private final ExpandableLayout dropdown;
    private final ImageView dropdownArrow;

    public DropdownListener(ExpandableLayout dropdown, ImageView dropdownArrow) {
        this.dropdown = dropdown;
        this.dropdownArrow = dropdownArrow;
    }

    @Override
    public void onClick(View view) {
        dropdownArrow.startAnimation(dropdown.isExpanded() ? DROPDOWN_CLOSE_ANIM : DROPDOWN_OPEN_ANIM);
        dropdown.toggle(true);
    }
}
