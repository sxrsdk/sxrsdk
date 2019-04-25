package com.samsungxr.animation;

import com.samsungxr.IAssetEvents;
import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRImportSettings;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRTexture;
import com.samsungxr.animation.keyframe.BVHImporter;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.Threads;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Attributes;

/**
 * Articulated character and associated animations.
 *
 * @see com.samsungxr.SXRAssetLoader
 * @see com.samsungxr.SXRExternalScene
 * @see SXRAvatar
 * @see SXRAnimationEngine
 */
public class SXRAvatar implements IEventReceiver, SXRAnimationQueue.IAnimationQueueEvents
{
    private static final String TAG = Log.tag(SXRAvatar.class);
    protected Map<String, Attachment> mAttachments = new HashMap<String, Attachment>();
    protected final SXRAnimationQueue mAnimsToPlay;
    protected SXRSkeleton mSkeleton;
    protected final SXRNode mAvatarRoot;
    protected SXREventReceiver mReceiver;
    protected EnumSet<SXRImportSettings> mImportSettings;

    protected class Attachment
    {
        protected final Map<String, String> mProperties = new HashMap<>();
        protected SXRNode mModelRoot;
        protected String[] mHidden;

        public Attachment(String type)
        {
            setProperty("type", type);
        }

        public Attachment() { }

        public String getProperty(String name)
        {
            return mProperties.get(name);
        }

        public void setProperty(String name, String value)
        {
            mProperties.put(name, value);
        }

        public SXRNode getModelRoot()
        {
            return mModelRoot;
        }

        public void setModelRoot(SXRNode modelRoot)
        {
            mModelRoot = modelRoot;
        }

        public boolean parseModelDescription(String jsonData)
        {
            if ((jsonData == null) || "".equals(jsonData))
            {
                return false;
            }
            try
            {
                JSONObject root = new JSONObject(jsonData);
                parseModelDescription(root);
                return true;
            }
            catch (JSONException ex)
            {
                return false;
            }
        }

        public void show()
        {
            if (mModelRoot != null)
            {
                if (mModelRoot.getParent() == null)
                {
                    mAvatarRoot.addChildObject(mModelRoot);
                }
                else
                {
                    mModelRoot.setEnable(true);
                }
                if (mHidden != null)
                {
                    for (String s : mHidden)
                    {
                        SXRNode node = mAvatarRoot.getNodeByName(s);
                        if (node != null)
                        {
                            node.setEnable(false);
                        }
                    }
                }
            }
        }

        public void hide()
        {
            if (mModelRoot != null)
            {
                mModelRoot.setEnable(false);
            }
            if (mHidden != null)
            {
                for (String s : mHidden)
                {
                    SXRNode node = mAvatarRoot.getNodeByName(s);
                    if (node != null)
                    {
                        node.setEnable(true);
                    }
                }
            }
        }

        protected void parseModelDescription(JSONObject root) throws JSONException
        {
            for (String propName : new String[] { "name", "type", "attachbone", "model", "bonemap" })
            {
                if (root.has(propName))
                {
                    setProperty(propName, root.getString(propName));
                }
            }
            if (root.has("hideparts"))
            {
                JSONArray hideparts = root.getJSONArray("hideparts");
                mHidden = new String[hideparts.length()];
                for (int i = 0; i < hideparts.length(); ++i)
                {
                    mHidden[i] = hideparts.getString(i);
                }
            }
        }
    }

    /**
     * Make an instance of the SXRAnimator component.
     * Auto-start is not enabled - a call to start() is
     * required to run the animations.
     *
     * @param ctx SXRContext for this avatar
     */
    public SXRAvatar(SXRContext ctx, String name)
    {
        mAnimsToPlay = new SXRAnimationQueue(ctx, this);
        mReceiver = new SXREventReceiver(this);
        mAvatarRoot = new SXRNode(ctx);
        mAvatarRoot.setName(name);
        mImportSettings = SXRImportSettings.getRecommendedSettingsWith(EnumSet.of(SXRImportSettings.OPTIMIZE_GRAPH, SXRImportSettings.NO_ANIMATION));
        Attachment avatarInfo = addAttachment("avatar");
        avatarInfo.setProperty("type", "avatar");
    }

