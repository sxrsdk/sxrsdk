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
import com.samsungxr.SXRTransform;
import com.samsungxr.animation.keyframe.BVHImporter;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
public class SXRAvatar extends SXRBehavior
    implements IEventReceiver, SXRAnimationQueue.IAnimationQueueEvents
{
    private static final String TAG = Log.tag(SXRAvatar.class);
    static private long TYPE_AVATAR = newComponentType(SXRAvatar.class);
    protected final SXRAnimationQueue mAnimsToPlay;
    protected Map<String, SXRNode> mAttachments = new HashMap<String, SXRNode>();
    protected SXRSkeleton mSkeleton;
    protected final SXRNode mAvatarRoot;
    protected SXREventReceiver mReceiver;
    protected EnumSet<SXRImportSettings> mImportSettings;

    private String mBoneMap = "";
    private SXRContext mContext;

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
        mContext = ctx;
        mAnimsToPlay = new SXRAnimationQueue(ctx, this);
        mReceiver = new SXREventReceiver(this);
        mType = getComponentType();
        mAvatarRoot = new SXRNode(ctx);
        mAvatarRoot.setName(name);
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

    public SXRAnimation addBlendAnimation(SXRAnimationQueue queue, SXRAnimator dst, SXRAnimator src, float duration)
    {
        SXRSkeletonAnimation skelOne = (SXRSkeletonAnimation) src.getAnimation(0);
        SXRSkeletonAnimation skelTwo = (SXRSkeletonAnimation) dst.getAnimation(0);;

        for (int i = 0; i < src.getAnimationCount(); ++i)
        {
            SXRAnimation anim = src.getAnimation(i);
            if (anim instanceof SXRPoseMapper)
            {
                SXRAnimationEngine.getInstance(mContext).stop(anim);
            }
        }
        for (int i = 0; i < dst.getAnimationCount(); ++i)
        {
            SXRAnimation anim  = dst.getAnimation(i);
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
        SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(skelTwo.getSkeleton(), skelOne.getSkeleton(), duration);
        dst.addAnimation(blendAnim);
        return blendAnim;
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
     * @param avatarResource    resource with avatar file.
     * @param modelDesc         model descriptor (for subclasses, unused in SXRAvatar).
     * @param attachBone        name of skeleton bone to attach this model to.
     * @see #removeModel(String)
     */
    public void loadModel(SXRAndroidResource avatarResource, String modelDesc, String attachBone)
    {
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, avatarResource);
        SXRNode modelRoot = new SXRNode(ctx);
        if (attachBone != null)
        {
            mAttachments.put(attachBone, modelRoot);
        }
        ctx.getAssetLoader().loadModel(volume, modelRoot, mImportSettings, true, mLoadModelHandler);
    }

    /**
     * Remove a model that was added as an attachment.
     * <p>
     * The first model loaded is the avatar body. Subsequent models
     * are attachments.
     * @param boneName  name of root skeleton bone in attachment skin.
     * @see #loadModel(SXRAndroidResource)
     */
    public void removeModel(String boneName)
    {
        SXRNode model = mAttachments.get(boneName);
        if (model != null)
        {
            mAvatarRoot.removeChildObject(model);
            mAttachments.remove(boneName);
        }
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
        mBoneMap = boneMap;
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
     * Starts the named animation.
     * @see SXRAvatar#stop(String)
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void start(String name)
    {
        mAnimsToPlay.start(name);
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
     * Start all of the avatar animations, causing them
     * to play one at a time in succession.
     * @param repeatMode SXRRepeatMode.REPEATED to repeatedly play,
     *                   SXRRepeatMode.ONCE to play only once
     *                   SXRRepeatMode.PINGPONG to play start to finish, finish to start;
     */
    public void startAll(int repeatMode)
    {
        mAnimsToPlay.startAll(repeatMode);
    }

    /**
     * Stops all of the animations associated with this animator.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop(String name)
    {
        mAnimsToPlay.stop(name);
    }

    /**
     * Center the avatar, taking into account the skeleton transformations.
     * @param model root of hierarchy containing avatar
     * @param pos   on input: camera position, on output: avatar position
     * @return scale factor
     */
    public float centerModel(SXRNode model, Vector3f pos)
    {
        SXRNode.BoundingVolume bv = model.getBoundingVolume();
        float sf = 1 / bv.radius;
        bv = model.getBoundingVolume();
        pos.x -= bv.center.x;
        pos.y -= bv.center.y;
        pos.z -= bv.center.z;
        return sf;
    }

    protected String mergeSkeleton(SXRSkeleton skel, SXRNode modelRoot)
    {
        String attachBone = skel.getBoneName(0);
        for (Map.Entry<String, SXRNode> item : mAttachments.entrySet())
        {
            if (item.getValue() == modelRoot)
            {
                attachBone = item.getKey();
                skel.setBoneName(0, item.getKey());
                break;
            }
        }
        int boneIndex = mSkeleton.getBoneIndex(attachBone);
        if (boneIndex < 0)
        {
            boneIndex = 0;
            attachBone = mSkeleton.getBoneName(0);
            skel.setBoneName(0, attachBone);
        }
        SXRNode srcRootBone = skel.getBone(0);
        if (srcRootBone != null)
        {
            SXRNode parent = mSkeleton.getBone(boneIndex);
            if (parent != null)
            {
                int n = srcRootBone.getChildrenCount();

                for (int i = 0; i < n; ++i)
                {
                    SXRNode child = srcRootBone.getChildByIndex(0);
                    if (skel.getBoneIndex(child.getName()) >= 0)
                    {
                        srcRootBone.removeChildObject(child);
                        parent.addChildObject(child);
                    }
                }
            }
        }
        mAttachments.put(attachBone, modelRoot);
        mSkeleton.merge(skel);
        List<SXRComponent> skins = modelRoot.getAllComponents(SXRSkin.getComponentType());
        for (SXRComponent c : skins)
        {
            SXRSkin skin = (SXRSkin) c;
            skin.setSkeleton(mSkeleton);
        }
        mAvatarRoot.addChildObject(modelRoot);
        return attachBone;
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
