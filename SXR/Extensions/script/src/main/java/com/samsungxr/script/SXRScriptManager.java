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

package com.samsungxr.script;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.IScriptEvents;
import com.samsungxr.script.javascript.RhinoScriptEngineFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

/**
 * The script manager class handles script engines, script attachment/
 * detachment with scriptable objects, and other operation related to
 * scripting.
 */
public class SXRScriptManager implements IScriptManager {
    private static final String TAG = SXRScriptManager.class.getSimpleName();
    public static final String VAR_NAME_SXRF = "gvrf";

    protected SXRContext mGvrContext;
    protected Map<String, ScriptEngine> mEngines;

    protected Map<String, Object> mGlobalVariables;

    protected Map<IScriptable, IScriptFile> mScriptMap;

    // For script bundles. All special targets start with @.
    public static final String TARGET_PREFIX = "@";
    public static final String TARGET_SXRMAIN = "@SXRMain";
    public static final String TARGET_SXRAPPLICATION = "@SXRApplication";

    interface TargetResolver {
        IScriptable getTarget(SXRContext gvrContext, String name);
    }

    static Map<String, TargetResolver> sBuiltinTargetMap;

    // Provide getters for non-scene-object targets.
    static {
        sBuiltinTargetMap = new TreeMap<String, TargetResolver>();

        // Target resolver for "@SXRMain"
        sBuiltinTargetMap.put(TARGET_SXRMAIN, new TargetResolver() {
            @Override
            public IScriptable getTarget(SXRContext gvrContext,
                                         String name) {
                return gvrContext.getApplication().getMain();
            }
        });

        // Target resolver for "@SXRActivity"
        sBuiltinTargetMap.put(TARGET_SXRAPPLICATION, new TargetResolver() {
            @Override
            public IScriptable getTarget(SXRContext gvrContext,
                                         String name) {
                return gvrContext.getApplication();
            }
        });
    }

    /**
     * Constructor.
     *
     * @param gvrContext
     *     The SXR Context.
     */
    public SXRScriptManager(SXRContext gvrContext) {
        mGvrContext = gvrContext;
        mGlobalVariables = new TreeMap<String, Object>();
        mScriptMap = Collections.synchronizedMap(new HashMap<IScriptable, IScriptFile>());

        Thread.currentThread().setContextClassLoader(
                gvrContext.getActivity().getClassLoader());

        mGlobalVariables.put(VAR_NAME_SXRF, mGvrContext);
        initializeEngines();
    }

    private void initializeGlobalVariables() {
        mGlobalVariables.put(VAR_NAME_SXRF, mGvrContext);
    }

    private void initializeEngines() {
        mEngines = new TreeMap<String, ScriptEngine>();

        // Add languages
        mEngines.put(LANG_JAVASCRIPT, new RhinoScriptEngineFactory().getScriptEngine());

        // Add variables to engines
        refreshGlobalBindings();
    }

    private void refreshGlobalBindings() {
        for (ScriptEngine se : mEngines.values()) {
            addGlobalBindings(se);
        }
    }

    @Override
    public void addGlobalBindings(final ScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (bindings == null) {
            bindings = engine.createBindings();
            engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        }

        synchronized (mGlobalVariables) {
            for (Map.Entry<String, Object> ent : mGlobalVariables.entrySet()) {
                bindings.put(ent.getKey(), ent.getValue());
            }
            mBindingsClosers.add(new Runnable() {
                @Override
                public void run() {
                    final Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
                    if (null != bindings) {
                        bindings.clear();
                    }
                    engine.setBindings(null, ScriptContext.GLOBAL_SCOPE);
                }
            });
        }
    }

    /**
     * Returns an engine based on language.
     *
     * @param language The name of the language. Please use constants
     * defined in {@code ScriptManager}, such as LANG_JAVASCRIPT.
     *
     * @return The engine object. {@code null} if the specified engine is
     * not found.
     */
    @Override
    public ScriptEngine getEngine(String language) {
        return mEngines.get(language);
    }

    /**
     * Add a variable to the scripting context.
     *
     * @param varName The variable name.
     * @param value The variable value.
     */
    @Override
    public void addVariable(String varName, Object value) {
        synchronized (mGlobalVariables) {
            mGlobalVariables.put(varName, value);
        }
        refreshGlobalBindings();
    }

