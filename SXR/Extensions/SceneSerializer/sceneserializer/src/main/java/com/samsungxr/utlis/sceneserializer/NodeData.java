package com.samsungxr.utlis.sceneserializer;

import com.samsungxr.SXRNode;

public class NodeData {
    private float[] modelMatrix;
    private String src;
    private String name;
    private transient SXRNode gvrNode;

    public NodeData() {
    }

    public static NodeData createNodeData(SXRNode gvrNode, String
            source) {
        NodeData sceneObjectData = new NodeData();
        sceneObjectData.setSrc(source);
        sceneObjectData.setGvrNode(gvrNode);
        sceneObjectData.setModelMatrix(gvrNode.getTransform().getModelMatrix());
        sceneObjectData.setName(gvrNode.getName());
        return sceneObjectData;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public float[] getModelMatrix() {
        return modelMatrix;
    }

    public void setModelMatrix(float[] modelMatrix) {
        this.modelMatrix = modelMatrix;
    }

    public SXRNode getGvrNode() {
        return gvrNode;
    }

    public void setGvrNode(SXRNode gvrNode) {
        this.gvrNode = gvrNode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
