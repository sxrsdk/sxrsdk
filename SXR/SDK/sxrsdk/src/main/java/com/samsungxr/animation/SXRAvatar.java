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
public class SXRAvatar extends SXRBehavior implements IEventReceiver
{
    private static final String TAG = Log.tag(SXRAvatar.class);
    static private long TYPE_AVATAR = newComponentType(SXRAvatar.class);
    protected final List<SXRAnimator> mAnimations;
    protected Map<String, SXRNode> mAttachments = new HashMap<String, SXRNode>();
    protected SXRSkeleton mSkeleton;
    protected final SXRNode mAvatarRoot;
    protected boolean mIsRunning;
    protected SXREventReceiver mReceiver;
    protected final List<SXRAnimator> mAnimQueue = new ArrayList<SXRAnimator>();
    protected int mRepeatMode = SXRRepeatMode.ONCE;
    protected int mRepeatCount = 1; //default

    private float repeatCounter = 0;
    private boolean reverse = false;
    private float mBlendFactor = 0;
    private String mBoneMap = "";
    private SXRContext mContext;
    private boolean mBlend = false;
    private boolean dummyAnimation = false; // flag used to enable or disable dummy animation
    private boolean order = false; //flag used to assign order names to animations for Avatar
    private int mNumAnimations = 0;

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
        mReceiver = new SXREventReceiver(this);
        mType = getComponentType();
        mAvatarRoot = new SXRNode(ctx);
        mAvatarRoot.setName(name);
        mAnimations = new CopyOnWriteArrayList<>();
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
     * Query the number of animations owned by this avatar.
     * @return number of animations added to this avatar
     */
    public int getAnimationCount()
    {
        return mAnimations.size();
    }

    /**
     * Sets the blend, blend duration and number of animations.
     * @param blend true to apply blend; false no blend.
     * @param blendFactor duration of blend.
     * @param numAnimations number of animations to be loaded for avatar
     */
    public void setBlend(boolean blend, float blendFactor, int numAnimations)
    {
        mBlend = blend;
        mBlendFactor = blendFactor;
        mNumAnimations = numAnimations;
    }
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
                        if(mBlend && mAnimQueue.size()>=2)
                        {
                            if(!order)
                            { orderAnimations(); } //assign order names to animations for Avatar

                            if(animator.getAnimationOrder()!=SXRAnimationOrder.INTER) {
                                addBlendAnimation(0, mBlendFactor, 1);
                            }
                        }

                        mAnimQueue.remove(0);
                        mIsRunning = false;