    /**
     * Get the event receiver for this avatar.
     * <p>
     * The avatar will generate events when assets are loaded,
     * animations are started or finished. Clients can observe
     * these events by attaching IAvatarEvent listeners to
     * this event receiver.
     */
    public SXREventReceiver getEventReceiver() { return mReceiver; }

    /**
     * Get the name of this avatar (supplied at construction time).BVH
     * @returns string with avatar name
     */
    public String getName() { return mAvatarRoot.getName(); }

    /**
     * Get the skeleton for the avatar.
     * <p>
     * The skeleton is part of the avatar model. When the asset loader loads
     * the avatar model, the skeleton should be part of the asset.
     * @return skeleton associated with the avatar
     */
    public  SXRSkeleton getSkeleton() { return mSkeleton; }

    /**
     * Get the root of the node hierarchy for the avatar.
     * <p>
     * The avatar model is constructed by the asset loader when the avatar
     * model is loaded. It contains the scene hierarchy with the skeleton
     * bones and the meshes for the avatar.
     * @return root of the avatar model hierarchy
     */
    public SXRNode getModel() { return mAvatarRoot; }

    /**
     * Determine if this avatar is currently animating.
     */
    public boolean isRunning() { return  mAnimsToPlay.isRunning(); }

    /**
     * Set the import settings for loading the avatar.
     * Avatars are always imported without animation. You can use
     * the import settings to determine whether or not you want
     * morphing, textures, etc.
     * @param settings {@link SXRImportSettings} with the import settings desired.
     */
    public void setImportSettings(EnumSet<SXRImportSettings> settings)
    {
        mImportSettings = settings;
        mImportSettings.add(SXRImportSettings.NO_ANIMATION);
    }

    /**
     * Query the number of animations owned by this avatar.
     * @return number of animations added to this avatar
     */
    public int getAnimationCount()
    {
        return mAnimsToPlay.getAnimationCount();
    }

    /**
     * Sets the blend duration.
     * @param blendFactor duration of blend.
     */
    public void setBlend(float blendFactor)
    {
        mAnimsToPlay.setBlend(blendFactor);
    }

