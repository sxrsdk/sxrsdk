package com.samsungxr.widgetlib.content_scene;

import android.view.MotionEvent;

import com.samsungxr.widgetlib.log.Log;
import org.joml.Vector3f;

import static com.samsungxr.utility.Log.tag;
import com.samsungxr.widgetlib.widget.FlingHandler;

/**
 * The basic {@link ContentSceneController.ContentScene} implementation supporting
 * left-right-up-down fling
 */
abstract public class ScrollableContentScene implements ContentSceneController.ContentScene {
    private String TAG = tag(ScrollableContentScene.class);
    private final int SCROLL_THRESHOLD = 50;
    private FlingHandler mFlingHandler = new FlingHandler() {
        private float startX;
        private float endX;
        private float startY;
        private float endY;

        @Override
        public boolean onStartFling(MotionEvent event, Vector3f cursorPosition) {
            endX = startX = event.getX();
            endY = startY = event.getY();
            Log.d(Log.SUBSYSTEM.INPUT, TAG, "startDrag: start = [%f, %f] end = [%f, %f] - %s",
                    startX, startY, endX, endY, event);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event, Vector3f cursorPosition) {
            if (startX == 0) {
                startX = event.getX();
            }
            endX = event.getX();

            if (startY == 0) {
                startY = event.getY();
            }
            endY = event.getY();
            Log.d(Log.SUBSYSTEM.INPUT, TAG, "drag: start = [%f, %f] end = [%f, %f] - %s",
                    startX, startY, endX, endY, event);

            return true;
        }

        @Override
        public void onEndFling(FlingAction fling) {
            Log.d(Log.SUBSYSTEM.INPUT, TAG, "endDrag: start = [%f, %f] end = [%f, %f] - %s",
                    startX, startY, endX, endY, fling);
            if (endX - startX > 0) {
                scrollLeft();
            } else {
                scrollRight();
            }
            endX = startX = 0;

            if (endY - startY > 0) {
                if(endY - startY > SCROLL_THRESHOLD) {
                    scrollUp();
                }
            } else {
                if(startY - endY > SCROLL_THRESHOLD) {
                    scrollDown();
                }
            }
            endY = startY = 0;
        }
    };

    @Override
    public FlingHandler getFlingHandler() {
        return mFlingHandler;
    }

    /**
     * Use one combination of Left-Right or Up-Down, but not both
     */
    protected void scrollLeft() {
    }

    /**
     * Use one combination of Left-Right or Up-Down, but not both
     */
    protected void scrollRight() {
    }

    /**
     * Use one combination of Left-Right or Up-Down, but not both
     */
    protected void scrollUp() {
    }

    /**
     * Use one combination of Left-Right or Up-Down, but not both
     */
    protected void scrollDown() {
    }

}
