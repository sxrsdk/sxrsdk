/* Copyright 2016 Samsung Electronics Co., LTD
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

package com.samsungxr;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An event receiver can contain a list of listeners, and deliver events to
 * them upon request. It can be added as a field of any event receiving
 * object, and handle events for them.
 *
 * To ensure all events can be received by scripts as well as listeners, this
 * class doesn't provide an API to directly deliver events to the listeners.
 * Instead, events should be delivered via the {@link SXREventManager}. Usage
 * of this {@code SXREventReceiver} is the following:
 *
 * <ol>
 * <li>Add interface {@link IEventReceiver} to a class which need use listeners to
 * handle events. Without listeners, it can only receive events from {@link SXREventManager}
 * by directly implementing a subclass of interface {@link IEvents}.</li>
 * <li>Add a field containing a {@link SXREventReceiver} object.</li>
 * <li>Implement the method {@link IEventReceiver#getEventReceiver()} to return
 * this {@link SXREventReceiver} object.</li>
 * </ol>
 * After implementing the above pattern, {@link SXREventManager#sendEvent(Object, Class, String, Object...)}
 * can be used to deliver events to the class. Note that a target object can only have
 * one listener for each event class. Subsequent listeners for the same target and class
 * are ignored.
 */
public class SXREventReceiver {
    protected IEventReceiver mOwner;
    protected List<IEvents> mListeners;

    /**
     * Constructs an event receiver for the host object.
     * @param owner The host object which owns this event receiver.
     */
    public SXREventReceiver(IEventReceiver owner) {
        mOwner = owner;

        // Copy-on-write list is used. Otherwise, it won't be thread-safe
        // unless we do copy-on-read, which is worse.
        mListeners = new CopyOnWriteArrayList<IEvents>();
    }

    /**
     * Adds a listener to the receiver. The listener can be a subclass
     * of {@link IEvents}, such as {@code IScriptEvents}, {@code ISensorEvents}
     * and so on.
     *
     * @param listener
     *         The listener to be added.
     */
    public void addListener(IEvents listener) {
        if (!mListeners.contains(listener))
        {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a listener from the receiver.
     *
     * @param listener
     *         The listener to be removed.
     */
    public void removeListener(IEvents listener) {
        mListeners.remove(listener);
    }

    /**
     * Gets all listeners. Don't use this method directly to deliver
     * events to the listeners. Instead, use SXREventManager to deliver
     * an event to the owner of this {@link SXREventReceiver}.
     */
    protected List<IEvents> getListeners() {
        return mListeners;
    }

    /**
     * Gets the owner of the event receiver.
     * @return The owner of the event receiver.
     */
    protected IEventReceiver getOwner() {
        return mOwner;
    }
}
