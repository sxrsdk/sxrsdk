/*
 * Copyright (c) 2016. Samsung Electronics Co., LTD
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sample.hand.template;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a hand.
 */
public class IOHand {
    private static int PALM_KEY = 0;
    private Map<Integer, IOFinger> fingerMap;
    private Map<Integer, SXRNode> auxNodes;
    private SXRNode handNode;

    /**
     * Create an {@link IOHand} object.
     *
     * @param gvrContext
     */
    public IOHand(SXRContext gvrContext) {
        handNode = new SXRNode(gvrContext);

        // Create the fingers
        IOFinger index = new IOFinger(IOFinger.INDEX, handNode);
        IOFinger middle = new IOFinger(IOFinger.MIDDLE, handNode);
        IOFinger ring = new IOFinger(IOFinger.RING, handNode);
        IOFinger pinky = new IOFinger(IOFinger.PINKY, handNode);
        IOFinger thumb = new IOFinger(IOFinger.THUMB, handNode);

        fingerMap = new HashMap<Integer, IOFinger>(5);

        // Create a map to add auxiliary objects that may be added to the IOHand
        auxNodes = new HashMap<Integer, SXRNode>();

        // add the fingers to the map.
        fingerMap.put(IOFinger.INDEX, index);
        fingerMap.put(IOFinger.MIDDLE, middle);
        fingerMap.put(IOFinger.RING, ring);
        fingerMap.put(IOFinger.PINKY, pinky);
        fingerMap.put(IOFinger.THUMB, thumb);
    }

    /**
     * Return the {@link SXRNode} that is controlled by this {@link IOHand}
     *
     * @return
     */
    public SXRNode getNode() {
        return handNode;
    }

    /**
     * Add a {@link SXRNode} to this {@link IOHand}
     *
     * @param key         an int value that uniquely helps identify this {@link SXRNode}.
     *                    So that
     *                    it can easily be looked up later on.
     * @param sceneObject {@link SXRNode} that is to be added.
     */
    public void addNode(int key, SXRNode sceneObject) {
        // only add if not present
        if (!auxNodes.containsKey(key)) {
            auxNodes.put(key, sceneObject);
            handNode.addChildObject(sceneObject);
        }
    }

    /**
     * A convenience method to add a scene object that represents the palm
     *
     * @param sceneObject
     */
    public void addPalmNode(SXRNode sceneObject) {
        addNode(PALM_KEY, sceneObject);
    }

    /**
     * Use the provided key to look up the {@link SXRNode}
     *
     * @param key
     * @return
     */
    public SXRNode getNode(int key) {
        return auxNodes.get(key);
    }

    /**
     * Get the {@link SXRNode} corresponding to the palm (if there is one)
     *
     * @return
     */
    public SXRNode getPalmNode() {
        return auxNodes.get(PALM_KEY);
    }

    /**
     * Get the {@link IOFinger} corresponding to the given {@link IOFinger} type.
     *
     * @param type
     * @return
     */
    public IOFinger getIOFinger(int type) {
        return fingerMap.get(type);
    }
}
