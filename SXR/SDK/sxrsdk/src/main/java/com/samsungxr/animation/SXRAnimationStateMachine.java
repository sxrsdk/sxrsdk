/* Copyright 2018 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr.animation;


import com.samsungxr.SXRContext;


import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Finite state machine that can start and stop animations.
 * <p>
 * This FSM has predefined actions to play and stop animations.
 * It has an {@link SXRAnimationQueue} which contains a set of
 * loaded animations. The state machine can start or stop
 * any of these by name.
 * @see SXRStateMachine
 */
public class SXRAnimationStateMachine extends SXRStateMachine implements SXRAnimationQueue.IAnimationQueueEvents
{
    protected final SXRAnimationQueue mAnimQueue;

    /**
     * Action to play a specific animation.
     */
    public static class Play extends Action
    {
        private String  mAnimName;

        /**
         * Construction an action which plays a given animation.
         * <p>
         * The animation must be known to the {@link SXRAnimationQueue}
         * associated with this state machine.
         * </p>
         * @param sm        {@link SXRStateMachine} which owns this action.
         * @param animName  String with name of animation to play.
         */
        public Play(SXRStateMachine sm, String animName)
        {
            super(sm, "playanimation");
            mAnimName = animName;
        }

        /**
         * Construction an action which plays a given animation.
         * <p>
         * The animation must be known to the {@link SXRAnimationQueue}
         * associated with this state machine.
         * </p>
         * @param sm        {@link SXRStateMachine} which owns this action.
         * @param json      JSONObject with animation properties.
         */
        public Play(SXRStateMachine sm, JSONObject json) throws JSONException
        {
            super(sm, json);
            mAnimName = json.getString("name");
        }

        /**
         * Get the name of the animation this action will play.
         * @return String with animation name.
         */
        public String getAnimationName() { return mAnimName; }

        public void run()
        {
            ((SXRAnimationStateMachine) mStateMachine).getAnimationQueue().startNext(mAnimName);
        }

        public String asJSON()
        {
            String s = super.asJSON();
            s += ", \"name\" : \"" + mAnimName + "\"";
            return s;
        }
    }


    /**
     * Action to randomly play an animation.
     * The animation is chosen by matching a name pattern
     * against the animations currently in the queue.
     */
    public static class PlayRandom extends Action
    {
        private final String  mAnimName;
        private final List<String> mAnimNames = new ArrayList<String>();

        /**
         * Construction an action which plays a random animation.
         * <p>
         * The input animation name is used as a search pattern
         * to select a set of animations from the animation queue
         * associated with this state machine. Any name which contains
         * the pattern is a candidate for play. One animation is
         * selected randomly from all of the matching candidates.
         * The animation must be known to the {@link SXRAnimationQueue}
         * associated with this state machine.
         * </p>
         * @param sm        {@link SXRStateMachine} which owns this action.
         * @param animName  String with pattern for animation to play.
         */
        public PlayRandom(SXRStateMachine sm, String animName)
        {
            super(sm, "playrandom");
            mAnimName = animName;
        }

        /**
         * Construction an action which plays a raqndom animation.
         * <p>
         * The input animation name is used as a search pattern
         * to select a set of animations from the animation queue
         * associated with this state machine. Any name which contains
         * the pattern is a candidate for play. One animation is
         * selected randomly from all of the matching candidates.
         * The animation must be known to the {@link SXRAnimationQueue}
         * associated with this state machine.
         * </p>
         * @param sm        {@link SXRStateMachine} which owns this action.
         * @param json      JSONObject with action properties.
         */
        public PlayRandom(SXRStateMachine sm, JSONObject json) throws JSONException
        {
            super(sm, json);
            mAnimName = json.getString("name");
        }

        /**
         * Get the name of the animation selection pattern.
         * @return String with name passed to the constructur.
         */
        public String getAnimationName() { return mAnimName; }

