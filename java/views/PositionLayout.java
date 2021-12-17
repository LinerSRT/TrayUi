import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public class PositionLayout extends ViewGroup {
    private OnDrawListener onDrawListener;
    private OnInterceptTouchListener onInterceptTouchListener;


    public PositionLayout(Context context) {
        super(context);
    }

    public PositionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PositionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void placeChild(View view, float x, float y) {
        if (view.getParent() == this) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            layoutParams.x = x;
            layoutParams.y = y;
            view.offsetLeftAndRight(Math.round(x) - view.getLeft());
            view.offsetTopAndBottom(Math.round(y) - view.getTop());
            invalidate();
            return;
        }
        throw new RuntimeException(view.getClass().getSimpleName() + " is not a child of " + getClass().getSimpleName());
    }

    public void setOnInterceptTouchListener(OnInterceptTouchListener onInterceptTouchListener) {
        this.onInterceptTouchListener = onInterceptTouchListener;
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        this.onDrawListener = onDrawListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (onInterceptTouchListener != null)
            return onInterceptTouchListener.onInterceptTouch(motionEvent);
        return super.onInterceptTouchEvent(motionEvent);
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (onDrawListener != null) {
            onDrawListener.onPostChildrenDraw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        child.measure(
                getChildMeasureSpec(parentWidthMeasureSpec, layoutParams.leftMargin + layoutParams.rightMargin, layoutParams.width),
                getChildMeasureSpec(parentHeightMeasureSpec, layoutParams.topMargin + layoutParams.bottomMargin, layoutParams.height)
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            if (child.getVisibility() != GONE) {
                int x = Math.round(layoutParams.x);
                int y = Math.round(layoutParams.y);
                child.layout(
                        x,
                        y,
                        x+ child.getMeasuredWidth(),
                        y+ child.getMeasuredHeight()
                );
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams((MarginLayoutParams) p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @SuppressLint("CustomViewStyleable")
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public float x;
        public float y;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray typedArray = c.obtainStyledAttributes(attrs, R.styleable.PositionLayout);
            x = typedArray.getFloat(R.styleable.PositionLayout_pl_x, 0f);
            y = typedArray.getFloat(R.styleable.PositionLayout_pl_y, 0f);
            typedArray.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }

    public interface OnDrawListener {
        void onPostChildrenDraw(Canvas canvas);
    }

    public interface OnInterceptTouchListener {
        boolean onInterceptTouch(MotionEvent motionEvent);
    }
}