    /**
     * Attach a script file to a scriptable target.
     *
     * @param target The scriptable target.
     * @param scriptFile The script file object.
     */
    @Override
    public void attachScriptFile(IScriptable target, IScriptFile scriptFile) {
        mScriptMap.put(target, scriptFile);
        scriptFile.invokeFunction("onAttach", new Object[] { target });
    }

    /**
     * Detach any script file from a scriptable target.
     *
     * @param target The scriptable target.
     */
    @Override
    public void detachScriptFile(IScriptable target) {
        IScriptFile scriptFile = mScriptMap.remove(target);
        if (scriptFile != null) {
            scriptFile.invokeFunction("onDetach", new Object[] { target });
        }
    }

    /**
     * Gets a script file from a scriptable target.
     * @param target The scriptable target.
     * @return The script file or {@code null}.
     */
    public IScriptFile getScriptFile(IScriptable target) {
        return mScriptMap.get(target);
    }

    /**
     * Loads a script file using {@link SXRAndroidResource}.
     * @param resource The resource object.
     * @param language The language string.
     * @return A script file object or {@code null} if not found.
     * @throws IOException if script file cannot be read.
     * @throws SXRScriptException if script processing error occurs.
     */
    @Override
    public IScriptFile loadScript(SXRAndroidResource resource, String language) throws IOException, SXRScriptException {
        if (getEngine(language) == null) {
            mGvrContext.logError("Script language " + language + " unsupported", this);
            throw new SXRScriptException(String.format("The language is unknown: %s", language));
        }

        IScriptFile script = null;
        if (language.equals(LANG_JAVASCRIPT)) {
            script = new SXRJavascriptScriptFile(mGvrContext, resource.getStream());
        }

        resource.closeStream();
        return script;
    }

    /**
     * Load a script bundle file. It defines bindings between scripts and SXRf objects
     * (e.g., scene objects and the {@link SXRMain} object).
     *
     * @param filePath
     *        The path and filename of the script bundle.
     * @param volume
     *        The {@link SXRResourceVolume} from which to load the bundle file and scripts.
     * @return
     *         The loaded {@linkplain SXRScriptBundle script bundle}.
     *
     * @throws IOException if script bundle file cannot be read.
     */
    @Override
    public IScriptBundle loadScriptBundle(String filePath, SXRResourceVolume volume) throws IOException {
        SXRScriptBundle bundle = SXRScriptBundle.loadFromFile(mGvrContext, filePath, volume);
        return bundle;
    }

    /**
     * Binds a script bundle to a {@link SXRScene} object.
     *
     * @param scriptBundle
     *     The script bundle.
     * @param gvrMain
     *     The {@link SXRMain} to bind to.
     * @param bindToMainScene
     *     If {@code true}, also bind it to the main scene on the event {@link SXRMain#onAfterInit}.
     * @throws IOException if script bundle file cannot be read.
     * @throws SXRScriptException if script processing error occurs.
     */
    @Override
    public void bindScriptBundle(final IScriptBundle scriptBundle, final SXRMain gvrMain, boolean bindToMainScene)
            throws IOException, SXRScriptException {
        // Here, bind to all targets except SCENE_OBJECTS. Scene objects are bound when scene is set.
        bindHelper((SXRScriptBundle)scriptBundle, null, BIND_MASK_SXRSCRIPT | BIND_MASK_SXRACTIVITY);

        if (bindToMainScene) {
            final IScriptEvents bindToSceneListener = new SXREventListeners.ScriptEvents() {
                SXRScene mainScene = null;

                @Override
                public void onInit(SXRContext gvrContext) throws Throwable {
                    mainScene = gvrContext.getMainScene();
                }

                @Override
                public void onAfterInit() {
                    try {
                        bindScriptBundleToScene((SXRScriptBundle)scriptBundle, mainScene);
                    } catch (IOException e) {
                        mGvrContext.logError(e.getMessage(), this);
                    } catch (SXRScriptException e) {
                        mGvrContext.logError(e.getMessage(), this);
                    } finally {
                        // Remove the listener itself
                        gvrMain.getEventReceiver().removeListener(this);
                    }
                }
            };

            // Add listener to bind to main scene when event "onAfterInit" is received
            gvrMain.getEventReceiver().addListener(bindToSceneListener);
        }
    }

