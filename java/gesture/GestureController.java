import android.annotation.IntDef;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.liner.linerlauncher.trayui.UiMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
@SuppressWarnings("unused")
public class GestureController implements GestureRegistry{
    @CaptureMode
    private int captureMode;
    private SharedVelocityTracker sharedVelocityTracker;
    private final View view;
    private long eventTimeMillis;
    private List<RecognizerWrapper> recognizerWrapperList;
    private List<RecognizerWrapper> pendingRecognizers;
    private final Registry gestureRegistry = new Registry();
    private GestureRecognizer capturingRecognizer;
    private RecognizerWrapper capturingWrapper;

    public GestureController(View view) {
        this.view = view;
        this.captureMode = UNDECIDED;
        this.eventTimeMillis = -1;
        recognizerWrapperList = new ArrayList<>();
        capturingRecognizer = null;
        capturingWrapper = null;
    }

    public void setUiMode(UiMode uiMode){
        if(uiMode == UiMode.MODE_NONE){
            this.pendingRecognizers = gestureRegistry.getAllRecognizers();
        } else {
            this.pendingRecognizers = gestureRegistry.getRecognizers(uiMode);
        }
    }

    @CaptureMode
    public int getCaptureMode() {
        return captureMode;
    }

    private void markAlLAsReady(List<RecognizerWrapper> recognizerWrappers){
        for (RecognizerWrapper wrapper : recognizerWrappers)
            wrapper.status = RecognizerStatus.STATUS_READY;
    }

    @CallSuper
    public boolean onTouchEvent(@NonNull MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        boolean intercepted = false;
        switch (actionMasked) {
            case MotionEvent.ACTION_OUTSIDE:
                return false;
            case MotionEvent.ACTION_DOWN:
                if (motionEvent.getEventTime() == eventTimeMillis) {
                    return true;
                } else {
                    eventTimeMillis = motionEvent.getEventTime();
                    captureMode = UNDECIDED;
                    sharedVelocityTracker = SharedVelocityTracker.obtain();
                    capturingRecognizer = null;
                    capturingWrapper = null;
                    if(pendingRecognizers != null){
                        recognizerWrapperList = pendingRecognizers;
                        pendingRecognizers = null;
                    }
                    markAlLAsReady(recognizerWrapperList);
                    break;
                }
        }
        sharedVelocityTracker.addMovement(motionEvent);
        switch (captureMode) {
            case UNDECIDED:
                for (RecognizerWrapper recognizerWrapper : recognizerWrapperList) {
                    int status = recognizerWrapper.status;
                    if (status != RecognizerStatus.STATUS_INACTIVE) {
                        if (status == RecognizerStatus.STATUS_READY)
                            recognizerWrapper.status = RecognizerStatus.STATUS_OBSERVING;
                        int touchResult = recognizerWrapper.recognizer.onObservedTouchEvent(view, motionEvent, sharedVelocityTracker);
                        if (touchResult != GestureRecognizer.Result.OBSERVE) {
                            switch (touchResult) {
                                case GestureRecognizer.Result.CAPTURE:
                                    captureMode = CaptureMode.CAPTURED;
                                    capturingRecognizer = recognizerWrapper.recognizer;
                                    capturingWrapper = recognizerWrapper;
                                    recognizerWrapper.status = RecognizerStatus.STATUS_CAPTURING;
                                    deactivateNonCapturingRecognizers(motionEvent.getX(), motionEvent.getY());
                                    intercepted = true;
                                    break;
                                case GestureRecognizer.Result.IGNORE:
                                    recognizerWrapper.status = RecognizerStatus.STATUS_INACTIVE;
                                    recognizerWrapper.recognizer.onObservedTouchEvent(view, makeCancelEvent(motionEvent.getX(), motionEvent.getY()), sharedVelocityTracker);
                                    break;
                                case GestureRecognizer.Result.PROTECT:
                                    captureMode = CaptureMode.PROTECTED;
                                    deactivateNonCapturingRecognizers(motionEvent.getX(), motionEvent.getY());
                                    break;
                                default:
                                    throw new IllegalStateException("Illegal capture result: " + touchResult);
                            }
                        }
                        if (touchResult == GestureRecognizer.Result.CAPTURE || touchResult == GestureRecognizer.Result.PROTECT) {
                            intercepted = true;
                            break;
                        }
                    }
                }
                break;
            case CAPTURED:
                capturingRecognizer.onCapturedTouchEvent(view, motionEvent, sharedVelocityTracker);
                break;
            case PROTECTED:
                throw new IllegalStateException("Unhandled dispatch mode: " + captureMode);

        }

        if (actionMasked == MotionEvent.ACTION_CANCEL || actionMasked == MotionEvent.ACTION_UP) {
            sharedVelocityTracker.recycle();
        }

        return intercepted;
    }

