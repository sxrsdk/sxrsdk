package com.samsungxr.physics;

class SXRCollisionInfo {

    public final long bodyA;
    public final long bodyB;
    public final float[] normal;
    public final float distance;
    public final boolean isHit;

    public SXRCollisionInfo(long bodyA, long bodyB, float normal[], float distance, boolean isHit) {
        this.bodyA = bodyA;
        this.bodyB = bodyB;
        this.normal = normal;
        this.distance = distance;
        this.isHit = isHit;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SXRCollisionInfo))
            return false;
        if (obj == this)
            return true;

        SXRCollisionInfo cp = (SXRCollisionInfo) obj;
        return (this.bodyA == cp.bodyA && this.bodyB == cp.bodyB);
    }
}

