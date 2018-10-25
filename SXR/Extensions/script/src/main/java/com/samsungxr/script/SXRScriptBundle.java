package com.samsungxr.script;

import java.io.IOException;
import java.util.Arrays;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.utility.TextFile;

import com.google.gson.Gson;

/**
 * Represents a script bundle loaded from a JSON file, and its storage
 * volume.
 */
public class SXRScriptBundle implements IScriptBundle {
    /**
     * The content of the bundle file is loaded from a JSON file.
     */
    public static class SXRScriptBundleFile {
        public String name;
        public SXRScriptBindingEntry[] binding;

        @Override
        public String toString() {
            return "SXRScriptBundleFile [name=" + name + ", binding="
                    + Arrays.toString(binding) + "]";
        }
    }

    protected SXRContext gvrContext;

    /**
     * The contents of the script bundle from a JSON file.
     */
    protected SXRScriptBundleFile file;

    /**
     * The volume of the script bundle. The script bundle
     * is loaded from this volume, and it also serves as the default
     * volume for scripts referenced in the bundle.
     */
    protected SXRResourceVolume volume;

    /**
     * Returns the contents of the bundle.
     * @return The {@link SXRScriptBundleFile} object.
     */
    public SXRScriptBundleFile getContent() {
        return file;
    }

    /**
     * Loads a {@link SXRScriptBundle} from a file.
     * @param gvrContext
     *         The SXRContext to use for loading.
     * @param filePath
     *         The file name of the script bundle in JSON format.
     * @param volume
     *         The {@link SXRResourceVolume} from which to load script bundle.
     * @return
     *         The {@link SXRScriptBundle} object with contents from the JSON file.
     *
     * @throws IOException if the bundle cannot be loaded.
     */
    public static SXRScriptBundle loadFromFile(SXRContext gvrContext, String filePath,
            SXRResourceVolume volume) throws IOException {
        SXRAndroidResource fileRes = volume.openResource(filePath);
        String fileText = TextFile.readTextFile(fileRes.getStream());
        fileRes.closeStream();

        SXRScriptBundle bundle = new SXRScriptBundle();
        Gson gson = new Gson();
        try {
            bundle.gvrContext = gvrContext;
            bundle.file = gson.fromJson(fileText, SXRScriptBundleFile.class);
            bundle.volume = volume;
            return bundle;
        } catch (Exception e) {
            throw new IOException("Cannot load the script bundle", e);
        }
    }

    @Override
    public String toString() {
        return "SXRScriptBundle [file=" + file + ", volume=" + volume + "]";
    }
}
