import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 17.12.2021, пятница
 **/
public class TrayController implements TrayUi.TrayPositionListener {
    private final TrayContainer trayContainer;
    private final GestureController gestureController;
    private final EnumMap<UiMode, TrayUi> trayUiEnumMap;
    private final EnumMap<UiMode, TrayPositionController> trayPositionControllerEnumMap;

    @SuppressLint("ClickableViewAccessibility")
    public TrayController(TrayContainer trayContainer) {
        this.trayContainer = trayContainer;
        this.trayUiEnumMap = new EnumMap<>(UiMode.class);
        this.trayPositionControllerEnumMap = new EnumMap<>(UiMode.class);
        this.gestureController = new GestureController(trayContainer);
        this.gestureController.setUiMode(UiMode.MODE_NONE);
        this.trayContainer.setOnInterceptTouchListener(motionEvent -> {
            gestureController.onTouchEvent(motionEvent);
            return false;
        });
        this.trayContainer.setOnTouchListener((v, event) -> gestureController.onTouchEvent(event));
    }


    public void attachTray(UiMode uiMode, View view, DragDirection dragDirection) {
        trayContainer.placeTray(view);
        TrayUi trayUi = createTray(uiMode, view, dragDirection);
        TrayPositionController positionController = createTrayPositionController(trayUi, dragDirection);
        trayPositionControllerEnumMap.put(uiMode, positionController);
        EdgeSwipeGestureRecognizer edgeSwipeGestureRecognizer = createRecognizer(positionController, dragDirection);
        gestureController.registerRecognizer(
                uiMode,
                edgeSwipeGestureRecognizer
        );
        gestureController.setUiMode(UiMode.MODE_NONE);
    }

    public void openTray(UiMode uiMode) {
        openTray(uiMode, true);
    }

    public void openTray(UiMode uiMode, boolean smooth) {
        TrayPositionController trayPositionController = getTrayControllerFor(uiMode);
        trayPositionController.openTray(smooth);
    }

    public void closeTray(UiMode uiMode) {
        closeTray(uiMode, true);
    }

    public void closeTray(UiMode uiMode, boolean smooth) {
        TrayPositionController trayPositionController = getTrayControllerFor(uiMode);
        trayPositionController.closeTray(smooth);
    }

    public void toggleTray(UiMode uiMode) {
        toggleTray(uiMode, true);
    }

    public void toggleTray(UiMode uiMode, boolean smooth) {
        TrayPositionController trayPositionController = getTrayControllerFor(uiMode);
        if (trayPositionController.isTrayOpen()) {
            trayPositionController.closeTray(smooth);
        } else if (trayPositionController.isTrayClosed()) {
            trayPositionController.openTray(smooth);
        }
    }

    public boolean isTrayOpened(UiMode uiMode){
        return getTrayControllerFor(uiMode).isTrayOpen();
    }

    public boolean isTrayClosed(UiMode uiMode){
        return getTrayControllerFor(uiMode).isTrayClosed();
    }

    public boolean isTrayBeingDragged(UiMode uiMode){
        return getTrayControllerFor(uiMode).isTrayBeingDragged();
    }

    public void addTrayPositionListener(UiMode uiMode, TrayUi.TrayPositionListener trayPositionListener){
        getTrayControllerFor(uiMode).addTrayPositionListener(trayPositionListener);
    }

    @NonNull
    public TrayPositionController getTrayControllerFor(UiMode uiMode) {
        if (trayPositionControllerEnumMap.containsKey(uiMode)) {
            TrayPositionController positionController = trayPositionControllerEnumMap.get(uiMode);
            if (positionController != null) {
                return positionController;
            } else {
                throw new RuntimeException(uiMode + " tray not attached to homeView!");
            }
        } else {
            throw new RuntimeException(uiMode + " tray not attached to homeView!");
        }
    }

    private TrayUi createTray(UiMode uiMode, View view, DragDirection dragDirection) {
        TrayUi trayUi = TrayUi.attach(view, dragDirection);
        trayUi.setPosition(TrayUi.TrayPosition.POSITION_RETRACTED, 0.0f);
        trayUi.addTrayPositionListener(this);
        trayUiEnumMap.put(uiMode, trayUi);
        return trayUi;
    }

    private TrayPositionController createTrayPositionController(TrayUi trayUi, DragDirection dragDirection) {
        return new TrayPositionController(
                trayUi,
                ViewConfiguration.get(trayContainer.getContext()).getScaledMinimumFlingVelocity(),
                Views.dpToPx(8),
                dragDirection,
                1f
        );
    }

    private EdgeSwipeGestureRecognizer createRecognizer(TrayPositionController trayPositionController, DragDirection dragDirection) {
        @DragGestureRecognizer.DragType
        int dragType;
        switch (dragDirection) {
            case FROM_TOP:
            case FROM_BOTTOM:
                dragType = DragGestureRecognizer.DragType.DRAG_VERTICAL;
                break;
            case FROM_LEFT:
            case FROM_RIGHT:
                dragType = DragGestureRecognizer.DragType.DRAG_HORIZONTAL;
                break;
            default:
                dragType = DragGestureRecognizer.DragType.DRAG_NONE;
        }
        return new EdgeSwipeGestureRecognizer(
                trayPositionController,
                ViewConfiguration.get(trayContainer.getContext()).getScaledTouchSlop() * 2,
                dragType
        );
    }


    @Override
    public void onTrayFullyOpen(TrayUi trayUi) {
        if (trayUiEnumMap.containsValue(trayUi)) {
            UiMode uiMode = getKeysByValue(trayUiEnumMap, trayUi);
            if (uiMode != null) {
                gestureController.setUiMode(uiMode);
            }
        }
    }

    @Override
    public void onTrayFullyRetracted(TrayUi trayUi) {
        gestureController.setUiMode(UiMode.MODE_NONE);
    }

    @Override
    public void onTrayPartiallyOpen(TrayUi trayUi, float progress) {
        if (trayUiEnumMap.containsValue(trayUi)) {
            UiMode uiMode = getKeysByValue(trayUiEnumMap, trayUi);
            if (uiMode != null) {
                gestureController.setUiMode(uiMode);
            }
        }
    }

    public static <T, E> T getKeysByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
