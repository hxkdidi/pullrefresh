package com.ken.pullview.indicator;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;


public class BallSpinFadeLoaderIndicator extends BaseIndicatorController {

    public static final float SCALE = 1.0f;

    public static final int ALPHA = 255;

    float[] scaleFloats = new float[]{SCALE,
            SCALE,
            SCALE,
            SCALE,
            SCALE,
            SCALE,
            SCALE,
            SCALE};

    int[] alphas = new int[]{ALPHA,
            ALPHA,
            ALPHA,
            ALPHA,
            ALPHA,
            ALPHA,
            ALPHA,
            ALPHA};


    @Override
    public void draw(Canvas canvas, Paint paint) {
        float radius = getWidth() / 10;
        for (int i = 0; i < 8; i++) {
            canvas.save();
            Point point = circleAt(getWidth(), getHeight(), getWidth() / 2 - radius, i * (Math.PI / 4));
            canvas.translate(point.x, point.y);
            canvas.scale(scaleFloats[i], scaleFloats[i]);
            paint.setAlpha(alphas[i]);
            canvas.drawCircle(0, 0, radius, paint);
            canvas.restore();
        }
    }

    /**
     * 圆O的圆心为(a,b),半径为R,点A与到X轴的为角α.
     * 则点A的坐标为(a+R*cosα,b+R*sinα)
     *
     * @param width
     * @param height
     * @param radius
     * @param angle
     * @return
     */
    Point circleAt(int width, int height, float radius, double angle) {
        float x = (float) (width / 2 + radius * (Math.cos(angle)));
        float y = (float) (height / 2 + radius * (Math.sin(angle)));
        return new Point(x, y);
    }

    List<ValueAnimator> valueAnimators;

    @Override
    public void createAnimation() {
        stopAnimation();
        if (valueAnimators == null) {
            valueAnimators = new ArrayList<>(3);
        }
        int[] delays = {0, 120, 240, 360, 480, 600, 720, 780, 840};
        for (int i = 0; i < 8; i++) {
            final int index = i;
            ValueAnimator scaleAnim = ValueAnimator.ofFloat(1, 0.4f, 1);
            valueAnimators.add(scaleAnim);
            scaleAnim.setDuration(500);
            scaleAnim.setRepeatCount(-1);
            scaleAnim.setStartDelay(delays[i]);
            scaleAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    scaleFloats[index] = (float) animation.getAnimatedValue();
                    postInvalidate();
                }
            });
            scaleAnim.start();
        }
    }

    @Override
    public void stopAnimation() {
        if (valueAnimators != null && valueAnimators.size() > 0) {
            for (ValueAnimator valueAnimator : valueAnimators) {
                if (valueAnimator != null) {
                    valueAnimator.cancel();
                }
            }
        }
        if (valueAnimators != null) {
            valueAnimators.clear();
            valueAnimators = null;
        }
    }

    final class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }


}