                        if (mAnimQueue.size() > 0)
                        {
                            animator = mAnimQueue.get(0);
                            animator.setBlend(mBlend, mBlendFactor);
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

                //REPEATED
                if (mRepeatMode == SXRRepeatMode.REPEATED)
                {
                   repeatCounter++;
                   if(repeatCounter < mRepeatCount || mRepeatCount < 0){
                      startAll(mRepeatMode, mRepeatCount);
                   }
                }

                //PINGPONG
                if (mRepeatMode == SXRRepeatMode.PINGPONG)
                {
                    if(!dummyAnimation)
                    {
                      playDummyAnimation(animator); //play dummy animation till stillRunning (SXRAnimation) set to false for the last animation
                    }
                    else
                    {
                      repeatCounter = repeatCounter + 0.5f ; //increment in halves for PINGPONG

                      if(repeatCounter < mRepeatCount || mRepeatCount<0) {
                         reverseAnimations();
                         setRepeatModeAndCount();
                         startAll(mRepeatMode, mRepeatCount);
                      }
                      dummyAnimation =false;
                    }
                }
                default: break;
            }
        }
    };

    /**
     * Assign order names to the animations in Avatar: FIRST, MIDDLE, and LAST
     */
    private void orderAnimations()
    {
        for(int i=1; i<mAnimQueue.size()-1; i++)
        {
            mAnimQueue.get(i).setAnimationOrder(SXRAnimationOrder.MIDDLE);
        }
        mAnimQueue.get(mAnimQueue.size()-1).setAnimationOrder(SXRAnimationOrder.LAST);

        order = true;
    }

    /**
     * Add blend animation to the avatar
     * @param index index of current finished animation in mAnimQueue
     * @param duration duration of blend animation
     * @param position add blend animation to the given position in mAnimQueue
     */
    private void addBlendAnimation(int index, float duration, int position)
    {
        SXRSkeletonAnimation skelOne = (SXRSkeletonAnimation)mAnimQueue.get(index).getAnimation(0);
        SXRSkeletonAnimation skelTwo = (SXRSkeletonAnimation)mAnimQueue.get(index+1).getAnimation(0);

        SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(getModel(), duration, skelOne, skelTwo, skelOne.getSkeleton(), reverse);
        SXRPoseMapper retargeterP = new SXRPoseMapper(getSkeleton(), skelOne.getSkeleton(), duration);
        retargeterP.setBoneMap(mBoneMap);

        SXRAnimator temp = new SXRAnimator(mContext);
        temp.addAnimation(blendAnim);
        temp.addAnimation(retargeterP);
        temp.setAnimationOrder(SXRAnimationOrder.INTER);
        mAnimQueue.add(position,temp);
    }

    /**
     * Play dummy animation to delay start playing next animation
     * @param animator currently finished animation
     */
    private void playDummyAnimation(SXRAnimator animator)
    {
        addPoseAnimation(animator, 0.1f, 0); //duration 0.1 to delay playing next animation, value can be altered as needed
        animator = mAnimQueue.get(0);
        animator.start(mOnFinish);
        dummyAnimation = true;
    }

    /**
     * Add pose animation to the avatar
     * @param animator currently finished animation
     * @param duration duration of pose animation
     * @param position add pose animation to the given position in mAnimQueue
     */
    public void addPoseAnimation(SXRAnimator animator, float duration, int position)
    {
        SXRSkeletonAnimation skelAnim = (SXRSkeletonAnimation)animator.getAnimation(0);
        SXRPose poseOne = ((SXRSkeletonAnimation) animator.getAnimation(0)).getSkeleton().getPose();

        SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(getModel(), duration, poseOne, poseOne, skelAnim.getSkeleton());
        SXRPoseMapper retargeterP = new SXRPoseMapper(getSkeleton(), skelAnim.getSkeleton(), duration);

        retargeterP.setBoneMap(mBoneMap);
        SXRAnimator temp = new SXRAnimator(mContext);
        temp.addAnimation(blendAnim);
        temp.addAnimation(retargeterP);

        mAnimQueue.add(position,temp);
    }
    /**
     * Reverse the order of animations in the list and alter the names accordingly
     * that is FIRST to LAST & LAST to FIRST
     */
    private void reverseAnimations()
    {
        reverse = !reverse;

        Collections.reverse(mAnimations); //reverse the order of animations in the list
        mAnimations.get(0).setAnimationOrder(SXRAnimationOrder.FIRST);
        mAnimations.get(mAnimations.size()-1).setAnimationOrder(SXRAnimationOrder.LAST);
    }

    private void setRepeatModeAndCount()
    {
        for (SXRAnimator anim : mAnimations)
        {
            anim.setRepeatCount(1); //set default value 1
            anim.setRepeatMode(mRepeatMode);
            anim.setReverse(reverse); //set reverse true to play animations from backwards or vice versa
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
        EnumSet<SXRImportSettings> settings = SXRImportSettings.getRecommendedSettingsWith(EnumSet.of(SXRImportSettings.OPTIMIZE_GRAPH, SXRImportSettings.NO_ANIMATION));
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, avatarResource);
        SXRNode modelRoot = new SXRNode(ctx);
        if (attachBone != null)
        {
            mAttachments.put(attachBone, modelRoot);
        }
        ctx.getAssetLoader().loadModel(volume, modelRoot, settings, true, mLoadModelHandler);
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
            EnumSet<SXRImportSettings> settings = SXRImportSettings.getRecommendedSettingsWith(
                    EnumSet.of(SXRImportSettings.OPTIMIZE_GRAPH, SXRImportSettings.NO_TEXTURING, SXRImportSettings.NO_MORPH));

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
     *                   SXRRepeatMode.PINGPONG to play start to finish, finish to start;
     * @param repeatCount play the avatar animations as given count
     *                    -1 play repeatedly
     */
    public void startAll(int repeatMode, int repeatCount)
    {
        mRepeatMode = repeatMode;
        mRepeatCount = repeatCount;
        for (SXRAnimator anim : mAnimations)
        {
            if(mBlend && mNumAnimations>=2 && !order) {
                anim.setBlend(mBlend, mBlendFactor);
                anim.setAnimationOrder(SXRAnimationOrder.FIRST);
            }
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
        int x = mAnimations.size();
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

    public void centerModel(SXRNode model)
    {
        SXRNode.BoundingVolume bv = model.getBoundingVolume();
        model.getTransform().setPosition(-bv.center.x, -bv.center.y, -bv.center.z - 1.5f * bv.radius);
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