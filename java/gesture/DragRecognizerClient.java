import android.annotation.IntDef;
import android.view.MotionEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public interface DragRecognizerClient {
    @IntDef({RecognizeResult.RESULT_WAIT, RecognizeResult.RESULT_START_DRAG})
    @Retention(RetentionPolicy.SOURCE)
    @interface RecognizeResult {
        int RESULT_START_DRAG = 1;
        int RESULT_WAIT = 0;
    }

    void onDrag(float x, float y, float dX, float dY);

    void onDragEnd(float x, float y, float xVelocity, float yVelocity, boolean z);

    void onDragStart(float x, float y);

    @RecognizeResult
    int onTouchDown(MotionEvent motionEvent);

    boolean validateDrag(float f, float f2, float f3, float f4, float f5, float f6, boolean z);
}