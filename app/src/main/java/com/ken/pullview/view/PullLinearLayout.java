package com.ken.pullview.view;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.ken.pullview.Pull;

public class PullLinearLayout extends LinearLayout implements Pull {

    public PullLinearLayout(Context context) {
        super(context);
    }

    public PullLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean canPullDown() {
        return true;
    }

    @Override
    public boolean canPullUp() {
        return true;
    }

}
