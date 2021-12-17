import android.annotation.IntDef;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.liner.linerlauncher.util.Math2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public class DragGestureRecognizer {
    private DragDescriptor currentDrag;
    private long downTime;
    private float prevX;
    private float prevY;
    private float startX;
    private float startY;
    private final int touchSlop;
    private VelocityTracker velocityTracker;
    private final DragDescriptor[] dragDescriptors;
    private final DragDescriptor dragNone = new DragDescriptor(DragType.DRAG_NONE);
    private final DragDescriptor dragHorizontal = new DragDescriptor(DRAG_HORIZONTAL);
    private final DragDescriptor dragHorizontalStrict = new DragDescriptor(DRAG_HORIZONTAL_STRICT);
    private final DragDescriptor dragVertical = new DragDescriptor(DRAG_VERTICAL);
    private final DragDescriptor dragFreeForm = new DragDescriptor(DRAG_FREE_FORM);
    private boolean trackVelocity = true;
    @RecognitionMode
    private int mode = 0;
    private int prevAction = -1;
    private final PointF syntheticPoint = new PointF();
    private final PointF unusedPoint = new PointF();



    public DragGestureRecognizer(int touchSlop) {
        this.dragDescriptors = new DragDescriptor[]{
                dragNone,
                dragHorizontal,
                dragVertical,
                dragFreeForm
        };
        this.currentDrag = dragNone;
        this.touchSlop = touchSlop;
    }

    @DragType
    public int getValidatedDrag() {
        if (currentDrag.valid)
            return currentDrag.type;
        return DragType.DRAG_NONE;
    }


    public void setClient(@DragType int dragType, DragRecognizerClient dragRecognizerClient) {
        DragDescriptor dragDescriptor = getDrag(dragType);
        if (dragDescriptor != this.dragNone) {
            dragDescriptor.client = dragRecognizerClient;
            return;
        }
        throw new IllegalArgumentException("Invalid drag type: " + dragType);
    }

    public void setRecognitionMode(@RecognitionMode int mode) {
        this.mode = mode;
    }

    public void setShouldTrackVelocity(boolean trackVelocity) {
        this.trackVelocity = trackVelocity;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return onTouchEvent(motionEvent, null);
    }

    public boolean onTouchEvent(MotionEvent motionEvent, VelocityProvider velocityProvider) {
        boolean intercepted = false;
        float xVelocity;
        float yVelocity;
        if (trackVelocity || velocityProvider != null) {
            int actionMasked = motionEvent.getActionMasked();
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            boolean initialTouch = true;
            switch (actionMasked){
                case MotionEvent.ACTION_DOWN:
                    if (!(prevAction == MotionEvent.ACTION_DOWN && motionEvent.getDownTime() == downTime && x == startX && y == startY))
                        initialTouch = false;
                    if (!initialTouch) {
                        downTime = motionEvent.getDownTime();
                        startX = x;
                        startY = y;
                        prevX = x;
                        prevY = y;
                        if (velocityTracker != null) {
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }
                        if (trackVelocity) {
                            velocityTracker = VelocityTracker.obtain();
                            velocityTracker.addMovement(motionEvent);
                        }
                        this.currentDrag = testForPreemptedDragStart(motionEvent);
                        if (currentDrag != dragNone) {
                            validateDragAndEmitStartEvent(currentDrag, startX, startY, startX, startY, startX, startY, true);
                            intercepted = currentDrag.valid;
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (velocityTracker != null)
                        velocityTracker.addMovement(motionEvent);
                    if (currentDrag != dragNone) {
                        emitProgressEvent(currentDrag, x, y, x - prevX, y - prevY);
                    } else {
                        DragDescriptor dragDescriptor = characterizeDrag(x, y);
                        if (dragDescriptor != dragNone) {
                            currentDrag = dragDescriptor;
                            PointF dragStartPoint = getDragStartPoint(x, y);
                            validateDragAndEmitStartEvent(currentDrag, startX, startY, dragStartPoint.x, dragStartPoint.y, x, y, false);
                            emitProgressEvent(currentDrag, x, y, x - dragStartPoint.x, y - dragStartPoint.y);
                        }
                    }
                    prevX = x;
                    prevY = y;
                    intercepted = currentDrag != dragNone && currentDrag.valid;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (currentDrag != dragNone) {
                        if (velocityTracker != null) {
                            velocityTracker.computeCurrentVelocity(1000);
                            xVelocity = velocityTracker.getXVelocity();
                            yVelocity = velocityTracker.getYVelocity();
                            velocityTracker.recycle();
                            velocityTracker = null;
                        } else if (velocityProvider != null) {
                            yVelocity = velocityProvider.getYVelocity();
                            xVelocity = velocityProvider.getXVelocity();
                        } else {
                            xVelocity = 0.0f;
                            yVelocity = 0.0f;
                        }
                        emitEndEvent(currentDrag, x, y, xVelocity, yVelocity, actionMasked == MotionEvent.ACTION_CANCEL);
                    }
                    currentDrag = dragNone;
                    break;
                default:
                    intercepted = false;
            }
            prevAction = actionMasked;
            return intercepted;
        }
        throw new IllegalArgumentException("velocityProvider cannot be null if trackVelocity is false.");
    }

    private void validateDragAndEmitStartEvent(DragDescriptor dragDescriptor, float f, float f2, float x, float y, float f5, float f6, boolean z) {
        dragDescriptor.valid = true;
        if (dragDescriptor.client != null)
            dragDescriptor.valid = dragDescriptor.client.validateDrag(f, f2, x, y, f5, f6, z);
        if (dragDescriptor.client != null && dragDescriptor.valid)
            dragDescriptor.client.onDragStart(x, y);
    }

    private void emitProgressEvent(DragDescriptor dragDescriptor, float x, float y, float dX, float dY) {
        if (dragDescriptor.client != null && dragDescriptor.valid) {
            dragDescriptor.client.onDrag(x, y, dX, dY);
        }
    }

    private void emitEndEvent(DragDescriptor dragDescriptor, float x, float y, float xVelocity, float yVelocity, boolean z) {
        if (dragDescriptor.client != null && dragDescriptor.valid) {
            dragDescriptor.client.onDragEnd(x, y, xVelocity, yVelocity, z);
        }
    }

    private DragDescriptor testForPreemptedDragStart(MotionEvent motionEvent) {
        for (DragDescriptor dragDescriptor : dragDescriptors) {
            if (dragDescriptor.client != null){
                int result = dragDescriptor.client.onTouchDown(motionEvent);
                if(result == DragRecognizerClient.RecognizeResult.RESULT_START_DRAG)
                    return dragDescriptor;
            }
        }
        return dragNone;
    }

    private DragDescriptor getDrag(@DragType int dragType) {
        switch (dragType) {
            case DRAG_VERTICAL:
                return dragVertical;
            case DRAG_HORIZONTAL:
                return dragHorizontal;
            case DRAG_FREE_FORM:
                return dragFreeForm;
            case DRAG_HORIZONTAL_STRICT:
                return dragHorizontalStrict;
            default:
                return dragNone;
        }
    }

    private PointF getDragStartPoint(float x, float y) {
        if (Math2.intersectCircleAndLine(prevX, prevY, x, y, startX, startY, touchSlop, syntheticPoint, unusedPoint) == 1)
            return syntheticPoint;
        throw new RuntimeException(String.format(Locale.getDefault(), "Invalid number of intersections for line ((%f, %f), (%f, %f)) and circle ((%f, %f), %d)", prevX, prevY, x, y, startX, startY, touchSlop));
    }

    private DragDescriptor characterizeDrag(float x, float y) {
        if (Math2.distance(startX, startY, x, y) < ((float) touchSlop))
            return dragNone;
        if (mode == MODE_FREE_FORM)
            return dragFreeForm;
        if (Math.abs(x - startX) >= Math.abs(y - startY) * 2f && dragHorizontalStrict.client != null)
            return dragHorizontalStrict;
        if (Math.abs(x - startX) >= Math.abs(y - startY))
            return dragHorizontal;
        return dragVertical;
    }

    public static class DragDescriptor {
        public DragRecognizerClient client;
        @DragType
        public int type;
        public boolean valid = true;

        public DragDescriptor(@DragType int type) {
            this.type = type;
        }
    }


    @IntDef({DragType.DRAG_NONE, DRAG_VERTICAL, DRAG_HORIZONTAL, DRAG_HORIZONTAL_STRICT, DRAG_FREE_FORM, DragType.DRAG_INVALID})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DragType {
        int DRAG_NONE = 0;
        int DRAG_VERTICAL = 1;
        int DRAG_HORIZONTAL = 2;
        int DRAG_FREE_FORM = 3;
        int DRAG_INVALID = 4;
        int DRAG_HORIZONTAL_STRICT = 5;
    }

    @IntDef({MODE_FREE_FORM, RecognitionMode.MODE_SNAP_TO_AXIS})
    @Retention(RetentionPolicy.SOURCE)
    @interface RecognitionMode {
        int MODE_SNAP_TO_AXIS = 0;
        int MODE_FREE_FORM = 1;
    }
}
