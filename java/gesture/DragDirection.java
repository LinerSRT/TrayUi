import android.annotation.IntDef;
import android.util.Log;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public enum DragDirection {
    FROM_TOP(1, 1),
    FROM_BOTTOM(1, -1),
    FROM_LEFT(2, 1),
    FROM_RIGHT(2, -1);
    @Orientation
    private final int dragType;
    private final int sigNum;

    DragDirection(@Orientation int dragType, int sigNum) {
        if (!(dragType == Orientation.HORIZONTAL || dragType == Orientation.VERTICAL))
            throw new RuntimeException("Orientation should be HORIZONTAL or VERTICAL!");
        this.dragType = dragType;
        this.sigNum = sigNum;
    }

    @Orientation
    public int getDragType() {
        return dragType;
    }

    public int getSizeAlongDragAxis(View view) {
        if (dragType == Orientation.VERTICAL) {
            return view.getHeight();
        }
        return view.getWidth();
    }

    public float getOpenPosition(View view) {
        return 0f;
    }

    public float getRetractedPosition(View view) {
        return (float) ((-getSizeAlongDragAxis(view)) * sigNum);
    }

    public void setAbsolutePosition(View view, float position) {
        if (dragType == Orientation.VERTICAL)
            view.setY(position);
        else
            view.setX(position);
    }

    public void setTranslation(View view, int translation) {
        if (dragType == Orientation.VERTICAL)
            view.setTranslationY((float) (translation * sigNum));
        else
            view.setTranslationX((float) (translation * sigNum));
    }

    public void placeChildInParent(View view, PositionLayout positionLayout, float position) {
        if (dragType == Orientation.VERTICAL)
            positionLayout.placeChild(view, 0.0f, position);
        else
            positionLayout.placeChild(view, position, 0.0f);
    }

    public float getMinPosition(View view) {
        if (sigNum == 1)
            return (float) (-getSizeAlongDragAxis(view));
        return 0f;
    }

    public float getMaxPosition(View view) {
        if (sigNum == 1)
            return 0f;
        return (float) getSizeAlongDragAxis(view);
    }

    public float getLeadingEdge(View view, float position) {
        if (sigNum == 1)
            return position + ((float) getSizeAlongDragAxis(view));
        return position;
    }

    public float getDragProgress(View view, float position) {
        if (sigNum == 1) {
            return getLeadingEdge(view, position) / ((float) getSizeAlongDragAxis(view));
        }
        return 1f - (getLeadingEdge(view, position) / ((float) getSizeAlongDragAxis(view)));
    }

    public float getDistanceToFinger(float x, float y, int endX, float endY) {
        return (((dragType == Orientation.VERTICAL ? y : x) - ((float) (endX * sigNum))) - endY) * ((float) sigNum);
    }

    public float applyDeltaInOpenDirection(float x, float y) {
        return x + (y * ((float) sigNum));
    }

    public float applyDragDelta(float position, float x, float y) {
        return position + (dragType == Orientation.VERTICAL ? y : x);
    }

    public float getDragVelocity(float x, float y) {
        return dragType == Orientation.VERTICAL ? y : x;
    }

    public boolean isVelocityEnoughForFling(float velocityX, float velocityY, float minimalFlingVelocity) {
        return Math.abs(dragType == Orientation.VERTICAL ? velocityY : velocityX) > minimalFlingVelocity;
    }

    public boolean isVelocityInOpenDirection(float velocityX, float velocityY) {
        return Math.signum(dragType == Orientation.VERTICAL ? velocityY : velocityX) == sigNum;
    }

    public boolean isVelocityInCloseDirection(float x, float y) {
        return Math.signum((dragType == Orientation.VERTICAL ? y : x)) == ((float) (-sigNum));
    }

    public boolean isPointWithinDragStartThreshold(float x, float y, float size, float leadingEdge, float f5) {
        return sigNum == 1 ?
                (dragType == Orientation.VERTICAL ? y : x) <= Math.max(size, leadingEdge) :
                (dragType == Orientation.VERTICAL ? y : x) >= Math.min(f5 - size, leadingEdge);
    }


    public boolean isPointWithinLeadingEdge(float x, float y, int offset, float leadingEdge) {
        return sigNum == 1 ?
                (dragType == Orientation.VERTICAL ? y : x) - ((float) offset) < leadingEdge :
                (dragType == Orientation.VERTICAL ? y : x) + ((float) offset) > leadingEdge;
    }


    @IntDef({Orientation.HORIZONTAL, Orientation.VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    @interface Orientation {
        int HORIZONTAL = 2;
        int VERTICAL = 1;
    }
}