    /**
     * Called when an animation on this avatar has started.
     * @param animator  {@link }SXRAnimator} containing the animation that started.
     */
    public void onAnimationStarted(SXRAnimationQueue queue, SXRAnimator animator)
    {
        Log.d("ANIMATOR", "Start %s", animator.getName());
        animator.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                                                             SXRAvatar.IAvatarEvents.class,
                                                             "onAnimationStarted",
                                                             SXRAvatar.this,
                                                             animator);
    }

    /**
     * Called when an animation on this avatar has completed a cycle.
     * @param animator  {@link SXRAnimator} containing the animation that finished.
     */
    public void onAnimationFinished(SXRAnimationQueue queue, SXRAnimator animator)
    {
        Log.d("ANIMATOR", "Stop %s", animator.getName());
        animator.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                                                             SXRAvatar.IAvatarEvents.class,
                                                             "onAnimationFinished",
                                                             SXRAvatar.this,
                                                             animator);
    }

    protected Attachment addAttachment(String name)
    {
        Attachment a = mAttachments.get(name);
        if (a != null)
        {
            return a;
        }
        a = new Attachment(name);
        mAttachments.put(name, a);
        return a;
    }

    public SXRAnimation addBlendAnimation(SXRAnimationQueue queue, SXRAnimator dst, SXRAnimator src, float duration)
    {
        SXRSkeletonAnimation skelOne = null;
        SXRSkeletonAnimation skelTwo = null;

        for (int i = 0; i < src.getAnimationCount(); ++i)
        {
            SXRAnimation anim = src.getAnimation(i);

            if (anim instanceof SXRSkeletonAnimation)
            {
                skelOne = (SXRSkeletonAnimation) anim;
            }
            if (anim instanceof SXRPoseMapper)
            {
                SXRAnimationEngine.getInstance(dst.getSXRContext()).stop(anim);
            }
        }
        for (int i = 0; i < dst.getAnimationCount(); ++i)
        {
            SXRAnimation anim  = dst.getAnimation(i);
            if (anim instanceof SXRSkeletonAnimation)
            {
                skelTwo = (SXRSkeletonAnimation) anim;
            }
            if (anim instanceof SXRPoseInterpolator)
            {
                anim.reset();
                return anim;
            }
            if (anim instanceof SXRPoseMapper)
            {
                SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(skelTwo.getSkeleton(), skelOne.getSkeleton(), duration);
                dst.removeAnimation(anim);
                dst.addAnimation(blendAnim);
                dst.addAnimation(anim);
                return blendAnim;
            }
        }
        if ((skelOne != null) && (skelTwo != null))
        {
            SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(skelTwo.getSkeleton(), skelOne.getSkeleton(), duration);
            dst.addAnimation(blendAnim);
            return blendAnim;
        }
        return null;
    }

    public void removeBlendAnimation(SXRAnimationQueue queue, SXRAnimator a)
    {
        for (int i = 0; i < a.getAnimationCount(); ++i)
        {
            SXRAnimation anim = a.getAnimation(i);
            if (anim instanceof SXRPoseInterpolator)
            {
                a.removeAnimation(anim);
            }
        }
    }

    public boolean isBlending(SXRAnimationQueue queue, SXRAnimator a)
    {
        for (int i = 0; i < a.getAnimationCount(); ++i)
        {
            SXRAnimation anim = a.getAnimation(i);
            if ((anim instanceof SXRPoseInterpolator) && !anim.isFinished())
            {
                return true;
            }
        }
        return false;
    }

    public void reverseBlendAnimation(SXRAnimationQueue queue, SXRAnimator a)
    {
        a.setReverse(!a.getReverse());
        for (int i = 0; i < a.getAnimationCount(); ++i)
        {
            SXRAnimation anim = a.getAnimation(i);
            if (anim instanceof SXRPoseInterpolator)
            {
                anim.setReverse(false);
            }
        }
    }

    /**
     * Load the avatar base model
     * @param avatarResource    resource with avatar model
     * Load the avatar base model or an attachment.
     * <p>
     * The first model loaded is the avatar base model. It defines the
     * skeleton of the avatar. Subsequent calls load attachments to
     * this skeleton. The skeleton of the attachment is merged with
     * the avatar skeleton so that a single skeleton drives
     * the avatar and all of its attachments.
     * @param avatarResource    resource with avatar mode.
     * @see #removeModel(String)
     */
    public void loadModel(SXRAndroidResource avatarResource)
    {
        loadModel(avatarResource, null, null);
    }

    /**
     * Load the avatar base model or an attachment.
     * <p>
     * The first model loaded is the avatar base model. It defines the
     * skeleton of the avatar. Subsequent calls load attachments to
     * this skeleton. The skeleton of the attachment is merged with
     * the avatar skeleton so that a single skeleton drives
     * the avatar and all of its attachments.
     * <p>
     * If the root bone of the attachment skeleton is found
     * in the avatar skeleton, the attachment skeleton is put
     * under this bone. Otherwise it is put under the root bone.
     * @param avatarResource    resource with avatar or model file.
     * @param modelDesc         avatar descriptor (for subclasses, unused in SXRAvatar).
     * @see #removeModel(String)
     */
    public void loadModel(SXRAndroidResource avatarResource, String modelDesc)
    {
        loadModel(avatarResource, modelDesc, null);
    }

    /**
     * Load an attachment onto the avatar.
     * <p>
     * The skeleton of the attachment is merged with
     * the avatar skeleton so that a single skeleton drives
     * the avatar and all of its attachments.
     * @param modelResource    resource with avatar file.
     * @param modelDesc        model descriptor (for subclasses, unused in SXRAvatar).
     * @param modelName        name of model (used to refer to it later)
     * @see #removeModel(String)
     */
    public void loadModel(SXRAndroidResource modelResource, String modelDesc, String modelName)
    {
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, modelResource);
        SXRNode modelRoot = new SXRNode(ctx);
        Attachment modelInfo;
        if (modelName != null)
        {
            removeModel(modelName);
            modelInfo = addAttachment(modelName);
         }
        else
        {
            modelInfo = mAttachments.get("avatar");
            mSkeleton = null;
        }
        modelInfo.setModelRoot(modelRoot);
        modelInfo.parseModelDescription(modelDesc);
        ctx.getAssetLoader().loadModel(volume, modelRoot, mImportSettings, true, mLoadModelHandler);
    }

    /**
     * Remove a model that was added as an attachment.
     * <p>
     * The first model loaded is the avatar body. Subsequent models
     * are attachments.
     * @param modelName  name of model to remove.
     * @see #loadModel(SXRAndroidResource)
     */
    public boolean removeModel(String modelName)
    {
        Attachment a = mAttachments.get(modelName);
        if (a != null)
        {
            SXRNode root = a.getModelRoot();
            if (root != null)
            {
                a.hide();
                mAvatarRoot.removeChildObject(root);
            }
            mAttachments.remove(modelName);
            return true;
        }
        return false;
    }

    protected Attachment findModel(SXRNode modelRoot)
    {
        for (Attachment a : mAttachments.values())
        {
            if (a.getModelRoot() == modelRoot)
            {
                return a;
            }
        }
        return null;
    }

    public String findModelName(SXRNode modelRoot)
    {
        if (modelRoot == null)
        {
            return null;
        }
        for (Map.Entry<String, Attachment> e : mAttachments.entrySet())
        {
            Attachment a = e.getValue();
            if (a.getModelRoot() == modelRoot)
            {
                return e.getKey();
            }
        }
        return null;
    }

    public void setProperty(String propName, String val)
    {
        Attachment a = mAttachments.get("avatar");
        a.setProperty(propName, val);
    }

    public String getProperty(String propName)
    {
        Attachment a = mAttachments.get("avatar");
        return a.getProperty(propName);
    }


    /**
     * Scale the avatar uniformly.
     * <p>
     * This function modifies the inverse bind pose matrices
     * in the {@link SXRSkin} components for the meshes
     * as well as scaling the vertex positions.
     * The {@link SXRSkeleton} pose is changed to match
     * the new geometry.
     * <p>
     * One consequence of scaling the avatar is that
     * animations which worked at the previous scale
     * may no longer work as the positions will be
     * incorrect.
     * </p>
     * @param sf    floating point scale factor
     * @see SXRSkeleton#scaleSkin(SXRNode, float)
     */
    public void scaleAvatar(float sf)
    {
        mSkeleton.scaleSkin(mAvatarRoot, sf);
    }

    /**
     * Clear the avatar model and all of its attachments.
     */
    public void clearAvatar()
    {
        SXRNode previousAvatar = (mAvatarRoot.getChildrenCount() > 0) ?
                mAvatarRoot.getChildByIndex(0) : null;

        if (previousAvatar != null)
        {
            mAvatarRoot.removeChildObject(previousAvatar);
        }
    }

    /**
     * Load an animation for the current avatar.
     * @param animResource resource with the animation
     * @param boneMap optional bone map to map animation skeleton to avatar
     */
    public void loadAnimation(final SXRAndroidResource animResource, final String boneMap)
    {
        final String filePath = animResource.getResourcePath();
        final SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, animResource);

        if (filePath.endsWith(".bvh"))
        {
            Threads.spawn(new Runnable()
            {
                public void run()
                {
                    SXRAnimator animator = new SXRAnimator(ctx);
                    animator.setName(filePath);
                    try
                    {
                        BVHImporter importer = new BVHImporter(ctx);
                        SXRSkeletonAnimation skelAnim;

                        if (boneMap != null)
                        {
                            SXRSkeleton skel = importer.importSkeleton(animResource);
                            skelAnim = importer.readMotion(skel);
                            animator.addAnimation(skelAnim);

                            SXRPoseMapper retargeter =
                                new SXRPoseMapper(mSkeleton, skel, skelAnim.getDuration());
                            retargeter.setBoneMap(boneMap);
                            animator.addAnimation(retargeter);
                        }
                        else
                        {
                            skelAnim = importer.importAnimation(animResource, mSkeleton);
                            animator.addAnimation(skelAnim);
                        }
                        addAnimation(animator);
                        ctx.getEventManager().sendEvent(SXRAvatar.this,
                                                        IAvatarEvents.class,
                                                        "onAnimationLoaded",
                                                        SXRAvatar.this,
                                                        animator,
                                                        filePath,
                                                        null);
                    }
                    catch (IOException ex)
                    {
                        ctx.getEventManager().sendEvent(SXRAvatar.this,
                                                        IAvatarEvents.class,
                                                        "onAnimationLoaded",
                                                        SXRAvatar.this,
                                                        null,
                                                        filePath,
                                                        ex.getMessage());
                    }
                }
            });
        }
        else
        {
            EnumSet<SXRImportSettings> settings = EnumSet.of(SXRImportSettings.TRIANGULATE,
                                                             SXRImportSettings.FLIP_UV,
                                                             SXRImportSettings.LIMIT_BONE_WEIGHT,
                                                             SXRImportSettings.CALCULATE_TANGENTS,
                                                             SXRImportSettings.NO_TEXTURING,
                                                             SXRImportSettings.SORTBY_PRIMITIVE_TYPE);

            SXRNode animRoot = new SXRNode(ctx);
            ctx.getAssetLoader().loadModel(volume, animRoot, settings, false, mLoadAnimHandler);
        }
    }


    /**
     * Adds an animation to this avatar.
     *
     * @param anim animation to add
     * @see SXRAvatar#removeAnimation(SXRAnimator)
     * @see SXRAvatar#clear()
     */
    public void addAnimation(SXRAnimator anim)
    {
        mAnimsToPlay.add(anim);
    }

    /**
     * Gets an animation from this avatar.
     *
     * @param index index of animation to get
     * @see SXRAvatar#addAnimation(SXRAnimator)
     */

    public SXRAnimator getAnimation(int index)
    {
        return mAnimsToPlay.get(index);
    }

    /**
     * Removes an animation from this avatar.
     *
     * @param anim animation to remove
     * @see SXRAvatar#addAnimation(SXRAnimator)
     * @see SXRAvatar#clear()
     */
    public void removeAnimation(SXRAnimator anim)
    {
        mAnimsToPlay.remove(anim);
    }

    /**
     * Removes all the animations from this avatar.
     * <p>
     * The state of the animations are not changed when removed. For example,
     * if the animations are already running they are not be stopped.
     *
     * @see SXRAvatar#removeAnimation(SXRAnimator)
     * @see SXRAvatar#addAnimation(SXRAnimator)
     */
    public void clear()
    {
        mAnimsToPlay.clear();
    }

    /**
     * Starts the named animation at the end of the currently
     * playing animation sequence.
     * @see SXRAvatar#stop(String)
     * @see SXRAvatar#startNext(String)
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void start(String name)
    {
        mAnimsToPlay.start(name);
    }

    /**
     * Add the given animation to this avatar and start it
     * immediately after the currently playing animation.
     * @param name  name of animation to start
     * @see SXRAvatar#stop(String)
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void startNext(String name)
    {
        mAnimsToPlay.startNext(name);
    }

    /**
     * Find the animation associated with this avatar with the given name.
     * @param name  name of animation to look for
     * @return {@link SXRAnimator} animation found or null if none with that name
     */
    public SXRAnimator findAnimation(String name)
    {
        return mAnimsToPlay.findAnimation(name);
    }

    /**
     * Starts the animation with the given index.
     * @param animIndex 0-based index of {@link SXRAnimator} to start;
     * @see #start(String)
     * @see #stop(String)
     */
    public SXRAnimator start(int animIndex)
    {
        return mAnimsToPlay.start(animIndex);
    }

    /**
     * Start all of the avatar animations.
     * The animations will play consecutively.
     * They may overlap if blending is requested.
     * @param repeatMode controls animation sequencing:
     * <table>
     *     <tr>
     *         <td>SXRRepeatMode.ONCE</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             Stop after the last aniation.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.REPEATED</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last aniation is played, start at the first
     *             animation and play the sequence repeatedly.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.PINGPONG</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last animation is played, start at the last
     *             animation and play the backwards, running the animations
     *             in reverse. Continue playing the sequence forward and
     *             then backwards.
     *         </td>
     *     </tr>
     * </table>
     */
    public void startAll(int repeatMode)
    {
        mAnimsToPlay.startAll(repeatMode);
    }

    /**
     * Start all of the avatar animations
     * whose name contains the pattern string.
     * The animations will play consecutively.
     * They may overlap if blending is requested.
     * @param pattern String to match
     * @param repeatMode controls animation sequencing:
     * <table>
     *     <tr>
     *         <td>SXRRepeatMode.ONCE</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             Stop after the last aniation.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.REPEATED</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last aniation is played, start at the first
     *             animation and play the sequence repeatedly.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.PINGPONG</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last animation is played, start at the last
     *             animation and play the backwards, running the animations
     *             in reverse. Continue playing the sequence forward and
     *             then backwards.
     *         </td>
     *     </tr>
     * </table>
     */
    public void startAll(String pattern, int repeatMode)
    {
        mAnimsToPlay.startAll(pattern, repeatMode);
    }

    /**
     * Stops the named animation.
     * @param name  name of animation to stop.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop(String name)
    {
        mAnimsToPlay.stop(name);
    }

    /**
     * Stops all of the animations associated with this animator.
     * @see SXRAvatar#startAll()
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop()
    {
        mAnimsToPlay.stop();
    }

    /**
     * Center the avatar, taking into account the skeleton transformations.
     * @param model root of hierarchy containing avatar
     * @param pos   on input: camera position, on output: avatar position
     * @return scale factor
     */
    public float centerModel(SXRNode model, Vector3f pos)
    {
        if (mSkeleton != null)
        {
            Vector3f center = new Vector3f();
            float r = mSkeleton.getCenter(center);
            pos.x -= center.x;
            pos.y -= center.y;
            pos.z -= center.z;
            return r;
        }
        else
        {
            SXRNode.BoundingVolume bv = model.getBoundingVolume();
            pos.x -= bv.center.x;
            pos.y -= bv.center.y;
            pos.z -= bv.center.z;
            return bv.radius;
        }
    }

    protected Attachment mergeSkeleton(SXRSkeleton skel, SXRNode modelRoot)
    {
        String attachBone = null;
        Attachment a = findModel(modelRoot);

        if (a != null)
        {
            attachBone = a.getProperty("attachbone");
        }
        else
        {
            a = addAttachment(skel.getBoneName(0));
        }
        mSkeleton.merge(skel, attachBone);
        List<SXRComponent> skins = modelRoot.getAllComponents(SXRSkin.getComponentType());
        for (SXRComponent c : skins)
        {
            SXRSkin skin = (SXRSkin) c;
            skin.setSkeleton(mSkeleton);
        }
        if (modelRoot.getParent() == null)
        {
            mAvatarRoot.addChildObject(modelRoot);
        }
        return a;
    }

    protected Attachment onLoadAvatar(SXRNode modelRoot, SXRSkeleton skel, String filePath)
    {
        Attachment a = addAttachment("avatar");
        a.setProperty("type", "avatar");
        if (a.getProperty("model") == null)
        {
            a.setProperty("model", filePath);
        }
        if (a.getProperty("name") != null)
        {
            mAvatarRoot.setName(a.getProperty("name"));
        }
        while (mAvatarRoot.getChildrenCount() > 0)
        {
            SXRNode child = mAvatarRoot.getChildByIndex(0);
            mAvatarRoot.removeChildObject(child);
        }
        mSkeleton = skel;
        mSkeleton.poseFromBones();
        mAvatarRoot.addChildObject(modelRoot);
        return a;
    }

    protected Attachment onLoadModel(SXRNode modelRoot, SXRSkeleton skel, String filePath)
    {
        Attachment a = null;
        if (skel != null)
        {
            a = mergeSkeleton(skel, modelRoot);
            a.show();
        }
        else
        {
            a = findModel(modelRoot);
            if (a != null)
            {
                a.show();
            }
            else if (modelRoot.getParent() == null)
            {
                mAvatarRoot.addChildObject(modelRoot);
            }
        }
        return a;
    }

    protected IAssetEvents mLoadModelHandler = new IAssetEvents()
    {
        public void onAssetLoaded(SXRContext context, SXRNode modelRoot, String filePath, String errors)
        {
            String eventName = "onModelLoaded";

            if (modelRoot != null)
            {
                List<SXRComponent> skeletons = modelRoot.getAllComponents(SXRSkeleton.getComponentType());

                if (skeletons.size() > 0)
                {
                    SXRSkeleton skel = (SXRSkeleton) skeletons.get(0);
                    if (mSkeleton != null)
                    {
                        onLoadModel(modelRoot, skel, filePath);
                    }
                    else
                    {
                        onLoadAvatar(modelRoot, skel, filePath);
                        modelRoot = mAvatarRoot;
                        eventName = "onAvatarLoaded";
                    }
                }
                else if (mSkeleton != null)
                {
                    onLoadModel(modelRoot, null, filePath);
                }
                else
                {
                    errors += "Avatar skeleton not found";
                    Log.e(TAG, "Avatar skeleton not found in asset file " + filePath);
                }
            }
            context.getEventManager().sendEvent(SXRAvatar.this,
                    IAvatarEvents.class,
                    eventName,
                    SXRAvatar.this,
                    modelRoot,
                    filePath,
                    errors);
        }

        public void onModelLoaded(SXRContext context, SXRNode model, String filePath) { }
        public void onTextureLoaded(SXRContext context, SXRTexture texture, String filePath) { }
        public void onModelError(SXRContext context, String error, String filePath) { }
        public void onTextureError(SXRContext context, String error, String filePath) { }
    };

    public interface IAvatarEvents extends IEvents
    {
        public void onAvatarLoaded(SXRAvatar avatar, SXRNode avatarRoot, String filePath, String errors);
        public void onModelLoaded(SXRAvatar avatar, SXRNode avatarRoot, String filePath, String errors);
        public void onAnimationLoaded(SXRAvatar avatar, SXRAnimator animator, String filePath, String errors);
        public void onAnimationStarted(SXRAvatar avatar, SXRAnimator animator);
        public void onAnimationFinished(SXRAvatar avatar, SXRAnimator animator);
    }

    protected IAssetEvents mLoadAnimHandler = new IAssetEvents()
    {
        public void onAssetLoaded(SXRContext context, SXRNode animRoot, String filePath, String errors)
        {
            SXRAnimator animator = (SXRAnimator) animRoot.getComponent(SXRAnimator.getComponentType());
            if (animator == null)
            {
                if (errors == null)
                {
                    errors = "No animations found in " + filePath;
                }
                context.getEventManager().sendEvent(SXRAvatar.this,
                        IAvatarEvents.class,
                        "onAnimationLoaded",
                        SXRAvatar.this,
                        null,
                        filePath,
                        errors);
                return;
            }

            SXRSkeletonAnimation skelAnim = (SXRSkeletonAnimation) animator.getAnimation(0);
            SXRSkeleton skel = skelAnim.getSkeleton();
            if (skel != mSkeleton)
            {
                SXRPoseMapper poseMapper = new SXRPoseMapper(mSkeleton, skel, skelAnim.getDuration());

                animator.addAnimation(poseMapper);
            }
            addAnimation(animator);
            context.getEventManager().sendEvent(SXRAvatar.this,
                    IAvatarEvents.class,
                    "onAnimationLoaded",
                    SXRAvatar.this,
                    animator,
                    filePath,
                    errors);
        }

        public void onModelLoaded(SXRContext context, SXRNode model, String filePath) { }
        public void onTextureLoaded(SXRContext context, SXRTexture texture, String filePath) { }
        public void onModelError(SXRContext context, String error, String filePath) { }
        public void onTextureError(SXRContext context, String error, String filePath) { }
    };
}
