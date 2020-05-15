package com.ken.pullview.indicator;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.View;

import com.ken.pullview.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * BallPulse
 * BallSpinFadeLoader
 */

public class LoadingIndicatorView extends View {

    //indicators
    public static final int BallPulse = 0;
    public static final int BallSpinFadeLoader = 1;


    @IntDef(flag = true,
            value = {
                    BallPulse,
                    BallSpinFadeLoader,
            })

    @Retention(RetentionPolicy.SOURCE)
    public @interface Indicator {
    }

    //Sizes (with defaults in DP)
    public static final int DEFAULT_SIZE = 45;

    //attrs
    int mIndicatorId;
    int mIndicatorColor;
    int mIndicatorSize;

    Paint mPaint;

    BaseIndicatorController mIndicatorController;

    private boolean mHasAnimation;


    public LoadingIndicatorView(Context context) {
        super(context);
        init(null, 0);
    }

    public LoadingIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LoadingIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LoadingIndicatorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.LoadingIndicatorView);
        mIndicatorId = a.getInt(R.styleable.LoadingIndicatorView_indicator, BallPulse);
        mIndicatorColor = a.getColor(R.styleable.LoadingIndicatorView_indicator_color, Color.WHITE);
        mIndicatorSize = a.getInteger(R.styleable.LoadingIndicatorView_indicator_size, DEFAULT_SIZE);
        a.recycle();
        mPaint = new Paint();
        mPaint.setColor(mIndicatorColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        applyIndicator();
    }

    private void applyIndicator() {
        switch (mIndicatorId) {
            case BallPulse:
                mIndicatorController = new BallPulseIndicator();
                break;
            case BallSpinFadeLoader:
                mIndicatorController = new BallSpinFadeLoaderIndicator();
                break;
        }
        mIndicatorController.setTarget(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureDimension(dp2px(mIndicatorSize), widthMeasureSpec);
        int height = measureDimension(dp2px(mIndicatorSize), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureDimension(int defaultSize, int measureSpec) {
        int result = defaultSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(defaultSize, specSize);
        } else {
            result = defaultSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawIndicator(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!mHasAnimation) {
            mHasAnimation = true;
        }
    }

    void drawIndicator(Canvas canvas) {
        mIndicatorController.draw(canvas, mPaint);
    }

    public void applyAnimation() {
        mIndicatorController.stopAnimation();
        mIndicatorController.createAnimation();
    }

    public void stopAnimation() {
        mIndicatorController.stopAnimation();
    }

    private int dp2px(int dpValue) {
        return (int) getContext().getResources().getDisplayMetrics().density * dpValue;
    }
}
