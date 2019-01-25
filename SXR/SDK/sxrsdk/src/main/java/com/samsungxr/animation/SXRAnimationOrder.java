package com.samsungxr.animation;

/** The four supported order names to assign for animations in the list.
 * FIRST is the order name given to first animation in the list.
 * MIDDLE is the order name given to all the animations in the list except for the first and last animations.
 * LAST is the order name given to last animation in the list.
 * INTER is the order name given to the between animations, for example blend animation.
 */
public enum SXRAnimationOrder {

        FIRST,

        MIDDLE,

        LAST,

        INTER;
}