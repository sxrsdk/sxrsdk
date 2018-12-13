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
package com.samsungxr;

/**
 * Interface for processing asset load events.
 * The asset loader raises an event whenever a texture
 * or a model is loaded. It also raises an event when
 * the entire asset (models and textures) is loaded.
 * This interface lets you observe  these events.
 */
public interface IAssetImportEvents extends IEvents
{
    /**
     * Called when a model is successfully loaded.
     * This event will not be raised until the model and all of
     * its textures are loaded.
     * @param model     SXRNode root of the model scene graph, null if model did not load.
     * @param filePath  File path or URL of the model.
     * @param errors    String with loading errors or null if successful.
     */
    public void onAssetLoaded(SXRNode model, String filePath, String errors);
    
    /**
     * Called when a model is successfully loaded.
     * This event is raised when the model file is loaded.
     * Texture files may still be loading in the background.
     * @param model root of the scene graph representing the model.
     * @param filePath  File path or URL of the model.
     */
    public void onModelLoaded(SXRNode model, String filePath);
    
    /**
     * Called when a texture is successfully loaded.
     * @param texture   SXRTexture created from loading the texture file.
     * @param filePath  File path or URL of the texture file.
     */
    public void onTextureLoaded(SXRTexture texture, String filePath);
    
    /**
     * Called when a model cannot be loaded.
     * @param context   SXRContext used to load the model
     * @param error     error message
     * @param filePath  File path or URL of the model that failed to load.
     */
    public void onModelError(SXRContext context, String error, String filePath);
    
    /**
     * Called when a texture cannot be loaded.
     * @param texture   texture whose image cannot be loaded.
     * @param error     error message
     * @param filePath  File path or URL of the texture that failed to load.
     */
    public void onTextureError(SXRTexture texture, String filePath, String error);
}
