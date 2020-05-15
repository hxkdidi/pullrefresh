package com.ken.pullview.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ken.pullview.Pull;
import com.ken.pullview.R;
import com.ken.pullview.indicator.LoadingIndicatorView;

/**
 * 自定义的布局，用来管理三个子控件，其中一个是下拉头，一个是包含内容的pullView（可以是实现Pull接口的的任何View），
 */
public class PullRefreshLayout extends RelativeLayout {

    private static final String TAG = "PullRefreshLayout";
    /**
     * last y
     */
    private int mLastMotionY;
    /**
     * last x
     */
    private int mLastMotionX;
    // 初始状态
    public static final int INIT = 0;
    // 释放刷新
    public static final int RELEASE_TO_REFRESH = 1;
    // 正在刷新
    public static final int REFRESHING = 2;
    // 释放加载
    public static final int RELEASE_TO_LOAD = 3;
    // 正在加载
    public static final int LOADING = 4;
    // 操作完毕
    public static final int DONE = 5;
    // 当前状态
    private int state = INIT;
    // 刷新回调接口
    private OnRefreshListener mListener;
    // 刷新成功
    public static final int SUCCEED = 0;
    // 刷新失败
    public static final int FAIL = 1;
    // 按下Y坐标，上一个事件点Y坐标
    private float downY, lastY;

    // 下拉的距离。注意：pullDownY和pullUpY不可能同时不为0
    public float pullDownY = 0;
    // 上拉的距离
    private float pullUpY = 0;
    // 释放刷新的距离
    private float refreshDist = 200;
    // 释放加载的距离
    private float loadMoreDist = 200;
    // 第一次执行布局
    private boolean isLayout = false;
    // 在刷新过程中滑动操作
    private boolean isTouch = false;
    // 手指滑动距离与下拉头的滑动距离比，中间会随正切函数变化
    private float radio = 2;
    // 下拉布局
    private View refresh_head;
    // 下拉头
    private View refreshView;
    // 刷新结果：成功或失败
    private TextView refreshStateTextView;
    /**
     * 头部旋转view
     */
    private LoadingIndicatorView refresh_progress;
    /**
     * 底部部旋转view
     */
    private LoadingIndicatorView load_progress;
    // 下拉布局
    private View load_foot;
    // 上拉头
    private View loadMoreView;
    // 加载结果：成功或失败
    private TextView loadStateTextView;
    // 实现了Pull接口的View
    private View pullView;
    // 过滤多点触碰
    private int mEvents;
    // 这两个变量用来控制pull的方向，如果不加控制，当情况满足可上拉又可下拉时没法下拉
    private boolean canPullDown = true;
    private boolean canPullUp = true;
    /**
     * 是否可以下拉刷新
     */
    private boolean canRefresh = true;
    /**
     * 是否检查网络
     */
    private boolean detectNetWork = true;
    /**
     * 是否可以加载更多
     */
    private boolean canLoadMore = true;
    /**
     * 自动下拉刷新  默认false
     */
    private boolean autoRefresh = false;
    // 下拉 上拉 偏移一点
    private int offset = 0;
    private boolean isFirst = false;
    /**
     * 是否显示没有更多加载文字描述
     */
    private boolean isShowLoadMoreMessage = false;
    private String showLoadMoreMessage = "没有更多数据了";
    private TextView tvNoMore;
    private View linear;
    private static final int TYPE_INIT = 0;
    private static final int TYPE_REFRESH = 1;
    private static final int TYPE_LOAD = 2;
    private int type = TYPE_INIT;
    private Context context;
    private long TIME = 0;

