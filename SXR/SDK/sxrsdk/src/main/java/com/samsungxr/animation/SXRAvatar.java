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

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Group of animations that can be collectively manipulated.
 *
 * Typically the animations belong to a particular model and
 * represent a sequence of poses for the model over time.
 * This class allows you to start, stop and set animation modes
 * for all the animations in the group at once.
 * An asset which has animations will have this component
 * attached to collect the animations for the asset.
 *
 * @see com.samsungxr.SXRAssetLoader
 * @see com.samsungxr.SXRExternalScene
 * @see SXRAvatar
 * @see SXRAnimationEngine
 */
public class SXRAvatar extends SXRBehavior implements IEventReceiver
{
    /**
     * Descriptor file format:
     * name: modelName,
     * bone: boneName,
     * replace: [ string, string, string, ... ]
     */
    protected class Attachment
    {
        protected final String mName;
        protected SXRNode mRootNode;
        protected SXRSkeleton mSkeleton;
        protected List<String> mReplaceParts = new ArrayList<>();
        protected List<SXRNode> mHidden;
        protected final Map<String, String> mProperties = new HashMap<>();

        public Attachment(String name)
        {
            mName = name;
        }

        public SXRSkeleton getSkeleton() { return mSkeleton; }

        public String getName() { return mName; }

        public SXRNode getRoot() { return mRootNode; }

        public void setProperty(String propName, String propVal)
        {
            mProperties.put(propName, propVal);
        }

        public String getProperty(String propName)
        {
            return mProperties.get(propName);
        }

        public void setRoot(SXRNode node) { mRootNode = node; }

        public boolean parseDescription(String jsonData)
        {
            try
            {
                parseDescription(new JSONObject(jsonData));
                return true;
            }
            catch (JSONException ex)
            {
                Log.e(TAG, ex.getMessage());
                return false;
            }
        }

        protected void parseDescription(JSONObject root) throws JSONException
        {
            if (root.has("attachbone"))
            {
                mProperties.put("attachbone", root.getString("attachbone"));
            }
            if (root.has("replace"))
            {
                JSONArray replaceParts = root.getJSONArray("replace");

                for (int i = 0; i < replaceParts.length(); ++i)
                {
                    String s = replaceParts.getString(i);
                    mReplaceParts.add(s);
                }
            }
        }

        public void show()
        {
            if (mHidden == null)
            {
                mHidden = new ArrayList<SXRNode>();
                for (String name : mReplaceParts)
                {
                    SXRNode node = mRootNode.getNodeByName(name);
                    if (node == null)
                    {
                        Log.w(TAG, "Node %s cannot be hidden", name);
                    }
                    node.setEnable(false);
                    mHidden.add(node);
                }
            }
            else
            {
                for (SXRNode node : mHidden)
                {
                    node.setEnable(false);
                }
            }
            mRootNode.setEnable(true);
        }

        public void hide()
        {
            if (mHidden != null)
            {
                for (SXRNode node : mHidden)
                {
                    node.setEnable(true);
                }
            }
            mRootNode.setEnable(false);
        }

        public void remove()
        {
            hide();
        }
    }

    private static final String TAG = Log.tag(SXRAvatar.class);
    static private long TYPE_AVATAR = newComponentType(SXRAvatar.class);
    protected final List<SXRAnimator> mAnimations;
    protected Map<String, Attachment> mAttachments = new HashMap<String, Attachment>();
    protected SXRSkeleton mSkeleton;
    protected final SXRNode mAvatarRoot;
    protected boolean mIsRunning;
    protected SXREventReceiver mReceiver;
    protected final List<SXRAnimator> mAnimQueue = new ArrayList<SXRAnimator>();
    protected int mRepeatMode = SXRRepeatMode.ONCE;
    protected EnumSet<SXRImportSettings> mImportSettings;

    /**
     * Make an instance of the SXRAnimator component.
     * Auto-start is not enabled - a call to start() is
     * required to run the animations.
     *
     * @param ctx SXRContext for this avatar
     */
    public SXRAvatar(SXRContext ctx, String name)
    {
        super(ctx);
        mReceiver = new SXREventReceiver(this);
        mType = getComponentType();
        mAvatarRoot = new SXRNode(ctx);
        mAvatarRoot.setName(name);
        mAnimations = new CopyOnWriteArrayList<>();
        mImportSettings = SXRImportSettings.getRecommendedSettingsWith(EnumSet.of(SXRImportSettings.OPTIMIZE_GRAPH, SXRImportSettings.NO_ANIMATION));
    }

