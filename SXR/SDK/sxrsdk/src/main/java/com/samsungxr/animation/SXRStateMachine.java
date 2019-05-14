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

import com.samsungxr.IErrorEvents;
import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRShader;
import com.samsungxr.utility.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;


public class SXRStateMachine
{
    protected State mCurrentState = null;
    protected Map<String, Class<? extends Action>> mActions;
    protected List<State> mStates;

    public static abstract class Action implements Runnable
    {
        protected String mType;
        protected SXRStateMachine mStateMachine;

        public Action(SXRStateMachine sm, String type)
        {
            mStateMachine = sm;
            mType = type;
        }

        public Action(SXRStateMachine sm, JSONObject json)
        {
            mStateMachine = sm;
            mType = json.optString("action");
        }

        public String getType() { return mType; }

        public void runIf(String s) { }

        public String asJSON()
        {
            return "\"action\" : \"" + mType + "\"";
        }
    };

    public static class ActionGroup extends Action
    {
        protected List<Action> mActions;
        ActionGroup(SXRStateMachine sm)
        {
            super(sm, "group");
            mActions = new ArrayList<Action>();
        }

        public final List<Action> getActions() { return mActions; }

        public void addAction(Action a)
        {
            mActions.add(a);
        }

        public void run()
        {
            for (Action a : mActions)
            {
                a.run();
            }
        }

        public void runIf(String stateName)
        {
            for (Action a : mActions)
            {
                a.runIf(stateName);
            }
        }
    };

    public static class GoToState extends Action
    {
        private String mNextState;

        public  GoToState(SXRStateMachine sm, String nextState)
        {
            super(sm, "goto");
            mNextState = nextState;
        }

        public GoToState(SXRStateMachine sm, JSONObject json) throws JSONException
        {
            super(sm, json);
            mNextState = json.getString("state");
        }

        public String getNextState() { return mNextState; }

        public void runIf(String stateName)
        {
            if (mNextState.equals(stateName))
            {
                run();
            }
        }

        public String asJSON()
        {
            String s = super.asJSON();
            s += "\"state\" : \"" + mNextState + "\"";
            return s;
        }

        public void run()
        {
            mStateMachine.gotoState(mNextState);
        }
    }

    protected class State
    {
        private String mName;
        private Map<String, Action> mActions = new HashMap<String, Action>();

        public State(String name)
        {
            mName = name;
        }

        public void enter()
        {
            onEvent("enter");
        }

        public void leave()
        {
            onEvent("leave");
        }

        public void onEvent(String event)
        {
            Action a = mActions.get(event);
            if (a != null)
            {
                a.run();
            }
        }

        public void gotoState(String nextState)
        {
            Action action = mActions.get("goto");

            if (action != null)
            {
                action.runIf(nextState);
            }
        }

        public void addAction(String event, Action action)
        {
            Action a = findAction(event);

            if (a == null)
            {
                mActions.put(event, action);
            }
            else if (a instanceof ActionGroup)
            {
                ((ActionGroup) a).addAction(action);
            }
            else
            {
                ActionGroup g = new ActionGroup(SXRStateMachine.this);
                g.addAction(a);
                g.addAction(action);
                mActions.put(event, g);
            }
        }

        public String getName() { return mName; }

        public final Action findAction(String event) { return mActions.get(event); }

        public String asJSON()
        {
            String s = "   {\n  \"state\" : \"" + mName + " \",\n    \"actions\" : [\n";
            String actions = "";
            for (Map.Entry<String, Action> e : mActions.entrySet())
            {
                if (actions.length() > 0)
                {
                    actions += ",";
                }
                actions += "    {\n    \"";
                actions += e.getKey() + "\" : {\n";
                actions += "    " + e.getValue().asJSON() + "\n    },\n  }";
            }
            if (actions.length() > 0)
            {
                return "{\n  \"state\" : \"" + mName + " \",\n    \"actions\" : [\n" + actions + " \n  ]";
            }
            else
            {
                return "{\n  \"state\" : \"" + mName + " \"}";
            }
        }
    }

