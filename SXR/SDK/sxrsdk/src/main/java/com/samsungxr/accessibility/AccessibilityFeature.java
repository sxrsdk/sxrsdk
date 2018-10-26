package com.samsungxr.accessibility;

/*
 * Copyright 2015 Samsung Electronics Co., LTD
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

import java.util.ArrayList;
import java.util.Locale;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.R;

import android.content.Context;
import android.media.AudioManager;

final class AccessibilityFeature {

    private String volumeUp;
    private String talkBack;
    private String disableTalkBack;
    private String volumeDown;
    private String zoomIn;
    private String zoomOut;
    private String invertedColors;
    private SXRAccessibilityManager managerFeatures;
    private AudioManager mAudioManager;
    private SXRContext mGvrContext;
    private static AccessibilityFeature instance;

    private AccessibilityFeature(SXRContext gvrContext) {
        mGvrContext = gvrContext;
        mAudioManager = (AudioManager) gvrContext.getActivity().getSystemService(Context.AUDIO_SERVICE);
        managerFeatures = new SXRAccessibilityManager(gvrContext);
    }

    public static synchronized AccessibilityFeature getInstance(SXRContext gvrContext) {
        if (instance == null) {
            instance = new AccessibilityFeature(gvrContext);
        }

        return instance;
    }

    /**
     * Load all string recognize.
     */
    private void loadCadidateString() {
        volumeUp = mGvrContext.getActivity().getResources().getString(R.string.volume_up);
        volumeDown = mGvrContext.getActivity().getResources().getString(R.string.volume_down);
        zoomIn = mGvrContext.getActivity().getResources().getString(R.string.zoom_in);
        zoomOut = mGvrContext.getActivity().getResources().getString(R.string.zoom_out);
        invertedColors = mGvrContext.getActivity().getResources().getString(R.string.inverted_colors);
        talkBack = mGvrContext.getActivity().getResources().getString(R.string.talk_back);
        disableTalkBack = mGvrContext.getActivity().getResources().getString(R.string.disable_talk_back);
    }

    /**
     * get the result speech recognize and find match in strings file.
     * 
     * @param speechResult
     */
    public void findMatch(ArrayList<String> speechResult) {

        loadCadidateString();

        for (String matchCandidate : speechResult) {

            Locale localeDefault = mGvrContext.getActivity().getResources().getConfiguration().locale;
            if (volumeUp.equals(matchCandidate)) {
                startVolumeUp();
                break;
            } else if (volumeDown.toLowerCase(localeDefault).equals(matchCandidate.toLowerCase(localeDefault))) {
                startVolumeDown();
                break;
            } else if (zoomIn.toLowerCase(localeDefault).equals(matchCandidate.toLowerCase(localeDefault))) {
                startZoomIn();
                break;
            } else if (zoomOut.toLowerCase(localeDefault).equals(matchCandidate.toLowerCase(localeDefault))) {
                startZoomOut();
                break;
            } else if (invertedColors.toLowerCase(localeDefault).equals(matchCandidate.toLowerCase(localeDefault))) {
                startInvertedColors();
                break;
            } else if (talkBack.toLowerCase(localeDefault).equals(matchCandidate.toLowerCase(localeDefault))) {
                enableTalkBack();
            } else if (disableTalkBack.toLowerCase(localeDefault).equals(matchCandidate.toLowerCase(localeDefault))) {
                disableTalkBack();
            }
        }
    }

    /**
     * find all accessibility object and set active true for enable talk back.
     */
    private void enableTalkBack() {
        SXRNode[] sceneObjects = mGvrContext.getMainScene().getWholeNodes();
        for (SXRNode sceneObject : sceneObjects) {
            if (sceneObject instanceof SXRAccessiblityObject)
                if (((SXRAccessiblityObject) sceneObject).getTalkBack() != null)
                    ((SXRAccessiblityObject) sceneObject).getTalkBack().setActive(true);
        }
    }

    /**
     * find all accessibility object and set active false for enable talk back.
     */
    private void disableTalkBack() {
        SXRNode[] sceneObjects = mGvrContext.getMainScene().getWholeNodes();
        for (SXRNode sceneObject : sceneObjects) {
            if (sceneObject instanceof SXRAccessiblityObject)
                if (((SXRAccessiblityObject) sceneObject).getTalkBack() != null)
                    ((SXRAccessiblityObject) sceneObject).getTalkBack().setActive(false);

        }
    }

    /**
     * Raise volume.
     */
    private void startVolumeUp() {
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    /**
     * Lower volume.
     */
    private void startVolumeDown() {
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    /**
     * Active zoom in
     */
    private void startZoomIn() {
        managerFeatures.getZoom().zoomIn(mGvrContext.getMainScene());
    }

    /**
     * Active zoom out
     */
    private void startZoomOut() {
        managerFeatures.getZoom().zoomOut(mGvrContext.getMainScene());
    }

    /**
     * Active inverter colors
     */
    private void startInvertedColors() {
        if (managerFeatures.getInvertedColors().isInverted()) {
            managerFeatures.getInvertedColors().turnOff(mGvrContext.getMainScene());
        } else {
            managerFeatures.getInvertedColors().turnOn(mGvrContext.getMainScene());
        }
    }

}
