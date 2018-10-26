package com.samsungxr.script;

import java.io.IOException;
import java.util.HashMap;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.script.SXRScriptBehaviorBase;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.IErrorEvents;
import com.samsungxr.IPickEvents;
import com.samsungxr.ISceneEvents;
import com.samsungxr.utility.FileNameUtils;
import com.samsungxr.SXRPicker;
import com.samsungxr.IPickEvents;
import com.samsungxr.ISensorEvents;
import com.samsungxr.SensorEvent;

/**
 * Attaches a Java or Lua script to a scene object.
 * <p>
 * These script callbacks are invoked if they are present:
 *      onEarlyInit(SXRContext) called after script is loaded
 *      onAfterInit()           called when the script becomes active
 *                              (this component is attached to a scene object and enabled)
 *      onStep()                called every frame if this component is enabled
 *                              and attached to a scene object
 *      onEnter(SXRNode, SXRPicker.SXRPickedObject)
 *                              called when picking ray enters an object
 *      onExit(SXRNode)
 *                              called when picking ray exits an object
 *      onInside(SXRNode, SXRPicker.SXRPickedObject)
 *                              called when picking ray is inside an object
 *      onPick(SXRPicker)       called when picking selection changes
 *      onNoPick(SXRPicker)     called when nothing is picked
 *
 */
public class SXRScriptBehavior extends SXRScriptBehaviorBase implements IPickEvents, ISensorEvents, ISceneEvents
{
    static private Object[] noargs = new Object[0];
    protected SXRScriptFile mScriptFile = null;
    protected boolean mIsAttached = false;
    protected int mPickEvents = 0xF;
    protected String mLanguage = SXRScriptManager.LANG_JAVASCRIPT;
    private String mLastError;
    private SXRScene mScene = null;
    
    private final int ON_ENTER = 1;
    private final int ON_EXIT = 2;
    private final int ON_PICK = 4;
    private final int ON_NOPICK = 8;
    private final int PICK_EVENTS = (ON_ENTER | ON_EXIT | ON_PICK | ON_NOPICK);
    
    /**
     * Constructor for a script behavior component.
     * @param gvrContext    The current SXRF context
     */
    public SXRScriptBehavior(SXRContext gvrContext)
    {
        super(gvrContext);
        mHasFrameCallback = false;
        mType = TYPE_SCRIPT_BEHAVIOR;
        mIsAttached = false;
        mLanguage = SXRScriptManager.LANG_JAVASCRIPT;
        gvrContext.getEventReceiver().addListener(this);
    }

    /**
     * Constructor for a script behavior component.
     * @param gvrContext    The current SXRF context
     * @param scriptFile    Path to the script file.
     * @throws IOException if script file cannot be read.
     * @throws SXRScriptException if script processing error occurs.
     */
    public SXRScriptBehavior(SXRContext gvrContext, String scriptFile) throws IOException, SXRScriptException
    {
        super(gvrContext);
        mHasFrameCallback = false;
        mType = TYPE_SCRIPT_BEHAVIOR;
        mIsAttached = false;
        mLanguage = SXRScriptManager.LANG_JAVASCRIPT;
        gvrContext.getEventReceiver().addListener(this);
        setFilePath(scriptFile);
    }

    public SXRScriptFile getScriptFile()
    {
        return mScriptFile;
    }
    
    /**
     * Sets the path to the script file to load and loads the script.
     * 
     * @param filePath path to script file
     * @throws IOException if the script cannot be read.
     * @throws SXRScriptException if a script processing error occurs.
     */
    public void setFilePath(String filePath) throws IOException, SXRScriptException
    {
        SXRResourceVolume.VolumeType volumeType = SXRResourceVolume.VolumeType.ANDROID_ASSETS;
        String fname = filePath.toLowerCase();
        
        mLanguage = FileNameUtils.getExtension(fname);        
        if (fname.startsWith("sd:"))
        {
            volumeType = SXRResourceVolume.VolumeType.ANDROID_SDCARD;
        }
        else if (fname.startsWith("http:") || fname.startsWith("https:"))
        {
            volumeType = SXRResourceVolume.VolumeType.NETWORK;            
        }
        SXRResourceVolume volume = new SXRResourceVolume(getSXRContext(), volumeType,
                FileNameUtils.getParentDirectory(filePath));
        SXRAndroidResource resource = volume.openResource(filePath);
         
        setScriptFile((SXRScriptFile)getSXRContext().getScriptManager().loadScript(resource, mLanguage));
    }
    
    /**
     * Loads the script from a text string.
     * @param scriptText text string containing script to execute.
     * @param language language ("js" or "lua")
     */
    public void setScriptText(String scriptText, String language)
    {
        SXRScriptFile newScript = new SXRJavascriptScriptFile(getSXRContext(), scriptText);
        mLanguage = SXRScriptManager.LANG_JAVASCRIPT;
        setScriptFile(newScript);
    }
    
