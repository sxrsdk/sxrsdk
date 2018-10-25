package com.samsungxr.utlis.sceneserializer;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRImportSettings;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.SXRTexture;
import com.samsungxr.IAssetEvents;
import com.samsungxr.scene_objects.SXRCubeSceneObject;
import com.samsungxr.scene_objects.SXRSphereSceneObject;
import com.samsungxr.utility.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class SceneSerializer {
    private static final String TAG = SceneSerializer.class.getSimpleName();
    private static final String DEFAULT_SCENE_NAME = "scene.json";
    private transient static final String CUBEMAP_EXTENSION = ".zip";
    private transient static final float DEFAULT_ENVIRONMENT_SCALE = 200.0f;
    private Gson gson;
    private SceneData sceneData;
    private SceneLoaderListener sceneLoaderListener;

    public interface SceneLoaderListener {
        void onEnvironmentLoaded(SXRSceneObject envSceneObject);
        void onSceneObjectLoaded(SXRSceneObject sceneObject);
    }

    public SceneSerializer() {
        gson = new Gson();
    }

    public void importScene(SXRContext gvrContext, SXRScene gvrScene, SceneLoaderListener
            sceneLoaderListener) {
        File location = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + DEFAULT_SCENE_NAME);
        importScene(gvrContext,gvrScene,location,sceneLoaderListener);
    }

    public void importScene(SXRContext gvrContext, SXRScene gvrScene, File location,
                            SceneLoaderListener sceneLoaderListener) {
        this.sceneLoaderListener = sceneLoaderListener;
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = null;
        try {
            jsonElement = parser.parse(new FileReader(location));
            sceneData = gson.fromJson(jsonElement,SceneData.class);
        } catch (FileNotFoundException e) {
            Log.d(TAG,"Could not load scene from file");
        }
        loadEnvironment(gvrContext, gvrScene);
        loadSceneObjects(gvrContext, gvrScene);
    }

    public void exportScene() throws IOException {
        File location = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + DEFAULT_SCENE_NAME);
        exportScene(location);
    }

    public void exportScene(File location) throws IOException {
        if(sceneData == null) {
            return;
        }
        sceneData.prepareForExport();
        String scene = gson.toJson(sceneData);

        FileWriter fw = new FileWriter(location);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(scene);
        bw.close();
    }

    public void setEnvironmentData(String fullPath) {
        setEnvironmentData(fullPath, DEFAULT_ENVIRONMENT_SCALE);
    }

    public void setEnvironmentData(String src, float scale) {
        initializeSceneData();
        if(sceneData.getEnvironmentData() == null) {
            sceneData.setEnvironmentData(new EnvironmentData());
        }
        sceneData.getEnvironmentData().setSrc(src);
        if(scale <= 0) {
            scale = DEFAULT_ENVIRONMENT_SCALE;
        }
        sceneData.getEnvironmentData().setScale(scale);
    }

    private void initializeSceneData() {
        if(sceneData == null) {
            sceneData = new SceneData();
        }
    }

    public void addToSceneData(SXRSceneObject sceneObject, String source) {
        initializeSceneData();
        sceneData.addToSceneData(sceneObject, source);
    }

    public void removeFromSceneData(SXRSceneObject sceneObject) {
        initializeSceneData();
        sceneData.removeFromSceneData(sceneObject);
    }

    private void loadEnvironment(SXRContext gvrContext, SXRScene gvrScene) {
        if(sceneData == null || sceneData.getEnvironmentData() == null || sceneData
                .getEnvironmentData().getSrc() == null) {
            if(sceneLoaderListener != null) {
                sceneLoaderListener.onEnvironmentLoaded(null);
            }
            return;
        }
        EnvironmentData environmentData = sceneData.getEnvironmentData();
        SXRAndroidResource resource = null;
        try {
            resource = new SXRAndroidResource(environmentData.getSrc());
        } catch (IOException e) {
            Log.e(TAG, "Could not load texture file:%s", e.getMessage());
            if(sceneLoaderListener != null) {
                sceneLoaderListener.onEnvironmentLoaded(null);
            }
            return;
        }
        float targetScale = environmentData.getScale();
        if(targetScale == 0) {
            targetScale = DEFAULT_ENVIRONMENT_SCALE;
            environmentData.setScale(targetScale);
        }
        SXRSceneObject environmentSceneObject;
        if (environmentData.getSrc().endsWith(CUBEMAP_EXTENSION)) {
            SXRMaterial cubemapMaterial = new SXRMaterial(gvrContext,
                    SXRMaterial.SXRShaderType.Cubemap.ID);
            environmentSceneObject = new SXRCubeSceneObject(gvrContext, false,
                    cubemapMaterial);
            environmentSceneObject.getTransform().setScale(targetScale, targetScale, targetScale);
            SXRTexture futureCubeTexture = gvrContext.getAssetLoader().loadCubemapTexture
                    (resource);
            environmentSceneObject.getRenderData().getMaterial().setMainTexture
                    (futureCubeTexture);
            gvrScene.addSceneObject(environmentSceneObject);
        } else {
            SXRMaterial material = new SXRMaterial(gvrContext);
            environmentSceneObject = new SXRSphereSceneObject(gvrContext, false,
                    material);
            environmentSceneObject.getTransform().setScale(targetScale, targetScale, targetScale);
            SXRTexture futureSphereTexture = gvrContext.getAssetLoader().loadTexture(resource);
            environmentSceneObject.getRenderData().getMaterial().setMainTexture(futureSphereTexture);
            gvrScene.addSceneObject(environmentSceneObject);
        }
        if(sceneLoaderListener != null) {
            sceneLoaderListener.onEnvironmentLoaded(environmentSceneObject);
        }
    }

    private void loadSceneObjects(SXRContext gvrContext, SXRScene gvrScene) {
        if(sceneData == null) {
            return;
        }
        List<SceneObjectData> sceneObjectDataList = sceneData.getSceneObjectDataList();
        if(sceneObjectDataList == null) {
            return;
        }
        AssetObserver assetObserver = new AssetObserver(sceneObjectDataList, gvrContext, gvrScene);
        gvrContext.getEventReceiver().addListener(assetObserver);
        assetObserver.startLoading();
    }

    private class AssetObserver implements IAssetEvents {
        Collection<SceneObjectData> sceneObjectDatas;
        SXRContext context;
        SXRScene scene;
        Iterator<SceneObjectData> iterator;
        SceneObjectData currentSod;

        AssetObserver(Collection<SceneObjectData> sceneObjectDatas, SXRContext context, SXRScene
                scene) {
            this.sceneObjectDatas = sceneObjectDatas;
            this.scene = scene;
            this.context = context;
        }

        void startLoading() {
            iterator = sceneObjectDatas.iterator();
            loadNextAsset();
        }

        @Override
        public void onAssetLoaded(SXRContext context, SXRSceneObject model, String filePath,
                                  String errors) {
            if (currentSod != null && currentSod.getSrc().endsWith(filePath)) {
                model.getTransform().setModelMatrix(currentSod.getModelMatrix());
                model.setName(currentSod.getName());
                currentSod.setGvrSceneObject(model);
                scene.addSceneObject(model);
                if(sceneLoaderListener != null) {
                    sceneLoaderListener.onSceneObjectLoaded(model);
                }
                loadNextAsset();
            }
        }

        @Override
        public void onModelLoaded(SXRContext context, SXRSceneObject model, String filePath) {
            if (currentSod != null && currentSod.getSrc().endsWith(filePath)) {
                model.getTransform().setModelMatrix(currentSod.getModelMatrix());
                model.setName(currentSod.getName());
                currentSod.setGvrSceneObject(model);
                scene.addSceneObject(model);
                if(sceneLoaderListener != null) {
                    sceneLoaderListener.onSceneObjectLoaded(model);
                }
                loadNextAsset();
            }
        }

        @Override
        public void onTextureLoaded(SXRContext context, SXRTexture texture, String filePath) {
            if (currentSod != null && currentSod.getSrc().endsWith(filePath)) {
                Log.d(TAG, "Texture loaded:%s", filePath);
            }
        }

        @Override
        public void onModelError(SXRContext context, String error, String filePath) {
            if (currentSod != null && currentSod.getSrc().endsWith(filePath)) {
                Log.e(TAG, "Model Loading Error for %s", filePath);
                iterator.remove();
                loadNextAsset();
            }
        }

        @Override
        public void onTextureError(SXRContext context, String error, String filePath) {
            if (currentSod != null && currentSod.getSrc().endsWith(filePath)) {
                Log.e(TAG, "Texture Loading error for %s", filePath);
                loadNextAsset();
            }
        }

        private void loadNextAsset() {
            while (iterator.hasNext()) {
                currentSod = iterator.next();
                try {
                    context.getAssetLoader().loadModel(
                            "sd:" + currentSod.getSrc(), SXRImportSettings.getRecommendedSettings(),
                            true, null);
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "Could not load model:%s from sdcard:%s", currentSod.getSrc(),
                            e.getMessage());
                    iterator.remove();
                }
            }
            currentSod = null;
        }
    }


}
