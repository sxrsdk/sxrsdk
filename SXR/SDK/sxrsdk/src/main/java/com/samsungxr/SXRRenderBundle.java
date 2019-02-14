/* Copyright 2015 Samsung Electronics Co., LTD
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
package com.samsungxr;

import com.samsungxr.utility.Log;
import com.samsungxr.utility.VrAppSettings;

/** A container for various services and pieces of data required for rendering. */
class SXRRenderBundle {
    protected SXRContext mSXRContext;
    private  SXRShaderManager mShaderManager;
    private  SXRRenderTexture mPostEffectRenderTextureA = null;
    private  SXRRenderTexture mPostEffectRenderTextureB = null;
    private  SXRRenderTarget mEyeCaptureRenderTarget = null;

    protected int  mSampleCount;
    protected int mWidth, mHeight;
    protected SXRRenderTarget[] mLeftEyeRenderTarget = new SXRRenderTarget[3];
    protected SXRRenderTarget [] mRightEyeRenderTarget = new SXRRenderTarget[3];
    protected SXRRenderTarget [] mMultiviewRenderTarget = new SXRRenderTarget[3];


    private SXRRenderTexture mEyeCapturePostEffectRenderTextureA = null;
    private SXRRenderTexture mEyeCapturePostEffectRenderTextureB = null;

    SXRRenderBundle(SXRContext gvrContext) {
        mSXRContext = gvrContext;
        mShaderManager = new SXRShaderManager(gvrContext);

        final VrAppSettings appSettings = mSXRContext.getApplication().getAppSettings();
        mSampleCount = appSettings.getEyeBufferParams().getMultiSamples() < 0 ? 0
                : appSettings.getEyeBufferParams().getMultiSamples();
        if (mSampleCount > 1) {
            int maxSampleCount = SXRMSAA.getMaxSampleCount();
            if (mSampleCount > maxSampleCount && maxSampleCount > 1) {
                mSampleCount = maxSampleCount;
            }
        }
        mWidth = mSXRContext.getApplication().getAppSettings().getEyeBufferParams().getResolutionWidth();
        mHeight = mSXRContext.getApplication().getAppSettings().getEyeBufferParams().getResolutionHeight();
    }
    public void createRenderTarget(int index, SXRViewManager.EYE eye, SXRRenderTexture renderTexture){

        if(eye == SXRViewManager.EYE.MULTIVIEW)
            mMultiviewRenderTarget[index] = new SXRRenderTarget(renderTexture, mSXRContext.getMainScene(), true);

        else if(eye == SXRViewManager.EYE.LEFT)
            mLeftEyeRenderTarget[index] = new SXRRenderTarget(renderTexture, mSXRContext.getMainScene());
        else
            mRightEyeRenderTarget[index] = new SXRRenderTarget(renderTexture, mLeftEyeRenderTarget[index]);
    }


    public void createRenderTargetChain(boolean use_multiview){

        for(int i=0; i< 3; i++){
            if(use_multiview){
                addRenderTarget(mMultiviewRenderTarget[i].getNative(), SXRViewManager.EYE.MULTIVIEW.ordinal(), i);
            }
            else {
                addRenderTarget(mLeftEyeRenderTarget[i].getNative(), SXRViewManager.EYE.LEFT.ordinal(), i);
                addRenderTarget(mRightEyeRenderTarget[i].getNative(), SXRViewManager.EYE.RIGHT.ordinal(), i);
            }

        }
        for(int i=0; i< 3; i++){
            int index = (i+1) % 3;
            if(use_multiview)
                mMultiviewRenderTarget[i].attachRenderTarget(mMultiviewRenderTarget[index]);
            else {
                mLeftEyeRenderTarget[i].attachRenderTarget(mLeftEyeRenderTarget[index]);
                mRightEyeRenderTarget[i].attachRenderTarget(mRightEyeRenderTarget[index]);
            }
        }
    }
    public SXRRenderTarget getEyeCaptureRenderTarget() {
        if(mEyeCaptureRenderTarget == null){
            mEyeCaptureRenderTarget  = new SXRRenderTarget(new SXRRenderTexture(mSXRContext, mWidth, mHeight, mSampleCount), mSXRContext.getMainScene());
            mEyeCaptureRenderTarget.setCamera(mSXRContext.getMainScene().getMainCameraRig().getCenterCamera());
        }
        return  mEyeCaptureRenderTarget;
    }
    void beginRendering(int bufferIdx, SXRViewManager.EYE eye) {
        getRenderTexture(bufferIdx,eye).beginRendering();
    }
    public SXRRenderTexture getRenderTexture(int bufferIdx, SXRViewManager.EYE eye){

        if (eye == SXRViewManager.EYE.LEFT)
            return mLeftEyeRenderTarget[bufferIdx].getTexture();
        if (eye == SXRViewManager.EYE.RIGHT)
            return mRightEyeRenderTarget[bufferIdx].getTexture();
        if (eye == SXRViewManager.EYE.MULTIVIEW)
            return mMultiviewRenderTarget[bufferIdx].getTexture();

        Log.e("SXRRendleBundle", "incorrect Eye type");
        return null;
    }
    void endRendering(int bufferIdx, SXRViewManager.EYE eye) {
        getRenderTexture(bufferIdx,eye).endRendering();
    }

    public SXRRenderTarget getRenderTarget(SXRViewManager.EYE eye, int index){
        if(eye == SXRViewManager.EYE.LEFT)
            return mLeftEyeRenderTarget[index];
        if(eye == SXRViewManager.EYE.RIGHT)
            return mRightEyeRenderTarget[index];

        return mMultiviewRenderTarget[index];
    }
    public SXRRenderTexture getEyeCapturePostEffectRenderTextureA(){
        if(mEyeCapturePostEffectRenderTextureA == null)
            mEyeCapturePostEffectRenderTextureA  = new SXRRenderTexture(mSXRContext, mWidth , mHeight, mSampleCount, 1);
        return mEyeCapturePostEffectRenderTextureA;
    }

    public SXRRenderTexture getEyeCapturePostEffectRenderTextureB(){
        if(mEyeCapturePostEffectRenderTextureB == null)
            mEyeCapturePostEffectRenderTextureB = new SXRRenderTexture(mSXRContext, mWidth , mHeight, mSampleCount, 1);

        return mEyeCapturePostEffectRenderTextureB;
    }
    public SXRShaderManager getShaderManager() {
        return mShaderManager;
    }

    public SXRRenderTexture getPostEffectRenderTextureA() {
        if(mPostEffectRenderTextureA == null)
            mPostEffectRenderTextureA = new SXRRenderTexture(mSXRContext, mWidth , mHeight, mSampleCount, mSXRContext.getApplication().getAppSettings().isMultiviewSet() ? 2 : 1);
        return mPostEffectRenderTextureA;
    }

    public SXRRenderTexture getPostEffectRenderTextureB() {
        if(mPostEffectRenderTextureB == null)
            mPostEffectRenderTextureB = new SXRRenderTexture(mSXRContext, mWidth , mHeight, mSampleCount, mSXRContext.getApplication().getAppSettings().isMultiviewSet() ? 2 : 1);

        return mPostEffectRenderTextureB;
    }

    public void addRenderTarget(SXRRenderTarget target, SXRViewManager.EYE eye, int index)
    {
        addRenderTarget(target.getNative(), eye.ordinal(), index);
    }

    protected static native long getRenderTextureNative(long ptr);
    protected static native void addRenderTarget(long renderTarget, int eye, int index);
}
