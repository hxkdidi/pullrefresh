package com.ken.pullview.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;

import com.ken.pullview.Pull;

public class PullRecyclerView extends RecyclerView implements Pull {

    // 当作listView 使用
    public static final int ListView = 0;
    // GridView 使用
    public static final int GridView = 1;
    // 瀑布流 使用
    public static final int Waterfall = 2;
    private int Style = ListView;

    public PullRecyclerView(Context context) {
        super(context);
    }

    public PullRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void setStyle(int style) {
        Style = style;
    }

    public int getStyle() {
        return Style;
    }

    @Override
    public boolean canPullDown() {  // 是否允许下拉刷新
        int count;
        LayoutManager layoutManager = getLayoutManager();
        switch (Style) {
            case ListView:
            case GridView:
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager lm = (LinearLayoutManager) layoutManager;
                    if (lm != null) {
                        count = lm.getItemCount();
                        if (count == 0) {
                            // 没有item的时候也可以下拉刷新
                            return true;
                        } else if (lm.findFirstVisibleItemPosition() > 0) {
                            return false;
                        } else // 滑到ListView的顶部了
                            return lm.findViewByPosition(0) != null &&
                                    lm.findViewByPosition(0).getVisibility() == View.VISIBLE &&
                                    lm.findViewByPosition(0).getTop() >= 0;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            case Waterfall:
                if (layoutManager instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                    if (staggeredGridLayoutManager != null) {
                        count = staggeredGridLayoutManager.getItemCount();
                        if (count == 0) {
                            // 没有item的时候也可以下拉刷新
                            return true;
                        } else // 滑到ListView的顶部了
                            return staggeredGridLayoutManager.findViewByPosition(0) != null &&
                                    staggeredGridLayoutManager.findViewByPosition(0).getVisibility() == View.VISIBLE &&
                                    staggeredGridLayoutManager.findViewByPosition(0).getTop() >= 0;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
        }
        return false;
    }

    @Override
    public boolean canPullUp() {  // 是否允许上拉刷新
        int count = 0;
        LayoutManager layoutManager = getLayoutManager();
        switch (Style) {
            case ListView:
            case GridView:
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager lm = (LinearLayoutManager) layoutManager;
                    if (lm != null) {
                        count = lm.getItemCount();
                        if (count <= 0) {
                            // 没有item的时候也可以上拉加载
                            return true;
                        } else if (lm.findLastVisibleItemPosition() < count - 1) {
                            // 当前可见不是最后一个,没滑到底部了
                            return false;
                        } else if (lm.findViewByPosition(count - 1) != null &&
                                lm.findViewByPosition(count - 1).getVisibility() == View.VISIBLE) {
                            int a = lm.findViewByPosition(count - 1).getBottom();
                            int b = lm.findViewByPosition(count - 1).getHeight();
                            int c = lm.getHeight();
                            // 滑到底部了
                            return c >= a;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            case Waterfall:
                if (layoutManager instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                    if (staggeredGridLayoutManager != null) {
                        count = staggeredGridLayoutManager.getItemCount();
                        if (count <= 0) {
                            // 没有item的时候也可以上拉加载
                            return true;
                        } else if (staggeredGridLayoutManager.findViewByPosition(count - 1) != null &&
                                staggeredGridLayoutManager.findViewByPosition(count - 1).getVisibility() == View.VISIBLE) {
                            // 获取到最后一个item
                            int spanCount = staggeredGridLayoutManager.getSpanCount();
                            boolean isBottom = false;
                            for (int i = 0; i < spanCount; i++) {
                                isBottom = checkPositionIsBottom(staggeredGridLayoutManager, count - 1 - i);
                                if (!isBottom) {
                                    return isBottom;
                                }
                            }
                            return isBottom;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
        }
        return false;
    }

    private boolean checkPositionIsBottom(StaggeredGridLayoutManager manager, int postion) {
        View view = manager.findViewByPosition(postion);
        if (view == null) return true;
        int a = view.getBottom();
        int b = view.getHeight();
        int c = manager.getHeight();
        // 滑到底部了
        return c >= a;
    }
}
