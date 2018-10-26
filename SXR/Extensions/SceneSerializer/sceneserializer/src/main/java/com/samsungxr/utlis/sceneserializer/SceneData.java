package com.samsungxr.utlis.sceneserializer;

import com.samsungxr.SXRNode;
import com.samsungxr.utility.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SceneData {
    private transient static final String TAG = SceneData.class.getSimpleName();
    private EnvironmentData environmentData;
    private List<NodeData> sceneObjectDataList;
    private transient Set<String> sceneObjectNames;
    private transient int modelCounter;

    SceneData() {
    }

    List<NodeData> getNodeDataList() {
        return sceneObjectDataList;
    }

    void setNodeDataList(List<NodeData> sceneObjectDataList) {
        this.sceneObjectDataList = sceneObjectDataList;
    }

    EnvironmentData getEnvironmentData() {
        return environmentData;
    }

    void setEnvironmentData(EnvironmentData environmentData) {
        this.environmentData = environmentData;
    }

    void addToSceneData(SXRNode gvrNode, String filePath) {
        if (sceneObjectDataList == null) {
            sceneObjectDataList = new ArrayList<NodeData>();
        }
        if(sceneObjectNames == null) {
            sceneObjectNames = new HashSet<String>();
            for(NodeData data: sceneObjectDataList) {
                sceneObjectNames.add(data.getName());
            }
        }
        int end = filePath.lastIndexOf(".");
        int start = filePath.lastIndexOf(File.separator, end) + 1;
        String name = null;
        do {
            name = filePath.substring(start, end) + "_" +modelCounter++;
        } while(sceneObjectNames.contains(name));
        sceneObjectNames.add(name);
        Log.d(TAG, "Setting model name to:%s", name);
        gvrNode.setName(name);
        NodeData sod = NodeData.createNodeData(gvrNode, filePath);

        sceneObjectDataList.add(sod);
    }

    void removeFromSceneData(SXRNode gvrNode) {
        Iterator<NodeData> iterator = sceneObjectDataList.iterator();
        while (iterator.hasNext()) {
            NodeData sod = iterator.next();
            if (sod.getGvrNode() == gvrNode) {
                iterator.remove();
                return;
            }
        }
    }

    void prepareForExport() {
        if(sceneObjectDataList == null) {
            return;
        }
        for (NodeData sod : sceneObjectDataList) {
            SXRNode so = sod.getGvrNode();
            if(so != null) {
                sod.setModelMatrix(so.getTransform().getModelMatrix());
                sod.setName(sod.getGvrNode().getName());
            }
        }
    }
}