    static public long getComponentType() { return TYPE_AVATAR; }

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
    public boolean isRunning() { return mIsRunning; }


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
        return mAnimations.size();
    }

    /**
     * Called when an animation on this avatar has started.
     * @param animator  {@link }SXRAnimator} containing the animation that started.
     */
    public void onAnimationStarted(SXRAnimator animator) { }

    /**
     * Called when an animation on this avatar has completed a cycle.
     * @param animator  {@link }SXRAnimator} containing the animation that finished.
     * @param animation {@link SXRAnimation} that finished..
     */
    public void onAnimationFinished(SXRAnimator animator, SXRAnimation animation) { }

    protected Attachment addAttachment(String name)
    {
        Attachment a = mAttachments.get(name);

        if (a == null)
        {
            a = new Attachment(name);
            mAttachments.put(name, a);
        }
        return a;
    }

    protected Attachment findAttachment(String name)
    {
        return mAttachments.get(name);
    }

    protected Attachment findAttachment(SXRNode root)
    {
        for (Attachment a :  mAttachments.values())
        {
            if (a.getRoot() == root)
            {
                return a;
            }
        }
        return null;
    }

    public void setProperty(String name, String value)
    {
        Attachment a = addAttachment("avatar");
        a.setProperty(name, value);
    }

    public String getProperty(String name)
    {
        Attachment a = findAttachment("avatar");
        if (a != null)
        {
            return a.getProperty(name);
        }
        return null;
    }

    protected SXROnFinish mOnFinish = new SXROnFinish()
    {
        public void finished(SXRAnimation animation)
        {
            int numEvents = 0;
            SXRAnimator animator = null;
            synchronized (mAnimQueue)
            {
                if (mAnimQueue.size() > 0)
                {
                    animator = mAnimQueue.get(0);
                    if (animator.findAnimation(animation) >= 0)
                    {
                        mAnimQueue.remove(0);
                        mIsRunning = false;
                        if (mAnimQueue.size() > 0)
                        {
                            animator = mAnimQueue.get(0);
                            animator.start(mOnFinish);
                            numEvents = 2;
                        }
                        else
                        {
                            numEvents = 1;
                        }
                    }
                }
            }
            switch (numEvents)
            {
                case 2:
                onAnimationFinished(animator, animation);
                mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                                                                        IAvatarEvents.class,
                                                                        "onAnimationFinished",
                                                                        SXRAvatar.this,
                                                                        animator,
                                                                        animation);
                onAnimationStarted(animator);
                mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                                                                        IAvatarEvents.class,
                                                                        "onAnimationStarted",
                                                                        SXRAvatar.this,
                                                                        animator);
                break;

                case 1:
                onAnimationStarted(animator);
                mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                                                                        IAvatarEvents.class,
                                                                        "onAnimationStarted",
                                                                        SXRAvatar.this,
                                                                        animator);
                if (mRepeatMode == SXRRepeatMode.REPEATED)
                {
                    startAll(mRepeatMode);
                }

                default: break;
            }
        }
    };

    /**
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
     * @param modelResource     resource with avatar file.
     * @param modelDesc         model descriptor (for subclasses, unused in SXRAvatar).
     * @param attachmentName    name of skeleton bone to attach this model to.
     * @see #removeModel(String)
     */
    public void loadModel(SXRAndroidResource modelResource, String modelDesc, String attachmentName)
    {
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, modelResource);
        SXRNode modelRoot = new SXRNode(ctx);

        if (attachmentName == null)
        {
            attachmentName = "avatar";
        }
        Attachment a = addAttachment(attachmentName);

        a.setRoot(modelRoot);
        a.setProperty("model", modelResource.getResourcePath());
        a.parseDescription(modelDesc);
        ctx.getAssetLoader().loadModel(volume, modelRoot, mImportSettings, true, mLoadModelHandler);
    }

    /**
     * Remove a model that was added as an attachment.
     * <p>
     * The first model loaded is the avatar body. Subsequent models
     * are attachments.
     * @param attachmentName  name of attachcment to remove.
     * @see #loadModel(SXRAndroidResource)
     */
    public void removeModel(String attachmentName)
    {
        Attachment a = mAttachments.get(attachmentName);
        if (a != null)
        {
            mAvatarRoot.removeChildObject(a.getRoot());
            mAttachments.remove(attachmentName);
        }
    }

    /**
     * Show a model that was previously added as an attachment.
     * @see #loadModel(SXRAndroidResource, String, String)
     * @see #hideModel(String)
     */
    public void showModel(String attachmentName)
    {
        Attachment a = mAttachments.get(attachmentName);
        if (a != null)
        {
            a.show();
        }
    }

    /**
     * Show a model that was previously added as an attachment.
     * The model remains part of the avatar, it is just not visible.
     * @see #loadModel(SXRAndroidResource, String, String)
     * @see #showModel(String)
     */
    public void hideModel(String attachmentName)
    {
        Attachment a = mAttachments.get(attachmentName);
        if (a != null)
        {
            a.hide();
        }
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
    public void loadAnimation(SXRAndroidResource animResource, String boneMap)
    {
        String filePath = animResource.getResourcePath();
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, animResource);

        if (filePath.endsWith(".bvh"))
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

                    SXRPoseMapper retargeter = new SXRPoseMapper(mSkeleton, skel, skelAnim.getDuration());
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
        else
        {
            EnumSet<SXRImportSettings> settings = EnumSet.of(SXRImportSettings.TRIANGULATE,
                                                             SXRImportSettings.FLIP_UV,
                                                             SXRImportSettings.LIMIT_BONE_WEIGHT,
                                                             SXRImportSettings.CALCULATE_TANGENTS,
                                                             SXRImportSettings.NO_ANIMATION,
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
        mAnimations.add(anim);
    }

    /**
     * Gets an animation from this avatar.
     *
     * @param index index of animation to get
     * @see SXRAvatar#addAnimation(SXRAnimator)
     */
    public SXRAnimator getAnimation(int index)
    {
        return mAnimations.get(index);
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
        mAnimations.remove(anim);
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
        mAnimations.clear();
    }

    /**
     * Starts the named animation.
     * @see SXRAvatar#stop(String)
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void start(String name)
    {
        SXRAnimator anim = findAnimation(name);

        if ((anim != null) && name.equals(anim.getName()))
        {
            start(anim);
            return;
        }
    }

    /**
     * Find the animation associated with this avatar with the given name.
     * @param name  name of animation to look for
     * @return {@link SXRAnimator} animation found or null if none with that name
     */
    public SXRAnimator findAnimation(String name)
    {
        for (SXRAnimator anim : mAnimations)
        {
            if (name.equals(anim.getName()))
            {
                return anim;
            }
        }
        return null;
    }

    /**
     * Starts the animation with the given index.
     * @param animIndex 0-based index of {@link SXRAnimator} to start;
     * @see SXRAvatar#stop()
     * @see #start(String)
     */
    public SXRAnimator start(int animIndex)
    {
        if ((animIndex < 0) || (animIndex >= mAnimations.size()))
        {
            throw new IndexOutOfBoundsException("Animation index out of bounds");
        }
        SXRAnimator anim = mAnimations.get(animIndex);
        start(anim);
        return anim;
    }

    /**
     * Start all of the avatar animations, causing them
     * to play one at a time in succession.
     * @param repeatMode SXRRepeatMode.REPEATED to repeatedly play,
     *                   SXRRepeatMode.ONCE to play only once
     */
    public void startAll(int repeatMode)
    {
        mRepeatMode = repeatMode;
        for (SXRAnimator anim : mAnimations)
        {
            start(anim);
        }
    }

    protected void start(SXRAnimator animator)
    {
        synchronized (mAnimQueue)
        {
            mAnimQueue.add(animator);
            if (mAnimQueue.size() > 1)
            {
                return;
            }
        }
        mIsRunning = true;
        animator.start(mOnFinish);
        mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                                                                IAvatarEvents.class,
                                                                "onAnimationStarted",
                                                                SXRAvatar.this,
                                                                animator);
    }

    /**
     * Evaluates the animation with the given index at the specified time.
     * @param animIndex 0-based index of {@link SXRAnimator} to start
     * @param timeInSec time to evaluate the animation at
     * @see SXRAvatar#stop()
     * @see #start(String)
     */
    public SXRAnimator animate(int animIndex, float timeInSec)
    {
        if ((animIndex < 0) || (animIndex >= mAnimations.size()))
        {
            throw new IndexOutOfBoundsException("Animation index out of bounds");
        }
        SXRAnimator anim = mAnimations.get(animIndex);
        anim.animate(timeInSec);
        return anim;
    }

    /**
     * Stops all of the animations associated with this animator.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop(String name)
    {
        SXRAnimator anim = findAnimation(name);

        if (anim != null)
        {
            mIsRunning = false;
            anim.stop();
        }
    }

    /**
     * Stops the currently running animation, if any.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop()
    {
        synchronized (mAnimQueue)
        {
            if (mIsRunning && (mAnimQueue.size() > 0))
            {
                mIsRunning = false;
                SXRAnimator animator = mAnimQueue.get(0);
                mAnimQueue.clear();
                animator.stop();
            }
        }
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
            SXRNode bone = mSkeleton.getBone(0);
            Matrix4f mtx1 = bone.getTransform().getModelMatrix4f();
            Matrix4f mtx2 = new Matrix4f();
            float[] bv = mSkeleton.getBound();
            Vector4f cmin = new Vector4f(bv[0], bv[1], bv[2], 1);
            Vector4f cmax = new Vector4f(bv[3], bv[4], bv[5], 1);

            mSkeleton.getPose().getLocalMatrix(0, mtx2);
            mtx2.invert(mtx2);
            mtx1.mul(mtx2, mtx2);
            cmin.mul(mtx2);
            cmax.mul(mtx2);

            float cx = (cmax.x + cmin.x) / 2;
            float cy = (cmax.y + cmin.y) / 2;
            float cz = (cmax.z + cmin.z) / 2;
            float r = Math.max(cmax.x - cmin.x,
                               Math.max(cmax.y - cmin.y,
                                        cmax.z - cmin.z));
            float sf = 0.5f / r;
            cx *= sf;
            cy *= sf;
            cz *= sf;
            pos.x -= cx;
            pos.y -= cy;
            pos.z -= cz;
            return sf;
        }
        else
        {
            SXRNode.BoundingVolume bv = model.getBoundingVolume();
            float sf = 1 / bv.radius;
            bv = model.getBoundingVolume();
            pos.x -= bv.center.x;
            pos.y -= bv.center.y;
            pos.z -= bv.center.z;
            return sf;
        }
    }

    protected Attachment mergeSkeleton(SXRSkeleton skel, SXRNode modelRoot)
    {
        String attachBone = null;
        Attachment a = findAttachment(modelRoot);
        if (a != null)
        {
            attachBone = a.getProperty("attachbone");
        }
        if (attachBone == null)
        {
            attachBone = skel.getBoneName(0);
        }

        int boneIndex = mSkeleton.getBoneIndex(attachBone);
        if ((a == null) && (boneIndex < 0))
        {
            skel.setBoneName(0, attachBone);
            a = addAttachment(attachBone);
            a.setRoot(modelRoot);
            a.setProperty("attachbone", attachBone);
        }
        mSkeleton.merge(skel);
        List<SXRComponent> skins = modelRoot.getAllComponents(SXRSkin.getComponentType());
        for (SXRComponent c : skins)
        {
            SXRSkin skin = (SXRSkin) c;
            skin.setSkeleton(mSkeleton);
        }
        mAvatarRoot.addChildObject(modelRoot);
        return a;
    }

    protected IAssetEvents mLoadModelHandler = new IAssetEvents()
    {
        public void onAssetLoaded(SXRContext context, SXRNode modelRoot, String filePath, String errors)
        {
            List<SXRComponent> skeletons = modelRoot.getAllComponents(SXRSkeleton.getComponentType());
            String eventName = "onModelLoaded";
            if ((errors != null) && !errors.isEmpty())
            {
                Log.e(TAG, "Asset load errors: " + errors);
            }
            if (skeletons.size() > 0)
            {
                SXRSkeleton skel = (SXRSkeleton) skeletons.get(0);
                if (mSkeleton != null)
                {
                    mergeSkeleton(skel, modelRoot);
                }
                else
                {
                    mSkeleton = skel;
                    mSkeleton.poseFromBones();
                    mAvatarRoot.addChildObject(modelRoot);
                    modelRoot = mAvatarRoot;
                    eventName = "onAvatarLoaded";
                }
            }
            else if (mSkeleton != null)
            {
                if (modelRoot.getParent() == null)
                {
                    mAvatarRoot.addChildObject(modelRoot);
                }
            }
            else
            {
                Log.e(TAG, "Avatar skeleton not found in asset file " + filePath);
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
        public void onAnimationFinished(SXRAvatar avatar, SXRAnimator animator, SXRAnimation animation);
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