    public SXRStateMachine(SXRContext ctx)
    {
        mActions = new HashMap<String, Class<? extends Action>>();
        mStates = new ArrayList<State>();
        defineAction("goto", GoToState.class);
    }

    public void defineAction(String type, Class<? extends Action> clazz)
    {
        mActions.put(type, clazz);
    }

    public void addState(State state)
    {
        if (!mStates.contains(state))
        {
            mStates.add(state);
        }
    }

    public void start()
    {
        if (mStates.size() == 0)
        {
            throw new UnsupportedOperationException("Cannot start if there are not states");
        }
        mCurrentState = mStates.get(0);
        mCurrentState.enter();
    }

    public void gotoState(String nextState)
    {
        State state = findState(nextState);
        if (state == null)
        {
            throw new UnsupportedOperationException("State not found " +state);
        }
        mCurrentState.leave();
        mCurrentState = state;
        state.gotoState(nextState);
        state.enter();
    }

    public String asJSON()
    {
        String s = "";
        for (State state : mStates)
        {
            if (s != "")
            {
                s += ",\n";
            }
            s += state.asJSON();
        }
        return "{ \"states\" : [\n" + s + "\n ]\n}";
    }

    public boolean parse(String jsonData)
    {
        try
        {
            JSONObject root = new JSONObject(jsonData);
            JSONArray states = root.getJSONArray("states");
            for (int i = 0; i < states.length(); ++i)
            {
                JSONObject jstate = states.getJSONObject(i);
                State state = new State(jstate.getString("state"));
                JSONArray jactions = jstate.getJSONArray("actions");
                addState(state);
                for (int a = 0; a < jactions.length(); ++a)
                {
                    JSONObject jentry = jactions.getJSONObject(a);
                    Iterator<String> events = jentry.keys();
                    while (events.hasNext())
                    {
                        String eventName = events.next();
                        Object o = jentry.get(eventName);
                        if (o instanceof JSONArray)
                        {
                            JSONArray jarr = (JSONArray) o;
                            for (int j = 0; j < jarr.length(); ++j)
                            {
                                JSONObject jaction = jarr.getJSONObject(j);
                                Action action = makeAction(jaction);

                                state.addAction(eventName, action);
                            }
                        }
                        else if (o instanceof JSONObject)
                        {
                            JSONObject jaction = (JSONObject) o;
                            Action action = makeAction(jaction);

                            state.addAction(eventName, action);
                        }
                    }
                }
            }
            return true;
        }
        catch (JSONException ex)
        {
            return false;
        }
    }


    protected Action makeAction(String actionType)
    {
        Class<? extends Action> actionClass = mActions.get(actionType);

        if (actionClass == null)
        {
            throw new IllegalArgumentException(actionType + " does not have a corresponding Action subclass");
        }
        try
        {
            Constructor<? extends Action> maker = actionClass.getDeclaredConstructor(SXRStateMachine.class, String.class);
            return maker.newInstance(this, actionType);
        }
        catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
        InvocationTargetException ex)
        {
            throw new IllegalArgumentException(actionType + " does not have a proper constructor " + ex.getMessage());
        }
    }

    protected Action makeAction(JSONObject json)
    {
        String actionType = json.optString("action");
        Class<? extends Action> actionClass = mActions.get(actionType);

        if (actionClass == null)
        {
            throw new IllegalArgumentException(actionType + " does not have a corresponding Action subclass");
        }
        try
        {
            Constructor<? extends Action> maker = actionClass.getDeclaredConstructor(SXRStateMachine.class, JSONObject.class);
            return maker.newInstance(this, json);
        }
        catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
            InvocationTargetException ex)
        {
            throw new IllegalArgumentException(actionType + " does not have a proper constructor " + ex.getMessage());
        }
    }

    protected State findState(String name)
    {
        for (State s : mStates)
        {
            if (s.getName().equals(name))
            {
                return s;
            }
        }
        return null;
    }

}
