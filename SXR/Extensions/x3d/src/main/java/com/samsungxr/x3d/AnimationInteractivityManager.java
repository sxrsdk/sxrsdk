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

package com.samsungxr.x3d;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.AssetDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.samsungxr.SXRAssetLoader;
import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRImage;
import com.samsungxr.SXRLight;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRPointLight;
import com.samsungxr.SXRSpotLight;
import com.samsungxr.SXRDirectLight;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRSwitch;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTextureParameters;
import com.samsungxr.SXRTransform;
import com.samsungxr.ISensorEvents;
import com.samsungxr.SensorEvent;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.animation.SXROnFinish;
import com.samsungxr.animation.SXRRepeatMode;
import com.samsungxr.animation.keyframe.SXRAnimationBehavior;
import com.samsungxr.animation.keyframe.SXRAnimationChannel;
import com.samsungxr.animation.keyframe.SXRNodeAnimation;

import com.samsungxr.animation.keyframe.SXRNodeAnimation;
import com.samsungxr.nodes.SXRVideoNode;
import com.samsungxr.nodes.SXRVideoNodePlayer;
import com.samsungxr.nodes.SXRTextViewNode;
import com.samsungxr.script.SXRJavascriptScriptFile;

import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.script.javascript.SXRJavascriptV8File;
import com.samsungxr.utility.Log;
import com.samsungxr.x3d.data_types.SFBool;
import com.samsungxr.x3d.data_types.SFColor;
import com.samsungxr.x3d.data_types.SFFloat;
import com.samsungxr.x3d.data_types.SFInt32;
import com.samsungxr.x3d.data_types.SFString;
import com.samsungxr.x3d.data_types.SFTime;
import com.samsungxr.x3d.data_types.SFVec2f;
import com.samsungxr.x3d.data_types.SFVec3f;
import com.samsungxr.x3d.data_types.SFRotation;
import com.samsungxr.x3d.data_types.MFString;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.script.Bindings;

import lu.flier.script.V8Object;


/**
 * @author m1.williams
 *         AnimationInteractivityManager will construct an InteractiveObject based on the to/from node/field Strings.
 *         If the Strings match actual objects (DEFitems, TimeSensors, Iterpolators, etc) from their
 *         respective array lists, then
 *         AnimationInteractivityManager will either construct an interactiveObject and add it to an array list,
 *         or modify an existing interactiveObject to include a 'pointer' (to the TimeSensor,
 *         Sensor, Interpolator, etc.) to tie all the sensors, timers, interpolators and
 *         defined items (Transform, Material, TextureTransform, Color, etc) into a single object.
 */

public class AnimationInteractivityManager {

    private static final String TAG = AnimationInteractivityManager.class.getSimpleName();
    private final static float FRAMES_PER_SECOND = 60.0f;
    private Vector<InteractiveObject> interactiveObjects = new Vector<InteractiveObject>();

    private static final String IS_OVER = "isOver";
    private static final String Is_ACTIVE = "isActive";

    private static final String ROTATION = "rotation";
    private static final String ORIENTATION = "orientation";
    private static final String TRANSLATION = "translation";
    private static final String POSITION = "position";
    private static final String SCALE = "scale";
    private static final String KEY_FRAME_ANIMATION = "KeyFrameAnimation_";
    private static final String INITIALIZE_FUNCTION = "initialize";
    private static final String GEARVR_INIT_JAVASCRIPT_FUNCTION_NAME = "GearVRinitJavaScript";

    public static final boolean V8JavaScriptEngine = true;


    private X3Dobject x3dObject = null;
    private SXRContext gvrContext = null;
    private SXRNode root = null;
    private Vector<DefinedItem> definedItems = null;
    private Vector<Interpolator> interpolators = null;
    private Vector<Sensor> sensors = null;
    private Vector<TimeSensor> timeSensors = null;
    private Vector<EventUtility> eventUtilities = null;
    private ArrayList<ScriptObject> scriptObjects = null;

    private AnchorImplementation anchorImplementation = null;
    private SXRAnimator gvrAnimator = null;
    private SXRAssetLoader.AssetRequest assetRequest = null;


    private PerFrameScripting perFrameScripting = new PerFrameScripting();
    private SensorImplementation sensorImplementation = new SensorImplementation();

    // Append this incremented value to SXRNode names to insure unique
    // SXRNodes when new SXRScene objects are generated to support animation
    private static int animationCount = 1;


    public AnimationInteractivityManager(X3Dobject x3dObject, SXRContext gvrContext,
                                         SXRNode root,
                                         Vector<DefinedItem> definedItems,
                                         Vector<Interpolator> interpolators,
                                         Vector<Sensor> sensors,
                                         Vector<TimeSensor> timeSensors,
                                         Vector<EventUtility> eventUtilities,
                                         ArrayList<ScriptObject> scriptObjects,
                                         Vector<Viewpoint> viewpoints,
                                         SXRAssetLoader.AssetRequest assetRequest

    ) {
        this.x3dObject = x3dObject;
        this.gvrContext = gvrContext;
        this.root = root; // helps search for SXRSCeneObjects by name
        this.definedItems = definedItems;
        this.interpolators = interpolators;
        this.sensors = sensors;
        this.timeSensors = timeSensors;
        this.eventUtilities = eventUtilities;
        this.scriptObjects = scriptObjects;
        this.assetRequest = assetRequest;

        gvrAnimator = new SXRAnimator(this.gvrContext, true);
        root.attachComponent(gvrAnimator);

        anchorImplementation = new AnchorImplementation(this.gvrContext, this.root, viewpoints);

    }

    /**
     * buildInteractiveObject represents one X3D <ROUTE /> tag.
     * This method matches the fromNode and toNode with objects in sensors, timeSensors,
     * interpolators and DEFinded Items array lists.  It will either construct a new
     * InteractiveObject if a related <ROUTE /> has not called this method, or modify
     * an InteractiveObject if a related <ROUTE /> has been parsed here.
     * For example if a <ROUTE myTouchSensor TO myTimeSensor /> has been parsed, then another
     * call to this method <ROUTE myTimeSensor TO myInterpolator /> will match the previous
     * "myTimeSensor" and modify that InteractiveObject
     * The 4 parameters are from an X3D <ROUTE /> node
     * For example: <ROUTE fromNode.fromField to toNode.toField />
     *
     * @param fromNode
     * @param fromField
     * @param toNode
     * @param toField
     */
    public void buildInteractiveObject(String fromNode, String fromField, String toNode, String toField) {
        Sensor routeFromSensor = null;
        TimeSensor routeToTimeSensor = null;
        TimeSensor routeFromTimeSensor = null;
        Interpolator routeToInterpolator = null;
        Interpolator routeFromInterpolator = null;
        EventUtility routeToEventUtility = null;
        EventUtility routeFromEventUtility = null;
        DefinedItem routeToDefinedItem = null;
        DefinedItem routeFromDefinedItem = null; // used passing items into a Script
        ScriptObject routeFromScriptObject = null;
        ScriptObject routeToScriptObject = null;

        // Get pointers to the Sensor, TimeSensor, Interpolator,
        // EventUtility (such as BooleanToggle), ScriptObject
        // and/or Defined Items based the nodes of this object
        for (Sensor sensor : sensors) {
            if (sensor.getName().equalsIgnoreCase(fromNode)) {
                routeFromSensor = sensor;
            }
        }

        for (TimeSensor timeSensor : timeSensors) {
            if (timeSensor.name.equalsIgnoreCase(toNode)) {
                routeToTimeSensor = timeSensor;
            } else if (timeSensor.name.equalsIgnoreCase(fromNode)) {
                routeFromTimeSensor = timeSensor;
            }
        }

        for (Interpolator interpolator : interpolators) {
            if (interpolator.name.equalsIgnoreCase(toNode)) {
                routeToInterpolator = interpolator;
            } else if (interpolator.name.equalsIgnoreCase(fromNode)) {
                routeFromInterpolator = interpolator;
            }
        }

        for (EventUtility eventUtility : eventUtilities) {
            if (eventUtility.getName().equalsIgnoreCase(toNode)) {
                routeToEventUtility = eventUtility;
            } else if (eventUtility.getName().equalsIgnoreCase(fromNode)) {
                routeFromEventUtility = eventUtility;
            }
        }

        for (ScriptObject scriptObject : scriptObjects) {
            if (scriptObject.getName().equalsIgnoreCase(toNode)) {
                routeToScriptObject = scriptObject;
            } else if (scriptObject.getName().equalsIgnoreCase(fromNode)) {
                routeFromScriptObject = scriptObject;
            }
        }

        for (DefinedItem definedItem : definedItems) {
            if (definedItem.getName().equalsIgnoreCase(toNode)) {
                routeToDefinedItem = definedItem;
            } else if (definedItem.getName().equalsIgnoreCase(fromNode)) {
                routeFromDefinedItem = definedItem;
            }
        }

        // Now build the InteractiveObject by assigning pointers
        // to an existing InteractiveObject matches non-null links
        // or create a new InteractiveObject.
        // The flow is to test where the ROUTE TO goes (instead of FROM)

        // ROUTE TO a TimeSensor
        if (routeToTimeSensor != null) {
            boolean routeToTimeSensorFound = false;
            for (InteractiveObject interactiveObject : interactiveObjects) {
                if (routeToTimeSensor == interactiveObject.getTimeSensor()) {
                    if (interactiveObject.getSensor() == null) {
                        //This sensor already exists inside an Interactive Object
                        interactiveObject.setSensor(routeFromSensor, fromField);
                        routeToTimeSensorFound = true;
                    }
                }
                else if (routeFromScriptObject != null) {
                    // a rare case where a Script node sends data to a TimeSensor
                    if (routeFromScriptObject == interactiveObject.getScriptObject()) {
                        //TODO: complete adding field to this Script Node when sent to TimeSensor
                        for (ScriptObject.Field field : routeFromScriptObject.getFieldsArrayList()) {
                            if (toField.equalsIgnoreCase(routeFromScriptObject.getFieldName(field))) {
                                interactiveObject.getScriptObject().setToTimeSensor(field, routeToTimeSensor, toField);
                                routeToTimeSensorFound = true;
                            }
                        }
                    }
                }
            }
            if (!routeToTimeSensorFound) {
                // construct a new interactiveObject for this sensor and timeSensor
                InteractiveObject interactiveObject = new InteractiveObject();
                interactiveObject.setSensor(routeFromSensor, fromField);
                interactiveObject.setTimeSensor(routeToTimeSensor);
                interactiveObjects.add(interactiveObject);
            }
        }  //  end route To TimeSensor

        // ROUTE TO an Interpolator (Position, Rotation, etc)
        if (routeToInterpolator != null) {
            boolean routeToInterpolatorFound = false;
            for (InteractiveObject interactiveObject : interactiveObjects) {
                if (routeToInterpolator == interactiveObject.getInterpolator()) {
                    if (interactiveObject.getTimeSensor() == null) {
                        //This sensor already exists as part of an interactive Object
                        interactiveObject.setTimeSensor(routeFromTimeSensor);
                        routeToInterpolatorFound = true;
                    }
                }
            }
            if (!routeToInterpolatorFound) {
                // construct a new interactiveObject for this sensor and timeSensor
                for (InteractiveObject interactiveObject : interactiveObjects) {
                    if (routeFromTimeSensor == interactiveObject.getTimeSensor()) {
                        if ((interactiveObject.getInterpolator() == null)
                                && (interactiveObject.getScriptObject() == null)) {
                            //This timer already exists as part of an interactive Object
                            interactiveObject.setInterpolator(routeToInterpolator);
                            routeToInterpolatorFound = true;
                        }
                    }
                }
            }
            if (!routeToInterpolatorFound) {
                // construct a new interactiveObject for this sensor and timeSensor
                InteractiveObject interactiveObject = new InteractiveObject();
                interactiveObject.setTimeSensor(routeFromTimeSensor);
                interactiveObject.setInterpolator(routeToInterpolator);
                interactiveObjects.add(interactiveObject);
            }
        }  //  end route To Interpolator

        // ROUTE TO an Event Utility (such as a BooleanToggle
        if (routeToEventUtility != null) {
            boolean routeToEventUtilityFound = false;
            for (InteractiveObject interactiveObject : interactiveObjects) {
                if (routeToEventUtility == interactiveObject.getEventUtility()) {
                    if ( (interactiveObject.getSensor() == null) && (routeFromSensor != null) ) {
                        interactiveObject.setSensor(routeFromSensor, fromField);
                        routeToEventUtilityFound = true;
                    }
                    else if ( (interactiveObject.getScriptObject() == null) && (routeFromScriptObject != null) ) {
                        for (ScriptObject.Field field : routeFromScriptObject.getFieldsArrayList()) {
                            if (fromField.equalsIgnoreCase(routeFromScriptObject.getFieldName(field))) {
                                routeFromScriptObject.setToEventUtility(field, routeToEventUtility, toField);
                                routeToEventUtilityFound = true;
                            }
                        }
                    }
                }
            }
            if (!routeToEventUtilityFound) {
                // construct a new interactiveObject for this sensor and timeSensor
                InteractiveObject interactiveObject = new InteractiveObject();
                interactiveObject.setSensor(routeFromSensor, fromField);
                interactiveObject.setEventUtility(routeToEventUtility);
                interactiveObject.setScriptObject(routeFromScriptObject);
                interactiveObjects.add(interactiveObject);
            }
        }  //  end routeToEventUtility

        // ROUTE TO a Script Object
        if (routeToScriptObject != null) {
            boolean routeToScriptObjectFound = false;
            for (InteractiveObject interactiveObject : interactiveObjects) {
                if (routeToScriptObject == interactiveObject.getScriptObject()) {
                    if ((interactiveObject.getSensor() == null) && (routeFromSensor != null)) {
                        //This sensor already exists as part of an interactive Object
                        interactiveObject.setSensor(routeFromSensor, fromField);
                        routeToScriptObjectFound = true;
                    }  //  end Sensor
                    else if ((interactiveObject.getDefinedItem() == null) && (routeFromDefinedItem != null)) {
                        //This defined Item already exists as part of an interactive Object
                        for (ScriptObject.Field field : routeToScriptObject.getFieldsArrayList()) {
                            if (toField.equalsIgnoreCase(routeToScriptObject.getFieldName(field))) {
                                routeToScriptObject.setFromDefinedItem(field, routeFromDefinedItem, fromField);
                            }
                        }
                        routeToScriptObjectFound = true;
                    }  // end definedItem
                    else if ((interactiveObject.getEventUtility() == null) && (routeFromEventUtility != null)) {
                        //This event utility already exists as part of an interactive Object
                        for (ScriptObject.Field field : routeToScriptObject.getFieldsArrayList()) {
                            if (toField.equalsIgnoreCase(routeToScriptObject.getFieldName(field))) {
                                routeToScriptObject.setFromEventUtility(field, routeFromEventUtility, fromField);
                            }
                        }
                        routeToScriptObjectFound = true;
                    } // end eventUtility

                    else if ((interactiveObject.getTimeSensor() == null) && (routeFromTimeSensor != null)) {
                        //This sensor already exists as part of an interactive Object
                        for (ScriptObject.Field field : routeToScriptObject.getFieldsArrayList()) {
                            if (toField.equalsIgnoreCase(routeToScriptObject.getFieldName(field))) {
                                routeToScriptObject.setFromTimeSensor(field, routeFromTimeSensor, fromField);
                            }
                        }
                        routeToScriptObjectFound = true;
                    } // end timeSensor

                }
            }
            if (!routeToScriptObjectFound) {
                // construct a new interactiveObject for this sensor and timeSensor
                InteractiveObject interactiveObject = new InteractiveObject();
                interactiveObject.setScriptObject(routeToScriptObject);
                if (routeFromSensor != null) {
                    interactiveObject.setSensor(routeFromSensor, fromField);
                }
                else if (routeFromDefinedItem != null) {
                    // happens when scripting and sending values from item to the script
                    interactiveObject.setDefinedItemFromField(routeFromDefinedItem, fromField);
                    for (ScriptObject.Field field : routeToScriptObject.getFieldsArrayList()) {
                        if (toField.equalsIgnoreCase(routeToScriptObject.getFieldName(field))) {
                            routeToScriptObject.setFromDefinedItem(field, routeFromDefinedItem, fromField);
                        }
                    }
                }
                else if (routeFromEventUtility != null) {
                    String fieldName = routeToScriptObject.getFieldName(routeToScriptObject.getField(0));
                    if ( !fieldName.equalsIgnoreCase(toField) ) {
                        // the first field, which is the name of the function
                        for (ScriptObject.Field field : routeToScriptObject.getFieldsArrayList()) {
                            if (toField.equalsIgnoreCase(routeToScriptObject.getFieldName(field))) {
                                routeToScriptObject.setFromEventUtility(field, routeFromEventUtility, fromField);
                            }
                        }
                    }
                    else interactiveObject.setEventUtility(routeFromEventUtility);
                }
                else if (routeFromTimeSensor != null) {
                    String fieldName = routeToScriptObject.getFieldName(routeToScriptObject.getField(0));
                    if ( !fieldName.equalsIgnoreCase(toField) ) {
                        // the first field, which is the name of the function
                        for (ScriptObject.Field field : routeToScriptObject.getFieldsArrayList()) {
                            if (toField.equalsIgnoreCase(routeToScriptObject.getFieldName(field))) {
                                routeToScriptObject.setFromTimeSensor(field, routeFromTimeSensor, fromField);
                            }
                        }
                    }
                    else interactiveObject.setTimeSensor(routeFromTimeSensor);
                }
                interactiveObjects.add(interactiveObject);
            }
        }  //  end routeToScriptObject

        // ROUTE TO a DEFind Object
        if (routeToDefinedItem != null) {
            boolean routeToDEFinedItemFound = false;
            for (InteractiveObject interactiveObject : interactiveObjects) {
                if ((routeFromInterpolator == interactiveObject.getInterpolator()) &&
                        (routeFromInterpolator != null)) {
                    if (interactiveObject.getDefinedItemToField() == null) {
                        interactiveObject.setDefinedItemToField(routeToDefinedItem, toField);
                        routeToDEFinedItemFound = true;
                    }
                } else if ((routeFromEventUtility == interactiveObject.getEventUtility()) &&
                        (routeFromEventUtility != null)) {
                    if (interactiveObject.getDefinedItemToField() == null) {
                        interactiveObject.setDefinedItemToField(routeToDefinedItem, toField);
                        routeToDEFinedItemFound = true;
                    }
                } else if ((routeFromScriptObject == interactiveObject.getScriptObject()) &&
                        (routeFromScriptObject != null)) {
                    if (interactiveObject.getDefinedItemToField() == null) {
                        for (ScriptObject.Field field : routeFromScriptObject.getFieldsArrayList()) {
                            if (fromField.equalsIgnoreCase(routeFromScriptObject.getFieldName(field))) {
                                routeFromScriptObject.setToDefinedItem(field, routeToDefinedItem, toField);
                            }
                        }
                        routeToDEFinedItemFound = true;
                    }
                }
            }
            if (!routeToDEFinedItemFound) {
                // construct a new interactiveObject for this sensor and timeSensor
                InteractiveObject interactiveObject = new InteractiveObject();
                interactiveObject.setInterpolator(routeFromInterpolator);
                interactiveObject.setDefinedItemToField(routeToDefinedItem, toField);
                if (routeFromSensor != null)
                    interactiveObject.setSensor(routeFromSensor, fromField);
                if (routeFromScriptObject != null) {
                    for (ScriptObject.Field field : routeFromScriptObject.getFieldsArrayList()) {
                        if (fromField.equalsIgnoreCase(routeFromScriptObject.getFieldName(field))) {
                            routeFromScriptObject.setToDefinedItem(field, routeToDefinedItem, toField);
                        }
                    }

                }
                if (routeFromEventUtility != null) {
                    interactiveObject.setEventUtility(routeFromEventUtility);
                }
                interactiveObjects.add(interactiveObject);
            }
        }  //  end if routeToDefinedItem != null
    }  //  end buildInteractiveObject


