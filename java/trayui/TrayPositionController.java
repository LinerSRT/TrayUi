import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.IntDef;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public class TrayPositionController implements DragRecognizerClient {
    private static final long TRAY_ZOOP_TO_FINGER_DURATION = 300;
    private final DragDirection dragDirection;
    private boolean dragging;
    private boolean lockedOpen;
    private final int fingerFudgeWidth;
    private final float minFlingVelocity;
    private final RelativeTrayPositionAnimator scootUnderFingerAnimator;
    private Animate trayAnimator;
    @TrayStatus
    private int trayStatus;
    private final TrayUi trayUi;
    private final float watchFaceDragHeightThreshold;

    @IntDef({TrayStatus.STATUS_OPEN, STATUS_RETRACTED})
    @Retention(RetentionPolicy.SOURCE)
    @interface TrayStatus {
        int STATUS_OPEN = 0;
        int STATUS_RETRACTED = 1;
    }

    public TrayPositionController(
            TrayUi trayUi,
            float minFlingVelocity,
            int fingerFudgeWidth,
            DragDirection dragDirection,
            float watchFaceDragHeightThreshold
    ) {
        this.trayStatus = STATUS_RETRACTED;
        this.dragging = false;
        this.trayUi = trayUi;
        this.minFlingVelocity = minFlingVelocity;
        this.fingerFudgeWidth = fingerFudgeWidth;
        this.scootUnderFingerAnimator = new RelativeTrayPositionAnimator(trayUi);
        this.dragDirection = dragDirection;
        this.watchFaceDragHeightThreshold = watchFaceDragHeightThreshold;
    }

    private void dragTrayBy(float x, float y) {
        float position = trayUi.getPosition();
        float dragDelta = dragDirection.applyDragDelta(position, x, y);
        float clamp = Math2.clamp(dragDelta, trayUi.getMinPosition(), trayUi.getMaxPosition());
        trayUi.setPosition(TrayPosition.POSITION_ABSOLUTE, clamp);
    }

    private void snapToStablePosition(float velocityX, float velocityY) {
        if (dragDirection.isVelocityEnoughForFling(velocityX, velocityY, minFlingVelocity)) {
            trayStatus = dragDirection.isVelocityInOpenDirection(velocityX, velocityY) ? STATUS_OPEN : STATUS_RETRACTED;
        } else {
            if (isTrayAnimating())
                return;
            float openProgress = trayUi.getTrayOpenProgress();
            switch (trayStatus) {
                case STATUS_OPEN:
                    trayStatus = openProgress <= .5f ? STATUS_RETRACTED : STATUS_OPEN;
                    break;
                case STATUS_RETRACTED:
                    trayStatus = openProgress >= .5f ? STATUS_OPEN : STATUS_RETRACTED;
                    break;
            }
        }
        animateToState(trayStatus);
    }

    @Override
    public void onDragStart(float startX, float startY) {
        dragging = true;
        scootUnderFingerAnimator.cancel();
        float distanceToFinger = dragDirection.getDistanceToFinger(startX, startY, fingerFudgeWidth, trayUi.getLeadingEdge());
        if (distanceToFinger > 0f) {
            scootUnderFingerAnimator.start(distanceToFinger, TRAY_ZOOP_TO_FINGER_DURATION, new AccelerateDecelerateInterpolator());
        }
    }

    @Override
    public void onDrag(float startX, float startY, float endX, float endY) {
        if (dragging)
            dragTrayBy(endX, endY);
    }

    @Override
    public void onDragEnd(float startX, float startY, float endX, float endY, boolean cancelAction) {
        if (dragging) {
            endDrag();
            snapToStablePosition(endX, endY);
        }
    }

    @Override
    public boolean validateDrag(float x, float y, float f3, float f4, float f5, float f6, boolean z) {
        if (trayStatus == TrayStatus.STATUS_OPEN) {
            return ((trayUi.getPositionMode() == TrayUi.TrayPosition.POSITION_ABSOLUTE) || (dragDirection.isVelocityInCloseDirection(f5 - x, f6 - y) && !lockedOpen)) && dragDirection.isPointWithinLeadingEdge(x, y, fingerFudgeWidth, trayUi.getLeadingEdge());
        } else if (trayStatus == STATUS_RETRACTED) {
            return dragDirection.isPointWithinDragStartThreshold(
                    x,
                    y,
                    ((float) trayUi.getTrayParentSizeAlongDragAxis()) * watchFaceDragHeightThreshold,
                    trayUi.getLeadingEdge(),
                    trayUi.getMaxPosition()) && dragDirection.isVelocityInOpenDirection(f5 - x, f6 - y);
        } else {
            throw new RuntimeException("Illegal status: " + trayStatus);
        }
    }

    @Override
    @RecognizeResult
    public int onTouchDown(MotionEvent motionEvent) {
        if (isTrayAnimating()) {
            return RecognizeResult.RESULT_START_DRAG;
        }
        return RecognizeResult.RESULT_WAIT;
    }

    public void addTrayPositionListener(TrayUi.TrayPositionListener trayPositionListener) {
        trayUi.addTrayPositionListener(trayPositionListener);
    }

    public boolean isTrayBeingDragged() {
        return dragging;
    }

    public boolean isTrayOpen() {
        return trayStatus == STATUS_OPEN;
    }

    public boolean isTrayClosed() {
        return trayStatus == STATUS_RETRACTED;
    }

    public boolean isTrayAnimating() {
        return trayAnimator != null && trayAnimator.isStarted();
    }

    public void closeTray(boolean smooth) {
        if (smooth) {
            animateToState(STATUS_RETRACTED);
        } else {
            snapToState(STATUS_RETRACTED);
        }
    }


    public void openTray(boolean smooth) {
        if (smooth) {
            animateToState(STATUS_OPEN);
        } else {
            snapToState(STATUS_OPEN);
        }
    }

    public void snapToState(@TrayStatus int status) {
        trayStatus = status;
        trayUi.setPosition(status == STATUS_OPEN ? TrayPosition.POSITION_OPEN : TrayPosition.POSITION_RETRACTED, 0.0f);
    }


    public void animateToState(@TrayStatus int status) {
        endDrag();
        @TrayPosition
        int trayPosition = status == STATUS_OPEN ? TrayPosition.POSITION_OPEN : TrayPosition.POSITION_RETRACTED;
        trayAnimator = new Animate.Builder(new Animate.Callback() {
            @Override
            public void onUpdate(ValueAnimator animator) {
                trayUi.setPosition(TrayPosition.POSITION_ABSOLUTE, (Float) animator.getAnimatedValue("trayPosition"));
            }

            @Override
            public void onStart() {

            }

            @Override
            public void onFinish() {
                trayStatus = status;
                trayUi.setPosition(trayPosition, 0.0f);
            }
        })
                .widthDuration(300)
                .withInterpolator(new AccelerateDecelerateInterpolator())
                .withValues(PropertyValuesHolder.ofFloat("trayPosition", trayUi.getPosition(), trayUi.getAbsolutePosition(trayPosition)))
                .build();
        if (trayPosition != trayUi.getPositionMode()) {
            trayAnimator.stop();
            scootUnderFingerAnimator.cancel();
            if (!trayAnimator.isRunning()) {
                trayAnimator.start();
            } else {
                snapToState(status);
            }
        }
    }

    private void endDrag() {
        dragging = false;
        scootUnderFingerAnimator.cancel();
    }

    public void setLockedOpen(boolean lockedOpen) {
        this.lockedOpen = lockedOpen;
    }



    public static class RelativeTrayPositionAnimator extends SimpleFloatAnimator {
        private float prevValue = 0.0f;
        private final TrayUi trayUi;

        public RelativeTrayPositionAnimator(TrayUi trayUi) {
            this.trayUi = trayUi;
        }

        @Override
        public void start(float from, float to, long duration, TimeInterpolator timeInterpolator) {
            throw new UnsupportedOperationException("Call start(delta) instead.");
        }

        public void start(float f, long j, TimeInterpolator timeInterpolator) {
            this.prevValue = 0.0f;
            super.start(0.0f, f, j, timeInterpolator);
        }

        @Override
        protected void onUpdate(float value) {
            this.trayUi.incrementPosition(value - this.prevValue);
            this.prevValue = value;
        }
    }
}