    /**
     * Set the SXRScriptFile to execute.
     * @param scriptFile SXRScriptFile with script already loaded.
     * If the script contains a function called "onEarlyInit"
     * it is called if the script file is valid.
     */
    public void setScriptFile(SXRScriptFile scriptFile)
    {
        if (mScriptFile != scriptFile)
        {
            detachScript();
            mScriptFile = scriptFile;
        }
    }
    
    /**
     * Invokes the script associated with this component.
     * This function invokes the script even if the
     * component is not enabled and not attached to
     * a scene object.
     * @see SXRScriptFile#invoke() invoke
     */
    public void invoke()
    {
        if (mScriptFile != null)
        {
            mScriptFile.invoke();
        }
    }

    public void onInit(SXRContext context, SXRScene scene)
    {
        mScene = scene;
        startPicking();
    }

    public void onAfterInit() { }

    public void onStep() { }

    public void onAttach(SXRNode owner)
    {
        super.onAttach(owner);
        attachScript(owner);
    }

    public void onEnable()
    {
        super.onEnable();
        attachScript(null);
    }
    
    public void onDetach(SXRNode owner)
    {
        detachScript();
        super.onDetach(owner);
    }
    
    public void onDrawFrame(float frameTime)
    {
        invokeFunction("onStep", noargs);
    }

    public void onEnter(SXRNode sceneObj, SXRPicker.SXRPickedObject hit)
    {
         if ((sceneObj == getOwnerObject()) && !invokeFunction("onPickEnter", new Object[] { sceneObj, hit }))
         {
             mPickEvents &= ~ON_ENTER;
             if (mPickEvents == 0)
             {
                 stopPicking();
             }
         }
    }

    public void onExit(SXRNode sceneObj)
    {
        if ((sceneObj == getOwnerObject()) && !invokeFunction("onPickExit", new Object[] { sceneObj }))
        {
            mPickEvents &= ~ON_EXIT;
            if (mPickEvents == 0)
            {
                stopPicking();
            }
        }
    }

    public void onPick(SXRPicker picker)
    {
        if (!invokeFunction("onPick", new Object[] { picker }))
        {
            mPickEvents &= ~ON_PICK;
            if (mPickEvents == 0)
            {
                stopPicking();
            }
        }
    }

    public void onNoPick(SXRPicker picker)
    {
       if (!invokeFunction("onNoPick", new Object[] { picker }))
       {
           mPickEvents &= ~ON_NOPICK;
           if (mPickEvents == 0)
           {
               stopPicking();
           }
       }
    }

    public void onSensorEvent(SensorEvent event)
    {
        invokeFunction("onSensorEvent", new Object[] { event });
    }

    public void onInside(SXRNode sceneObj, SXRPicker.SXRPickedObject hit) { }

    protected void attachScript(SXRNode owner)
    {
        if (owner == null)
        {
            owner = getOwnerObject();
        }
        if (!mIsAttached && (mScriptFile != null) && isEnabled() && (owner != null) && owner.isEnabled())
        {
            getSXRContext().getScriptManager().attachScriptFile(owner, mScriptFile);
            mIsAttached = true;
            owner.getEventReceiver().addListener(this);
            if (invokeFunction("onStep", noargs))
            {
                mHasFrameCallback = true;
                startListening();
            }
            startPicking();
        }
    }

    protected void startPicking()
    {
        SXRScene scene = mScene;
        mPickEvents = PICK_EVENTS;
        if (mScene == null)
        {
            scene = getSXRContext().getMainScene();
        }
        scene.getEventReceiver().addListener(this);
    }

    protected void stopPicking()
    {
        SXRScene scene = mScene;
        if (mScene == null)
        {
            scene = getSXRContext().getMainScene();
        }
        scene.getEventReceiver().removeListener(this);
    }

    protected void detachScript()
    {
        SXRNode owner = getOwnerObject();
        
        if (mIsAttached && (owner != null))
        {
            getSXRContext().getScriptManager().detachScriptFile(owner);
            owner.getEventReceiver().removeListener(this);
            mIsAttached = false;
            mHasFrameCallback = true;
            stopPicking();
            stopListening();
        }
    }

    /**
     * Calls a function script associated with this component.
     * The function is called even if the component
     * is not enabled and not attached to a scene object.
     * @param funcName name of script function to call.
     * @param args function parameters as an array of objects.
     * @return true if function was called, false if no such function
     * @see com.samsungxr.script.SXRScriptFile#invokeFunction(String, Object[]) invokeFunction
     */
    public boolean invokeFunction(String funcName, Object[] args)
    {
        mLastError = null;
        if (mScriptFile != null)
        {
            if (mScriptFile.invokeFunction(funcName, args))
            {
                return true;
            }
        }
        mLastError = mScriptFile.getLastError();
        if ((mLastError != null) && !mLastError.contains("is not defined"))
        {
            getSXRContext().logError(mLastError, this);
        }
        return false;
    }
}
