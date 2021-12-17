import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public class EdgeSwipeGestureRecognizer implements GestureRecognizer {
    @DragGestureRecognizer.DragType
    private final int dragType;
    private final DragGestureRecognizer dragGestureRecognizer;

    public EdgeSwipeGestureRecognizer(DragRecognizerClient dragRecognizerClient, int touchSlop, @DragGestureRecognizer.DragType int dragType) {
        this.dragType = dragType;
        dragGestureRecognizer = new DragGestureRecognizer(touchSlop);
        dragGestureRecognizer.setShouldTrackVelocity(false);
        dragGestureRecognizer.setClient(dragType, dragRecognizerClient);
    }

    @Override
    @Result
    public int onObservedTouchEvent(@NonNull View view, @NonNull MotionEvent motionEvent, @NonNull VelocityProvider velocityProvider) {
        dragGestureRecognizer.onTouchEvent(motionEvent, velocityProvider);
        int validated = dragGestureRecognizer.getValidatedDrag();
        if(validated == dragType)
            return CAPTURE;
        if(validated == DragGestureRecognizer.DragType.DRAG_NONE)
            return OBSERVE;
        return IGNORE;
    }

    @Override
    public void onCapturedTouchEvent(@NonNull View view, @NonNull MotionEvent motionEvent, @NonNull VelocityProvider velocityProvider) {
        dragGestureRecognizer.onTouchEvent(motionEvent, velocityProvider);
    }
}