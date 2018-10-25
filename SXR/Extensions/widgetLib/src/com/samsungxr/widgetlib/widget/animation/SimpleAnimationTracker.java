package com.samsungxr.widgetlib.widget.animation;

import android.util.Pair;

import com.samsungxr.widgetlib.log.Log;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXROnFinish;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static com.samsungxr.utility.Threads.spawn;

public final class SimpleAnimationTracker {
    private final ConcurrentHashMap<SXRSceneObject, Pair<SXRAnimation, SXROnFinish>> tracker;
    private final LinkedBlockingQueue<AnimationRequest> requestQueue;

    public final Runnable clearTracker = new Runnable() {

        @Override
        public void run() {
            clear();
        }
    };

    /**
     * Creates SimpleAnimationTracker
     */
    public SimpleAnimationTracker(SXRContext gvrContext) {
        tracker = new ConcurrentHashMap<SXRSceneObject, Pair<SXRAnimation, SXROnFinish>>();
        requestQueue = new LinkedBlockingQueue<AnimationRequest>();
        // start animation request worker thread
        spawn(new AnimationWorker(requestQueue));
    }


    public void clear() {
        if (tracker != null) {
            tracker.clear();
        }
        if (requestQueue != null) {
            requestQueue.clear();
        }
    }

    public void interrupt(final SXRSceneObject target) {
        stop(target, tracker.remove(target));
    }


    public boolean interruptAll() {
        boolean ret = false;
        for (SXRSceneObject target: tracker.keySet()) {
            interrupt(target);
            ret = true;
        }
        return ret;
    }

    public boolean inProgress(final SXRSceneObject target) {
        return tracker.containsKey(target);
    }

    private void stop(final SXRSceneObject target, final Pair<SXRAnimation, SXROnFinish> pair) {
        if (null != pair) {
            target.getSXRContext().getAnimationEngine().stop(pair.first);
            Log.v(TAG, "stopping running animation for target %s",
                  target.getName());
            runUserFinisher(pair);
        }
    }

    public void track(final SXRSceneObject target, final SXRAnimation anim) {
        track(target, anim, null, null);
    }

    public void track(final SXRSceneObject target, final SXRAnimation anim, final SXROnFinish finisher) {
        track(target, anim, null, finisher);
    }

    public void track(final SXRSceneObject target, final SXRAnimation anim, final Runnable starter, final SXROnFinish finisher) {
        if (target == null || anim == null) {
            return;
        }
        // add request to the queue
        try {
            requestQueue.put(new AnimationRequest(target, anim, starter, finisher));
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, e, "track()");
        }
    }

    class AnimationRequest {
        private SXRAnimation anim;
        private SXROnFinish finisher;
        private Runnable starter;
        private SXRSceneObject target;
        AnimationRequest(final SXRSceneObject target, final SXRAnimation anim, final Runnable starter, final SXROnFinish finisher) {
            this.target = target;
            this.anim = anim;
            this.finisher = finisher;
            this.starter = starter;
        }

        void process() {
            final Pair<SXRAnimation, SXROnFinish> pair;
            pair = tracker
                        .put(target, new Pair<SXRAnimation, SXROnFinish>(
                                anim, finisher));

            stop(target, pair);

            anim.setOnFinish(new SXROnFinish() {
                @Override
                public final void finished(final SXRAnimation animation) {
                    final Pair<SXRAnimation, SXROnFinish> pair;
                    pair = tracker.remove(target);
                    if (null != pair) {
                        runUserFinisher(pair);
                    }
                }
            });

            if (starter != null) {
                starter.run();
            }
            anim.start(target.getSXRContext().getAnimationEngine());
        }
    }

    private static class AnimationWorker implements Runnable {
        private final LinkedBlockingQueue<AnimationRequest> queue;

        public AnimationWorker(LinkedBlockingQueue<AnimationRequest> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // Get the next animation request item off of the queue
                    AnimationRequest request = queue.take();

                    // Process animation request
                    if (request != null) {
                        request.process();
                    }
                }
                catch ( InterruptedException ie ) {
                    break;
                }
            }
        }
    }

    private void runUserFinisher(final Pair<SXRAnimation, SXROnFinish> pair) {
        if (null != pair.second) {
            try {
                pair.second.finished(pair.first);
            } catch (final Exception e) {
                Log.e(TAG, "exception in finisher", e);
            }
        }
    }

    private static final String TAG = "SimpleAnimationTracker";
}
