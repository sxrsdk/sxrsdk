package com.samsungxr.scene_objects;

import android.view.Surface;

/**
 * Use to supply different media player objects to SXRVideoSceneObject.
 * For example in addition to the default MediaPlayer this can be used to make SXRVideoSceneObject
 * utilize the ExoPlayer.
 * @param <T>
 */
public interface SXRVideoSceneObjectPlayer<T> {
    /**
     * Returns the actual player instance
     */
    T getPlayer();

    /**
     * Called by SXRVideoSceneObject when it has the surface that can be passed to the media player
     * @param surface
     */
    void setSurface(Surface surface);

    /**
     * Called by SXRVideoSceneObject when it is done with the media player; the media player and
     * associated resources can be released.
     */
    void release();

    /**
     * Called by SXRVideoSceneObject to determine whether the Surface it creates internally can
     * be released immediately after being passed to the media player. If this returns false it
     * means that the implementation is responsible for releasing the Surface.
     */
    boolean canReleaseSurfaceImmediately();

    /**
     * Pause playback
     */
    void pause();

    /**
     * Start playback
     */
    void start();

    /**
     * Is the player currently playing
     */
    boolean isPlaying();
}
