package com.samsungxr.physics;

/**
 * Information returned for physics collisions.
 * This structure applies to both enter
 * (objects started to collide) and exit
 * (objects finished colliding) events.
 */
class SXRCollisionInfo
{
    /**
     * First body involved in collision.
     */
    public final long bodyA;

    /**
     * Second body involved in collision.
     */
    public final long bodyB;

    /**
     * Normal vector to collision point.
     */
    public final float[] normal;

    /**
     * Distance between objects which collided.
     */
    public final float distance;

    /**
     * True if objects are colliding now.
     * This is false for collision exits.
     */
    public final boolean isHit;

    /**
     * Construct a record of a collision.
     * @param bodyA     First body involved in collision.
     * @param bodyB     Second body involved in collision.
     * @param normal    Normal vector to collision point.
     * @param distance  Distance between objects which collided.
     * @param isHit     rue if objects are colliding now.
     */
    public SXRCollisionInfo(long bodyA, long bodyB, float normal[], float distance, boolean isHit)
    {
        this.bodyA = bodyA;
        this.bodyB = bodyB;
        this.normal = normal;
        this.distance = distance;
        this.isHit = isHit;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof SXRCollisionInfo))
            return false;
        if (obj == this)
            return true;

        SXRCollisionInfo cp = (SXRCollisionInfo) obj;
        return (this.bodyA == cp.bodyA && this.bodyB == cp.bodyB);
    }
}

