import android.annotation.IntDef;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public interface GestureRecognizer {
    @IntDef({Result.OBSERVE, Result.IGNORE, Result.CAPTURE, Result.PROTECT})
    @Retention(RetentionPolicy.SOURCE)
    @interface Result {
        int OBSERVE = 0;
        int IGNORE = 1;
        int CAPTURE = 2;
        int PROTECT = 3;
    }

    void onCapturedTouchEvent(@NonNull View view, @NonNull MotionEvent motionEvent, @NonNull VelocityProvider velocityProvider);

    @Result
    int onObservedTouchEvent(@NonNull View view, @NonNull MotionEvent motionEvent, @NonNull VelocityProvider velocityProvider);
}