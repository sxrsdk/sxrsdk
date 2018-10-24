package com.samsungxr.script;

import com.samsungxr.GVRResourceVolume;

/**
 * A script entry in a script description file in JSON format.
 *
 * The format of the file is:
 *
 * <pre>
 * [
 *   {
 *      target: "boy",
 *      script: "script/boy.js",
 *      language: "js"
 *   },
 *   {
 *      target: "@GVRMain",
 *      script: "http://mysite/script.lua",
 *      language: "lua",
 *      volumeType: "url"
 *   }
 * ]
 * </pre>
 */
public class GVRScriptBindingEntry {
    /**
     * The target to bind the script to. The string can be a name of a scene object,
     * or a special object beginning with the '@' character, such as the GVRMain
     * object represented by "@activity".
     */
    public String target;

    /**
     * The path of the script.
     */
    public String script;

    /**
     * The language of the script.
     */
    public String language;

    /**
     * The type of the volume. It corresponds to the enum {@link com.samsungxr.GVRResourceVolume.VolumeType}.
     * If this field is omitted, it defaults to the volume from which the {@link GVRScriptBundle}
     * is loaded.
     */
    public String volumeType;
}