import android.annotation.IntDef;
import android.view.View;
import android.view.ViewGroup;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public class TrayUi implements View.OnLayoutChangeListener {
    @TrayPosition
    private int trayPosition;
    private float position;
    private final DragDirection dragDirection;
    private final View view;
    private final List<TrayPositionListener> listeners = new ArrayList<>();


    public static TrayUi attach(View view, DragDirection dragDirection) {
        TrayUi trayUi = new TrayUi(view, dragDirection);
        view.addOnLayoutChangeListener(trayUi);
        return trayUi;
    }

    private TrayUi(View view, DragDirection dragDirection) {
        this.view = view;
        this.dragDirection = dragDirection;
    }

    public void setPosition(@TrayPosition int trayPosition, float position){
        int previousTrayPosition = this.trayPosition;
        this.trayPosition = trayPosition;
        this.position = position;
        applyPosition();
        for(TrayPositionListener listener:listeners){
            if(previousTrayPosition == POSITION_RETRACTED && trayPosition != POSITION_RETRACTED) {
                listener.onBeforeTrayOpen(this);
            } else if(trayPosition == POSITION_OPEN){
                listener.onTrayFullyOpen(this);
            } else if(trayPosition == POSITION_RETRACTED){
                listener.onTrayFullyRetracted(this);
            } else if(trayPosition == POSITION_ABSOLUTE){
                listener.onTrayPartiallyOpen(this, getTrayOpenProgress());
            }
        }
    }

    public void addTrayPositionListener(TrayPositionListener trayPositionListener) {
        listeners.add(trayPositionListener);
    }

    public float getLeadingEdge() {
        return dragDirection.getLeadingEdge(view, getAbsolutePosition(trayPosition));
    }

    public float getMinPosition() {
        return dragDirection.getMinPosition(view);
    }

    public float getMaxPosition() {
        return dragDirection.getMaxPosition(view);
    }

    public float getTrayOpenProgress() {
        return Math2.clamp(dragDirection.getDragProgress(view, getAbsolutePosition(trayPosition)), 0.0f, 1.0f);
    }

    public int getTrayParentSizeAlongDragAxis() {
        if (view.getParent() != null) {
            return dragDirection.getSizeAlongDragAxis((View) view.getParent());
        }
        return 0;
    }

    @TrayPosition
    public int getPositionMode() {
        return trayPosition;
    }

    public float getPosition() {
        return getAbsolutePosition(trayPosition);
    }

    public float getAbsolutePosition(@TrayPosition int trayPosition) {
        switch (trayPosition){
            case POSITION_RETRACTED:
                return dragDirection.getRetractedPosition(view);
            case POSITION_ABSOLUTE:
                return position;
            case POSITION_OPEN:
               return dragDirection.getOpenPosition(view);
        }
        throw new RuntimeException("Illegal position: " + trayPosition);
    }

    private void applyPosition() {
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        if(!(viewGroup instanceof PositionLayout))
            return;
        PositionLayout positionLayout = (PositionLayout) viewGroup;
        if (trayPosition == POSITION_ABSOLUTE) {
            dragDirection.setAbsolutePosition(view, getPosition());
            return;
        }
        dragDirection.setTranslation(view, 0);
        dragDirection.placeChildInParent(view, positionLayout, getPosition());
    }

    @Override
    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        applyPosition();
    }

    public void incrementPosition(float position) {
        setPosition(POSITION_ABSOLUTE, dragDirection.applyDeltaInOpenDirection(getPosition(), position));
    }


    public interface TrayPositionListener {
        default void onTrayFullyOpen(TrayUi trayUi) {
        }

        default void onTrayFullyRetracted(TrayUi trayUi) {
        }

        default void onTrayPartiallyOpen(TrayUi trayUi, float progress) {
        }

        default void onBeforeTrayOpen(TrayUi trayUi) {

        }
    }

    @IntDef({TrayPosition.POSITION_ABSOLUTE, TrayPosition.POSITION_OPEN, TrayPosition.POSITION_OPEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TrayPosition{
        int POSITION_ABSOLUTE = 2;
        int POSITION_OPEN = 0;
        int POSITION_RETRACTED = 1;
    }

}