        public void run()
        {
            SXRAnimationQueue q = ((SXRAnimationStateMachine) mStateMachine).getAnimationQueue();
            SXRAnimator a = null;
            int index = 0;
            String name;

            if (mAnimNames.size() == 0)
            {
                int n = q.getAnimationCount();
                for (int i = 0; i < n; ++i)
                {
                    a = q.get(i);
                    if (a.getName().contains(mAnimName))
                    {
                        mAnimNames.add(a.getName());
                    }
                }
            }
            do
            {
                index = (int) (Math.random() * mAnimNames.size());
                name = mAnimNames.get(index);
                a = q.findAnimation(name);
            }
            while (name.startsWith("Copy-"));
            if (a.isRunning())
            {
                a = new SXRAnimator(a);
                name = "Copy-" + name;
                a.setName(name);
                q.add(a);
            }
            q.startNext(name);
        }

        public String asJSON()
        {
            String s = super.asJSON();
            s += ", \"name\" : \"" + mAnimName + "\"";
            return s;
        }
    }

    /**
     * Action which stops the currently running animation, if any.
     */
    public static class Stop extends Action
    {
        private String  mAnimName;

        /**
         * Construction an action which stops the given animation.
         * <p>
         * The animation must be known to the {@link SXRAnimationQueue}
         * associated with this state machine.
         * </p>
         * @param sm        {@link SXRStateMachine} which owns this action.
         * @param animName  String with name of animation to stop.
         */
        public Stop(SXRStateMachine sm, String animName)
        {
            super(sm, "stopanimation");
            mAnimName = animName;
        }

        /**
         * Construction an action which stops a given animation.
         * <p>
         * The animation must be known to the {@link SXRAnimationQueue}
         * associated with this state machine.
         * </p>
         * @param sm        {@link SXRStateMachine} which owns this action.
         * @param json      JSONObject with animation properties.
         */
        public Stop(SXRStateMachine sm, JSONObject json) throws JSONException
        {
            super(sm, json);
            mAnimName = json.getString("name");
        }

        /**
         * Get the name of the animation.
         * @return String with name passed to the constructur.
         */
        public String getAnimationName() { return mAnimName; }

        public void run()
        {
            ((SXRAnimationStateMachine) mStateMachine).getAnimationQueue().stop(mAnimName);
        }

        public String asJSON()
        {
            String s = super.asJSON();
            s += ", \"name\" : \"" + mAnimName + "\"";
            return s;
        }
    }

    /**
     * Construct an animation state machine which uses
     * the given animation queue.
     * @param queue {@link SXRAnimationQueue} for scheduling animations.
     */
    public SXRAnimationStateMachine(SXRAnimationQueue queue)
    {
        super();
        mAnimQueue = queue;
        defineAction("playanimation", Play.class);
        defineAction("stopanimation", Stop.class);
        defineAction("playrandom", PlayRandom.class);
    }

    public void start()
    {
        if (!mIsRunning)
        {
            mAnimQueue.getEventReceiver().addListener(this);
            super.start();
        }
    }

    public void stop()
    {
        mAnimQueue.getEventReceiver().removeListener(this);
        super.stop();
    }

    public SXRAnimationQueue getAnimationQueue() { return mAnimQueue; }


    public void onAnimationStarted(SXRAnimationQueue queue, SXRAnimator animator)
    {

    }

    public void onAnimationFinished(SXRAnimationQueue queue, SXRAnimator animator)
    {
        if (animator.getName().startsWith("Copy-"))
        {
            queue.remove(animator);
        }
        mCurrentState.onEvent("finish");
    }

    public void removeBlendAnimation(SXRAnimationQueue queue, SXRAnimator animator)
    {

    }

    public SXRAnimation addBlendAnimation(SXRAnimationQueue queue,SXRAnimator a1, SXRAnimator a2, float blendDuration)
    {
        return null;
    }

    public void reverseBlendAnimation(SXRAnimationQueue queue, SXRAnimator a)
    {

    }

    public boolean isBlending(SXRAnimationQueue q, SXRAnimator a)
    {
        return false;
    }

}
