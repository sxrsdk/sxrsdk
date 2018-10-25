package com.samsungxr.script;

import java.io.IOException;
import java.io.InputStream;

import com.samsungxr.SXRContext;
import com.samsungxr.script.IScriptManager;

/**
 * Represents a Javascript file. The script text can be loaded in one
 * of the following ways.
 * <ul>
 * <li>
 *   Loaded from a {@link com.samsungxr.SXRAndroidResource} using {@link SXRScriptManager#loadScript(com.samsungxr.SXRAndroidResource, String)}.
 * </li>
 * <li>
 *   Constructed locally and then set the text using {@link #setScriptText(String)}.
 * </li>
 * <li>
 *   Constructed locally and then load the text using {@link #load(InputStream)}.
 * </li>
 * </ul>
 *
 * Once a script text is set or loaded, you can invoke functions in the
 * script using {@link SXRScriptFile#invokeFunction(String, Object[])},
 * or attach it to a scriptable object using {@link SXRScriptManager#attachScriptFile(IScriptable, SXRScriptFile)}
 * to handle events delivered to it.
 */
public class SXRJavascriptScriptFile extends SXRScriptFile {
    /**
     * Loads a Javascript file from {@code inputStream}.
     *
     * @param gvrContext
     *     The SXR Context.
     * @param inputStream
     *     The input stream from which the script is loaded.
     * @throws IOException if the script cannot be read.
     */
    public SXRJavascriptScriptFile(SXRContext gvrContext, InputStream inputStream) throws IOException {
        super(gvrContext, IScriptManager.LANG_JAVASCRIPT);
        load(inputStream);
    }

    /**
     * Loads a Javascript file from a text string.
     *
     * @param gvrContext
     *     The SXR Context.
     * @param scriptText
     *     String containing a Javascript program.
     */
    public SXRJavascriptScriptFile(SXRContext gvrContext, String scriptText) {
        super(gvrContext, IScriptManager.LANG_JAVASCRIPT);
        setScriptText(scriptText);
    }
    
    protected String getInvokeStatement(String eventName, Object[] params) {
        StringBuilder sb = new StringBuilder();

        // function name
        sb.append(eventName);
        sb.append("(");

        // params
        for (int i = 0; i < params.length; ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(getDefaultParamName(i));
        }

        sb.append(");");
        return sb.toString();
    }
}