    /**
     * BuildInteractiveObjectFromAnchor is a special type of interactive object in that it does not get
     * built using ROUTE's.
     *
     * @param anchorSensor is the Sensor that describes the sensor set to an Anchor
     * @param anchorDestination is either another Viewpoint, url to a web site or another x3d scene
     */
    public void BuildInteractiveObjectFromAnchor(Sensor anchorSensor, String anchorDestination) {
        InteractiveObject interactiveObject = new InteractiveObject();
        interactiveObject.setSensor(anchorSensor, anchorDestination);
        interactiveObjects.add(interactiveObject);
    }

    /**
     * initAnimationsAndInteractivity() called when we parse </scene> in
     * an X3D file.  This method will parse the array list of InteractiveObjects
     * determining which are animations (when interactiveObject.sensor is null)
     * or which are interactive and thus have a event attached to invoke the
     * animation upon a TouchSensor, Anchor, etc.
     */
    public void initAnimationsAndInteractivity() {
        for (InteractiveObject interactiveObject : interactiveObjects) {
            SXRAnimationChannel gvrAnimationChannel = null;
            SXRNodeAnimation gvrKeyFrameAnimation = null;
            SXRNode gvrAnimatedObject = null;

            // both animated and interactive objects currently must have a time
            // sensor, interpolator and a Transform node with a DEF="..." parameter
            if ((interactiveObject.getTimeSensor() != null) &&
                    (interactiveObject.getInterpolator() != null) &&
                    (interactiveObject.getDefinedItem() != null)) {
                // Set up the animation objects, properties
                //   first construct the animation channel based on translation, rotation, scale, etc.
                if ((interactiveObject.getDefinedItemToField().toLowerCase().endsWith(TRANSLATION)) ||
                        (interactiveObject.getDefinedItemToField().toLowerCase().endsWith(POSITION))) {
                    gvrAnimatedObject = root
                            .getNodeByName((interactiveObject.getDefinedItem().getName() + x3dObject.TRANSFORM_TRANSLATION_));
                    gvrAnimationChannel = new SXRAnimationChannel(
                            gvrAnimatedObject.getName(),
                            interactiveObject.getInterpolator().key.length, 0, 0,
                            SXRAnimationBehavior.LINEAR, SXRAnimationBehavior.LINEAR);
                    for (int j = 0; j < interactiveObject.getInterpolator().key.length; j++) {
                        gvrAnimationChannel.setPosKeyVector(j,
                                interactiveObject.getInterpolator().key[j]
                                        * interactiveObject.getTimeSensor().getCycleInterval(),
                                        new float[] { interactiveObject.getInterpolator().keyValue[j * 3],
                                        interactiveObject.getInterpolator().keyValue[j * 3 + 1],
                                        interactiveObject.getInterpolator().keyValue[j * 3 + 2] });
                    }
                }  //  end translation

                else if ((interactiveObject.getDefinedItemToField().toLowerCase().endsWith(ROTATION)) ||
                        (interactiveObject.getDefinedItemToField().toLowerCase().endsWith(ORIENTATION))) {
                    gvrAnimatedObject = root
                            .getNodeByName((interactiveObject.getDefinedItem().getName() + x3dObject.TRANSFORM_ROTATION_));
                    gvrAnimationChannel = new SXRAnimationChannel(
                            gvrAnimatedObject.getName(), 0,
                            interactiveObject.getInterpolator().key.length, 0,
                            SXRAnimationBehavior.DEFAULT, SXRAnimationBehavior.DEFAULT);

                    for (int j = 0; j < interactiveObject.getInterpolator().key.length; j++) {
                        AxisAngle4f axisAngle4f = new AxisAngle4f(
                                interactiveObject.getInterpolator().keyValue[j * 4 + 3],
                                interactiveObject.getInterpolator().keyValue[j * 4],
                                interactiveObject.getInterpolator().keyValue[j * 4 + 1],
                                interactiveObject.getInterpolator().keyValue[j * 4 + 2]);
                        Quaternionf q = new Quaternionf(axisAngle4f);
                        float[] tmp = new float[]{ q.x, q.y, q.z, q.w};
                        gvrAnimationChannel.setRotKeyQuaternion(j,
                                interactiveObject.getInterpolator().key[j]
                                        * interactiveObject.getTimeSensor().getCycleInterval(),
                                        tmp);
                    }
                }   //  end rotation

                else if (interactiveObject.getDefinedItemToField().toLowerCase().endsWith(SCALE)) {
                    gvrAnimatedObject = root
                            .getNodeByName((interactiveObject.getDefinedItem().getName() + x3dObject.TRANSFORM_SCALE_));
                    gvrAnimationChannel = new SXRAnimationChannel(
                            gvrAnimatedObject.getName(), 0, 0,
                            interactiveObject.getInterpolator().key.length, SXRAnimationBehavior.DEFAULT,
                            SXRAnimationBehavior.DEFAULT);
                    for (int j = 0; j < interactiveObject.getInterpolator().key.length; j++) {
                        gvrAnimationChannel.setScaleKeyVector(j,
                                interactiveObject.getInterpolator().key[j]
                                        * interactiveObject.getTimeSensor().getCycleInterval(),
                                        new float[] { interactiveObject.getInterpolator().keyValue[j * 3],
                                        interactiveObject.getInterpolator().keyValue[j * 3 + 1],
                                        interactiveObject.getInterpolator().keyValue[j * 3 + 2] });
                    }
                }  //  end scale
                else {
                    Log.e(TAG, "'" + interactiveObject.getDefinedItemToField() + "' not implemented");
                }

                // Second, set up the KeyFrameAnimation object
                if (gvrAnimatedObject != null) {
                    gvrKeyFrameAnimation = new SXRNodeAnimation(
                            gvrAnimatedObject.getName() + KEY_FRAME_ANIMATION + animationCount,
                            gvrAnimatedObject,
                            interactiveObject.getTimeSensor().getCycleInterval(),
                            gvrAnimationChannel);
                    if (interactiveObject.getTimeSensor().getLoop()) {
                        gvrKeyFrameAnimation.setRepeatMode(SXRRepeatMode.REPEATED);
                        gvrKeyFrameAnimation.setRepeatCount(-1);
                    }
                    animationCount++;
                    interactiveObject.getTimeSensor().addSXRKeyFrameAnimation( gvrKeyFrameAnimation );

                    // Third, determine if this will be animation only, or
                    // interactive triggered in picking
                    if (interactiveObject.getSensor() == null) {
                        // this is an animation without interactivity
                        gvrAnimator.addAnimation(gvrKeyFrameAnimation);

                    } else {
                        // this is an interactive object
                        final InteractiveObject interactiveObjectFinal = interactiveObject;
                        final SXRNodeAnimation gvrKeyFrameAnimationFinal = gvrKeyFrameAnimation;
                        interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                                new SXRNode.SceneVisitor()
                                {
                                    public boolean visit (SXRNode obj)
                                    {
                                        obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                        return true;
                                    }
                                });

                        interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                            boolean isRunning;

                            @Override
                            public void onSensorEvent(SensorEvent event) {
                                //Setup SensorEvent callback here
                                if ((event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_OVER)) ||
                                        (event.isActive() && interactiveObjectFinal.getSensorFromField().equals(Sensor
                                                .IS_ACTIVE))) {
                                    if (!isRunning) {
                                        isRunning = true;
                                        interactiveObjectFinal.getSensor().setHitPoint(event.getPickedObject().getHitLocation());
                                        gvrKeyFrameAnimationFinal.start(gvrContext.getAnimationEngine())
                                                .setOnFinish(new SXROnFinish() {
                                                    @Override
                                                    public void finished(SXRAnimation animation) {
                                                        isRunning = false;
                                                    }
                                                });
                                    }
                                }
                            }
                        });

                    }
                } else {
                    Log.e(TAG, "'" + interactiveObject.getDefinedItem().getName() + "' possibly not found in the scene.");
                }
            }  // end if at least timer, interpolator and defined object

            // Sensor (such as TouchSensor) to an EventUnity (such as BoleanToggle)
            //   to a DEFined Object
            else if ((interactiveObject.getSensor() != null) &&
                    (interactiveObject.getEventUtility() != null) &&
                    (interactiveObject.getDefinedItem() != null)) {
                // a sensor, eventUtility (such as BooleanToggle) and defined object found
                final InteractiveObject interactiveObjectFinal = interactiveObject;
                final Vector<InteractiveObject> interactiveObjectsFinal = interactiveObjects;

                if (interactiveObject.getSensor().getSensorType() == Sensor.Type.TOUCH) {
                    interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                            new SXRNode.SceneVisitor()
                            {
                                public boolean visit (SXRNode obj)
                                {
                                    obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                    return true;
                                }
                            });
                    interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                        boolean stateChanged = false;

                        @Override
                        public void onSensorEvent(SensorEvent event) {
                            if ((event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_OVER)) ||
                                    (event.isActive() && interactiveObjectFinal.getSensorFromField().equals(Sensor
                                            .IS_ACTIVE))) {
                                if (!stateChanged) {
                                    stateChanged = true;
                                    EventUtility eventUtility = interactiveObjectFinal.getEventUtility();
                                    eventUtility.setToggle(!eventUtility.getToggle());
                                    for (InteractiveObject interactiveObject : interactiveObjectsFinal) {
                                        if (interactiveObject.getEventUtility() == interactiveObjectFinal.getEventUtility()) {
                                            SXRNode gvrNode = root
                                                    .getNodeByName(interactiveObject.getDefinedItem().getName());
                                            SXRComponent gvrComponent = gvrNode.getComponent(
                                                    SXRLight.getComponentType());
                                            gvrComponent.setEnable(eventUtility.getToggle());
                                        }
                                    }
                                }
                            } else if (!event.isActive() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_ACTIVE)) {
                                stateChanged = false;
                            } else if (!event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor
                                    .IS_OVER)) {
                                stateChanged = false;
                            }
                        }
                    });
                }  // end if sensor == TOUCH
            }  // end if at least sensor, eventUtility and defined object

            // Sensor (PlaneSensor, TouchSensor, etc) to a Script
            //   that sets properties of a DEFined Object
            else if (interactiveObject.getScriptObject() != null) {
                // A Sensor with a Script and defined object found
                final InteractiveObject interactiveObjectFinal = interactiveObject;

                if (interactiveObject.getSensor() != null) {
                    if (interactiveObject.getSensor().getSensorType() == Sensor.Type.PLANE) {
                        // a Plane Sensor
                        interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                                new SXRNode.SceneVisitor()
                                {
                                    public boolean visit (SXRNode obj)
                                    {
                                        obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                        return true;
                                    }
                                });
                        interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                            boolean initialized = false;
                            SXRCameraRig gvrCameraRig = gvrContext.getMainScene().getMainCameraRig();
                            Vector3f initCameraDir = null;
                            float[] initPlaneTranslation = new float[3];
                            float[] initHitLocation = null;
                            float[] planeTranslation = new float[3];
                            SXRNode gvrNode = null;

                            @Override
                            public void onSensorEvent(SensorEvent event) {
                                if (event.isActive()) {
                                    SXRPicker.SXRPickedObject gvrPickedObject = event.getPickedObject();
                                    if ( !initialized ) {
                                        initialized = true;
                                        float[] lookAt = gvrCameraRig.getLookAt();
                                        initCameraDir = new Vector3f(lookAt[0], lookAt[1], lookAt[2]);
                                        initHitLocation = gvrPickedObject.getHitLocation();
                                        SXRNode hitObjectNode = gvrPickedObject.getHitObject();
                                        // Primitives are a child of the SXRNode with the name.
                                        if ( hitObjectNode.getName().isEmpty() ) {
                                            hitObjectNode = hitObjectNode.getParent();
                                        }
                                        gvrNode = root
                                                .getNodeByName((hitObjectNode.getName() + x3dObject.TRANSFORM_TRANSLATION_));

                                        initPlaneTranslation[0] = gvrNode.getTransform().getPositionX();
                                        initPlaneTranslation[1] = gvrNode.getTransform().getPositionY();
                                        initPlaneTranslation[2] = gvrNode.getTransform().getPositionZ();
                                    }
                                    // initialize the input values for planeSensor and run the javaScript.
                                    planeTranslation[0] = gvrNode.getTransform().getPositionX();
                                    planeTranslation[1] = gvrNode.getTransform().getPositionY();
                                    Object[] parameters = SetJavaScriptArguments(interactiveObjectFinal, planeTranslation[0], planeTranslation[1], 0, 0,true);
                                    ScriptObject scriptObject = interactiveObjectFinal.getScriptObject();
                                    ScriptObject.Field firstField = scriptObject.getField(0);
                                    RunScript(interactiveObjectFinal, scriptObject.getFieldName(firstField), parameters);

                                }
                                else {
                                    initialized = false;
                                }
                            }
                        });

                    }  // end if sensor == PLANESensor
                    else if (interactiveObject.getSensor().getSensorType() == Sensor.Type.TOUCH) {
                        // A Touch Sensor
                        interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                                new SXRNode.SceneVisitor()
                                {
                                    public boolean visit (SXRNode obj)
                                    {
                                        obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                        return true;
                                    }
                                });
                        interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                            boolean stateChanged = false;
                            boolean isActiveDone = false;

                            @Override
                            public void onSensorEvent(SensorEvent event) {

                                Object[] parameters = SetJavaScriptArguments(interactiveObjectFinal, event.isOver(), 0, 0, 0, stateChanged);
                                ScriptObject scriptObject = interactiveObjectFinal.getScriptObject();
                                ScriptObject.Field firstField = scriptObject.getField(0);
                                String functionName = scriptObject.getFieldName(firstField);

                                if (interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_OVER)) {
                                    parameters[0] = event.isOver();
                                }
                                else if (interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_ACTIVE)) {
                                    parameters[0] = stateChanged;
                                }
                                if (scriptObject.getTimeStampParameter()) {
                                    parameters[1] = 0;  // set timeStamp to 0.  This isn't used for isOver/isActive events
                                }

                                if ((event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_OVER))) {
                                    // OVER an object with a sensor
                                    if (!stateChanged) {
                                        stateChanged = true;
                                        // Run this SCRIPT's actual JavaScript function
                                        RunScript(interactiveObjectFinal, functionName, parameters);
                                    }
                                } else if (event.isActive() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_ACTIVE)) {
                                    // CLICKED while over a sensored object
                                    stateChanged = !stateChanged;
                                    if (!isActiveDone) {
                                        // Run this SCRIPT's actual JavaScript function
                                        RunScript(interactiveObjectFinal, functionName, parameters);
                                    }
                                    isActiveDone = true;
                                } else if (!event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor
                                        .IS_OVER)) {
                                    // An "isOver event', but just existed being over the object - i.e. TouchSensor = false
                                    stateChanged = false;
                                    // Run this SCRIPT's actual JavaScript function
                                    RunScript(interactiveObjectFinal, functionName, parameters);
                                } else if (!event.isActive() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_ACTIVE)) {
                                    isActiveDone = false;
                                }
                            }
                        });
                    }  // end if sensor == TOUCH
                    else if (interactiveObject.getSensor().getSensorType() == Sensor.Type.CYLINDER) {
                        interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                                new SXRNode.SceneVisitor()
                                {
                                    public boolean visit (SXRNode obj)
                                    {
                                        obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                        return true;
                                    }
                                });
                        Sensor cylSensor = interactiveObject.getSensor();
                        final float minAngleFinal = cylSensor.getMinAngle().getValue();
                        final float maxAngleFinal = cylSensor.getMaxAngle().getValue();

                        interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                            boolean initialized = false;
                            Quaternionf cylinderRotation = new Quaternionf();
                            AxisAngle4f cylRotAxisAngle = new AxisAngle4f();
                            SXRNode gvrNode = null;

                            @Override
                            public void onSensorEvent(SensorEvent event) {
                                if (event.isActive()) {
                                    SXRPicker.SXRPickedObject gvrPickedObject = event.getPickedObject();
                                    if ( !initialized ) {
                                        initialized = true;
                                        SXRNode hitObjectNode = gvrPickedObject.getHitObject();
                                        // Primitives are a child of the SXRNode with the name.
                                        while ( hitObjectNode.getName().isEmpty() ) {
                                            hitObjectNode = hitObjectNode.getParent();
                                        }
                                        gvrNode = root
                                                .getNodeByName((hitObjectNode.getName() + x3dObject.TRANSFORM_ROTATION_));
                                    }  //  end initialization
                                    // initialize the input values for planeSensor and run the javaScript.
                                    cylinderRotation.w = gvrNode.getTransform().getRotationW();
                                    cylinderRotation.x = gvrNode.getTransform().getRotationX();
                                    cylinderRotation.y = gvrNode.getTransform().getRotationY();
                                    cylinderRotation.z = gvrNode.getTransform().getRotationZ();
                                    cylinderRotation.get( cylRotAxisAngle );

                                    // Quaternion to Axis-Angle flips the sign on the rotation
                                    if ( cylRotAxisAngle.angle > Math.PI) cylRotAxisAngle.angle = (float)(2*Math.PI - cylRotAxisAngle.angle);
                                    else cylRotAxisAngle.angle = -cylRotAxisAngle.angle;
                                    if (cylRotAxisAngle.angle < minAngleFinal ) cylRotAxisAngle.angle = minAngleFinal;
                                    else if (cylRotAxisAngle.angle > maxAngleFinal ) cylRotAxisAngle.angle = maxAngleFinal;

                                    Object[] parameters = SetJavaScriptArguments(interactiveObjectFinal, cylRotAxisAngle.angle, cylRotAxisAngle.x,
                                            cylRotAxisAngle.y, cylRotAxisAngle.z,true);
                                    ScriptObject scriptObject = interactiveObjectFinal.getScriptObject();
                                    ScriptObject.Field firstField = scriptObject.getField(0);
                                    RunScript(interactiveObjectFinal, scriptObject.getFieldName(firstField), parameters);
                                }
                                else {
                                    initialized = false;
                                }
                            }
                        });
                    }  //  end if Cylinder Sensor
                    else if (interactiveObject.getSensor().getSensorType() == Sensor.Type.SPHERE) {

                        //Set up the Sensor call back
                        interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                                new SXRNode.SceneVisitor()
                                {
                                    public boolean visit (SXRNode obj)
                                    {
                                        obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                        return true;
                                    }
                                });
                        interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                            boolean initialized = false;
                            Quaternionf sphereRotation = new Quaternionf();
                            AxisAngle4f sphereRotAxisAngle = new AxisAngle4f();
                            SXRNode gvrNode = null;

                            @Override
                            public void onSensorEvent(SensorEvent event) {
                                if (event.isActive()) {
                                    SXRPicker.SXRPickedObject gvrPickedObject = event.getPickedObject();
                                    if ( !initialized ) {
                                        initialized = true;
                                        SXRNode hitObjectNode = gvrPickedObject.getHitObject();
                                        // Primitives are a child of the SXRNode with the name.
                                        while ( hitObjectNode.getName().isEmpty() ) {
                                            hitObjectNode = hitObjectNode.getParent();
                                        }
                                        gvrNode = root
                                                .getNodeByName((hitObjectNode.getName() + x3dObject.TRANSFORM_ROTATION_));

                                    }  //  end initialization
                                    // initialize the input values for planeSensor and run the javaScript.
                                    sphereRotation.w = gvrNode.getTransform().getRotationW();
                                    sphereRotation.x = gvrNode.getTransform().getRotationX();
                                    sphereRotation.y = gvrNode.getTransform().getRotationY();
                                    sphereRotation.z = gvrNode.getTransform().getRotationZ();
                                    sphereRotation.get( sphereRotAxisAngle );

                                    // Quaternion to Axis-Angle flips the sign on the rotation
                                    if ( sphereRotAxisAngle.angle > Math.PI) sphereRotAxisAngle.angle = (float)(2*Math.PI - sphereRotAxisAngle.angle);
                                    else sphereRotAxisAngle.angle = -sphereRotAxisAngle.angle;
                                    Object[] parameters = SetJavaScriptArguments(interactiveObjectFinal, sphereRotAxisAngle.angle, sphereRotAxisAngle.x,
                                            sphereRotAxisAngle.y, sphereRotAxisAngle.z,true);
                                    ScriptObject scriptObject = interactiveObjectFinal.getScriptObject();
                                    ScriptObject.Field firstField = scriptObject.getField(0);
                                    RunScript(interactiveObjectFinal, scriptObject.getFieldName(firstField), parameters);
                                }
                                else {
                                    initialized = false;
                                }
                            }  // end onSensorEvent
                        });
                    } // end if sphere Sensor
                }   // end if sensor != null
                else if (interactiveObject.getTimeSensor() != null) {
                    // TimeSensor means this Script will be called per-frame
                    // set up the call-back
                    interactiveObject.getScriptObject().setScriptCalledPerFrame(true);
                    perFrameScripting.setInteractiveObjectVars(interactiveObjectFinal);
                } // time sensor != null

            }  // end if a Script (that likely includes a sensor)

            else if ((interactiveObject.getSensor() != null) &&
                    (interactiveObject.getDefinedItem() != null)) {
                // a sensor and defined object, such as a TouchSensor
                //    to a Boolean such as light on/off
                final InteractiveObject interactiveObjectFinal = interactiveObject;
                if (interactiveObject.getSensor().getSensorType() == Sensor.Type.TOUCH) {
                    interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                            new SXRNode.SceneVisitor() {
                                public boolean visit(SXRNode obj) {
                                    obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                    return true;
                                }
                            });
                    interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                        boolean isMovieStateSet = false;

                        @Override
                        public void onSensorEvent(SensorEvent event) {
                            //Setup SensorEvent callback here
                            SXRNode gvrNode = root
                                    .getNodeByName(interactiveObjectFinal.getDefinedItem().getName());
                            SXRComponent gvrComponent = gvrNode.getComponent(SXRLight.getComponentType());

                            if (gvrComponent != null) {
                                if (event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_OVER)) {
                                    if (gvrComponent != null) gvrComponent.setEnable(true);
                                } else {
                                    if (gvrComponent != null) gvrComponent.setEnable(false);
                                }
                            } else if (gvrNode instanceof SXRVideoNode) {
                                // isOver, but only go thru once per isOver.
                                if (event.isOver() && !isMovieStateSet) {
                                    SXRVideoNode gvrVideoNode = (SXRVideoNode) gvrNode;
                                    SXRVideoNodePlayer gvrVideoNodePlayer = gvrVideoNode.getMediaPlayer();
                                    try {
                                        if (interactiveObjectFinal.getSensorFromField().contains("touchTime")) {
                                            if (interactiveObjectFinal.getDefinedItemToField().endsWith("stopTime")) {
                                                gvrVideoNodePlayer.pause();
                                                ExoPlayer exoPlayer = (ExoPlayer) gvrVideoNodePlayer.getPlayer();
                                                exoPlayer.seekTo(0);
                                            } else if (interactiveObjectFinal.getDefinedItemToField().endsWith("pauseTime")) {
                                                gvrVideoNodePlayer.pause();
                                            } else if (interactiveObjectFinal.getDefinedItemToField().endsWith("startTime")) {
                                                gvrVideoNodePlayer.start();
                                            } else {
                                                Log.e(TAG, "Error: ROUTE to MovieTexture, " + interactiveObjectFinal.getDefinedItemToField() + " not implemented");
                                            }
                                        } else {
                                            Log.e(TAG, "Error: ROUTE to MovieTexture, " + interactiveObjectFinal.getSensorFromField() + " not implemented");
                                        }
                                    } catch (IllegalStateException e) {
                                        Log.e(TAG, "X3D Movie Texture: IllegalStateException: " + e);
                                        e.printStackTrace();
                                    } catch (Exception e) {
                                        Log.e(TAG, "X3D Movie Texture Exception: " + e);
                                        e.printStackTrace();
                                    }
                                    isMovieStateSet = true;
                                } // end if event.isOver()
                                else if (!event.isOver() && isMovieStateSet) {
                                    // No longer over the TouchSensor
                                    isMovieStateSet = false;
                                }
                            } // end if gvrNode
                        }
                    });
                }  // end if sensor == TOUCH
                else if (interactiveObject.getSensor().getSensorType() == Sensor.Type.PLANE) {
                    interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                            new SXRNode.SceneVisitor() {
                                public boolean visit(SXRNode obj) {
                                    obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                    return true;
                                }
                            });
                    interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                        boolean isActive = false;

                        @Override
                        public void onSensorEvent(SensorEvent event) {
                            if (interactiveObjectFinal.getSensor().getEnabled()) {
                                if (event.isActive() && !isActive) {
                                    isActive = true;
                                    SXRPicker.SXRPickedObject gvrPickedObject = event.getPickedObject();

                                    sensorImplementation.registerDrawFrameListerner(gvrPickedObject, interactiveObjectFinal);
                                } else if (!event.isActive() || !event.isOver()) {
                                    sensorImplementation.unregisterDrawFrameListerner();
                                    SXRPicker.SXRPickedObject gvrPickedObject = null;
                                    isActive = false;
                                }
                            }// if PlaneSensor is enabled
                        }   // end onSensorEvent
                    });
                }  // end if sensor == PLANESensor
                else if (interactiveObject.getSensor().getSensorType() == Sensor.Type.CYLINDER) {
                    interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                            new SXRNode.SceneVisitor()
                            {
                                public boolean visit (SXRNode obj)
                                {
                                    obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                    return true;
                                }
                            });
                    interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                        boolean isActive = false;

                        @Override
                        public void onSensorEvent(SensorEvent event) {
                            if (interactiveObjectFinal.getSensor().getEnabled()) {
                                if (event.isActive() && !isActive) {
                                    isActive = true;
                                    SXRPicker.SXRPickedObject gvrPickedObject = event.getPickedObject();
                                    sensorImplementation.registerDrawFrameListerner(gvrPickedObject, interactiveObjectFinal);
                                } else if (!event.isActive() || !event.isOver()) {
                                    sensorImplementation.unregisterDrawFrameListerner();
                                    isActive = false;
                                }
                            }// if CylinderSensor is enabled
                        }   // end onSensorEvent
                    });
                }  // end if sensor == CylinderSensor
                else if (interactiveObject.getSensor().getSensorType() == Sensor.Type.SPHERE) {
                    interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                            new SXRNode.SceneVisitor()
                            {
                                public boolean visit (SXRNode obj)
                                {
                                    obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                    return true;
                                }
                            });
                    interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                        boolean isActive = false;

                        @Override
                        public void onSensorEvent(SensorEvent event) {
                            if (interactiveObjectFinal.getSensor().getEnabled()) {
                                if (event.isActive() && !isActive) {
                                    isActive = true;
                                    SXRPicker.SXRPickedObject gvrPickedObject = event.getPickedObject();
                                    sensorImplementation.registerDrawFrameListerner(gvrPickedObject, interactiveObjectFinal);
                                } else if (!event.isActive() || !event.isOver()) {
                                    sensorImplementation.unregisterDrawFrameListerner();
                                    isActive = false;
                                }
                            }// if SphereSensor is enabled
                        }   // end onSensorEvent
                    });
                }  // end if sensor == SphereSensor

            }  //  end sensor and definedItem != null
            // Sensor (such as TouchSensor) to an EventUnity (such as BoleanToggle)
            else if ((interactiveObject.getSensor() != null) &&
                    (interactiveObject.getEventUtility() != null)) {
                // a sensor, eventUtility (such as BooleanToggle) and defined object found
                final InteractiveObject interactiveObjectFinal = interactiveObject;
                final Vector<InteractiveObject> interactiveObjectsFinal = interactiveObjects;

                if (interactiveObject.getSensor().getSensorType() == Sensor.Type.TOUCH) {
                    interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                            new SXRNode.SceneVisitor()
                            {
                                public boolean visit (SXRNode obj)
                                {
                                    obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                                    return true;
                                }
                            });
                    interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
                        boolean stateChanged = false;

                        @Override
                        public void onSensorEvent(SensorEvent event) {
                            if ((event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_OVER)) ||
                                    (event.isActive() && interactiveObjectFinal.getSensorFromField().equals(Sensor
                                            .IS_ACTIVE))) {
                                if (!stateChanged) {
                                    // only change state upon first rollover, not the 'roll off'
                                    stateChanged = true;
                                    EventUtility eventUtility = interactiveObjectFinal.getEventUtility();
                                    eventUtility.setToggle(!eventUtility.getToggle());
                                }
                            } else if (!event.isActive() && interactiveObjectFinal.getSensorFromField().equals(Sensor.IS_ACTIVE)) {
                                stateChanged = false;
                            } else if (!event.isOver() && interactiveObjectFinal.getSensorFromField().equals(Sensor
                                    .IS_OVER)) {
                                stateChanged = false;
                            }
                        }
                    });
                }  // end if sensor == TOUCH
            }  // end if at least sensor, and eventUtility
            else if (interactiveObject.getSensor() != null) {
                // Likely this is an Anchor tag since there are no routes with it
                if ( interactiveObject.getSensor().getSensorType() == Sensor.Type.ANCHOR) {
                    anchorImplementation.AnchorInteractivity( interactiveObject );
                }  //  end if Sensor Type is Anchor
            } // end sensor != null

        }  // end for loop traversing through all interactive objects
        // Initiate all the animations, both keyframe and procedural
        if (perFrameScripting.getRunState()) {
            final SXRDrawFrameListener mOnDrawFrame = new DrawFrame();
            gvrContext.registerDrawFrameListener(mOnDrawFrame);
        }
    }   //  end initAnimationsAndInteractivity.


    private final class SensorActiveDrawFrame implements SXRDrawFrameListener {
        @Override
        public void onDrawFrame(float frameTime) {
            sensorImplementation.onSensorActiveDrawFrame(frameTime);
        }
    }

    // Supports PlaneSensor CylinderSensor, SphereSensor
    private class SensorImplementation {

        SXRDrawFrameListener mSensorOnDrawFrame = null;
        InteractiveObject mInteractiveObjectFinal = null;
        SXRPicker.SXRPickedObject mSXRPickedObject = null;
        SXRNode mSXRNode = null;
        Sensor.Type mSensorType;
        String fromField = "";
        String toField = "";
        float[] initHitLocation = new float[3];
        float initHitDistance = 0;
        Vector3f initCameraDir = null;
        // values and booleans for checking min and max distance of PlaneSensor
        float[] initPlaneTranslation = new float[3];
        SFVec2f mMinPosition = new SFVec2f(0, 0);
        SFVec2f mMaxPosition = new SFVec2f(-1, -1);
        boolean mCheckXpos = false;
        boolean mCheckYpos = false;
        // values for CylinderSensor
        float[] initRotation = new float[4];
        SFFloat mMinAngle = new SFFloat(0);
        SFFloat mMaxAngle = new SFFloat(-1);
        boolean mClampAngle = false;
        final int xAxis = 1;
        final int yAxis = 2;
        final int zAxis = 3;
        int rotationAxis = yAxis;
        Quaternionf initQuat = new Quaternionf();

        boolean run = false;
        SXRCameraRig gvrCameraRig = null;


        final void registerDrawFrameListerner(SXRPicker.SXRPickedObject gvrPickedObject, final InteractiveObject interactiveObjectFinal ) {
            mSensorOnDrawFrame = new SensorActiveDrawFrame();
            gvrContext.registerDrawFrameListener(mSensorOnDrawFrame);
            run = true;
            mSXRPickedObject = gvrPickedObject;
            mInteractiveObjectFinal = interactiveObjectFinal;
            gvrCameraRig = gvrContext.getMainScene().getMainCameraRig();

            if ( mInteractiveObjectFinal != null ) {
                if (mInteractiveObjectFinal.getSensor() != null) {
                    // initialize the 'from sensor information
                    Sensor sensor = mInteractiveObjectFinal.getSensor();
                    if (sensor.getSensorType() == Sensor.Type.PLANE) {
                        mSensorType = Sensor.Type.PLANE;
                        mMinPosition = sensor.getMinValues();
                        mMaxPosition = sensor.getMaxValues();
                        if ( mMinPosition.getX() <= mMaxPosition.getX()) mCheckXpos = true;
                        if ( mMinPosition.getY() <= mMaxPosition.getY()) mCheckYpos = true;
                        if (StringFieldMatch(mInteractiveObjectFinal.getSensorFromField(), "translation")) {
                            fromField = "translation";
                        } else if (StringFieldMatch(mInteractiveObjectFinal.getSensorFromField(), "trackPoint")) {
                            fromField = "trackPoint";
                        } else {
                            Log.e(TAG, "Plane Sensor: not supported 'from field': " + mInteractiveObjectFinal.getSensorFromField() );
                        }
                    } else if (sensor.getSensorType() == Sensor.Type.CYLINDER) {
                        mSensorType = Sensor.Type.CYLINDER;
                        mMinAngle = sensor.getMinAngle();
                        mMaxAngle = sensor.getMaxAngle();
                        //        int rotationAxis = yAxis;
                        SFRotation axisRotation = sensor.getAxisRotation();
                        if ( axisRotation.getX() == 1) rotationAxis = xAxis;
                        else if ( axisRotation.getY() == 1) rotationAxis = yAxis;
                        else if ( axisRotation.getZ() == 1) rotationAxis = zAxis;

                        if ( mMinAngle.getValue() <= mMaxAngle.getValue()) mClampAngle = true;
                        if (StringFieldMatch(mInteractiveObjectFinal.getSensorFromField(), "rotation")) {
                            fromField = "rotation";
                        } else {
                            Log.e(TAG, "Cylinder Sensor: not supported 'from field': " + mInteractiveObjectFinal.getSensorFromField() );
                        }
                    } else if (sensor.getSensorType() == Sensor.Type.SPHERE) {
                        mSensorType = Sensor.Type.SPHERE;
                        if (StringFieldMatch(mInteractiveObjectFinal.getSensorFromField(), "rotation")) {
                            fromField = "rotation";
                        } else if (StringFieldMatch(mInteractiveObjectFinal.getSensorFromField(), "trackPoint")) {
                            fromField = "trackPoint";
                            initHitLocation = mSXRPickedObject.getHitLocation();
                            Log.e(TAG, "Sphere Sensor 'trackPoint' not implemented" );
                        } else {
                            Log.e(TAG, "Sphere Sensor: not supported 'from field': " + mInteractiveObjectFinal.getSensorFromField() );
                        }
                    } else {
                        Log.e(TAG, "Unsupported or Undefined Sensor.");
                    }
                } else {
                    Log.e(TAG, "Cylinder, Plane or Sphere Sensor not set");
                }

                // initialize the 'to' object information
                if (mInteractiveObjectFinal.getDefinedItem() != null) {
                    mSXRNode = mInteractiveObjectFinal.getDefinedItem().getSXRNode();

                    if ( mSXRNode.getName().isEmpty() ) {
                        mSXRNode = mSXRNode.getParent();
                    }
                    if (mSensorType == Sensor.Type.PLANE) {

                        mSXRNode = root
                                .getNodeByName((mSXRNode.getName() + x3dObject.TRANSFORM_TRANSLATION_));

                        if (mSXRNode != null) {
                            if (StringFieldMatch(mInteractiveObjectFinal.getDefinedItemToField(), "translation")) {
                                toField = "translation";
                                initPlaneTranslation[0] = mSXRNode.getTransform().getPositionX();
                                initPlaneTranslation[1] = mSXRNode.getTransform().getPositionY();
                                initPlaneTranslation[2] = mSXRNode.getTransform().getPositionZ();
                            } else {
                                Log.e(TAG, "Plane Sensor: not supported 'to field': " + mInteractiveObjectFinal.getDefinedItemToField());
                            }
                        } else {
                            Log.e(TAG, "Problem with Plane Sensor: no receiving object.");
                        }
                    }  //  end if PLANESensor
                    else if (mSensorType == Sensor.Type.CYLINDER) {

                        SXRNode gvrNodeTranslation = root
                                .getNodeByName((mSXRNode.getName() + x3dObject.TRANSFORM_TRANSLATION_));
                        initPlaneTranslation[0] = gvrNodeTranslation.getTransform().getPositionX();
                        initPlaneTranslation[1] = gvrNodeTranslation.getTransform().getPositionY();
                        initPlaneTranslation[2] = gvrNodeTranslation.getTransform().getPositionZ();

                        mSXRNode = root
                                .getNodeByName((mSXRNode.getName() + x3dObject.TRANSFORM_ROTATION_));

                        if (mSXRNode != null) {
                            if (StringFieldMatch(mInteractiveObjectFinal.getDefinedItemToField(), "rotation")) {
                                toField = "rotation";
                                SXRTransform transform = mSXRNode.getTransform();
                                Quaternionf quat = new Quaternionf(transform.getRotationX(),
                                        transform.getRotationY(), transform.getRotationZ(), transform.getRotationW() );
                                initQuat = new Quaternionf(transform.getRotationX(),
                                        transform.getRotationY(), transform.getRotationZ(), transform.getRotationW() );
                                AxisAngle4f axisAngle = new AxisAngle4f();
                                quat.get( axisAngle );
                                initRotation[0] = axisAngle.angle;
                                initRotation[1] = axisAngle.x;
                                initRotation[2] = axisAngle.y;
                                initRotation[3] = axisAngle.z;
                                // Could be a value like 4.75 radians so change it to -1.57 radians
                                if ( initRotation[0] > Math.PI ) initRotation[0] -= 2 * (float) Math.PI;
                                else if ( initRotation[0] < -Math.PI ) initRotation[0] += 2 * (float) Math.PI;
                            } else {
                                Log.e(TAG, "Cylinder Sensor: not supported 'to field': " + mInteractiveObjectFinal.getDefinedItemToField());
                            }
                        } else {
                            Log.e(TAG, "Problem with Cylinder Sensor: no receiving object.");
                        }
                    }   //  end if CylinderSensor
                    else if (mSensorType == Sensor.Type.SPHERE) {

                        SXRNode gvrNodeTranslation = root
                                .getNodeByName((mSXRNode.getName() + x3dObject.TRANSFORM_TRANSLATION_));
                        initPlaneTranslation[0] = gvrNodeTranslation.getTransform().getPositionX();
                        initPlaneTranslation[1] = gvrNodeTranslation.getTransform().getPositionY();
                        initPlaneTranslation[2] = gvrNodeTranslation.getTransform().getPositionZ();

                        mSXRNode = root
                                .getNodeByName( mSXRNode.getName() + x3dObject.TRANSFORM_ROTATION_ );

                        if (mSXRNode != null) {
                            if (StringFieldMatch(mInteractiveObjectFinal.getDefinedItemToField(), "rotation")) {
                                toField = "rotation";
                                SXRTransform transform = mSXRNode.getTransform();
                                Quaternionf quat = new Quaternionf(transform.getRotationX(),
                                        transform.getRotationY(), transform.getRotationZ(), transform.getRotationW() );
                                AxisAngle4f axisAngle = new AxisAngle4f();
                                quat.get( axisAngle );
                                initQuat.set( quat );
                            } else {
                                Log.e(TAG, "Sphere Sensor: not supported 'to field': " + mInteractiveObjectFinal.getDefinedItemToField());
                            }
                        } else {
                            Log.e(TAG, "Problem with Sphere Sensor: no receiving object.");
                        }
                    }   //  end if SphereSensor
                }  //  end if DEFined object != null
            }
            else {
                Log.e(TAG, "Issue with Cylinder, Plane or Sphere Sensor");
            }
            initHitLocation = mSXRPickedObject.getHitLocation();
            initHitDistance = mSXRPickedObject.getHitDistance();
            float[] lookAt = gvrCameraRig.getLookAt();
            initCameraDir = new Vector3f(lookAt[0], lookAt[1], lookAt[2]);
        } //  end registerDrawFrameListerner

        final void unregisterDrawFrameListerner() {

            if (mSensorOnDrawFrame != null) gvrContext.unregisterDrawFrameListener(mSensorOnDrawFrame);
            mSensorOnDrawFrame = null;
            mInteractiveObjectFinal = null;
            mSXRPickedObject = null;
            mSXRNode = null;
            fromField = "";
            toField = "";
            run = false;
        }

        public boolean getRunState() {
            return run;
        }

        final void onSensorActiveDrawFrame(float frameTime) {
            if (mSensorType == Sensor.Type.PLANE) {
                if (mSXRNode != null && mSXRPickedObject != null) {
                    float[] lookAt = gvrCameraRig.getLookAt();
                    Vector3f cameraDir = new Vector3f(lookAt[0], lookAt[1], lookAt[2]);
                    cameraDir.sub(initCameraDir);
                    float x = initPlaneTranslation[0] + cameraDir.x * initHitDistance;
                    float y = initPlaneTranslation[1] + cameraDir.y * initHitDistance;
                    if (mCheckXpos) {
                        if ((x >= mMinPosition.getX()) && (x <= mMaxPosition.getX())) {
                            mSXRNode.getTransform().setPositionX(x);
                        }
                    } else mSXRNode.getTransform().setPositionX(x);
                    if (mCheckYpos) {
                        if ((y >= mMinPosition.getY()) && (y <= mMaxPosition.getY())) {
                            mSXRNode.getTransform().setPositionY(y);
                        }
                    } else mSXRNode.getTransform().setPositionY(y);

                    if (fromField.equalsIgnoreCase("trackPoint") && toField.equalsIgnoreCase("translation")) {
                        // trackPoint to translation
                        mSXRNode.getTransform().setPositionZ(initHitLocation[2]);
                    }
                }
            }  //  end if PlaneSensor
            else if ( mSensorType == Sensor.Type.CYLINDER ) {
                if (mSXRNode != null && mSXRPickedObject != null) {
                    float[] lookAt = gvrCameraRig.getLookAt();
                    Vector3f cameraDir = new Vector3f(lookAt[0], lookAt[1], lookAt[2]);
                    cameraDir.sub(initCameraDir);
                    float rotation = initRotation[0] + (initPlaneTranslation[0] + cameraDir.x * initHitDistance) * (float)Math.PI/2;
                    if ( mClampAngle ) {
                        if (rotation < mMinAngle.getValue() ) rotation = mMinAngle.getValue();
                        else if (rotation > mMaxAngle.getValue() ) rotation = mMaxAngle.getValue();
                    }

                    AxisAngle4f axisAngle = new AxisAngle4f(rotation, 0, 1, 0);
                    Quaternionf quat = new Quaternionf();
                    quat.set( axisAngle );

                    mSXRNode.getTransform().setRotation(quat.w, quat.x, quat.y, quat.z);
                }
            } // end if CyinderSensor
            else if ( mSensorType == Sensor.Type.SPHERE ) {
                if (mSXRNode != null && mSXRPickedObject != null) {
                    float[] lookAt = gvrCameraRig.getLookAt();
                    Vector3f cameraDir = new Vector3f(lookAt[0], lookAt[1], lookAt[2]);
                    cameraDir.sub(initCameraDir);
                    if (StringFieldMatch(mInteractiveObjectFinal.getSensorFromField(), "rotation")) {
                        float xRotation = (cameraDir.x * initHitDistance) * (float) Math.PI / 2;
                        float yRotation = (cameraDir.y * initHitDistance) * (float) Math.PI / 2;

                        AxisAngle4f axisAngleX = new AxisAngle4f(xRotation, 0, 1, 0);
                        Quaternionf quatX = new Quaternionf();
                        quatX.set(axisAngleX);

                        AxisAngle4f axisAngleY = new AxisAngle4f(yRotation, 1, 0, 0);
                        Quaternionf quatY = new Quaternionf();
                        quatY.set(axisAngleY);

                        quatX.mul(quatY);
                        quatX.mul(initQuat);
                        mSXRNode.getTransform().setRotation(quatX.w, quatX.x, quatX.y, quatX.z);
                    } else if (StringFieldMatch(mInteractiveObjectFinal.getSensorFromField(), "trackPoint")) {
                        float xLoc = (cameraDir.x * initHitDistance) ;
                        float yLoc = (cameraDir.y * initHitDistance) ;
                        mSXRNode.getTransform().setPositionX(xLoc);
                        mSXRNode.getTransform().setPositionY(yLoc);
                        mSXRNode.getTransform().setPositionZ(1.0f - (xLoc*xLoc + yLoc*yLoc));
                    } else {
                        Log.e(TAG, "Sphere Sensor: not supported 'from field': " + mInteractiveObjectFinal.getSensorFromField() );
                    }
                }
            } // end if SphereSensor
        }  //  end onSensorActiveDrawFrame

    }  //  end class SensorImplementation


    // Supports when TimeSensor per-frame calls invoke Script
    private class PerFrameScripting {

        InteractiveObject interactiveObjectFinal = null;
        ScriptObject scriptObject = null;
        ScriptObject.Field firstField = null;
        String functionName;
        Object[] parameters = null;
        boolean run = false;
        boolean firstFrameRun_MustInitalize = true;
        float accumulatedTime = 0;
        float cycleInterval = 1;

        final void setInteractiveObjectVars(InteractiveObject interactiveObjectFinal) {

            this.interactiveObjectFinal = interactiveObjectFinal;
            scriptObject = interactiveObjectFinal.getScriptObject();
            firstField = scriptObject.getField(0);
            functionName = scriptObject.getFieldName(firstField);

            String javaScriptCode = scriptObject.getJavaScriptCode();
            int index = 0;
            while (index != -1) {
                index = javaScriptCode.indexOf("function", index);
                if (index != -1) {
                    String funcName = javaScriptCode.substring(index, javaScriptCode.indexOf('(', index));
                    funcName = funcName.substring(funcName.indexOf(' ') + 1, funcName.length());
                    javaScriptCode = javaScriptCode.substring(javaScriptCode.indexOf('(', index), javaScriptCode.length());
                    String paramterString = javaScriptCode.substring(1, javaScriptCode.indexOf(')'));
                    if (paramterString.indexOf(',') != -1) {
                        // we have two parameters to this function and thus the second parameter is the timeStamp
                        scriptObject.setTimeStampParameter(true);
                    }
                    javaScriptCode = javaScriptCode.substring(javaScriptCode.indexOf(')') + 1, javaScriptCode.length());
                }
            }

            cycleInterval = this.interactiveObjectFinal.getTimeSensor().getCycleInterval();
            if (cycleInterval <= 0) cycleInterval = 1;

            BuildInitJavaScript(interactiveObjectFinal);

            parameters = SetJavaScriptArguments(this.interactiveObjectFinal, 0, 0, 0, 0,false); // false is just a place holder
            parameters[0] = 0;
            if (scriptObject.getTimeStampParameter()) parameters[1] = 0;

            run = true;
        }  //  end setInteractiveObjectVars

        public boolean getRunState() {
            return run;
        }

        final void onDrawFrame(float frameTime) {
            if ( interactiveObjectFinal.getScriptObject().getInitializationDone() ) {
                if ( firstFrameRun_MustInitalize ) {
                    String paramString = "var params =[";
                    for (int i = 0; i < parameters.length; i++ ) {
                        paramString += (parameters[i] + ", ");
                    }
                    paramString = paramString.substring(0, (paramString.length()-2)) + "];";

                    SXRJavascriptV8File gvrJavascriptV8File = interactiveObjectFinal.getScriptObject().getSXRJavascriptV8File();

                    final SXRJavascriptV8File gvrJavascriptV8FileFinal = gvrJavascriptV8File;
                    final Object[] parametersFinal = parameters;
                    final String paramStringFinal = paramString;
                    gvrContext.runOnGlThread(new Runnable() {
                        @Override
                        public void run() {
                            RunInitializeScriptThread( gvrJavascriptV8FileFinal, interactiveObjectFinal, parametersFinal, paramStringFinal);
                            firstFrameRun_MustInitalize = false;
                        }
                    });
                }
                // once we run through the initialization of this script, then we can Run the script
                parameters = SetJavaScriptArguments(this.interactiveObjectFinal, 0, 0, 0, 0,false); // false is just a place holder
                accumulatedTime += frameTime;
                parameters[0] = accumulatedTime % cycleInterval;
                if (scriptObject.getTimeStampParameter()) parameters[1] = accumulatedTime;
                // Run this SCRIPT's actal JavaScript function
                RunScript(interactiveObjectFinal, functionName, parameters);
            }
        }  //  end onDrawFrame
    }  //  end private class PerFrameScripting



    private final class DrawFrame implements SXRDrawFrameListener {
        @Override
        public void onDrawFrame(float frameTime) {
            perFrameScripting.onDrawFrame(frameTime);
        }
    }

    /* Allows string matching fields, handling mis-matched case, if value has 'set' so
    'set_translation' and 'translation' or 'translation_changed' and 'translation' match.
    Also gets rid of leading and trailing spaces.  All possible in JavaScript and X3D Routes.
     */
    private boolean StringFieldMatch (String original, String matching) {
        boolean equal = false;
        original = original.toLowerCase().trim();
        matching = matching.toLowerCase();
        if (original.endsWith(matching)) {
            equal = true;
        }
        else if (original.startsWith(matching)) {
            equal = true;
        }
        return equal;
    }

    // funtion called each event and sets the arguments (parameters)
    // from INPUT_ONLY and INPUT_OUTPUT to the function that 'compiles' and run JavaScript
    private Object[] SetJavaScriptArguments(InteractiveObject interactiveObj,
                                            Object argument0, Object argument1, Object argument2, Object argument3, boolean stateChanged) {
        ArrayList<Object> scriptParameters = new ArrayList<Object>();

        ScriptObject scriptObject = interactiveObj.getScriptObject();

        // Get the parameters/values passed to the Script/JavaScript
        for (ScriptObject.Field field : scriptObject.getFieldsArrayList()) {
            if ((scriptObject.getFieldAccessType(field) == ScriptObject.AccessType.INPUT_OUTPUT) ||
                    (scriptObject.getFieldAccessType(field) == ScriptObject.AccessType.INPUT_ONLY)) {
                String fieldType = scriptObject.getFieldType(field);
                DefinedItem definedItem = scriptObject.getFromDefinedItem(field);
                EventUtility eventUtility = scriptObject.getFromEventUtility(field);
                TimeSensor timeSensor = scriptObject.getFromTimeSensor(field);


                if (fieldType.equalsIgnoreCase("SFBool")) {
                    if (definedItem != null) {
                        if (definedItem.getSXRNode() != null) {
                            SXRComponent gvrComponent = definedItem.getSXRNode().getComponent(
                                    SXRLight.getComponentType());
                            if (gvrComponent != null) {
                                scriptParameters.add(gvrComponent.isEnabled());
                            }
                        }
                    }
                    else if (eventUtility != null) {
                        scriptParameters.add( eventUtility.getToggle() );
                    }
                    else if (interactiveObj.getSensorFromField() != null) {
                        if (interactiveObj.getSensorFromField().equals(Sensor.IS_OVER)) {
                            scriptParameters.add(argument0);
                        }
                        else if (interactiveObj.getSensorFromField().equals(Sensor.IS_ACTIVE)) {
                            scriptParameters.add(!stateChanged);
                        }
                    }
                    else if ( interactiveObj.getEventUtility() != null) {
                        scriptParameters.add( interactiveObj.getEventUtility().getToggle() );
                    }
                }  // end if SFBool
                else if ((fieldType.equalsIgnoreCase("SFVec2f")) && (definedItem == null)) {
                    // data from a Plane Sensor
                    if (interactiveObj.getSensorFromField() != null) {
                        if (interactiveObj.getSensor().getSensorType() == Sensor.Type.PLANE) {
                            scriptParameters.add( argument0 );
                            scriptParameters.add( argument1 );
                        }
                    }
                }
                else if ((fieldType.equalsIgnoreCase("SFFloat")) && (definedItem == null)) {
                    if (timeSensor != null) {
                        scriptParameters.add( timeSensor.getCycleInterval() );
                    }
                    else scriptParameters.add(argument0); // the time passed in from an SFTime node
                }
                else if ((fieldType.equalsIgnoreCase("SFRotation")) && (definedItem == null)) {
                    // data from a Cylinder or Sphere Sensor
                    if (interactiveObj.getSensorFromField() != null) {
                        if (interactiveObj.getSensor().getSensorType() == Sensor.Type.CYLINDER) {
                            scriptParameters.add(argument0);
                            scriptParameters.add(argument1);
                            scriptParameters.add(argument2);
                            scriptParameters.add(argument3);
                        }
                        else if (interactiveObj.getSensor().getSensorType() == Sensor.Type.SPHERE) {
                            scriptParameters.add(argument0);
                            scriptParameters.add(argument1);
                            scriptParameters.add(argument2);
                            scriptParameters.add(argument3);
                        }
                    }
                }
                else if (scriptObject.getFromDefinedItem(field) != null) {
                    if (fieldType.equalsIgnoreCase("SFColor")) {
                        float[] color = {0, 0, 0};
                        if (definedItem.getSXRMaterial() != null) {
                            if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "diffuseColor") ) {
                                float[] diffuseColor = definedItem.getSXRMaterial().getVec4("diffuse_color");
                                for (int i = 0; i < 3; i++) {
                                    color[i] = diffuseColor[i]; // only get the first 3 values, not the alpha value
                                }
                            } else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "emissiveColor") ) {
                                float[] emissiveColor = definedItem.getSXRMaterial().getVec4("emissive_color");
                                for (int i = 0; i < 3; i++) {
                                    color[i] = emissiveColor[i]; // only get the first 3 values, not the alpha value
                                }
                            } else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "specularColor") ) {
                                float[] specularColor = definedItem.getSXRMaterial().getVec4("specular_color");
                                for (int i = 0; i < 3; i++) {
                                    color[i] = specularColor[i]; // only get the first 3 values, not the alpha value
                                }
                            }
                        } else if (definedItem.getSXRNode() != null) {
                            // likely a light object so get its properties
                            SXRComponent gvrComponent = definedItem.getSXRNode().getComponent(
                                    SXRLight.getComponentType());
                            if (gvrComponent != null) {
                                float[] lightColor = {0, 0, 0, 0};
                                if (gvrComponent instanceof SXRSpotLight) {
                                    SXRSpotLight gvrSpotLightBase = (SXRSpotLight) gvrComponent;
                                    lightColor = gvrSpotLightBase.getDiffuseIntensity();
                                } else if (gvrComponent instanceof SXRPointLight) {
                                    SXRPointLight gvrPointLightBase = (SXRPointLight) gvrComponent;
                                    lightColor = gvrPointLightBase.getDiffuseIntensity();
                                } else if (gvrComponent instanceof SXRDirectLight) {
                                    SXRDirectLight gvrDirectLightBase = (SXRDirectLight) gvrComponent;
                                    lightColor = gvrDirectLightBase.getDiffuseIntensity();
                                }
                                for (int i = 0; i < 3; i++) {
                                    color[i] = lightColor[i]; // only get the first 3 values, not the alpha value
                                }
                            }
                        }
                        // append the parameters of the SFColor passed to the SCRIPT's javascript code
                        for (int i = 0; i < color.length; i++) {
                            scriptParameters.add(color[i]);
                        }
                    }  // end if SFColor

                    else if (fieldType.equalsIgnoreCase("SFRotation")) {
                        if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "rotation") ) {
                            if (definedItem.getSXRNode() != null) {
                                // Likely the rotation in a Transform / SXRTransform
                                // GearVRf saves rotations as quaternions, but X3D scripting uses AxisAngle
                                // So, these values were saved as AxisAngle in the DefinedItem object
                                scriptParameters.add(definedItem.getAxisAngle().x);
                                scriptParameters.add(definedItem.getAxisAngle().y);
                                scriptParameters.add(definedItem.getAxisAngle().z);
                                scriptParameters.add(definedItem.getAxisAngle().angle);
                            }
                        }  // rotation parameter
                        else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "orientation") ) {
                            if ( definedItem.getViewpoint() != null ) {
                                // have a Viewpoint which for the time-being means get the current direction of the camera
                                float[] lookAt = gvrContext.getMainScene().getMainCameraRig().getLookAt();
                                Vector3f cameraDir = new Vector3f(lookAt[0], lookAt[1], lookAt[2]);
                                Quaternionf q = ConvertDirectionalVectorToQuaternion(cameraDir);
                                AxisAngle4f cameraAxisAngle = new AxisAngle4f();
                                q.get(cameraAxisAngle);
                                scriptParameters.add( cameraAxisAngle.x );
                                scriptParameters.add( cameraAxisAngle.y );
                                scriptParameters.add( cameraAxisAngle.z );
                                scriptParameters.add( cameraAxisAngle.angle );
                            }
                        }  // orientation parameter
                    }  // end if SFRotation
                    else if (fieldType.equalsIgnoreCase("SFVec3f")) {
                        if (definedItem.getSXRNode() != null) {
                            SXRComponent gvrComponent = definedItem.getSXRNode().getComponent(
                                    SXRLight.getComponentType());
                            if (gvrComponent != null) {
                                // it's a light
                                float[] parameter = {0, 0, 0};
                                if (gvrComponent instanceof SXRSpotLight) {
                                    SXRSpotLight gvrSpotLightBase = (SXRSpotLight) gvrComponent;
                                    if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "attenuation") ) {
                                        parameter[0] = gvrSpotLightBase.getAttenuationConstant();
                                        parameter[1] = gvrSpotLightBase.getAttenuationLinear();
                                        parameter[2] = gvrSpotLightBase.getAttenuationQuadratic();
                                    } else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "location") ) {
                                        parameter[0] = definedItem.getSXRNode().getTransform().getPositionX();
                                        parameter[1] = definedItem.getSXRNode().getTransform().getPositionY();
                                        parameter[2] = definedItem.getSXRNode().getTransform().getPositionZ();
                                    } else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "direction") ) {
                                        parameter[0] = definedItem.getDirection().x;
                                        parameter[1] = definedItem.getDirection().y;
                                        parameter[2] = definedItem.getDirection().z;
                                    }
                                } else if (gvrComponent instanceof SXRPointLight) {
                                    SXRPointLight gvrPointLightBase = (SXRPointLight) gvrComponent;
                                    if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "attenuation") ) {
                                        parameter[0] = gvrPointLightBase.getAttenuationConstant();
                                        parameter[1] = gvrPointLightBase.getAttenuationLinear();
                                        parameter[2] = gvrPointLightBase.getAttenuationQuadratic();
                                    } else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "location") ) {
                                        parameter[0] = definedItem.getSXRNode().getTransform().getPositionX();
                                        parameter[1] = definedItem.getSXRNode().getTransform().getPositionY();
                                        parameter[2] = definedItem.getSXRNode().getTransform().getPositionZ();
                                    }
                                } else if (gvrComponent instanceof SXRDirectLight) {
                                    SXRDirectLight gvrDirectLightBase = (SXRDirectLight) gvrComponent;
                                    if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "direction") ) {
                                        parameter[0] = definedItem.getDirection().x;
                                        parameter[1] = definedItem.getDirection().y;
                                        parameter[2] = definedItem.getDirection().z;
                                    }
                                }  // end SXRDirectLight
                                // append the parameters of the lights passed to the SCRIPT's javascript code
                                for (int i = 0; i < parameter.length; i++) {
                                    scriptParameters.add(parameter[i]);
                                }
                            }  //  end gvrComponent != null. it's a light
                            else {
                                if (definedItem.getSXRNode() != null) {
                                    if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "translation") ) {
                                        SXRNode gvrNodeTranslation = root
                                                .getNodeByName((definedItem.getSXRNode().getName() + x3dObject.TRANSFORM_TRANSLATION_));
                                        scriptParameters.add(gvrNodeTranslation.getTransform().getPositionX());
                                        scriptParameters.add(gvrNodeTranslation.getTransform().getPositionY());
                                        scriptParameters.add(gvrNodeTranslation.getTransform().getPositionZ());
                                    } else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "scale") ) {
                                        SXRNode gvrNodeScale = root
                                                .getNodeByName((definedItem.getSXRNode().getName() + x3dObject.TRANSFORM_SCALE_));
                                        scriptParameters.add(gvrNodeScale.getTransform().getScaleX());
                                        scriptParameters.add(gvrNodeScale.getTransform().getScaleY());
                                        scriptParameters.add(gvrNodeScale.getTransform().getScaleZ());
                                    }
                                }
                            }
                        }  // end SFVec3f SXRNode
                    }  // end if SFVec3f

                    else if (fieldType.equalsIgnoreCase("SFVec2f")) {
                        float[] parameter = {0, 0};
                        if (definedItem.getSXRMaterial() != null) {
                            if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "translation") ) {
                                parameter[0] = definedItem.getTextureTranslation().getX();
                                parameter[1] = -definedItem.getTextureTranslation().getY();
                            }
                            else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "scale") ) {
                                parameter[0] = definedItem.getTextureScale().getX();
                                parameter[1] = definedItem.getTextureScale().getY();
                            }
                            else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "center") ) {
                                parameter[0] = -definedItem.getTextureCenter().getX();
                                parameter[1] = definedItem.getTextureCenter().getY();
                            }
                            // append the parameters of the lights passed to the SCRIPT's javascript code
                            for (int i = 0; i < parameter.length; i++) {
                                scriptParameters.add(parameter[i]);
                            }
                        }  // end SFVec3f SXRMaterial
                    }  // end if SFVec2f

                    else if (fieldType.equalsIgnoreCase("SFFloat")) {
                        if (definedItem.getSXRMaterial() != null) {
                            if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "shininess") ) {
                                scriptParameters.add(
                                        definedItem.getSXRMaterial().getFloat("specular_exponent")
                                );
                            }
                            else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "transparency") ) {
                                scriptParameters.add(definedItem.getSXRMaterial().getOpacity());
                            }

                            else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "rotation") ) {
                                scriptParameters.add(definedItem.getTextureRotation().getValue());
                            }

                        } else if (definedItem.getSXRNode() != null) {
                            // checking if it's a light
                            SXRComponent gvrComponent = definedItem.getSXRNode().getComponent(
                                    SXRLight.getComponentType());
                            if (gvrComponent != null) {
                                float parameter = 0;
                                if (gvrComponent instanceof SXRSpotLight) {
                                    SXRSpotLight gvrSpotLightBase = (SXRSpotLight) gvrComponent;
                                    if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "beamWidth") ) {
                                        parameter = gvrSpotLightBase.getInnerConeAngle() * (float) Math.PI / 180;
                                    } else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "cutOffAngle") ) {
                                        parameter = gvrSpotLightBase.getOuterConeAngle() * (float) Math.PI / 180;
                                    }
                                } else if (gvrComponent instanceof SXRPointLight) {
                                    SXRPointLight gvrPointLightBase = (SXRPointLight) gvrComponent;

                                } else if (gvrComponent instanceof SXRDirectLight) {
                                    SXRDirectLight gvrDirectLightBase = (SXRDirectLight) gvrComponent;

                                }
                                scriptParameters.add(parameter);
                            }
                        }
                        else if (definedItem.getSXRVideoNode() != null) {
                            SXRVideoNodePlayer gvrVideoNodePlayer = definedItem.getSXRVideoNode().getMediaPlayer();
                            if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field),"speed") ) {
                                if ( gvrVideoNodePlayer == null) {
                                    // special case upon initialization of the movie texture, so the speed is init to 1
                                    scriptParameters.add( 1 );
                                }
                                else if ( gvrVideoNodePlayer.getPlayer() == null) {
                                    ; // could occur prior to movie engine is setup
                                }
                                else {
                                    ExoPlayer exoPlayer = (ExoPlayer) gvrVideoNodePlayer.getPlayer();
                                    PlaybackParameters currPlaybackParamters = exoPlayer.getPlaybackParameters();
                                    scriptParameters.add( currPlaybackParamters.speed );
                                }
                            } // end check for speed
                        }  // end if SFFloat SXRVideoNode
                    }  // end if SFFloat
                    else if (fieldType.equalsIgnoreCase("SFTime")) {
                        if (definedItem.getSXRVideoNode() != null) {
                            SXRVideoNodePlayer gvrVideoNodePlayer = definedItem.getSXRVideoNode().getMediaPlayer();
                            if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field),"duration") ) {
                                if ( gvrVideoNodePlayer == null) {
                                    // special case upon initialization of the movie texture, so the speed is init to 1
                                    scriptParameters.add( 1 );
                                }
                                else if ( gvrVideoNodePlayer.getPlayer() == null) {
                                    ; // could occur prior to movie engine is setup
                                }
                                else {
                                    ExoPlayer exoPlayer = (ExoPlayer) gvrVideoNodePlayer.getPlayer();
                                    scriptParameters.add( exoPlayer.getDuration() );
                                }
                            } // end check for duration
                            else if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field),"elapsedTime") ) {
                                if ( gvrVideoNodePlayer == null) {
                                    // special case upon initialization of the movie texture, so the elapsedTime is init to 0
                                    scriptParameters.add( 0 );
                                }
                                else if ( gvrVideoNodePlayer.getPlayer() == null) {
                                    ; // could occur prior to movie engine is setup
                                }
                                else {
                                    ExoPlayer exoPlayer = (ExoPlayer) gvrVideoNodePlayer.getPlayer();
                                    // getContentPosition is for possible advertisements, and returns the same
                                    // value as getCurrentPosition if no ads.
                                    scriptParameters.add( exoPlayer.getContentPosition() );
                                }
                            } // end check for elapsedTime
                        }  // end if SFTime SXRVideoNode
                    } // end if SFTime
                    else if (fieldType.equalsIgnoreCase("SFInt32")) {
                        int parameter = 0;
                        if (definedItem.getSXRNode() != null) {
                            SXRComponent gvrComponent = definedItem.getSXRNode().getComponent(SXRSwitch.getComponentType());
                            if (gvrComponent != null) {
                                if (gvrComponent instanceof SXRSwitch) {
                                    // We have a Switch node
                                    SXRSwitch gvrSwitch = (SXRSwitch) gvrComponent;
                                    parameter = gvrSwitch.getSwitchIndex();
                                }
                            }
                        }
                        scriptParameters.add(parameter);
                    }
                    else if (fieldType.equalsIgnoreCase("SFString")) {
                        if ( definedItem.getSXRTextViewNode() != null) {
                            if (scriptObject.getFromDefinedItemField(field).equalsIgnoreCase("style")) {
                                SXRTextViewNode.fontStyleTypes styleType =
                                        definedItem.getSXRTextViewNode().getStyleType();
                                String fontStyle = "";
                                if (styleType == SXRTextViewNode.fontStyleTypes.PLAIN)
                                    fontStyle = "PLAIN";
                                else if (styleType == SXRTextViewNode.fontStyleTypes.BOLD)
                                    fontStyle = "BOLD";
                                else if (styleType == SXRTextViewNode.fontStyleTypes.BOLDITALIC)
                                    fontStyle = "BOLDITALIC";
                                else if (styleType == SXRTextViewNode.fontStyleTypes.ITALIC)
                                    fontStyle = "ITALIC";
                                if (fontStyle != "") scriptParameters.add("\'" + fontStyle + "\'");
                                else Log.e(TAG, "style in ROUTE not recognized.");
                            }
                        }
                    }  //  end SFString
                    else if (fieldType.equalsIgnoreCase("MFString")) {
                        //TODO: will need to handle multiple strings particularly for Text node
                        SXRTexture gvrTexture = definedItem.getSXRTexture();
                        if (gvrTexture != null) {
                            // have a url containting a texture map
                            if ( StringFieldMatch( scriptObject.getFromDefinedItemField(field), "url") ) {
                                SXRImage gvrImage = gvrTexture.getImage();
                                if ( gvrImage != null ) {
                                    if ( gvrImage.getFileName() != null) {
                                        scriptParameters.add("\'" + gvrImage.getFileName() + "\'");
                                    }
                                }
                                else Log.e(TAG, "ImageTexture name not DEFined");
                            }
                            else Log.e(TAG, "ImageTexture SCRIPT node url field not found");
                        }
                        else Log.e(TAG, "Unable to set MFString in SCRIPT node");
                    } // end MFString
                }  //  end if definedItem != null
            }  //  end INPUT_ONLY, INPUT_OUTPUT (only ways to pass parameters to JS parser
        }  // for loop checking for parameters passed to the JavaScript parser

        // create the parameters array
        if (scriptObject.getTimeStampParameter())
            scriptParameters.add(1, 0); // insert the timeStamp parameter if it's used
        Object[] parameters = new Object[scriptParameters.size()];
        for (int i = 0; i < scriptParameters.size(); i++) {
            parameters[i] = scriptParameters.get(i);
        }
        return parameters;
    }  //  end  SetJavaScriptArguments

    private void RunInitializeScriptThread (SXRJavascriptV8File gvrJavascriptV8FileFinal, InteractiveObject interactiveObjectFinal, Object[] parametersFinal, String paramStringFinal) {
        boolean complete = gvrJavascriptV8FileFinal.invokeFunction(GEARVR_INIT_JAVASCRIPT_FUNCTION_NAME, parametersFinal, paramStringFinal);
        if (complete) {
            // No errors in the GearVR_Init function, so continue to cal the init function if there are any.
            // if the objects required for this function were constructed, then
            //   check if this <SCRIPT> has an initialize() method that is run just once.
            if (gvrJavascriptV8FileFinal.getScriptText().contains(INITIALIZE_FUNCTION)) {
                // <SCRIPT> node initialize() functions set inputOnly values
                // so we don't continue to run the main script method.
                // http://www.web3d.org/documents/specifications/19775-1/V3.2/Part01/components/scripting.html#Script
                complete = gvrJavascriptV8FileFinal.invokeFunction(INITIALIZE_FUNCTION, parametersFinal, "");
                if ( !complete ) {
                    Log.e(TAG, "Error with initialize() function in SCRIPT '" +
                            interactiveObjectFinal.getScriptObject().getName() + "'");
                }
            }
        } else {
            Log.e(TAG, "Error parsing / running initializing V8 JavaScript function in SCRIPT '" +
                    interactiveObjectFinal.getScriptObject().getName() + "'");
        }
    }  //  end RunInitializeScriptThread

    /**
     * method runs the Script's initialize() method
     */
    public void InitializeScript() {

        for (InteractiveObject interactiveObject : interactiveObjects) {
            if (interactiveObject.getScriptObject() != null) {

                BuildInitJavaScript(interactiveObject);
                Object[] parameters = SetJavaScriptArguments(interactiveObject, 0, 0, 0, 0,false);
                parameters[0] = 0;
                if (interactiveObject.getScriptObject().getTimeStampParameter()) parameters[1] = 0;

                if ( V8JavaScriptEngine ) {
                    // Using V8 JavaScript compiler and run-time engine
                    SXRJavascriptV8File gvrJavascriptV8File = interactiveObject.getScriptObject().getSXRJavascriptV8File();
                    // Append the X3D data type constructors to the end of the JavaScript file
                    if ( !interactiveObject.getScriptObject().getInitializationDone()) {
                        gvrJavascriptV8File.setScriptText(gvrJavascriptV8File.getScriptText() +
                                interactiveObject.getScriptObject().getGearVRinitJavaScript());

                        if ( !interactiveObject.getScriptObject().getScriptCalledPerFrame() ) {
                            // only initialize if this is not called per frame
                            // initialization for scripts called per frame must be called
                            // when we begin the first frame due to V8 engine start-up
                            String paramString = "var params =[";
                            for (int i = 0; i < parameters.length; i++ ) {
                                paramString += (parameters[i] + ", ");
                            }
                            paramString = paramString.substring(0, (paramString.length()-2)) + "];";

                            final SXRJavascriptV8File gvrJavascriptV8FileFinal = gvrJavascriptV8File;
                            final InteractiveObject interactiveObjectFinal = interactiveObject;
                            final Object[] parametersFinal = parameters;
                            final String paramStringFinal = paramString;
                            gvrContext.runOnGlThread(new Runnable() {
                                @Override
                                public void run() {
                                    RunInitializeScriptThread( gvrJavascriptV8FileFinal, interactiveObjectFinal, parametersFinal, paramStringFinal);
                                }
                            });
                        }  // ! per frame script
                    }
                    interactiveObject.getScriptObject().setInitializationDone(true);

                } //  end of running initialize functions in V8 JavaScript engine
                else {
                    // Using older Mozilla Rhino engine
                    SXRJavascriptScriptFile gvrJavascriptFile = interactiveObject.getScriptObject().getSXRJavascriptScriptFile();

                    // Append the X3D data type constructors to the end of the JavaScript file
                    gvrJavascriptFile.setScriptText(gvrJavascriptFile.getScriptText() +
                            interactiveObject.getScriptObject().getGearVRinitJavaScript());

                    // Run the newly created method 'GEARVR_INIT_JAVASCRIPT_FUNCTION' that
                    //    constructs the objects required for this JavaScript program.
                    boolean complete = gvrJavascriptFile.invokeFunction(GEARVR_INIT_JAVASCRIPT_FUNCTION_NAME, parameters);
                    if (complete) {
                        // if the objects required for this function were constructed, then
                        //   check if this <SCRIPT> has an initialize() method that is run just once.
                        if (gvrJavascriptFile.getScriptText().contains(INITIALIZE_FUNCTION)) {
                            RunScript(interactiveObject, INITIALIZE_FUNCTION, parameters);
                        }
                    } else {
                        Log.e(TAG, "Error parsing / running initializing Rhino JavaScript function in SCRIPT '" +
                                interactiveObject.getScriptObject().getName() + "'");
                    }
                }  //  end using older Mozilla Rhino engine
            }  // end check for interactiveObject having a Script Object
        }  // end loop thru all interactiveObjects for a ScriptObject
    }  //  end InitializeScript method


    // Builds string that becomes the GearVRinitJavaScript() function
    // which will initialize / Construct the X3D data types used in
    // this SCRIPT node.
    private void BuildInitJavaScript(InteractiveObject interactiveObject) {
        String gearVRinitJavaScript = "function " + GEARVR_INIT_JAVASCRIPT_FUNCTION_NAME + "()\n{\n";

        //The first two arguments are for the event - could be time or an isOver/isActive boolean -
        // and the second argument is for the timeStamp which is the accumulated time for starting
        // the per Frame calls to JavaScript, or 0 for isOver/isActive touch events
        ScriptObject scriptObject = interactiveObject.getScriptObject();
        int argumentNum = 1;
        if (scriptObject.getTimeStampParameter()) argumentNum = 2;
        if ( interactiveObject.getSensor() != null ) {
            if ( interactiveObject.getSensor().getSensorType() == Sensor.Type.PLANE) {
                argumentNum = 2;
            }
            else if ( interactiveObject.getSensor().getSensorType() == Sensor.Type.CYLINDER) {
                argumentNum = 4;
            }
            else if ( interactiveObject.getSensor().getSensorType() == Sensor.Type.SPHERE) {
                argumentNum = 4;
            }
        }

        // Get the parameters on X3D data types that are included with this JavaScript
        if ( V8JavaScriptEngine ) {
            for (ScriptObject.Field field : scriptObject.getFieldsArrayList()) {
                String fieldType = scriptObject.getFieldType(field);
                if (scriptObject.getFromDefinedItem(field) != null) {
                    if ((fieldType.equalsIgnoreCase("SFColor")) || (fieldType.equalsIgnoreCase("SFVec3f"))) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "], params[" + (argumentNum + 1) + "], params[" + (argumentNum + 2) + "]);\n";
                        argumentNum += 3;
                    }  // end if SFColor of SFVec3f, a 3-value parameter
                    else if (fieldType.equalsIgnoreCase("SFRotation")) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "], params[" + (argumentNum + 1) + "], params[" + (argumentNum + 2)
                                + "], params[" + (argumentNum + 3) + "]);\n";
                        argumentNum += 4;
                    }  // end if SFRotation, a 4-value parameter
                    else if (fieldType.equalsIgnoreCase("SFVec2f")) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "], params[" + (argumentNum + 1) + "]);\n";
                        argumentNum += 2;
                    }  // end if SFVec2f, a 2-value parameter

                    else if ((fieldType.equalsIgnoreCase("SFFloat")) || (fieldType.equalsIgnoreCase("SFBool"))
                            || (fieldType.equalsIgnoreCase("SFInt32")) || (fieldType.equalsIgnoreCase("SFTime")) ) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "]);\n";
                        argumentNum += 1;
                    }  // end if SFFloat, SFBool, SFInt32 or SFTime - a single parameter
                    else if (fieldType.equalsIgnoreCase("SFString") ) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "]);\n";
                        argumentNum += 1;
                    }  // end if SFString
                    else if (fieldType.equalsIgnoreCase("MFString") ) {
                        // TODO: need MFString to support more than one argument due to being used for Text Strings
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "]);\n";
                        argumentNum += 1;
                    }  // end if MFString
                    else {
                        Log.e(TAG, "Error unsupported field type '" + fieldType + "' in SCRIPT '" +
                                interactiveObject.getScriptObject().getName() + "'");
                    }
                }
                else if (scriptObject.getFromEventUtility(field) != null) {
                    if (fieldType.equalsIgnoreCase("SFBool")) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "]);\n";
                        argumentNum += 1;
                    }  // end if SFBool
                }  //  end scriptObject.getFromEventUtility(field) != null
                else if (scriptObject.getFromTimeSensor(field) != null) {
                    if (fieldType.equalsIgnoreCase("SFFloat")) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( params[" + argumentNum + "]);\n";
                        argumentNum += 1;
                    }  // end if SFFloat
                }  //  end scriptObject.getFromTimeSensor(field) != null
            }  // for loop checking for parameters passed to the JavaScript parser
        }  //  end if V8 engine
        else {
            // Mozilla Rhino engine
            for (ScriptObject.Field field : scriptObject.getFieldsArrayList()) {
                String fieldType = scriptObject.getFieldType(field);

                if (scriptObject.getFromDefinedItem(field) != null) {
                    if ((fieldType.equalsIgnoreCase("SFColor")) || (fieldType.equalsIgnoreCase("SFVec3f"))) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( arg" + argumentNum + ", arg" + (argumentNum + 1) + ", arg" + (argumentNum + 2) + ");\n";
                        argumentNum += 3;
                    }  // end if SFColor of SFVec3f
                    else if (fieldType.equalsIgnoreCase("SFRotation")) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( arg" + argumentNum + ", arg" + (argumentNum + 1) + ", arg" + (argumentNum + 2)
                                + ", arg" + (argumentNum + 3) + ");\n";
                        argumentNum += 4;
                    }  // end if SFRotation

                    else if ((fieldType.equalsIgnoreCase("SFFloat")) || (fieldType.equalsIgnoreCase("SFBool"))) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( arg" + argumentNum + ");\n";
                        argumentNum += 1;
                    }  // end if SFFloat or SFBool
                }  //  end scriptObject.getFromDefinedItem(field) != null
                else if (scriptObject.getFromEventUtility(field) != null) {
                    if (fieldType.equalsIgnoreCase("SFBool")) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( arg" + argumentNum + ");\n";
                        argumentNum += 1;
                    }  // end if SFBool
                }  //  end scriptObject.getFromEventUtility(field) != null
                else if (scriptObject.getFromTimeSensor(field) != null) {
                    if (fieldType.equalsIgnoreCase("SFFloat")) {
                        gearVRinitJavaScript += scriptObject.getFieldName(field) + " = new " + scriptObject.getFieldType(field) +
                                "( arg" + argumentNum + ");\n";
                        argumentNum += 1;
                    }  // end if SFFloat
                }  //  end scriptObject.getFromTimeSensor(field) != null

            }  // for loop checking for parameters passed to the JavaScript parser
        }  // end if Mozilla Rhino engine
        gearVRinitJavaScript += "}";
        scriptObject.setGearVRinitJavaScript(gearVRinitJavaScript);
    }  //  end  BuildInitJavaScript

    private void RunScriptThread (SXRJavascriptV8File gvrJavascriptV8FileFinal, InteractiveObject interactiveObjectFinal, String functionNameFinal, Object[] parametersFinal, String paramStringFinal) {
        boolean complete = gvrJavascriptV8FileFinal.invokeFunction(GEARVR_INIT_JAVASCRIPT_FUNCTION_NAME, parametersFinal, paramStringFinal);
        if ( complete ) {
            Bindings gvrFunctionBindingValues = gvrJavascriptV8FileFinal.getLocalBindings();
            //set the bindings from X3D Script field with inputOnly / inputOutput
            gvrJavascriptV8FileFinal.setInputValues(gvrFunctionBindingValues);
            // Now run this Script's actual function
            complete = gvrJavascriptV8FileFinal.invokeFunction(functionNameFinal, parametersFinal, paramStringFinal);

            if (complete) {
                // The JavaScript (JS) ran ok.  Now get the return
                // values (saved as X3D data types such as SFColor)
                //  stored in 'localBindings'.
                //  Then call SetResultsFromScript() to set the GearVR values
                Bindings returnedBindingValues = gvrJavascriptV8FileFinal.getLocalBindings();
                SetResultsFromScript(interactiveObjectFinal, returnedBindingValues);
            } // second complete check
            else {
                Log.e(TAG, "Error in SCRIPT node '" + interactiveObjectFinal.getScriptObject().getName() +
                        "' JavaScript function '" + functionNameFinal + "'");
            }
        } // first complete check
    }  //  end RunScriptThread


    // Run the JavaScript program, Output saved in localBindings
    private void RunScript(InteractiveObject interactiveObject, String functionName, Object[] parameters) {
        boolean complete = false;
        if ( V8JavaScriptEngine) {
            SXRJavascriptV8File gvrJavascriptV8File = interactiveObject.getScriptObject().getSXRJavascriptV8File();
            String paramString = "var params =[";
            for (int i = 0; i < parameters.length; i++ ) {
                paramString += (parameters[i] + ", ");
            }
            paramString = paramString.substring(0, (paramString.length()-2)) + "];";

            final SXRJavascriptV8File gvrJavascriptV8FileFinal = gvrJavascriptV8File;
            final InteractiveObject interactiveObjectFinal = interactiveObject;
            final String functionNameFinal = functionName;
            final Object[] parametersFinal = parameters;
            final String paramStringFinal = paramString;
            gvrContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    RunScriptThread (gvrJavascriptV8FileFinal, interactiveObjectFinal, functionNameFinal, parametersFinal, paramStringFinal);
                }
            });
        }  // end V8JavaScriptEngine
        else {
            // Mozilla Rhino engine
            SXRJavascriptScriptFile gvrJavascriptFile = interactiveObject.getScriptObject().getSXRJavascriptScriptFile();

            complete = gvrJavascriptFile.invokeFunction(functionName, parameters);
            if (complete) {
                // The JavaScript (JS) ran.  Now get the return
                // values (saved as X3D data types such as SFColor)
                //  stored in 'localBindings'.
                //  Then call SetResultsFromScript() to set the GearVR values
                Bindings localBindings = gvrJavascriptFile.getLocalBindings();
                SetResultsFromScript(interactiveObject, localBindings);
            } else {
                Log.e(TAG, "Error in SCRIPT node '" +  interactiveObject.getScriptObject().getName() +
                        "' running Rhino Engine JavaScript function '" + functionName + "'");
            }
        }
    }  // end function RunScript

    // Based on the inputs and javascript code, set the properties of the clsses in the SXR scene graph
    // these include the properties of lights, transforms and materials.
    // Possibly SXRMesh values too.
    private void SetResultsFromScript(InteractiveObject interactiveObjectFinal, Bindings localBindings) {
        // A SCRIPT can have mutliple defined objects, so we don't use getDefinedItem()
        // instead we go through the field values
        try {
            ScriptObject scriptObject = interactiveObjectFinal.getScriptObject();
            for (ScriptObject.Field fieldNode : scriptObject.getFieldsArrayList()) {
                if ((scriptObject.getFieldAccessType(fieldNode) == ScriptObject.AccessType.OUTPUT_ONLY) ||
                        (scriptObject.getFieldAccessType(fieldNode) == ScriptObject.AccessType.INPUT_OUTPUT)) {
                    String fieldType = scriptObject.getFieldType(fieldNode);
                    DefinedItem scriptObjectToDefinedItem = scriptObject.getToDefinedItem(fieldNode);
                    EventUtility scriptObjectToEventUtility = scriptObject.getToEventUtility(fieldNode);
                    Object returnedJavaScriptValue = localBindings.get(scriptObject.getFieldName(fieldNode));

                    // Script fields contain all the values that can be returned from a JavaScript function.
                    // However, not all JavaScript functions set returned-values, and thus left null.  For
                    // example the initialize() method may not set some Script field values, so don't
                    // process those and thus check if returnedJavaScriptValue != null
                    if ((returnedJavaScriptValue != null) && ( !(returnedJavaScriptValue instanceof V8Object) )) {
                        if (fieldType.equalsIgnoreCase("SFBool")) {
                            SFBool sfBool = (SFBool) returnedJavaScriptValue;
                            if ( scriptObjectToDefinedItem != null) {
                                if (scriptObjectToDefinedItem.getSXRNode() != null) {
                                    SXRComponent gvrComponent = scriptObjectToDefinedItem.getSXRNode().getComponent(
                                            SXRLight.getComponentType());
                                    if (gvrComponent != null) {
                                        gvrComponent.setEnable(sfBool.getValue());
                                    }
                                }  //  end if the Node has a light component attached
                            }  //  end scriptObjectToDefinedItem != null
                            else if ( scriptObjectToEventUtility != null) {
                                scriptObjectToEventUtility.setToggle(sfBool.getValue());
                            }
                            else if ( scriptObject.getToTimeSensor(fieldNode) != null) {
                                TimeSensor timeSensor = scriptObject.getToTimeSensor(fieldNode);
                                if ( StringFieldMatch( scriptObject.getFieldName(fieldNode), "loop") ) {
                                    timeSensor.setLoop( sfBool.getValue(), gvrContext );
                                }
                                if ( StringFieldMatch( scriptObject.getFieldName(fieldNode), "enabled") ) {
                                    timeSensor.setEnabled( sfBool.getValue(), gvrContext );
                                }
                            }
                            else {
                                Log.e(TAG, "Error: Not setting SFBool '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        }  //  end SFBool
                        else if (fieldType.equalsIgnoreCase("SFFloat")) {
                            SFFloat sfFloat = (SFFloat) returnedJavaScriptValue;
                            if (scriptObjectToDefinedItem.getSXRMaterial() != null) {
                                SXRMaterial gvrMaterial= scriptObjectToDefinedItem.getSXRMaterial();
                                // shininess and transparency are part of X3D Material node
                                if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "shininess") ) {
                                    gvrMaterial.setSpecularExponent(sfFloat.getValue());
                                } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "transparency") ) {
                                    gvrMaterial.setOpacity(sfFloat.getValue());
                                }
                                else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "rotation")  ) {
                                    // rotationn is part of TextureTransform node
                                    scriptObjectToDefinedItem.setTextureRotation( sfFloat.getValue() );

                                    Matrix3f textureTransform = SetTextureTransformMatrix(
                                            scriptObjectToDefinedItem.getTextureTranslation(),
                                            scriptObjectToDefinedItem.getTextureCenter(),
                                            scriptObjectToDefinedItem.getTextureScale(),
                                            scriptObjectToDefinedItem.getTextureRotation());

                                    float[] texMtx = new float[9];
                                    textureTransform.get(texMtx);
                                    gvrMaterial.setFloatArray("texture_matrix", texMtx);
                                }
                                else {
                                    Log.e(TAG, "Error: Not setting SFFloat to Material '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                                }
                            } else if (scriptObjectToDefinedItem.getSXRNode() != null) {
                                SXRComponent gvrComponent = scriptObjectToDefinedItem.getSXRNode().getComponent(
                                        SXRLight.getComponentType());
                                if (gvrComponent != null) {
                                    if (gvrComponent instanceof SXRSpotLight) {
                                        SXRSpotLight gvrSpotLightBase = (SXRSpotLight) gvrComponent;
                                        if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "beamWidth") ) {
                                            gvrSpotLightBase.setInnerConeAngle(sfFloat.getValue() * 180 / (float) Math.PI);
                                        } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "cutOffAngle") ) {
                                            gvrSpotLightBase.setOuterConeAngle(sfFloat.getValue() * 180 / (float) Math.PI);
                                        } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "intensity") ) {
                                            //TODO: we aren't changing intensity since this would be multiplied by color
                                        } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "radius") ) {
                                            //TODO: radius is not currently supported in GearVR for the spot light
                                        }
                                    } else if (gvrComponent instanceof SXRPointLight) {
                                        SXRPointLight gvrPointLightBase = (SXRPointLight) gvrComponent;
                                        if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "intensity") ) {
                                            //TODO: we aren't changing intensity since this would be multiplied by color
                                        } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "radius") ) {
                                            //TODO: radius is not currently supported in GearVR for the point light
                                        }
                                    } else if (gvrComponent instanceof SXRDirectLight) {
                                        SXRDirectLight gvrDirectLightBase = (SXRDirectLight) gvrComponent;
                                        if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "intensity") ) {
                                            //TODO: we aren't changing intensity since SXR multiplies this by color
                                        }
                                    }
                                }  //  end presumed to be a light
                            }  //  end SXRScriptObject ! null
                            else if ( scriptObjectToDefinedItem.getSXRVideoNode() != null) {
                                if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "speed") ||
                                        StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "pitch") ) {
                                    SXRVideoNodePlayer gvrVideoNodePlayer = scriptObjectToDefinedItem.getSXRVideoNode().getMediaPlayer();
                                    ExoPlayer exoPlayer = (ExoPlayer) gvrVideoNodePlayer.getPlayer();

                                    PlaybackParameters currPlaybackParamters = exoPlayer.getPlaybackParameters();
                                    PlaybackParameters playbackParamters = new PlaybackParameters(sfFloat.getValue(), sfFloat.getValue());
                                    exoPlayer.setPlaybackParameters(playbackParamters);
                                }
                            }
                            else {
                                Log.e(TAG, "Error: Not setting SFFloat '" + scriptObject.getToDefinedItemField(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        }  //  end SFFloat
                        else if (fieldType.equalsIgnoreCase("SFTime")) {
                            SFTime sfTime = (SFTime) returnedJavaScriptValue;
                            if ( scriptObject.getToTimeSensor(fieldNode) != null) {
                                TimeSensor timeSensor = scriptObject.getToTimeSensor(fieldNode);
                                if ( StringFieldMatch( scriptObject.getFieldName(fieldNode), "startTime") ) {
                                    timeSensor.startTime = (float)sfTime.getValue();
                                }
                                else if ( StringFieldMatch( scriptObject.getFieldName(fieldNode), "stopTime") ) {
                                    timeSensor.stopTime = (float)sfTime.getValue();
                                }
                                else if ( StringFieldMatch( scriptObject.getFieldName(fieldNode), "cycleInterval") ) {
                                    timeSensor.setCycleInterval( (float)sfTime.getValue() );
                                }
                                else Log.e(TAG, "Error: Not setting " + (float)sfTime.getValue() + " to SFTime '" +
                                            scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                            else if ( scriptObject.getToDefinedItemField( fieldNode ) != null) {
                                DefinedItem definedItem = scriptObject.getToDefinedItem(fieldNode);
                                if ( definedItem.getSXRVideoNode() != null ) {
                                    SXRVideoNodePlayer gvrVideoNodePlayer = scriptObjectToDefinedItem.getSXRVideoNode().getMediaPlayer();
                                    ExoPlayer exoPlayer = (ExoPlayer) gvrVideoNodePlayer.getPlayer();
                                    if (StringFieldMatch(scriptObject.getToDefinedItemField(fieldNode), "startTime")) {
                                        exoPlayer.seekTo( (long)sfTime.getValue() );
                                    }
                                    else Log.e(TAG, "Error: MovieTexture " + scriptObject.getToDefinedItemField(fieldNode) + " in " +
                                            scriptObject.getName() + " script not supported." );
                                }
                                else Log.e(TAG, "Error: Not setting " + (float)sfTime.getValue() + " to SFTime. " +
                                        "MovieTexture node may not be defined to connect from SCRIPT '" + scriptObject.getName() + "'." );

                            }
                            else Log.e(TAG, "Error: Not setting SFTime '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                        }  //  end SFTime
                        else if (fieldType.equalsIgnoreCase("SFColor")) {
                            SFColor sfColor = (SFColor) returnedJavaScriptValue;
                            if (scriptObjectToDefinedItem.getSXRMaterial() != null) {
                                //  SFColor change to a SXRMaterial
                                if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "diffuseColor") ) {
                                    scriptObjectToDefinedItem.getSXRMaterial().setVec4("diffuse_color", sfColor.getRed(), sfColor.getGreen(), sfColor.getBlue(), 1.0f);
                                } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "specularColor") ) {
                                    scriptObjectToDefinedItem.getSXRMaterial().setVec4("specular_color", sfColor.getRed(), sfColor.getGreen(), sfColor.getBlue(), 1.0f);
                                } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "emissiveColor") ) {
                                    scriptObjectToDefinedItem.getSXRMaterial().setVec4("emissive_color", sfColor.getRed(), sfColor.getGreen(), sfColor.getBlue(), 1.0f);
                                }
                            }  //  end SFColor change to a SXRMaterial
                            else if (scriptObjectToDefinedItem.getSXRNode() != null) {
                                // SXRNode
                                SXRNode gvrNode = scriptObjectToDefinedItem.getSXRNode();
                                SXRComponent gvrComponent = gvrNode.getComponent(SXRLight.getComponentType());
                                if (gvrComponent != null) {
                                    if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "color") ) {
                                        // SFColor change to a SXRNode (likely a Light Component)
                                        if (gvrComponent instanceof SXRSpotLight) {
                                            SXRSpotLight gvrSpotLightBase = (SXRSpotLight) gvrComponent;
                                            gvrSpotLightBase.setDiffuseIntensity(sfColor.getRed(), sfColor.getGreen(), sfColor.getBlue(), 1);
                                        } else if (gvrComponent instanceof SXRPointLight) {
                                            SXRPointLight gvrPointLightBase = (SXRPointLight) gvrComponent;
                                            gvrPointLightBase.setDiffuseIntensity(sfColor.getRed(), sfColor.getGreen(), sfColor.getBlue(), 1);
                                        } else if (gvrComponent instanceof SXRDirectLight) {
                                            SXRDirectLight gvrDirectLightBase = (SXRDirectLight) gvrComponent;
                                            gvrDirectLightBase.setDiffuseIntensity(sfColor.getRed(), sfColor.getGreen(), sfColor.getBlue(), 1);
                                        }
                                    }
                                }
                            }  //  SFColor change to a SXRNode (likely a Light)
                            else {
                                Log.e(TAG, "Error: Not setting SFColor '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        }  //  end SFColor (to a light or Material)
                        else if (fieldType.equalsIgnoreCase("SFVec3f")) {
                            SFVec3f sfVec3f = (SFVec3f) returnedJavaScriptValue;
                            if (scriptObjectToDefinedItem.getSXRNode() != null) {
                                //  SFVec3f change to a SXRNode
                                SXRNode gvrNode = scriptObjectToDefinedItem.getSXRNode();
                                if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "translation")  ||
                                        StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "location") ) {
                                    // location applies to point light and spot light
                                    SXRNode gvrNodeTranslation = root
                                            .getNodeByName((scriptObjectToDefinedItem.getSXRNode().getName() + x3dObject.TRANSFORM_TRANSLATION_));
                                    if (gvrNodeTranslation != null)
                                        gvrNodeTranslation.getTransform().setPosition(sfVec3f.x, sfVec3f.y, sfVec3f.z);
                                    else
                                        gvrNode.getTransform().setPosition(sfVec3f.x, sfVec3f.y, sfVec3f.z);
                                } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "scale") ) {
                                    SXRNode gvrNodeScale = root
                                            .getNodeByName((scriptObjectToDefinedItem.getSXRNode().getName() + x3dObject.TRANSFORM_SCALE_));
                                    if (gvrNodeScale != null)
                                        gvrNodeScale.getTransform().setScale(sfVec3f.x, sfVec3f.y, sfVec3f.z);
                                    else
                                        gvrNode.getTransform().setScale(sfVec3f.x, sfVec3f.y, sfVec3f.z);
                                } else {
                                    // could be parameters for a light
                                    SXRComponent gvrComponent = gvrNode.getComponent(
                                            SXRLight.getComponentType());
                                    if (gvrComponent != null) {
                                        if (gvrComponent instanceof SXRSpotLight) {
                                            SXRSpotLight gvrSpotLightBase = (SXRSpotLight) gvrComponent;
                                            if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "attenuation") ) {
                                                gvrSpotLightBase.setAttenuation(sfVec3f.getX(), sfVec3f.getY(), sfVec3f.getZ());
                                            } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "direction") ) {
                                                scriptObjectToDefinedItem.setDirection(sfVec3f.x, sfVec3f.y, sfVec3f.z);
                                                Vector3f v3 = new Vector3f(sfVec3f).normalize();
                                                Quaternionf q = ConvertDirectionalVectorToQuaternion(v3);
                                                gvrSpotLightBase.getTransform().setRotation(q.w, q.x, q.y, q.z);
                                            } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "location") ) {
                                                gvrSpotLightBase.getTransform().setPosition(sfVec3f.getX(), sfVec3f.getY(), sfVec3f.getZ());
                                            }
                                        } else if (gvrComponent instanceof SXRPointLight) {
                                            SXRPointLight gvrPointLightBase = (SXRPointLight) gvrComponent;
                                            if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "attenuation") ) {
                                                gvrPointLightBase.setAttenuation(sfVec3f.getX(), sfVec3f.getY(), sfVec3f.getZ());
                                            } else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "location") ) {
                                                gvrPointLightBase.getTransform().setPosition(sfVec3f.getX(), sfVec3f.getY(), sfVec3f.getZ());
                                            }
                                        } else if (gvrComponent instanceof SXRDirectLight) {
                                            SXRDirectLight gvrDirectLightBase = (SXRDirectLight) gvrComponent;
                                            if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "direction") ) {
                                                scriptObjectToDefinedItem.setDirection(sfVec3f.x, sfVec3f.y, sfVec3f.z);
                                                Vector3f v3 = new Vector3f(sfVec3f).normalize();
                                                Quaternionf q = ConvertDirectionalVectorToQuaternion(v3);
                                                gvrDirectLightBase.getTransform().setRotation(q.w, q.x, q.y, q.z);
                                            }
                                        }
                                    }
                                }  // end it could be a light
                            }  // end SXRNode with SFVec3f
                            else {
                                Log.e(TAG, "Error: Not setting SFVec3f '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        }  //  end SFVec3f
                        else if (fieldType.equalsIgnoreCase("SFVec2f")) {
                            SFVec2f sfVec2f = (SFVec2f) returnedJavaScriptValue;
                            if (scriptObjectToDefinedItem.getSXRMaterial() != null) {
                                //  SFVec2f change to a Texture Transform
                                SXRMaterial gvrMaterial= scriptObjectToDefinedItem.getSXRMaterial();
                                if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "translation")  ||
                                        StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "scale")  ||
                                        StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "center")  ) {

                                    if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "translation")  )
                                        scriptObjectToDefinedItem.setTextureTranslation(sfVec2f.getX(), -sfVec2f.getY());
                                    else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "scale")  )
                                        scriptObjectToDefinedItem.setTextureScale( sfVec2f.getX(), sfVec2f.getY() );
                                    else if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "center")  )
                                        scriptObjectToDefinedItem.setTextureCenter( -sfVec2f.getX(), sfVec2f.getY());


                                    Matrix3f textureTransform = SetTextureTransformMatrix(
                                            scriptObjectToDefinedItem.getTextureTranslation(),
                                            scriptObjectToDefinedItem.getTextureCenter(),
                                            scriptObjectToDefinedItem.getTextureScale(),
                                            scriptObjectToDefinedItem.getTextureRotation());
                                    float[] texMtx = new float[9];
                                    textureTransform.get(texMtx);
                                    gvrMaterial.setFloatArray("texture_matrix", texMtx);
                                }

                                else {
                                    Log.e(TAG, "Error: Not setting SFVec2f '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                                }
                            }
                            else {
                                Log.e(TAG, "Error: Not setting SFVec2f '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        } // end SFVec2f
                        else if (fieldType.equalsIgnoreCase("SFRotation")) {
                            SFRotation sfRotation = (SFRotation) returnedJavaScriptValue;
                            if (scriptObjectToDefinedItem.getSXRNode() != null) {
                                //  SFRotation change to a SXRNode
                                if ( StringFieldMatch( scriptObject.getToDefinedItemField(fieldNode), "rotation") ) {
                                    scriptObjectToDefinedItem.setAxisAngle(sfRotation.angle, sfRotation.x, sfRotation.y, sfRotation.z);

                                    SXRNode gvrNodeRotation = root
                                            .getNodeByName((scriptObjectToDefinedItem.getSXRNode().getName() + x3dObject.TRANSFORM_ROTATION_));
                                    float angleDegrees = (float) Math.toDegrees(sfRotation.angle);  // convert radians to degrees
                                    if (gvrNodeRotation != null) {
                                        gvrNodeRotation.getTransform().setRotationByAxis(angleDegrees, sfRotation.x, sfRotation.y, sfRotation.z);
                                    } else {
                                        scriptObjectToDefinedItem.getSXRNode().getTransform().setRotationByAxis(sfRotation.angle, sfRotation.x, sfRotation.y, sfRotation.z);
                                    }
                                }  //  definedItem != null
                            }  // end SXRNode with SFRotation
                            else {
                                Log.e(TAG, "Error: Not setting SFRotation '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        }  //  end SFRotation
                        else if (fieldType.equalsIgnoreCase("SFInt32")) {
                            try {
                                SFInt32 sfInt32 = new SFInt32(new Integer(returnedJavaScriptValue.toString()).intValue() );
                                if (scriptObjectToDefinedItem.getSXRNode() != null) {
                                    // Check if the field is 'whichChoice', meaning it's a Switch node
                                    if ( StringFieldMatch(scriptObject.getToDefinedItemField(fieldNode), "whichChoice") ) {
                                        SXRNode gvrSwitchNode = scriptObject.getToDefinedItem(fieldNode).getSXRNode();
                                        SXRComponent gvrComponent = gvrSwitchNode.getComponent(SXRSwitch.getComponentType());
                                        if (gvrComponent instanceof SXRSwitch) {
                                            // Set the value inside the Switch node
                                            SXRSwitch gvrSwitch = (SXRSwitch) gvrComponent;
                                            // Check if we are to switch to a value out of range (i.e. no mesh exists)
                                            // and thus set to not show any object.
                                            if ( (gvrSwitchNode.getChildrenCount() <= sfInt32.getValue()) ||
                                                    (sfInt32.getValue() < 0) ) {
                                                sfInt32.setValue( gvrSwitchNode.getChildrenCount() );
                                            }
                                            gvrSwitch.setSwitchIndex( sfInt32.getValue() );
                                        }
                                    }
                                }  // end SXRNode with SFInt32
                                else {
                                    Log.e(TAG, "Error: Not setting SFInt32 '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'.");
                                }
                            }
                            catch (Exception e) {
                                Log.e(TAG, "Error: Not setting SFInt32 '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT " + scriptObject.getName() + "'.");
                                Log.e(TAG, "Exception: " + e);
                            }
                        }  //  end SFInt32
                        else if (fieldType.equalsIgnoreCase("SFString")) {
                            SFString sfString = (SFString) returnedJavaScriptValue;
                            if (scriptObjectToDefinedItem.getSXRTextViewNode() != null) {
                                SXRTextViewNode gvrTextViewNode = scriptObjectToDefinedItem.getSXRTextViewNode();
                                if (scriptObject.getToDefinedItemField(fieldNode).endsWith("style")) {
                                    String value = sfString.getValue().toUpperCase();
                                    SXRTextViewNode.fontStyleTypes fontStyle = null;
                                    if ( value.equals("BOLD")) fontStyle = SXRTextViewNode.fontStyleTypes.BOLD;
                                    else if ( value.equals("ITALIC")) fontStyle = SXRTextViewNode.fontStyleTypes.ITALIC;
                                    else if ( value.equals("BOLDITALIC")) fontStyle = SXRTextViewNode.fontStyleTypes.BOLDITALIC;
                                    else if ( value.equals("PLAIN")) fontStyle = SXRTextViewNode.fontStyleTypes.PLAIN;
                                    if ( fontStyle != null ) {
                                        gvrTextViewNode.setTypeface(gvrContext,
                                                gvrTextViewNode.getFontFamily(), fontStyle);
                                    }
                                    else {
                                        Log.e(TAG, "Error: " + value + " + not recognized X3D FontStyle style in SCRIPT '" + scriptObject.getName() + "'." );
                                    }
                                }
                                else Log.e(TAG, "Error: Setting not SFString  value'" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                            else {
                                Log.e(TAG, "Error: Not setting SFString '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        }  //  end SFString
                        else if (fieldType.equalsIgnoreCase("MFString")) {
                            MFString mfString = (MFString) returnedJavaScriptValue;

                            if (scriptObjectToDefinedItem.getSXRTexture() != null) {
                                SXRTexture gvrTexture = scriptObjectToDefinedItem.getSXRTexture();
                                //  MFString change to a SXRTexture object
                                if (StringFieldMatch(scriptObject.getToDefinedItemField(fieldNode), "url")) {
                                    if (scriptObjectToDefinedItem.getSXRMaterial() != null) {
                                        // We have the SXRMaterial that contains a SXRTexture
                                        if (!gvrTexture.getImage().getFileName().equals(mfString.get1Value(0))) {
                                            // Only loadTexture if it is different than the current
                                            SXRAssetLoader.TextureRequest request = new SXRAssetLoader.TextureRequest(assetRequest,
                                                    gvrTexture, mfString.get1Value(0));
                                            assetRequest.loadTexture(request);
                                        }
                                    } // end having SXRMaterial containing SXRTexture
                                    else {
                                        Log.e(TAG, "Error: No SXRMaterial associated with MFString Texture url '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'.");
                                    }
                                }  //  definedItem != null
                                else {
                                    Log.e(TAG, "Error: No url associated with MFString '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'.");
                                }
                            }  // end SXRTexture != null

                            else if (scriptObjectToDefinedItem.getSXRVideoNode() != null) {
                                //  MFString change to a SXRVideoNode object
                                if (StringFieldMatch(scriptObject.getToDefinedItemField(fieldNode), "url")) {
                                    try {
                                        SXRVideoNodePlayer gvrVideoNodePlayer = scriptObjectToDefinedItem.getSXRVideoNode().getMediaPlayer();
                                        ExoPlayer exoPlayer = (ExoPlayer) gvrVideoNodePlayer.getPlayer();
                                        exoPlayer.stop();

                                        final DataSource.Factory dataSourceFactory = new DataSource.Factory() {
                                            @Override
                                            public DataSource createDataSource() {
                                                return new AssetDataSource(gvrContext.getContext());
                                            }
                                        };
                                        final MediaSource mediaSource = new ExtractorMediaSource(Uri.parse("asset:///" + mfString.get1Value(0)),
                                                dataSourceFactory,
                                                new DefaultExtractorsFactory(), null, null);
                                        exoPlayer.prepare(mediaSource);
                                        Log.e(TAG, "Load movie " + mfString.get1Value(0) + ".");
                                        gvrVideoNodePlayer.start();
                                    } catch (Exception e) {
                                        Log.e(TAG, "X3D MovieTexture Asset " + mfString.get1Value(0) + " not loaded." + e);
                                        e.printStackTrace();
                                    }

                                }  //  end definedItem != null, contains url
                                else {
                                    Log.e(TAG, "Error: No MovieTexure url associated with MFString '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                                }
                            }  // end SXRVideoNode != null

                            if (scriptObjectToDefinedItem.getSXRTextViewNode() != null) {
                                SXRTextViewNode gvrTextViewNode = scriptObjectToDefinedItem.getSXRTextViewNode();
                                if (scriptObject.getToDefinedItemField(fieldNode).equalsIgnoreCase("string")) {
                                    gvrTextViewNode.setText(mfString.get1Value(0));
                                }
                                else Log.e(TAG, "Error: Setting not MFString string '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "'." );
                            }
                        }  //  end MFString
                        else {
                            Log.e(TAG, "Error: " + fieldType + " in '" + scriptObject.getFieldName(fieldNode) + "' value from SCRIPT '" + scriptObject.getName() + "' not supported." );
                        }
                    }  //  end value != null
                    else {
                        Log.e(TAG, "Warning: " + fieldType + " '" + scriptObject.getFieldName(fieldNode) + "' from SCRIPT '" + scriptObject.getName() + "' may not be set.");
                    }
                }  //  end OUTPUT-ONLY or INPUT_OUTPUT
            }  // end for-loop list of fields for a single script
        } catch (Exception e) {
            Log.e(TAG, "Error setting values returned from JavaScript in SCRIPT '" +
                    interactiveObjectFinal.getScriptObject().getName() +
                    "'. Check JavaScript or ROUTE's.  Exception: " + e);

        }
    }  //  end  SetResultsFromScript


    /**
     * Converts a vector into a quaternion.
     * Used for the direction of spot and directional lights
     * Called upon initialization and updates to those vectors
     *
     * @param d
     */
    public Quaternionf ConvertDirectionalVectorToQuaternion(Vector3f d) {
        d.negate();
        Quaternionf q = new Quaternionf();
        // check for exception condition
        if ((d.x == 0) && (d.z == 0)) {
            // exception condition if direction is (0,y,0):
            // straight up, straight down or all zero's.
            if (d.y > 0) { // direction straight up
                AxisAngle4f angleAxis = new AxisAngle4f(-(float) Math.PI / 2, 1, 0,
                        0);
                q.set(angleAxis);
            } else if (d.y < 0) { // direction straight down
                AxisAngle4f angleAxis = new AxisAngle4f((float) Math.PI / 2, 1, 0, 0);
                q.set(angleAxis);
            } else { // All zero's. Just set to identity quaternion
                q.identity();
            }
        } else {
            d.normalize();
            Vector3f up = new Vector3f(0, 1, 0);
            Vector3f s = new Vector3f();
            d.cross(up, s);
            s.normalize();
            Vector3f u = new Vector3f();
            d.cross(s, u);
            u.normalize();
            Matrix4f matrix = new Matrix4f(s.x, s.y, s.z, 0, u.x, u.y, u.z, 0, d.x,
                    d.y, d.z, 0, 0, 0, 0, 1);
            q.setFromNormalized(matrix);
        }
        return q;
    } // end ConvertDirectionalVectorToQuaternion

    public Matrix3f SetTextureTransformMatrix(SFVec2f translation, SFVec2f center, SFVec2f scale, SFFloat rotation) {
        // Texture Transform Matrix equation:
        // TC' = -C * S * R * C * T * TC
        // matrix is:
        //    moo m10 m20
        //    m01 m11 m21
        //    m02 m12 m22

        Matrix3f translationMatrix = new Matrix3f().identity();
        translationMatrix.m02 = translation.x;
        translationMatrix.m12 = translation.y;

        Matrix3f centerMatrix = new Matrix3f().identity();
        centerMatrix.m02 = -center.x;
        centerMatrix.m12 = -center.y;
        Matrix3f negCenterMatrix = new Matrix3f().identity();
        negCenterMatrix.m02 = center.x;
        negCenterMatrix.m12 = center.y;

        Matrix3f scaleMatrix = new Matrix3f().scale(scale.x, scale.y, 1);

        Matrix3f rotationMatrix = new Matrix3f().identity().rotateZ(rotation.getValue());

        Matrix3f textureTransformMatrix = new Matrix3f().identity();
        return textureTransformMatrix.mul(negCenterMatrix).mul(scaleMatrix).mul(rotationMatrix).mul(centerMatrix).mul(translationMatrix);
    }


} //  end AnimationInteractivityManager class