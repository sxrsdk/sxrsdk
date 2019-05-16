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

public class SXRAnimationStateMachine extends SXRStateMachine implements SXRAnimationQueue.IAnimationQueueEvents
{
    protected final SXRAnimationQueue mAnimQueue;

    public static class Play extends Action
    {
        private String  mAnimName;

        public Play(SXRStateMachine sm, String animName)
        {
            super(sm, "playanimation");
            mAnimName = animName;
        }

        public Play(SXRStateMachine sm, JSONObject json) throws JSONException
        {
            super(sm, json);
            mAnimName = json.getString("name");
        }

        public String getAnimationName() { return mAnimName; }

        public void run()
        {
            ((SXRAnimationStateMachine) mStateMachine).getAnimationQueue().start(mAnimName);
        }

        public String asJSON()
        {
            String s = super.asJSON();
            s += ", \"name\" : \"" + mAnimName + "\"";
            return s;
        }
    }

    public static class PlayRandom extends Action
    {
        private final String  mAnimName;
        private final List<String> mAnimNames = new ArrayList<String>();

        public PlayRandom(SXRStateMachine sm, String animName)
        {
            super(sm, "playrandom");
            mAnimName = animName;
        }

        public PlayRandom(SXRStateMachine sm, JSONObject json) throws JSONException
        {
            super(sm, json);
            mAnimName = json.getString("name");
        }

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
            q.start(name);
        }

        public String asJSON()
        {
            String s = super.asJSON();
            s += ", \"name\" : \"" + mAnimName + "\"";
            return s;
        }
    }

    public static class Stop extends Action
    {
        private String  mAnimName;

        public Stop(SXRStateMachine sm, String animName)
        {
            super(sm, "stopanimation");
            mAnimName = animName;
        }

        public Stop(SXRStateMachine sm, JSONObject json) throws JSONException
        {
            super(sm, json);
            mAnimName = json.getString("name");
        }

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

    public SXRAnimationStateMachine(SXRContext ctx, SXRAnimationQueue queue)
    {
        super(ctx);
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
