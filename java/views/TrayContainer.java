import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public class TrayContainer extends PositionLayout {
    public TrayContainer(Context context) {
        super(context);
    }

    public TrayContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrayContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    public void placeTray(View view){
        if (view.getLayoutParams() == null)
            view.setLayoutParams(new PositionLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(view);
    }
}