    private void initView() {
        // 初始化下拉布局
        refresh_head = refreshView.findViewById(R.id.refresh_head);
        refresh_progress = (LoadingIndicatorView) refreshView.findViewById(R.id.refresh_progress);
        refreshStateTextView = (TextView) refreshView.findViewById(R.id.state_tv);
        // 初始化上拉布局
        load_foot = loadMoreView.findViewById(R.id.load_foot);
        tvNoMore = (TextView) loadMoreView.findViewById(R.id.tvNoMore);
        linear = loadMoreView.findViewById(R.id.linear);
        load_progress = (LoadingIndicatorView) loadMoreView.findViewById(R.id.load_progress);
        loadStateTextView = (TextView) loadMoreView.findViewById(R.id.load_state_tv);
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    public PullRefreshLayout(Context context) {
        super(context);
        initView(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        this.context = context;
        requestFocus();
    }

    private void relayoutRefresh(float y, final OnRefreshFinish finish) {
        float end = 0f;
        if (!isTouch) {
            // 正在刷新，且没有往上推的话则悬停，显示"正在刷新..."
            if (state == REFRESHING && y > refreshDist) {
                end = refreshDist;
            } else if (state == DONE && y == refreshDist) {
                end = 0;
            }
        }

        long time = 800L;
        if (Math.abs(y) > refreshDist) {
            time = 300L;
        } else {
            time = 200L;
        }

        ValueAnimator animation = ValueAnimator.ofFloat(y, end).setDuration(time);
        animation.cancel();
        animation.start();
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue());
                if (pullDownY > 0) {
                    pullDownY = value;
                }
                if (pullDownY <= 0) {
                    // 已完成回弹
                    pullDownY = 0;
                    refresh_progress.stopAnimation();
                    // 隐藏下拉头时有可能还在刷新，只有当前状态不是正在刷新时才改变状态
                    if (state != REFRESHING && state != LOADING) {
                        changeState(INIT);
                    }
                    if (finish != null) {
                        finish.onFinish();
                    }
                } else {
                    requestLayout();
                }
            }
        });
    }

    private void relayoutLoadMore(float y, final OnRefreshFinish finish) {
        float end = 0f;
        if (!isTouch) {
            // 正在刷新，且没有往上推的话则悬停，显示"正在刷新..."
            if (state == LOADING && -y > loadMoreDist) {
                end = -loadMoreDist;
            } else if (state == DONE) {
                end = 0f;
            }
        }
        long time = 800;
        if (Math.abs(y) > loadMoreDist) {
            time = 300;
        } else {
            time = 200;
        }
        ValueAnimator animation = ValueAnimator.ofFloat(y, end);
        animation.setDuration(time);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue());
                if (pullUpY < 0) {
                    pullUpY = value;
                }
                if (pullUpY >= 0) {
                    // 已完成回弹
                    pullUpY = 0;
                    load_progress.stopAnimation();
                    // 隐藏下拉头时有可能还在刷新，只有当前状态不是正在刷新时才改变状态
                    if (state != REFRESHING && state != LOADING) {
                        changeState(INIT);
                    }
                    if (finish != null) {
                        finish.onFinish();
                    }
                } else {
                    requestLayout();
                }
            }
        });
        animation.cancel();
        animation.start();
    }

    private void hide(OnRefreshFinish finish) {
        if (pullDownY > 0) {
            relayoutRefresh(pullDownY, finish);
            return;
        } else if (pullUpY < 0) {
            relayoutLoadMore(pullUpY, finish);
            return;
        }
        if (finish != null && pullDownY == 0 && pullUpY == 0) {
            finish.onFinish();
        }
    }

    private void changeState(int to) {
        if (linear == null) {
            return;
        }
        linear.setVisibility(VISIBLE);
        tvNoMore.setVisibility(GONE);
        state = to;
        switch (state) {
            case INIT:
                // 下拉布局初始状态
                refreshStateTextView.setText(R.string.pull_to_refresh);
                loadStateTextView.setText(R.string.pull_up_to_load);
                refresh_progress.stopAnimation();
                load_progress.stopAnimation();
                break;
            case RELEASE_TO_REFRESH:
                // 释放刷新状态
                refreshStateTextView.setText(R.string.release_to_refresh);
                refresh_progress.stopAnimation();
                load_progress.stopAnimation();
                break;
            case REFRESHING:
                // 正在刷新状态
                refreshStateTextView.setText(R.string.refreshing);
                refresh_progress.applyAnimation();
                break;
            case RELEASE_TO_LOAD:
                // 释放加载状态
                loadStateTextView.setText(R.string.release_to_load);
                break;
            case LOADING:
                // 正在加载状态
                loadStateTextView.setText(R.string.is_loading);
                load_progress.applyAnimation();
                break;
            case DONE:
                // 刷新或加载完毕
                refresh_progress.stopAnimation();
                load_progress.stopAnimation();
                break;
        }
    }

    /**
     * 不限制上拉或下拉
     */
    private void releasePull() {
        canPullDown = true;
        canPullUp = true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        int y = (int) e.getRawY();
        int x = (int) e.getRawX();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 首先拦截down事件,记录y坐标
                mLastMotionY = y;
                mLastMotionX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                // deltaY > 0 是向下运动< 0是向上运动
                int deltaY = y - mLastMotionY;
                int deltaX = x - mLastMotionX;
                if (Math.abs(deltaX * 3) > Math.abs(deltaY)) {
                    return false;
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return false;
    }

    /*
     * （非 Javadoc）由父控件决定是否分发事件，防止事件冲突
     */

    // 滑动距离及坐标
    private float xLast, yLast;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (canRefresh) {
            refresh_head.setVisibility(View.VISIBLE);
        } else {
            refresh_head.setVisibility(View.GONE);
        }

        if (canLoadMore) {
            load_foot.setVisibility(View.VISIBLE);
        } else {
            if (isShowLoadMoreMessage) {
                linear.setVisibility(GONE);
                tvNoMore.setVisibility(VISIBLE);
                tvNoMore.setText(showLoadMoreMessage);
            } else {
                load_foot.setVisibility(View.GONE);
            }
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                lastY = downY;
                mEvents = 0;
                releasePull();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // 过滤多点触碰
                mEvents = -1;
                break;
            case MotionEvent.ACTION_MOVE:
     /*防止水平和横向滑动冲突*/
                float curX = ev.getX();
                float curY = ev.getY();
                float yAdd = Math.abs(curY - yLast);
                float xAdd = Math.abs(curX - xLast);
                xLast = curX;
                yLast = curY;
                if (xAdd > yAdd) {
                    // 事件分发交给父类
                    super.dispatchTouchEvent(ev);
                    return true;
                }
    /*防止水平和横向滑动冲突*/
                if (mEvents == 0) {
                    if (((Pull) pullView).canPullDown() && canPullDown
                            && state != LOADING) {
                        // 可以下拉，正在加载时不能下拉
                        // 对实际滑动距离做缩小，造成用力拉的感觉
                        pullDownY = pullDownY + (ev.getY() - lastY) / radio;
                        if (pullDownY < 0) {
                            pullDownY = 0;
                            canPullDown = false;
                            canPullUp = true;
                        }
                        if (pullDownY > getMeasuredHeight())
                            pullDownY = getMeasuredHeight();
                        if (state == REFRESHING) {
                            // 正在刷新的时候触摸移动
                            isTouch = true;
                        }
                    } else if (((Pull) pullView).canPullUp() && canPullUp
                            && state != REFRESHING) {
                        // 可以上拉，正在刷新时不能上拉
                        pullUpY = pullUpY + (ev.getY() - lastY) / radio;
                        if (pullUpY > 0) {
                            pullUpY = 0;
                            canPullDown = true;
                            canPullUp = false;
                        }
                        if (pullUpY < -getMeasuredHeight())
                            pullUpY = -getMeasuredHeight();
                        if (state == LOADING) {
                            // 正在加载的时候触摸移动
                            isTouch = true;
                        }
                    } else
                        releasePull();
                } else
                    mEvents = 0;
                lastY = ev.getY();
                // 根据下拉距离改变比例
                radio = (float) (2 + 2 * Math.tan(Math.PI / 2 / getMeasuredHeight()
                        * (pullDownY + Math.abs(pullUpY))));
                requestLayout();
                if (pullDownY <= refreshDist && state == RELEASE_TO_REFRESH) {
                    // 如果下拉距离没达到刷新的距离且当前状态是释放刷新，改变状态为下拉刷新
                    if (canRefresh) {
                        changeState(INIT);
                    }
                }
                if (pullDownY >= refreshDist && state == INIT) {
                    // 如果下拉距离达到刷新的距离且当前状态是初始状态刷新，改变状态为释放刷新
                    if (canRefresh) {
                        changeState(RELEASE_TO_REFRESH);
                    }
                }
                // 下面是判断上拉加载的，同上，注意pullUpY是负值
                if (-pullUpY <= loadMoreDist && state == RELEASE_TO_LOAD) {
                    if (canLoadMore) {
                        changeState(INIT);
                    }
                }
                if (-pullUpY >= loadMoreDist && state == INIT) {
                    if (canLoadMore) {
                        changeState(RELEASE_TO_LOAD);
                    }
                }
                // 因为刷新和加载操作不能同时进行，所以pullDownY和pullUpY不会同时不为0，因此这里用(pullDownY +
                // Math.abs(pullUpY))就可以不对当前状态作区分了
                if ((pullDownY + Math.abs(pullUpY)) > 8) {
                    // 防止下拉过程中误触发长按事件和点击事件
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (pullDownY > refreshDist || -pullUpY > loadMoreDist)
                    // 正在刷新时往下拉（正在加载时往上拉），释放后下拉头（上拉头）不隐藏
                    isTouch = false;
                if (state == RELEASE_TO_REFRESH) {
                    if (canRefresh) {
                        changeState(REFRESHING);
                    }
                    // 刷新操作
                    if (mListener != null) {
                        if (canRefresh) {
                            type = TYPE_REFRESH;
                            TIME = System.currentTimeMillis();
                            if (!checkIsNetworkConnected()) {
                                finish(FAIL, new OnRefreshFinish() {
                                    @Override
                                    public void onFinish() {

                                    }
                                });
                            } else {
                                mListener.onRefresh(this);
                            }
                        }
                    }
                } else if (state == RELEASE_TO_LOAD) {
                    if (canLoadMore) {
                        changeState(LOADING);
                    }
                    // 加载操作
                    if (mListener != null)
                        if (canLoadMore) {
                            type = TYPE_LOAD;
                            TIME = System.currentTimeMillis();
                            if (!checkIsNetworkConnected()) {
                                finish(FAIL, new OnRefreshFinish() {
                                    @Override
                                    public void onFinish() {

                                    }
                                });
                            } else {
                                mListener.onLoadMore(this);
                            }
                        }
                }
                hide(null);
            default:
                break;
        }
        // 事件分发交给父类
        try {
            super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            Log.e(TAG, "dispatchTouchEvent: " + e.getMessage());
        }
        return true;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout");
        if (!isLayout) {
            // 这里是第一次进来的时候做一些初始化
            refreshView = getChildAt(0);
            pullView = getChildAt(1);
            loadMoreView = getChildAt(2);
            isLayout = true;
            initView();
            Log.d(TAG, "initView");
            refreshDist = ((ViewGroup) refreshView).getChildAt(0)
                    .getMeasuredHeight() + offset;
            loadMoreDist = ((ViewGroup) loadMoreView).getChildAt(0)
                    .getMeasuredHeight() + offset;
        }
        // 改变子控件的布局，这里直接用(pullDownY + pullUpY)作为偏移量，这样就可以不对当前状态作区分
        if (canRefresh && autoRefresh && !isFirst) {
            pullDownY = refreshDist;
            changeState(REFRESHING);
            isFirst = true;
            // 刷新操作
            if (mListener != null) {
                if (canRefresh && autoRefresh) {
                    type = TYPE_REFRESH;
                }
            }
        }
        refreshView.layout(0,
                (int) (pullDownY + pullUpY) - refreshView.getMeasuredHeight(),
                refreshView.getMeasuredWidth(), (int) (pullDownY + pullUpY));
        pullView.layout(0, (int) (pullDownY + pullUpY),
                pullView.getMeasuredWidth(), (int) (pullDownY + pullUpY)
                        + pullView.getMeasuredHeight());
        loadMoreView.layout(0,
                (int) (pullDownY + pullUpY) + pullView.getMeasuredHeight(),
                loadMoreView.getMeasuredWidth(),
                (int) (pullDownY + pullUpY) + pullView.getMeasuredHeight()
                        + loadMoreView.getMeasuredHeight());
        if (canLoadMore) {
            linear.setVisibility(VISIBLE);
            tvNoMore.setVisibility(GONE);
        } else {
            linear.setVisibility(GONE);
            tvNoMore.setVisibility(VISIBLE);
        }
    }

    /***
     * 是否允许下拉刷新
     *
     * @param canRefresh
     */
    public void setOnRefresh(boolean canRefresh) {
        this.canRefresh = canRefresh;
    }

    /***
     * 是否检查网络
     *
     * @param detectNetWork
     */
    public void setDetectNetWork(boolean detectNetWork) {
        this.detectNetWork = detectNetWork;
    }

    /***
     * 是否允许 上拉加载更多
     *
     * @param canLoadMore
     */
    public void setOnLoadMore(boolean canLoadMore) {
        this.canLoadMore = canLoadMore;
    }

    /**
     * 是否允许 上拉加载更多
     *
     * @param canLoadMore
     * @param str
     */
    public void setOnLoadMore(boolean canLoadMore, String str) {
        this.canLoadMore = canLoadMore;
        isShowLoadMoreMessage = !canLoadMore;
        showLoadMoreMessage = !TextUtils.isEmpty(str) ? str : showLoadMoreMessage;
        Log.d(TAG, "setOnLoadMore");
    }


    /**
     * TODO 有bug待修复
     *
     * @param autoRefresh
     */
    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
        isFirst = false;
        if (canRefresh && autoRefresh) {
            type = TYPE_REFRESH;
            requestLayout();
        }
    }

    private Handler handler = new Handler();

    public void finish(final int succeed, final OnRefreshFinish onRefreshFinish) {
        long Long = System.currentTimeMillis();
        long num = Long - TIME;
        if (num > 200) {
            setSucceed(succeed, onRefreshFinish);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setSucceed(succeed, onRefreshFinish);
                }
            }, 200 - num);
        }
    }

    /**
     * 判断是否有网
     *
     * @return
     */
    public boolean checkIsNetworkConnected() {
        if (!detectNetWork) {
            return true;
        }
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isAvailable() && info.isConnected());
    }

    private void setSucceed(int succeed, final OnRefreshFinish onRefreshFinish) {
        TIME = System.currentTimeMillis();
        if (type == TYPE_REFRESH) {
            switch (succeed) {
                case SUCCEED:
                    // 刷新成功
                    if (refreshStateTextView != null) {
                        refreshStateTextView.setText(context.getResources().getString(R.string.refresh_succeed));
                    }
                    break;
                case FAIL:
                default:
                    // 刷新失败
                    if (refreshStateTextView != null)
                        refreshStateTextView.setText(context.getResources().getString(R.string.refresh_fail));
                    break;
            }
            // 刷新结果停留200毫秒
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    changeState(DONE);
                    hide(onRefreshFinish);
                }
            }, 200);
        } else if (type == TYPE_LOAD) {
            switch (succeed) {
                case SUCCEED:
                    // 加载成功
                    if (loadStateTextView != null) {
                        loadStateTextView.setText(context.getResources().getString(R.string.load_succeed));
                    }
                    break;
                case FAIL:
                default:
                    // 加载失败
                    if (loadStateTextView != null) {
                        loadStateTextView.setText(context.getResources().getString(R.string.load_fail));
                    }
                    break;
            }
            // 刷新结果停留200毫秒
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    changeState(DONE);
                    hide(onRefreshFinish);
                }
            }, 200);
        } else {
            if (onRefreshFinish != null) {
                onRefreshFinish.onFinish();
            }
        }
    }

    /**
     * 刷新加载回调接口
     *
     * @author
     */
    public interface OnRefreshListener {
        /**
         * 刷新操作
         */
        void onRefresh(PullRefreshLayout pullRefreshLayout);

        /**
         * 加载操作
         */
        void onLoadMore(PullRefreshLayout pullRefreshLayout);
    }

    /**
     * 加载完成,头部和底部回弹完成
     */
    public interface OnRefreshFinish {

        /**
         * 加载操作
         */
        void onFinish();
    }
}
