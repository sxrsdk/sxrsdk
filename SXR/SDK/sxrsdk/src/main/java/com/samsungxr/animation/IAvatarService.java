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
 * <p>
 * o particular implementation is favored for the
 * avatar database. It could be a web service or
 * it could be files in the application.
 * For this reason, the interface for querying
 * the database and loading content is asynchronous.
 * </p>
 */
public interface IAvatarService
{
    /**
     * Callback for queries about avatars and attachments.
     * <p>
     * A query has properties which describe the kind of
     * avatar or attachment desired. These depend upon
     * the avatar database and are not dictated by
     * this interface.
     */
    public interface ReadHandler
    {
        /**
         * Called when an avatar query is successful.
         * @param result JSON array with the results from the query.
         */
        public void loaded(JSONArray result);

        /**
         * Called when an avatar query fails.
         * @param error String with the error message.
         */
        public void error(String error);
    };

    /**
     * Request results from the avatar database.
     * <p>
     * The input properties are added to the existing
     * properties on the request. If a property is previously set,
     * the new value replaces the old value silently.
     * The results will be presented in the form of a JSON array.
     * They depend on the implementation of the avatar database.
     * @param path       String with path to database
     * @param properties JSON object containing property keys and values.
     * @param handler    Handler to process results of query
     */
    public void get(String path, JSONObject properties, ReadHandler handler);

    /**
     * Load an avatar model with the given name.
     * <p>
     * It is assumed the avatar name was provided by querying the
     * avatar database and is a valid identifier for an avatar model.
     * To handle the results of loading the avatar, the caller
     * should attach a listener to the input avatar's event receiver
     * to process {@link com.samsungxr.animation.SXRAvatar.IAvatarEvents}.
     * @param avatarName name of avatar to find
     * @param avatar     {@link SXRAvatar} to get the new geometry.
     */
    public void loadAvatar(String avatarName, SXRAvatar avatar);

    /**
     * Load an attachment for the named avatar.
     * <p>
     * The attachment is be added to the avatar if it
     * is successfully loaded.
     * <p>
     * It is assumed the avatar and models names were provided by querying the
     * avatar database and is are valid identifiers.
     * To handle the results of loading the attachment, the caller
     * should attach a listener to the avatar's event receiver
     * to process {@link com.samsungxr.animation.SXRAvatar.IAvatarEvents}.
     * @param avatar    {@link SXRAvatar} to get the new geometry.
     * @param modelName name of attachment to find
     */
    public void loadModel(SXRAvatar avatar, String modelName);

    /**
     * Load an animation for the named avatar.
     * <p>
     * It is assumed the avatar and animation names were provided by querying the
     * avatar database and is are valid identifiers.
     * To handle the results of loading the attachment, the caller
     * should attach a listener to the avatar's event receiver
     * to process {@link com.samsungxr.animation.SXRAvatar.IAvatarEvents}.
     * @param avatar   {@link SXRAvatar} to get the new geometry.
     * @param animName name of animation to find
     */
    public void loadAnimation(SXRAvatar avatar, String animName);
}
