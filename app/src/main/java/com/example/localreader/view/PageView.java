package com.example.localreader.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import com.example.localreader.listener.TouchListener;
import com.example.localreader.util.PageFactory;

/**
 * Created by xialijuan on 2020/12/22.
 */
public class PageView extends View {
    private Context context;
    /**
     * 屏幕宽
     */
    private int screenWidth = 0;
    /**
     * 屏幕高
     */
    private int screenHeight = 0;
    /**
     * 是否移动了
     */
    private boolean move = false;
    /**
     * 是否翻到下一页
     */
    private boolean next = false;
    /**
     * 是否取消翻页
     */
    private boolean cancelPage = false;
    /**
     * 是否没下一页或上一页
     */
    private boolean noNext = false;
    private int downX = 0;
    private int downY = 0;
    private int moveX = 0;
    private int moveY = 0;
    /**
     * 翻页动画是否在执行
     */
    private boolean running = false;
    /**
     * 当前页
     */
    private Bitmap curPageBitmap = null;
    private Bitmap nextPageBitmap = null;
    private BaseFlip baseFlip;
    private Scroller scroller;
    private int bgColor = 0xFFCEC29C;
    private TouchListener mTouchListener;

    public PageView(Context context) {
        this(context, null);
    }

    public PageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initPage();
        scroller = new Scroller(getContext(), new LinearInterpolator());
        baseFlip = new CoverFlip(curPageBitmap, nextPageBitmap, screenWidth, screenHeight);
    }

    private void initPage() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        curPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);
        nextPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);
    }

    public Bitmap getCurPage() {
        return curPageBitmap;
    }

    public Bitmap getNextPage() {
        return nextPageBitmap;
    }

    public void setBgColor(int color) {
        bgColor = color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(bgColor);
        if (running) {
            baseFlip.drawMove(canvas);
        } else {
            baseFlip.drawStatic(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (PageFactory.getStatus() == PageFactory.Status.OPENING) {
            return true;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        baseFlip.setTouchPoint(x, y);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            actionDown(event);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            actionMove(x,y);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            actionUp(x);
        }
        return true;
    }

    /**
     * 当屏幕检测到第一个触点按下之后就会触发到这个事件
     * @param event
     */
    private void actionDown(MotionEvent event){
        downX = (int) event.getX();
        downY = (int) event.getY();
        moveX = 0;
        moveY = 0;
        move = false;
        noNext = false;
        next = false;
        running = false;
        baseFlip.setStartPoint(downX, downY);
        abortAnimation();
    }

    /**
     * 当触点在屏幕上移动时触发，触点在屏幕上停留也是会触发的
     * @param x
     * @param y
     * @return
     */
    private boolean actionMove(int x,int y){
        final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        // 判断是否移动了
        if (!move) {
            move = Math.abs(downX - x) > slop || Math.abs(downY - y) > slop;
        }
        if (move) {
            move = true;
            if (moveX == 0 && moveY == 0) {
                //判断翻得是上一页还是下一页
                if (x - downX > 0) {
                    next = false;
                } else {
                    next = true;
                }
                cancelPage = false;
                if (next) {
                    boolean isNext = mTouchListener.nextPage();
                    baseFlip.setDirection(BaseFlip.Direction.next);
                    if (!isNext) {
                        noNext = true;
                        return true;
                    }
                } else {
                    boolean isPre = mTouchListener.upPage();
                    baseFlip.setDirection(BaseFlip.Direction.up);
                    if (!isPre) {
                        noNext = true;
                        return true;
                    }
                }
            } else {
                //判断是否取消翻页
                if (next) {
                    if (x - moveX > 0) {
                        cancelPage = true;
                        baseFlip.setCancel(true);
                    } else {
                        cancelPage = false;
                        baseFlip.setCancel(false);
                    }
                } else {
                    if (x - moveX < 0) {
                        baseFlip.setCancel(true);
                        cancelPage = true;
                    } else {
                        baseFlip.setCancel(false);
                        cancelPage = false;
                    }
                }
            }
            moveX = x;
            moveY = y;
            running = true;
            this.postInvalidate();
        }
        return false;
    }

    /**
     * 当触点松开时被触发
     * @param x
     * @return
     */
    private boolean actionUp(int x){
        if (!move) {
            cancelPage = false;
            //是否点击了中间
            if (downX > screenWidth / 5 && downX < screenWidth * 4 / 5 && downY > screenHeight / 3 && downY < screenHeight * 2 / 3) {
                if (mTouchListener != null) {
                    mTouchListener.center();
                }
                return true;
            } else if (x < screenWidth / 2) {
                next = false;
            } else {
                next = true;
            }
            if (next) {
                boolean isNext = mTouchListener.nextPage();
                baseFlip.setDirection(BaseFlip.Direction.next);
                if (!isNext) {
                    return true;
                }
            } else {
                boolean isPre = mTouchListener.upPage();
                baseFlip.setDirection(BaseFlip.Direction.up);
                if (!isPre) {
                    return true;
                }
            }
        }
        if (cancelPage && mTouchListener != null) {
            mTouchListener.cancel();
        }
        if (!noNext) {
            running = true;
            baseFlip.startSliding(scroller);
            this.postInvalidate();
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            float x = scroller.getCurrX();
            float y = scroller.getCurrY();
            baseFlip.setTouchPoint(x, y);
            if (scroller.getFinalX() == x && scroller.getFinalY() == y) {
                running = false;
            }
            postInvalidate();
        }
        super.computeScroll();
    }

    /**
     * 中止动画
     */
    public void abortAnimation() {
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
            baseFlip.setTouchPoint(scroller.getFinalX(), scroller.getFinalY());
            postInvalidate();
        }
    }

    public void setTouchListener(TouchListener mTouchListener) {
        this.mTouchListener = mTouchListener;
    }
}
