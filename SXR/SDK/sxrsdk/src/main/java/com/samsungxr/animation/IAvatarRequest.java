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
package com.samsungxr.animation;

import com.samsungxr.IEvents;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTexture;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Interface for acccessing avatars and their attachments.
 */
public interface IAvatarRequest
{
    public interface ReadHandler
    {
        public void loaded(JSONArray result);
        public void error(String error);
    };

    public String getProperty(String propName);
    public void removeProperty(String propName);
    public void setProperty(String propName, String propValue);
    public void setProperties(JSONObject properties);
    public void get(String path, ReadHandler handler);
    public void get(String path, JSONObject properties, ReadHandler handler);
    public boolean loadAvatar(String avatarName);
    public boolean loadAvatar(String avatarName, JSONObject properties);
    public boolean loadModel(String avatarName, String modelName, JSONObject properties);
    public boolean loadModel(String avatarName, String modelName);
    public boolean loadAnimation(String avatarName, String animFile);
}
