package com.ken.pullview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.ken.pullview.Pull;


public class PullScrollView extends ScrollView implements Pull {

    public PullScrollView(Context context) {
        super(context);
    }

    public PullScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean canPullDown() {
        return getScrollY() == 0;
    }

    @Override
    public boolean canPullUp() {
        return getScrollY() >= (getChildAt(0).getHeight() - getMeasuredHeight());
    }

}
