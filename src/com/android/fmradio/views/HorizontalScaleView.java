package com.android.fmradio.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * 刻度尺-横向
 *
 * @version 1.0
 */
public class HorizontalScaleView extends HorizontalScrollView {

    public static final int MIN_WIDTH = 100; // 最小宽度
    public static final int SCALE_SPACE = 1; // 刻度值间隔

    public static final int DIRECTION_TOP = 0; // 刻度绘制方向
    public static final int DIRECTION_BOTTOM = 1; // 刻度绘制方向


    private DrawView mDrawView;
    private OnScaleValueListener mOnScaleChangeListener;
    private OnScaleDrawListener mOnScaleDrawListener;
    private List<ScaleItem> mScaleItems = new ArrayList<ScaleItem>();

    private Integer mScaleStart; // 起始刻度值
    private Integer mScaleEnd; // 结束刻度值
    private Integer mScaleDefault; // 默认刻度值
    private int mScaleWidth = 10; // 单个刻度的宽度or高度
    private int mDirection = DIRECTION_BOTTOM;// 刻度绘制方向
    private int mScaleSelectValue=1000;//default value

    private boolean mIsEnable = true;//default enable==true

    public HorizontalScaleView(Context context) {
        super(context);
        init(context);
    }

    public HorizontalScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalScaleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HorizontalScaleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mDrawView = new DrawView(context);
        mDrawView.setMinimumWidth(MIN_WIDTH);
        mDrawView.setMinimumHeight(MIN_WIDTH);
        addView(mDrawView);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.mIsEnable = enabled;
        super.setEnabled(enabled);
    }

    /**
     * 设置选中刻度值监听
     *
     * @param listener 监听器
     */
    public void setOnScaleChangeListener(OnScaleValueListener listener) {
        this.mOnScaleChangeListener = listener;
    }

    /**
     * 设置刻度绘制监听
     *
     * @param listener 监听器
     */
    public void setOnScaleDrawListener(OnScaleDrawListener listener) {
        this.mOnScaleDrawListener = listener;
    }

    /**
     * 设置刻度值的范围
     *
     * @param start 起始起始刻度值
     * @param end   结束刻度值
     */
    public void setScaleRange(int start, int end) {
        this.mScaleStart = start;
        this.mScaleEnd = end;
        if (mScaleEnd < mScaleStart) {
            throw new IllegalArgumentException("end不能小于start的值");
        }
    }

    /**
     * 设置默认刻度值
     *
     * @param def 刻度值
     */
    public void setScaleDefault(int def) {
        this.mScaleDefault = def;
        if (mScaleDefault < mScaleStart) {
            throw new IllegalArgumentException("def不能小于start的值");
        } else if (mScaleDefault > mScaleEnd) {
            throw new IllegalArgumentException("def不能大于end的值");
        }
    }

    /**
     * 获取刻度绘制方向
     *
     * @return
     */
    public int getDirection() {
        return mDirection;
    }

    /**
     * 设置刻度绘制方向
     *
     * @param direction
     */
    public void setDirection(int direction) {
        this.mDirection = direction;
    }

    /**
     * 获取刻度间隔宽度
     *
     * @return
     */
    public int getScaleWidth() {
        return mScaleWidth;
    }

    /**
     * 设置刻度间隔宽度
     *
     * @param scaleWidth
     */
    public void setScaleWidth(int scaleWidth) {
        this.mScaleWidth = scaleWidth;
    }

    /**
     * 设置选中刻度索引
     *
     * @param position
     * @param isScroll 是否校准滑动位置
     */
    private void setScaleSelectPosition(int position, boolean isScroll) {
        ScaleItem scaleItem = mScaleItems.get(position);
        final int selectPoint = scaleItem.point - getWidth() / 2;
        if (isScroll) {
            post(new Runnable() {
                @Override
                public void run() {
                    scrollTo(selectPoint, 0);
                }
            });
        }

        // 通知监听者
        if (mOnScaleChangeListener != null) {
            mOnScaleChangeListener.onValueChange(scaleItem.value);
        }
    }

    /**
     * 设置选中刻度索引
     *
     * @param position
     */
    public void setScaleSelectPosition(int position) {
        setScaleSelectPosition(position, true);
    }

    /**
     * 设置选中刻度值
     *
     * @param value
     */
    public void setScaleSelectValue(int value) {
        mScaleSelectValue=value;
        if (value < mScaleStart) {
            value = mScaleStart;
        } else if (value > mScaleEnd) {
            value = mScaleEnd;
        }
        setScaleSelectPosition((value - mScaleStart) / SCALE_SPACE);
    }

    public int getScaleSelectValue(){
        return mScaleSelectValue;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(!mIsEnable){
            return true;
        }
        int x = getScrollX();
        // 计算当前选中项，四舍五入
        int selectItem = Math.round((float) x / (float) mScaleWidth);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
                // 设置选中项
//                setScaleSelectPosition(selectItem, false);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 校准选中位置
                setScaleSelectPosition(selectItem);
                return false;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed && mScaleItems.size() == 0) {
            computeScaleItems(left, top, right, bottom);
        }
    }

    /**
     * 计算出所有的刻度坐标
     */
    private void computeScaleItems(int left, int top, int right, int bottom) {
        int width = right - left;
        int pointStart = width / 2;
        // 计算每个刻度的坐标
        int itemCount = (mScaleEnd - mScaleStart) / SCALE_SPACE;
        for (int i = 0; i <= itemCount; i++) {
            ScaleItem item = new ScaleItem();
            item.point = pointStart + (i * mScaleWidth);
            item.value = mScaleStart + (i * SCALE_SPACE);
            mScaleItems.add(item);
        }
        // 计算刻度尺总高度
        int drawWidth = ((mScaleEnd - mScaleStart) / SCALE_SPACE * mScaleWidth) + width;
        mDrawView.getLayoutParams().width = drawWidth;
        mDrawView.setMinimumWidth(drawWidth);
        // 设置默认选中位置
        setScaleSelectValue(mScaleDefault == null ? 0 : mScaleDefault);
    }

    /**
     * 绘制刻度尺的View
     */
    private class DrawView extends View {
        public DrawView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(60);
            textPaint.setAntiAlias(true);

            Paint linePaint=new Paint();
            linePaint.setColor(Color.WHITE);
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(4.0f);

            // 将所有刻度绘制出来
            for (ScaleItem item : mScaleItems) {
                // 是否已绘制
                boolean isDraw = false;
                if (mOnScaleDrawListener != null) {
                    // 自定义绘制方法
                    isDraw = mOnScaleDrawListener.onDrawScale(canvas, item.value, item.point, mDirection);
                }

                if (!isDraw) {
                    // 默认实现的绘制方法
                    if (mDirection == DIRECTION_BOTTOM) {
                        if (item.value % 5 == 0) {
                            // 大刻度
                            canvas.drawLine(item.point, getHeight() * 0.5F, item.point, getHeight(), linePaint);
                            canvas.drawText(getValueShow(item.value), item.point - (mScaleWidth * 1), getHeight() * 0.3F, textPaint);//(getHeight() * 0.75F)
                        } else {
                            // 普通小刻度
                            canvas.drawLine(item.point, getHeight() * 0.5F, item.point, getHeight() * 0.8F, linePaint);
                        }
                    } else {
                        if (item.value % 5 == 0) {
                            // 大刻度
                            canvas.drawLine(item.point, getHeight() * 0.5F, item.point, getHeight(), linePaint);
                            canvas.drawText(getValueShow((item.value)), item.point - (mScaleWidth * 1), getHeight() * 0.3F, textPaint);//(getHeight() * 0.35F)
                        } else {
                            // 普通小刻度
                            canvas.drawLine(item.point, getHeight() * 0.5F, item.point, getHeight() * 0.8F, linePaint);
                        }
                    }
                }
            }
        }

        /**
         * 获取要显示的刻度值
         *
         * @param value 刻度值
         * @return 要显示的内容
         */
        private String getValueShow(int value) {
            String text = null;
            if (mOnScaleChangeListener != null) {
                text = mOnScaleChangeListener.getValueText(value);
            }
            if (TextUtils.isEmpty(text)) {
                text = String.valueOf((float) value / 10);
            }
            return text;
        }
    }

    /**
     * 刻度（包含坐标、刻度值）
     */
    private static class ScaleItem {
        // 坐标
        int point;
        // 刻度值
        int value;

        @Override
        public String toString() {
            return "[" + value + "=" + point + "]";
        }
    }

    /**
     * 刻度值监听器
     *
     * @version 1.0
     */
    public interface OnScaleValueListener {

        /**
         * 选中刻度值改变
         * value 当前选中项的值
         */
        void onValueChange(int value);

        /**
         * 显示选中刻度值
         * value 当前选中项的值
         *
         * @return 要显示的内容
         */
        String getValueText(int value);

    }

    /**
     * 刻度绘制监听器
     *
     * @version 1.0
     */
    public interface OnScaleDrawListener {

        /**
         * 绘制刻度
         * canvas 画板
         * value 刻度值
         * point 刻度坐标
         * direction 刻度绘制方向
         *
         * @return 是否已处理（true:已处理,false:使用默认方式）
         */
        boolean onDrawScale(Canvas canvas, int value, int point, int direction);

    }

}
