package com.samsungxr.physics;

import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRSceneObject;

import java.util.List;

abstract class SXRPhysicsWorldObject extends SXRComponent {

    protected SXRPhysicsWorldObject(SXRContext gvrContext, long nativePointer) {
        super(gvrContext, nativePointer);
    }

    protected SXRPhysicsWorldObject(SXRContext gvrContext, long nativePointer, List<NativeCleanupHandler> cleanupHandlers) {
        super(gvrContext, nativePointer, cleanupHandlers);
    }

    /**
     * Returns the {@linkplain SXRWorld physics world} of this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The physics world of this {@link SXRRigidBody}
     */
    public SXRWorld getWorld() {
        return getWorld(getOwnerObject());
    }

    /**
     * Returns the {@linkplain SXRWorld physics world} of the {@linkplain com.samsungxr.SXRScene scene}.
     *
     * @param owner Owner of the {@link SXRRigidBody}
     * @return Returns the {@link SXRWorld} of the scene.
     */
    private static SXRWorld getWorld(SXRSceneObject owner) {
        return getWorldFromAscendant(owner);
    }

    /**
     * Looks for {@link SXRWorld} component in the ascendants of the scene.
     *
     * @param worldOwner Scene object to search for a physics world in the scene.
     * @return Physics world from the scene.
     */
    private static SXRWorld getWorldFromAscendant(SXRSceneObject worldOwner) {
        SXRComponent world = null;

        while (worldOwner != null && world == null) {
            world = worldOwner.getComponent(SXRWorld.getComponentType());
            worldOwner = worldOwner.getParent();
        }

        return (SXRWorld) world;
    }

    @Override
    public void onAttach(SXRSceneObject newOwner) {
        super.onAttach(newOwner);
        if (isEnabled()) {
            addToWorld(getWorld(newOwner));
        }
    }

    @Override
    public void onDetach(SXRSceneObject oldOwner) {
        super.onDetach(oldOwner);
        if (isEnabled()) {
            removeFromWorld(getWorld(oldOwner));
        }
    }

    @Override
    public void onNewOwnersParent(SXRSceneObject newOwnersParent) {
        if (isEnabled()) {
            addToWorld(getWorld(newOwnersParent));
        }
    }

    @Override
    public void onRemoveOwnersParent(SXRSceneObject oldOwnersParent) {
        if (isEnabled()) {
            removeFromWorld(getWorld(oldOwnersParent));
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        addToWorld(getWorld());
    }

    @Override
    public void onDisable() {
        super.onDisable();

        removeFromWorld(getWorld());
    }

    abstract protected void removeFromWorld(SXRWorld world);

    abstract protected void addToWorld(SXRWorld world);
}