    private MotionEvent makeCancelEvent(float x, float y) {
        return MotionEvent.obtain(eventTimeMillis, SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, x, y, 0);
    }

    private void deactivateNonCapturingRecognizers(float x, float y) {
        MotionEvent cancelEvent = makeCancelEvent(x, y);
        for (RecognizerWrapper wrapper : recognizerWrapperList) {
            if (wrapper.status != RecognizerStatus.STATUS_CAPTURING) {
                if (wrapper.status == RecognizerStatus.STATUS_OBSERVING)
                    wrapper.recognizer.onObservedTouchEvent(view, cancelEvent, sharedVelocityTracker);
                wrapper.status = RecognizerStatus.STATUS_INACTIVE;
            }
        }
        cancelEvent.recycle();
    }

    @Override
    public void registerRecognizer(UiMode uiMode, GestureRecognizer gestureRecognizer) {
         gestureRegistry.registerRecognizer(uiMode, gestureRecognizer);
    }

    public static class Registry implements GestureRegistry {
        private final EnumMap<UiMode, List<RecognizerWrapper>> library;

        private Registry() {
            this.library = new EnumMap<>(UiMode.class);
        }

        @Override
        public void registerRecognizer(UiMode uiMode, GestureRecognizer gestureRecognizer) {
            List<RecognizerWrapper> recognizerWrappers = library.get(uiMode);
            if (recognizerWrappers == null) {
                recognizerWrappers = new ArrayList<>();
                library.put(uiMode, recognizerWrappers);
            }
            recognizerWrappers.add(new RecognizerWrapper(gestureRecognizer));
            library.put(uiMode, recognizerWrappers);
        }

        public List<RecognizerWrapper> getRecognizers(UiMode uiMode) {
            List<RecognizerWrapper> recognizerWrappers = library.get(uiMode);
            if (recognizerWrappers != null)
                return recognizerWrappers;
            recognizerWrappers = new ArrayList<>();
            library.put(uiMode, recognizerWrappers);
            return recognizerWrappers;
        }

        public List<RecognizerWrapper> getAllRecognizers(){
            List<RecognizerWrapper> recognizerWrappers = new ArrayList<>();
            for(List<RecognizerWrapper> wrappers:library.values())
                recognizerWrappers.addAll(wrappers);
            return recognizerWrappers;
        }
    }


    public static class RecognizerWrapper {
        @RecognizerStatus
        public int status = 0;
        public final GestureRecognizer recognizer;

        public RecognizerWrapper(@NonNull GestureRecognizer gestureRecognizer) {
            this.recognizer = gestureRecognizer;
        }
    }

    @IntDef({CaptureMode.CAPTURED, CaptureMode.PROTECTED, UNDECIDED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CaptureMode {
        int CAPTURED = 1;
        int PROTECTED = 2;
        int UNDECIDED = 0;
    }

    @IntDef({RecognizerStatus.STATUS_READY, RecognizerStatus.STATUS_OBSERVING, RecognizerStatus.STATUS_INACTIVE, RecognizerStatus.STATUS_CAPTURING})
    @Retention(RetentionPolicy.SOURCE)
    @interface RecognizerStatus {
        int STATUS_CAPTURING = 3;
        int STATUS_INACTIVE = 0;
        int STATUS_OBSERVING = 2;
        int STATUS_READY = 1;
    }
}