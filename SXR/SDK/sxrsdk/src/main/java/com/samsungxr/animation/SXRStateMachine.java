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

/**
 * Infrastructure for a finite state machine.
 * <p>
 * This class is useful for scheduling events within your application.
 * The <i>state machine</i> has a set of states, each state has one or
 * more associated actions. An <i>action</i> is a task that is run when a
 * state is entered, exited or the when the previous state finishes.
 * <p>
 * The {@link SXRStateMachine.Action} class is the base for defining your
 * own actions. The only built-in action is {@link SXRStateMachine.GoToState}
 * which transitions to another state. During a transition, the <i>exit</i>
 * actions for the current state are executed and then the <i>enter</i>
 * action for the next state is executed.
 * <p>
 * A state machine can be imported or exported as a JSON file.
 * Each action is responsible for its own serialization.
 * </p>
 * @see SXRAnimationStateMachine
 */
public class SXRStateMachine
{
    protected State mCurrentState = null;
    protected Map<String, Class<? extends Action>> mActions;
    protected List<State> mStates;
    protected boolean mIsRunning = false;

    /**
     * Base class for all state machine actions.
     * Each action has a unique type which is used
     * to refer to the action in the JSON description.
     * An action can convert itself to JSON.
     * An action must be defined for the state machine
     * before it can be used.
     * @see SXRStateMachine#defineAction(String, Class)
     */
    public static abstract class Action implements Runnable
    {
        protected String mType;
        protected SXRStateMachine mStateMachine;

        /**
         * Construct an empty action of the given type.
         * @param sm    {@link SXRStateMachine} the action belongs to.
         * @param type  String type of the action.
         */
        public Action(SXRStateMachine sm, String type)
        {
            mStateMachine = sm;
            mType = type;
        }

        /**
         * Construct an action from a JSON object.
         * @param sm    {@link SXRStateMachine} the action belongs to.
         * @param json  JSONObject containing the action properties.
         */
        public Action(SXRStateMachine sm, JSONObject json)
        {
            mStateMachine = sm;
            mType = json.optString("action");
        }

        /**
         * Returns the type of the action.
         * @return type specified in the constructor or JSON file.
         */
        public String getType() { return mType; }

        /**
         * Execute the action if in the specified state.
         * @param state     state for executing the action.
         */
        public void runIf(String state) { }

        /**
         * Export the action as a JSON string.
         * @return JSON description of action.
         */
        public String asJSON()
        {
            return "\"action\" : \"" + mType + "\"";
        }
    };

    /***
     * Contains a group of actions.
     * Use this class when you want more than one action
     * to be associated with an enter of leave event.
     */
    public static class ActionGroup extends Action
    {
        protected List<Action> mActions;
        ActionGroup(SXRStateMachine sm)
        {
            super(sm, "group");
            mActions = new ArrayList<Action>();
        }

        /**
         * Get the list of actions in this group
         * @return {@link List} of actions.
         */
        public final List<Action> getActions() { return mActions; }

        /**
         * Add an action to this group.
         * @param a action to add.
         */
        public void addAction(Action a)
        {
            mActions.add(a);
        }

        /**
         * Execute the actions in this group.
         */
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

    /**
     * Action which causes a transition to a new state.
     */
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
                Log.d("STATE", "State %s: %s %s", getName(), event, a.asJSON());
                a.run();
            }
        }

        public void gotoState(String nextState)
        {
            Action action = mActions.get("goto");

            if (action != null)
            {
                Log.d("STATE", "State %s: %s goto %s", getName(), nextState);
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

    /**
     * Construct a new state machine.
     * Initially it was not have any states or actions.
     * The {@link GoToState} action is defined.
     */
    public SXRStateMachine()
    {
        mActions = new HashMap<String, Class<? extends Action>>();
        mStates = new ArrayList<State>();
        defineAction("goto", GoToState.class);
    }

    /**
     * Define the implementation for an action on this state machihne.
     * <p>
     * A custom action must be derived from {@link SXRStateMachine.Action}
     * and provide custom implementations for {@link SXRStateMachine.Action#asJSON()}
     * to export the action as JSON and {@link SXRStateMachine.Action#run}.
     * <p>
     * ro execute the action. It must also provide two constructors.
     * <i>MyAction(SXRStateMachine, String)</i> should define an empty
     * action of a given type. <i>MyAction(SXRStateMachine, JSONObject)</i>
     * should set the action properties from a JSON object.
     * </p>
     * @param type    String with type of action.
     * @param clazz   Class which implements the action.
     */
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

    /**
     * Determines whether the state machine is running or not.
     * @return true if running, else false.
     */
    public boolean isRunning() { return mIsRunning; }

    /**
     * Start the state machine and enter the first state.
     */
    public void start()
    {
        if (mStates.size() == 0)
        {
            throw new UnsupportedOperationException("Cannot start if there are not states");
        }
        mIsRunning = true;
        mCurrentState = mStates.get(0);
        mCurrentState.enter();
    }

    /**
     * Leave the current state and stop the state machine.
     */
    public void stop()
    {
        if (mStates.size() > 0)
        {
            mCurrentState = mStates.get(0);
            mCurrentState.leave();
        }
        mIsRunning = false;
    }

    /**
     * Transition the state machine to the given state.
     * <p>
     * The <i>leave</i> action is executed for the
     * current state and the <i>enter</i> action is executed for the new state.
     * @param nextState
     */
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

    /**
     * Generate a JSON description of the state machine.
     * @return String with JSON.
     */
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

    /**
     * Import a state machine from a JSON string.
     * @param jsonData  String with JSON date
     * @return true if successfully parsed, false if parse error
     */
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