    /**
     * Binds a script bundle to a scene.
     * @param scriptBundle
     *         The {@code SXRScriptBundle} object containing script binding information.
     * @param scene
     *         The scene to bind to.
     * @throws IOException if script bundle file cannot be read.
     * @throws SXRScriptException if script processing error occurs.
     */
    public void bindScriptBundleToScene(SXRScriptBundle scriptBundle, SXRScene scene) throws IOException, SXRScriptException {
        for (SXRNode sceneObject : scene.getNodes()) {
            bindBundleToNode(scriptBundle, sceneObject);
        }
    }

    /**
     * Binds a script bundle to scene graph rooted at a scene object.
     * @param scriptBundle
     *         The {@code SXRScriptBundle} object containing script binding information.
     * @param rootNode
     *         The root of the scene object tree to which the scripts are bound.
     * @throws IOException if script bundle file cannot be read.
     * @throws SXRScriptException if a script processing error occurs.
     */
    public void bindBundleToNode(SXRScriptBundle scriptBundle, SXRNode rootNode)
            throws IOException, SXRScriptException
    {
        bindHelper(scriptBundle, rootNode, BIND_MASK_SCENE_OBJECTS);
    }

    protected int BIND_MASK_SCENE_OBJECTS = 0x0001;
    protected int BIND_MASK_SXRSCRIPT     = 0x0002;
    protected int BIND_MASK_SXRACTIVITY   = 0x0004;

    // Helper function to bind script bundler to various targets
    protected void bindHelper(SXRScriptBundle scriptBundle, SXRNode rootNode, int bindMask)
            throws IOException, SXRScriptException
    {
        for (SXRScriptBindingEntry entry : scriptBundle.file.binding) {
            SXRAndroidResource rc;
            if (entry.volumeType == null || entry.volumeType.isEmpty()) {
                rc = scriptBundle.volume.openResource(entry.script);
            } else {
                SXRResourceVolume.VolumeType volumeType = SXRResourceVolume.VolumeType.fromString(entry.volumeType);
                if (volumeType == null) {
                    throw new SXRScriptException(String.format("Volume type %s is not recognized, script=%s",
                            entry.volumeType, entry.script));
                }
                rc = new SXRResourceVolume(mGvrContext, volumeType).openResource(entry.script);
            }

            SXRScriptFile scriptFile = (SXRScriptFile)loadScript(rc, entry.language);

            String targetName = entry.target;
            if (targetName.startsWith(TARGET_PREFIX)) {
                TargetResolver resolver = sBuiltinTargetMap.get(targetName);
                IScriptable target = resolver.getTarget(mGvrContext, targetName);

                // Apply mask
                boolean toBind = false;
                if ((bindMask & BIND_MASK_SXRSCRIPT) != 0 && targetName.equalsIgnoreCase(TARGET_SXRMAIN)) {
                    toBind = true;
                }

                if ((bindMask & BIND_MASK_SXRACTIVITY) != 0 && targetName.equalsIgnoreCase(TARGET_SXRAPPLICATION)) {
                    toBind = true;
                }

                if (toBind) {
                    attachScriptFile(target, scriptFile);
                }
            } else {
                if ((bindMask & BIND_MASK_SCENE_OBJECTS) != 0) {
                    if (targetName.equals(rootNode.getName())) {
                        attachScriptFile(rootNode, scriptFile);
                    }

                    // Search in children
                    SXRNode[] sceneObjects = rootNode.getNodesByName(targetName);
                    if (sceneObjects != null) {
                        for (SXRNode sceneObject : sceneObjects) {
                            SXRScriptBehavior b = new SXRScriptBehavior(sceneObject.getSXRContext());
                            b.setScriptFile(scriptFile);
                            sceneObject.attachComponent(b);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void destroy() {
        synchronized (mGlobalVariables) {
            for (final Runnable r : mBindingsClosers) {
                r.run();
            }
            mBindingsClosers.clear();
        }
    }

    private final HashSet<Runnable> mBindingsClosers = new HashSet<>();
}