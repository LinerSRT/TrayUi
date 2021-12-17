import android.util.Pools;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 16.12.2021, четверг
 **/
public class SharedVelocityTracker implements VelocityProvider {
    private static final Pools.SimplePool<SharedVelocityTracker> pool = new Pools.SimplePool<>(1);
    private boolean dirty = false;
    private VelocityTracker velocityTracker;

    public static SharedVelocityTracker obtain() {
        SharedVelocityTracker acquire = pool.acquire();
        if (acquire == null) {
            acquire = new SharedVelocityTracker();
        }
        acquire.initialize();
        return acquire;
    }

    void initialize() {
        this.dirty = false;
        this.velocityTracker = VelocityTracker.obtain();
    }

    public void recycle() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
            pool.release(this);
        }
    }

    public void addMovement(MotionEvent motionEvent) {
        if (velocityTracker != null) {
            dirty = true;
            velocityTracker.addMovement(motionEvent);
        }
    }

    @Override
    public float getXVelocity() {
        ensureFreshData();
        if (velocityTracker != null)
            return velocityTracker.getXVelocity();
        return 0;
    }

    @Override
    public float getXVelocity(int id) {
        ensureFreshData();
        if (velocityTracker != null)
            return velocityTracker.getXVelocity(id);
        return 0;
    }

    @Override
    public float getYVelocity() {
        if (this.velocityTracker != null) {
            ensureFreshData();
            if (velocityTracker != null)
                return velocityTracker.getYVelocity();
        }
        return 0;
    }

    @Override
    public float getYVelocity(int id) {
        ensureFreshData();
        if (velocityTracker != null)
            return velocityTracker.getYVelocity(id);
        return 0;
    }

    private void ensureFreshData() {
        if (dirty) {
            if (velocityTracker != null)
                velocityTracker.computeCurrentVelocity(1000);
            dirty = false;
        }
    }

    public void clear() {
        if (velocityTracker != null)
            velocityTracker.clear();
    }
}