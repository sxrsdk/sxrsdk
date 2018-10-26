
/* Copyright 2016 Samsung Electronics Co., LTD
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

package com.samsungxr.x3d;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXRExternalScene;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.ISensorEvents;
import com.samsungxr.SensorEvent;
import com.samsungxr.io.SXRControllerType;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.io.SXRInputManager;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.nodes.SXRTextViewNode;
import com.samsungxr.nodes.SXRViewNode;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.Threads;
import org.joml.Vector3f;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 * @author m1.williams
 *         AnchorImplementation handles the functions specific to X3D's Anchor node.
 *         An Anchor is a subclass of the TouchSensor (though only 'isActive' is
 *         implemented, there is no 'isOver').
 *         The Anchor can either:
 *          Animate to a new Viewpoint if the url begins with a # followed by the Viewpoint's DEF name
 *          Go to a new X3D scene if the url ends with ".x3d"
 *          Or open up a new web page.
 */

public class AnchorImplementation {

    private static final String TAG = AnchorImplementation.class.getSimpleName();

    private static final String IS_OVER = "isOver";
    private static final String Is_ACTIVE = "isActive";

    private SXRContext gvrContext = null;
    private SXRNode root = null;
    private Vector<Viewpoint> viewpoints = new Vector<Viewpoint>();

    private PerFrameWebViewControl perFrameWebViewControl = new PerFrameWebViewControl();

    private SXRSphereNode gvrScaleObject = null;
    private final float[] scaleControlInitPosition = {-2, 1.5f, 0};

    private Vector3f translationObjectTranslationLocal = new Vector3f();
    private Vector3f translationObjectTranslationGlobal = new Vector3f();
    // when the tranlsation icon is clicked on, saves the initial value for the camera's look at
    private float[] initialCameralookAt = new float[3];

    private final float[] webViewDimensions = {8.0f, 6.0f}; //web view is width x height
    private final Vector3f cubeUISize = new Vector3f(webViewDimensions[0], .5f, .2f);
    private final float[] cubeUIPosition = {0, (webViewDimensions[1] + cubeUISize.y)/2.0f, -cubeUISize.z/2.0f}; // center, above the web page

    private SXRTextViewNode gvrTextExitObject = null;
    private SXRTextViewNode gvrTextTranslationObject = null;
    private SXRTextViewNode gvrTextScaleObject = null;
    private SXRTextViewNode gvrTextRotateObject = null;
    private final float[] textExitPosition = {1.5f, 0, .125f};
    private final float[] textScalePosition = {-1.5f, 0, .125f};
    private final float[] textTranslatePosition = {-.5f, 0, .125f};
    private final float[] textRotatePosition = {.5f, 0, .125f};
    private float[] webPagePlusUIPosition = {1.0f, -1.0f, -5.0f};
    private float[] webPagePlusUIScale = {1, 1}; // retains current Web page Scale.  z is always 1
    private float[] beginWebPagePlusUIScale = {1, 1}; // retains the beginning Web page Scale when first clicked


    private float[] beginUIClickPos = {0, 0, 0}; // where we clicked on UI to control the web page U.I.
    private float[] diffWebPageUIClick = {0, 0, 0}; // difference between beginUIClickPos and webPagePlusUIPosition

    private float[] initialHitPoint = new float[3];


    private SXRNode webPagePlusUINode = null;

    private final int textColorDefault = Color.BLACK;
    private final int textColorIsOver   = Color.LTGRAY;
    private final int textColorIsActive = Color.WHITE;
    private final int textColorBackground = Color.CYAN;

    private final String TEXT_EXIT_NAME = "exit";
    private final String TEXT_TRANSLATE_NAME = "translate";
    private final String TEXT_ROTATE_NAME = "rotate";
    private final String TEXT_SCALE_NAME = "scale";

    private SXRDrawFrameListener mOnDrawFrame = null;
    private boolean webViewTranslation = false;
    private boolean webViewScale = false;
    private WebView gvrWebView = null;

    private String webPageContent = "";

    // temporary boolean to switch between just the web page display with
    //    UI controls or just click on the web page to close it all.
    private final boolean useWebPageTranformControls = false;
    private boolean webPageActive = false;
    private boolean webPageClosed = false;


    public AnchorImplementation(SXRContext gvrContext, SXRNode root, Vector<Viewpoint> viewpoints ) {
        this.gvrContext = gvrContext;
        this.root = root;
        this.viewpoints = viewpoints;
    }

    /**
     * AnchorInteractivity() called when an object associated with an Anchor tag
     * is clicked.  There are 3 possible outcomes:
     *    A web page is opened, we go to a new .x3d scene, or we go to a new Camera (Viewpoint)
     */
    public void AnchorInteractivity(InteractiveObject interactiveObject) {
        // The interactiveObject contains an Anchor tag.
        // Could be a link to another x3d page, <Viewpoint> (camera) or web page

        final InteractiveObject interactiveObjectFinal = interactiveObject;

        interactiveObject.getSensor().getOwnerObject().forAllDescendants(
                new SXRNode.SceneVisitor()
                {
                    public boolean visit (SXRNode obj)
                    {
                        obj.attachCollider(new SXRMeshCollider(gvrContext, true));
                        return true;
                    }
                });

        interactiveObject.getSensor().addISensorEvents(new ISensorEvents() {
            boolean isActiveDone = false;
            boolean newSceneLoaded = false;

            @Override
            public void onSensorEvent(SensorEvent event) {
                // Anchor event could either call for new .x3d file, new camera location
                // or to load a web page
                if (event.isActive()) {
                    if (!isActiveDone) {
                        isActiveDone = true;
                        String url = interactiveObjectFinal.getSensor().getAnchorURL();

                        // Get rid of any single or double quotes surrounding the filename.
                        // this happens when the url = '"filename.x3d"' for example
                        if ( (url.indexOf("\"") == 0) || (url.indexOf("\'") == 0) ) {
                            url = url.substring(1, url.length());
                        }
                        if ( (url.indexOf("\"") == (url.length()-1)) || (url.indexOf("\'") == (url.length()-1)) ) {
                            url = url.substring(0, url.length()-1);
                        }
                        if (url.toLowerCase().endsWith(".x3d")) {
                            if ( !newSceneLoaded ) {
                                // Go to another X3D scene
                                newSceneLoaded = true;
                                SXRExternalScene gvrExternalScene = new SXRExternalScene(gvrContext, url, true);
                                SXRNode gvrNodeAnchor = new SXRNode(gvrContext);
                                gvrNodeAnchor.attachComponent( gvrExternalScene );
                                boolean load = gvrExternalScene.load(gvrContext.getMainScene());
                                if (!load) Log.e(TAG, "Error loading new X3D scene " + url);
                                else {
                                    interactiveObjectFinal.getSensor().disable();
                                    Log.e(TAG, "New X3D scene " + url + " loaded.");
                                }

                            }
                        }  // end if .x3d file
                        else if (url.startsWith("#")) {
                            // go to another Viewpoint
                            SetNewViewpoint(url);
                        }  // end if new Viewpoint selected
                        else if ((url.endsWith(".xml")) || (url.endsWith(".rss"))) {
                            // Web page with XML, RSS data
                            if (!webPageActive && !webPageClosed) {
                                webPageActive = true;
                                webPageContent = "<HTML><BODY><FONT SIZE=7>Issue pressenting RSS / XML page</FONT></BODY></HTML>";
                                final String urlFinal = url;
                                Threads.spawn(new Runnable() {

                                    public void run() {
                                        File file = null;
                                        FileInputStream fileInputStream = null;
                                        try {
                                            Context context = gvrContext.getContext();
                                            file = gvrContext.getAssetLoader().downloadFile(context, urlFinal);
                                            fileInputStream = new FileInputStream(file);

                                            // Parse the XML/RSS file
                                            SAXParserFactory factory = SAXParserFactory.newInstance();
                                            SAXParser saxParser = factory.newSAXParser();
                                            UserHandler userhandler = new UserHandler();

                                            // CSS definitions will make XML/RSS data look nice
                                            webPageContent = "<html><body>";
                                            String css = "<style>" +
                                                    "#title { font-size: 60px; font-family: Arial; }" +
                                                    "#desc { font-size: 50px; font-family: Arial; }" +
                                                    "#link { font-size: 60px; font-family: Arial; }" +
                                                    "</style>";
                                            webPageContent += css;
                                            saxParser.parse(fileInputStream, userhandler);
                                            webPageContent += "</body></html>";

                                            LaunchWebPage(interactiveObjectFinal, urlFinal);
                                        } catch (FileNotFoundException e) {
                                            Log.e(TAG, "File " + urlFinal + " not found: " + e);
                                        } catch (IOException e) {
                                            Log.e(TAG, "File " + urlFinal + " IOException: " + e);
                                        } catch (Exception e) {
                                            Log.e(TAG, "File " + urlFinal + " exception: " + e);
                                        } finally {
                                            if (fileInputStream != null) {
                                                try {
                                                    fileInputStream.close();
                                                } catch (IOException e) {

                                                }
                                            }
                                            if (file != null) {
                                                file.delete();
                                            }
                                        }
                                    }
                                });
                            } // end if !webPageActive
                        } // end if rss or xml data

                        else {
                            if ( !webPageActive  && !webPageClosed) {
                                // Launch a web page window
                                LaunchWebPage(interactiveObjectFinal, url);
                                webPageActive = true;
                            }
                        }  // end launching a Web Page
                    }  // end if isActiveDone = false
                    else {
                        isActiveDone = false;
                    }
                }
                else if (!event.isActive() ) {
                    //Prevents closing the web page from invoking a new page on the item behind it.
                    webPageClosed = false;
                }
                else if (event.isOver()) {
                    SXRNode sensorObj = interactiveObjectFinal.getSensor().getOwnerObject();
                    if (sensorObj != null) {
                        SXRNode sensorObj2 = sensorObj.getChildByIndex(0);
                        if (sensorObj2 != null) {
                            SXRCameraRig mainCameraRig = gvrContext.getMainScene().getMainCameraRig();
                            float[] cameraPosition = new float[3];
                            cameraPosition[0] = mainCameraRig.getTransform().getPositionX();
                            cameraPosition[1] = mainCameraRig.getTransform().getPositionY();
                            cameraPosition[2] = mainCameraRig.getTransform().getPositionZ();
                        }
                    }
                } else {
                    SXRNode sensorObj = interactiveObjectFinal.getSensor().getOwnerObject();
                    if (sensorObj != null) {
                        SXRNode sensorObj2 = sensorObj.getChildByIndex(0);
                        if (sensorObj2 != null) {
                            SXRCameraRig mainCameraRig = gvrContext.getMainScene().getMainCameraRig();
                            float[] cameraPosition = new float[3];
                            cameraPosition[0] = mainCameraRig.getTransform().getPositionX();
                            cameraPosition[1] = mainCameraRig.getTransform().getPositionY();
                            cameraPosition[2] = mainCameraRig.getTransform().getPositionZ();
                        }
                    }
                }
            }  // end of onSensorEvent
        });  // end of AddISensorEvent

    }  //  end AnchorImplementation class


    private void SetNewViewpoint(String url) {
        Viewpoint vp = null;
        // get the name without the '#' sign
        String vpURL = url.substring(1, url.length());
        for (Viewpoint viewpoint : viewpoints) {
            if ( viewpoint.getName().equalsIgnoreCase(vpURL) ) {
                vp = viewpoint;
            }
        }
        if ( vp != null ) {
            // found the Viewpoint matching the url
            SXRCameraRig mainCameraRig = gvrContext.getMainScene().getMainCameraRig();
            float[] cameraPosition = vp.getPosition();
            mainCameraRig.getTransform().setPosition( cameraPosition[0], cameraPosition[1], cameraPosition[2] );

            // Set the Gaze controller position which is where the pick ray
            // begins in the direction of camera.lookt()
            SXRCursorController gazeController = null;
            SXRInputManager inputManager = gvrContext.getInputManager();

            List<SXRCursorController> controllerList = inputManager.getCursorControllers();

            for(SXRCursorController controller: controllerList){
                if(controller.getControllerType() == SXRControllerType.GAZE);
                {
                    gazeController = controller;
                    break;
                }
            }
            if ( gazeController != null) {
                gazeController.setOrigin(cameraPosition[0], cameraPosition[1], cameraPosition[2]);
            }
        }
        else {
            Log.e(TAG, "Viewpoint named " + vpURL + " not found (defined).");
        }
    }  //  end SetNewViewpoint


    private void LaunchWebPage(InteractiveObject interactiveObjectFinal, String url) {
        if (webPagePlusUINode == null) {
            final String urlFinal = url;
            final SXRNode gvrNodeAnchor = interactiveObjectFinal.getSensor().getOwnerObject();
            gvrContext.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // Launch a new WebView window and place the web page there.
                    gvrWebView = new WebView(gvrContext.getActivity());
                    gvrWebView.setInitialScale(100);
                    gvrWebView.setLayoutParams(new ViewGroup.LayoutParams(1600, 1200));
                    WebSettings webSettings = gvrWebView.getSettings();
                    webSettings.setJavaScriptEnabled(true);


                    if ((urlFinal.endsWith(".xml")) || (urlFinal.endsWith(".rss"))) {
                        if (gvrWebView != null) gvrWebView.loadData(webPageContent, "text/html", null);
                    } // end if rss or xml data
                    else {
                        // Open a Web page
                        gvrWebView.setWebViewClient(new WebViewClient() {
                            //TODO: replace for depricated code with:
                            // onReceivedError (WebView view, WebResourceRequest request, WebResourceError error)
                            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                                Log.e(TAG, "   setWebViewClient Error ");
                                Log.e(TAG, "   errorCode " + errorCode + "; description " + description + "; url " + failingUrl);
                            }
                        });
                        if (gvrWebView != null) gvrWebView.loadUrl(urlFinal);
                    }  //  end if url data - a web address

                    if (gvrWebView != null) {

                        SXRViewNode gvrWebViewNode = new SXRViewNode(gvrContext,
                                gvrWebView, webViewDimensions[0], webViewDimensions[1]);
                        gvrWebViewNode.setName("Web View");

                        gvrWebViewNode.getRenderData().getMaterial().setOpacity(1.0f);
                        gvrWebViewNode.getTransform().setPosition(0.0f, 0.0f, 0.0f);
                        gvrWebViewNode.getRenderData().setRenderMask(0);
                        gvrWebViewNode.getRenderData().setRenderMask(
                                SXRRenderData.SXRRenderMaskBit.Left
                                        | SXRRenderData.SXRRenderMaskBit.Right);

                        if (useWebPageTranformControls) WebPageTranformControls( gvrWebViewNode, gvrNodeAnchor, urlFinal);
                        else WebPageCloseOnClick( gvrWebViewNode, gvrNodeAnchor, urlFinal);

                    }
                }  // end run
            });
        }  // end if webPagePlusUINode == null
    }  //  end LaunchWebPage


    // Display web page, but no user interface - close
    private void WebPageCloseOnClick(SXRViewNode gvrWebViewNode, SXRNode gvrNodeAnchor, String url) {
        final String urlFinal = url;

        webPagePlusUINode = new SXRNode(gvrContext);
        webPagePlusUINode.getTransform().setPosition(webPagePlusUIPosition[0], webPagePlusUIPosition[1], webPagePlusUIPosition[2]);

        SXRScene mainScene = gvrContext.getMainScene();

        Sensor webPageSensor = new Sensor(urlFinal, Sensor.Type.TOUCH, gvrWebViewNode, true);
        final SXRNode gvrNodeAnchorFinal = gvrNodeAnchor;
        final SXRNode gvrWebViewNodeFinal = gvrWebViewNode;
        final SXRNode webPagePlusUINodeFinal = webPagePlusUINode;

        webPagePlusUINodeFinal.addChildObject(gvrWebViewNodeFinal);
        gvrNodeAnchorFinal.addChildObject(webPagePlusUINodeFinal);


        webPageSensor.addISensorEvents(new ISensorEvents() {
            boolean uiObjectIsActive = false;
            boolean clickDown = true;

            @Override
            public void onSensorEvent(SensorEvent event) {
                if (event.isActive()) {
                    clickDown = !clickDown;
                    if (clickDown) {
                        // Delete the WebView page
                        gvrNodeAnchorFinal.removeChildObject(webPagePlusUINodeFinal);
                        webPageActive = false;
                        webPagePlusUINode = null;
                        webPageClosed = true;  // Make sure click up doesn't open web page behind it
                    }
                }
            }
        });
    }  //  end WebPageCloseOnClick


    // When we implement controls for dragging, scaling, closing, etc. a WebView
    private void WebPageTranformControls(SXRViewNode gvrWebViewNode, SXRNode gvrNodeAnchor, String url) {
        final String urlFinal = url;
        SXRCubeNode gvrUICubeNode = new SXRCubeNode(gvrContext, true, cubeUISize);
        gvrUICubeNode.getTransform().setPosition(cubeUIPosition[0], cubeUIPosition[1], cubeUIPosition[2]);
        gvrUICubeNode.getRenderData().getMaterial().setDiffuseColor(.3f, .5f, .7f, 1);

        // Add the icons to close, scale, translate and rotate
        gvrTextExitObject = new SXRTextViewNode(gvrContext, 0.5f, 0.45f, " x ");
        gvrTextExitObject.setName(TEXT_EXIT_NAME);
        gvrTextExitObject.setTextColor(textColorDefault);
        gvrTextExitObject.setBackgroundColor(textColorBackground);
        gvrTextExitObject.getTransform().setPosition(textExitPosition[0], textExitPosition[1], textExitPosition[2]);
        gvrUICubeNode.addChildObject(gvrTextExitObject);
        gvrTextTranslationObject = new SXRTextViewNode(gvrContext, 0.5f, 0.45f, " t ");
        gvrTextTranslationObject.setName(TEXT_TRANSLATE_NAME);
        gvrTextTranslationObject.setTextColor(textColorDefault);
        gvrTextTranslationObject.setBackgroundColor(textColorBackground);
        gvrTextTranslationObject.getTransform().setPosition(textTranslatePosition[0], textTranslatePosition[1], textTranslatePosition[2]);
        gvrUICubeNode.addChildObject(gvrTextTranslationObject);

        gvrTextRotateObject = new SXRTextViewNode(gvrContext, 0.5f, 0.45f, " r ");
        gvrTextRotateObject.setName(TEXT_ROTATE_NAME);
        gvrTextRotateObject.setTextColor(textColorDefault);
        gvrTextRotateObject.setBackgroundColor(textColorBackground);
        gvrTextRotateObject.getTransform().setPosition(textRotatePosition[0], textRotatePosition[1], textRotatePosition[2]);
        gvrUICubeNode.addChildObject(gvrTextRotateObject);

        gvrTextScaleObject = new SXRTextViewNode(gvrContext, 0.5f, 0.45f, " s ");
        gvrTextScaleObject.setName(TEXT_SCALE_NAME);
        gvrTextScaleObject.setTextColor(textColorDefault);
        gvrTextScaleObject.setBackgroundColor(textColorBackground);
        gvrTextScaleObject.getTransform().setPosition(textScalePosition[0], textScalePosition[1], textScalePosition[2]);
        gvrUICubeNode.addChildObject(gvrTextScaleObject);

        // Currently req to show an object dynamically added
        webPagePlusUINode = new SXRNode(gvrContext);
        webPagePlusUINode.getTransform().setPosition(webPagePlusUIPosition[0], webPagePlusUIPosition[1], webPagePlusUIPosition[2]);

        // Set up sensor for the U.I.
        Sensor uibObjectSensor = new Sensor(urlFinal, Sensor.Type.TOUCH, gvrUICubeNode, true);
        final SXRNode gvrWebViewNodeFinal = gvrWebViewNode;
        final SXRNode gvrUICubeNodeFinal = gvrUICubeNode;
        final SXRNode webPagePlusUINodeFinal = webPagePlusUINode;

        final SXRNode gvrNodeAnchorFinal = gvrNodeAnchor;

        webPagePlusUINodeFinal.addChildObject(gvrWebViewNodeFinal);
        webPagePlusUINodeFinal.addChildObject(gvrUICubeNodeFinal);
        gvrNodeAnchorFinal.addChildObject(webPagePlusUINodeFinal);

        uibObjectSensor.addISensorEvents(new ISensorEvents() {
            boolean uiObjectIsActive = false;
            boolean clickDown = false;

            @Override
            public void onSensorEvent(SensorEvent event) {
                float[] hitLocation = event.getPickedObject().getHitLocation();
                float hitX = hitLocation[0];
                if (event.isActive()) {
                    clickDown = !clickDown;
                    if (clickDown) {
                        // only go through once even if object is clicked a long time
                        uiObjectIsActive = !uiObjectIsActive;
                        if (uiObjectIsActive) {
                            if (hitX > cubeUISize.x / 4.0f) {
                                // Delete the WebView page, control currently far right of web page
                                gvrContext.unregisterDrawFrameListener(mOnDrawFrame);
                                if (gvrTextScaleObject != null) {
                                    gvrWebViewNodeFinal.removeChildObject(gvrTextScaleObject);
                                }
                                if (gvrTextRotateObject != null) {
                                    gvrWebViewNodeFinal.removeChildObject(gvrTextRotateObject);
                                }
                                if (gvrTextTranslationObject != null) {
                                    gvrWebViewNodeFinal.removeChildObject(gvrTextTranslationObject);
                                }
                                gvrTextExitObject = null;
                                gvrTextScaleObject = null;
                                gvrTextRotateObject = null;
                                gvrTextTranslationObject = null;
                                gvrNodeAnchorFinal.removeChildObject(webPagePlusUINodeFinal);
                                webViewTranslation = false;
                                webViewScale = false;
                                gvrWebView = null;
                                webPagePlusUINode = null;
                                webPageActive = false;
                            } else if (hitX > 0) {
                                // TODO: Rotate the web view, control currently right side of control bar
                                webViewTranslation = false;
                            } else if (hitX > -cubeUISize.x / 4.0f) {
                                // Translate the web view, control currently left side of control bar
                                TranslationControl(gvrWebViewNodeFinal);
                            }   // end transltion web window, hit between 0 and .25,
                            else {
                                // Scale the web view, control currently in upper right corner
                                webViewTranslation = false;
                                ScaleControl(gvrWebViewNodeFinal, gvrNodeAnchorFinal, urlFinal);
                            }  // end scaling web window, hit between -2 and -1,
                        } else {  // uiObjectIsActive is false
                            if (mOnDrawFrame != null)
                                // wrap up any lose ends closing the web page.
                                gvrContext.unregisterDrawFrameListener(mOnDrawFrame);
                            if (hitX > 0) {
                                // TODO: Stop Rotating the web page
                            } else if (hitX > -cubeUISize.x / 4.0f) {
                                // Stop translating the web page
                                if (gvrTextTranslationObject != null) {
                                    gvrTextTranslationObject.setTextColor(textColorIsOver);
                                }
                                webViewTranslation = false;
                            }   // end transltion web window, hit between 0 and .25,
                            else if (hitX < -cubeUISize.x / 4.0f) {
                                // Stop scaling the web page
                                if (gvrTextScaleObject != null) {
                                    gvrTextScaleObject.setTextColor(textColorIsOver);
                                }
                                webViewScale = false;
                            }  // end scaling web window, hit between -2 and -1,
                        }
                    }
                } else if (event.isOver() && !uiObjectIsActive) {
                    // highlight the icons to give visual cue to users
                    if (hitX > cubeUISize.x / 4.0f) {
                        if (gvrTextExitObject != null)
                            gvrTextExitObject.setTextColor(textColorIsOver);
                        if (gvrTextRotateObject != null)
                            gvrTextRotateObject.setTextColor(textColorDefault);
                        if (gvrTextTranslationObject != null)
                            gvrTextTranslationObject.setTextColor(textColorDefault);
                        if (gvrTextScaleObject != null)
                            gvrTextScaleObject.setTextColor(textColorDefault);
                        webViewTranslation = false;
                        webViewScale = false;
                    } else if (hitX > 0) {
                        if (gvrTextExitObject != null) gvrTextExitObject.setTextColor(textColorDefault);
                        if (gvrTextRotateObject != null) gvrTextRotateObject.setTextColor(textColorIsOver);
                        if (gvrTextTranslationObject != null) gvrTextTranslationObject.setTextColor(textColorDefault);
                        if (gvrTextScaleObject != null) gvrTextScaleObject.setTextColor(textColorDefault);
                        webViewTranslation = false;
                        webViewScale = false;
                    }  // end scaling web window, hit between 0 and .25,
                    else if (hitX > -cubeUISize.x / 4.0f) {
                        if (gvrTextExitObject != null) gvrTextExitObject.setTextColor(textColorDefault);
                        if (gvrTextRotateObject != null) gvrTextRotateObject.setTextColor(textColorDefault);
                        if (gvrTextTranslationObject != null) gvrTextTranslationObject.setTextColor(textColorIsOver);
                        if (gvrTextScaleObject != null) gvrTextScaleObject.setTextColor(textColorDefault);
                        webViewTranslation = false;
                        webViewScale = false;
                    } else {
                        if (gvrTextExitObject != null) gvrTextExitObject.setTextColor(textColorDefault);
                        if (gvrTextRotateObject != null) gvrTextRotateObject.setTextColor(textColorDefault);
                        if (gvrTextTranslationObject != null) gvrTextTranslationObject.setTextColor(textColorDefault);
                        if (gvrTextScaleObject != null) gvrTextScaleObject.setTextColor(textColorIsOver);
                        webViewTranslation = false;
                        webViewScale = false;
                    }
                } else if (!event.isOver() && !uiObjectIsActive) {
                    if (gvrTextExitObject != null) gvrTextExitObject.setTextColor(textColorDefault);
                    if (gvrTextRotateObject != null)
                        gvrTextRotateObject.setTextColor(textColorDefault);
                    if (gvrTextTranslationObject != null)
                        gvrTextTranslationObject.setTextColor(textColorDefault);
                    if (gvrTextScaleObject != null) gvrTextScaleObject.setTextColor(textColorDefault);
                    webViewTranslation = false;
                    webViewScale = false;
                }
            }
        });
    }  //  end WebPageTranformControls

    private void TranslationControl(SXRNode gvrWebViewNodeFinal) {
        SXRScene mainScene = gvrContext.getMainScene();
        SXRCameraRig gvrCameraRig = mainScene.getMainCameraRig();
        initialCameralookAt = gvrCameraRig.getLookAt();

        gvrWebViewNodeFinal.getTransform().getModelMatrix4f().getTranslation(translationObjectTranslationGlobal);
        gvrWebViewNodeFinal.getTransform().getLocalModelMatrix4f().getTranslation(translationObjectTranslationLocal);

        if (gvrTextTranslationObject != null) {
            gvrTextTranslationObject.setTextColor(textColorIsActive);
        }
        webViewTranslation = true;

        for ( int i = 0; i < 3; i++ ) {
            if (i == 2) beginUIClickPos[i] = webPagePlusUIPosition[i] - cubeUIPosition[i];
            else beginUIClickPos[i] = webPagePlusUIPosition[i] + cubeUIPosition[i] + textTranslatePosition[i];
            diffWebPageUIClick[i] = beginUIClickPos[i] - webPagePlusUIPosition[i];
        }

        mOnDrawFrame = new DrawFrame();
        gvrContext.registerDrawFrameListener(mOnDrawFrame);
    }  //  end TranslationControl



    private void ScaleControl(SXRNode gvrWebViewNodeFinal, SXRNode  gvrNodeAnchorFinal, String urlFinal) {
        SXRScene mainScene = gvrContext.getMainScene();
        SXRCameraRig gvrCameraRig = mainScene.getMainCameraRig();
        initialCameralookAt = gvrCameraRig.getLookAt();

        gvrWebViewNodeFinal.getTransform().getModelMatrix4f().getTranslation(translationObjectTranslationGlobal);
        gvrWebViewNodeFinal.getTransform().getLocalModelMatrix4f().getTranslation(translationObjectTranslationLocal);

        if (gvrTextScaleObject != null) {
            gvrTextScaleObject.setTextColor(textColorIsActive);
        }
        webViewScale = true;

        for (int i = 0; i < 3; i++ ) {
            initialHitPoint[i] = (initialCameralookAt[i] * translationObjectTranslationGlobal.z) / initialCameralookAt[2];
        }
        beginWebPagePlusUIScale[0] = webPagePlusUIScale[0];
        beginWebPagePlusUIScale[1] = webPagePlusUIScale[1];

        mOnDrawFrame = new DrawFrame();
        gvrContext.registerDrawFrameListener(mOnDrawFrame);
    }  //  end ScaleControl


    private class PerFrameWebViewControl {

        final void onDrawFrame(float frameTime) {
            SXRScene mainScene = gvrContext.getMainScene();
            SXRCameraRig gvrCameraRig = mainScene.getMainCameraRig();
            float[] currentCameraLookAt = gvrCameraRig.getLookAt();
            if ( webViewScale ) {
                float zFactor = initialCameralookAt[2] / currentCameraLookAt[2];
                webPagePlusUIScale[0] = (currentCameraLookAt[0] / initialCameralookAt[0]) * zFactor * beginWebPagePlusUIScale[0];
                webPagePlusUIScale[1] = (currentCameraLookAt[1] / initialCameralookAt[1]) * zFactor * beginWebPagePlusUIScale[1];

                float[] currentHitPoint = new float[3];
                for (int i = 0; i < 3; i++ ) {
                    currentHitPoint[i] = (currentCameraLookAt[i] * translationObjectTranslationGlobal.z) / currentCameraLookAt[2];
                }

                if (webPagePlusUIScale[0] < 1 ) webPagePlusUIScale[0] = 1;
                else {
                    webPagePlusUIScale[0] = ((webPagePlusUIScale[0] - 1) / 2.0f) + 1;
                }
                if (webPagePlusUINode != null) webPagePlusUINode.getTransform().setScale(webPagePlusUIScale[0], webPagePlusUIScale[0], 1);

            }  // end if scale
            else if ( webViewTranslation ) {

                float[] transitionClickPt = {0, 0, translationObjectTranslationGlobal.z};
                transitionClickPt[0] = currentCameraLookAt[0] * translationObjectTranslationGlobal.z/currentCameraLookAt[2];
                transitionClickPt[1] = currentCameraLookAt[1] * translationObjectTranslationGlobal.z/currentCameraLookAt[2];
                for (int i = 0; i < 3; i++) {
                    transitionClickPt[i] -= diffWebPageUIClick[i];
                }
                if (webPagePlusUINode != null) webPagePlusUINode.getTransform().setPosition( transitionClickPt[0], transitionClickPt[1], transitionClickPt[2]);
            }
        }  //  end onDrawFrame
    }  //  end private class PerFrameScripting

    private final class DrawFrame implements SXRDrawFrameListener {
        @Override
        public void onDrawFrame(float frameTime) {
            perFrameWebViewControl.onDrawFrame(frameTime);
        }
    }

    /**
     * Prases RSS - XML data that is part of a web page
     * Inserts CSS DIV tags.
     */
    class UserHandler extends DefaultHandler {

        boolean title = false;
        boolean description = false;
        boolean link = false;
        String attributeValue;
        String titleString = "";
        String xmlElement;

        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("title")) {
                title = true;
            }
            else if (qName.equalsIgnoreCase("description")) {
                description = true;
            }
            else if (qName.equalsIgnoreCase("link")) {
                link = true;
            }
            else if (qName.equalsIgnoreCase("media:content")) {
                String url = "";
                int height = 96;
                int width = 128;
                String type = "";
                attributeValue = attributes.getValue("url");
                if (attributeValue != null) {
                    attributeValue = attributeValue.replace("\"", ""); // remove double and
                    // single quotes
                    attributeValue = attributeValue.replace("\'", "");
                    url = attributeValue;
                }
                attributeValue = attributes.getValue("height");
                if (attributeValue != null) {
                    try {
                        height = Integer.parseInt(attributeValue);
                    }
                    catch (Exception e) {};
                }
                attributeValue = attributes.getValue("width");
                if (attributeValue != null) {
                    try {
                        width = Integer.parseInt(attributeValue);
                    }
                    catch (Exception e) {};
                }
                attributeValue = attributes.getValue("type");
                if (attributeValue != null) {
                    try {
                        type = attributeValue;
                    }
                    catch (Exception e) {};
                }
                webPageContent += "<img src=\"" + url + "\"><P>";

            }
        }  //  end startElement

        public void endElement(String uri, String localName, String qName)
                throws SAXException {
        }  //  end endElement

        public void characters(char ch[], int start, int length) throws SAXException {
            if (title) {
                titleString = new String(ch, start, length);
                //webPageContent += "<DIV id=\"title\">" + titleString + "</DIV>";
                title = false;
            }
            else if (description) {
                xmlElement = new String(ch, start, length);
                webPageContent += "<DIV id=\"desc\">" + xmlElement + "</DIV>";
                description = false;
            }
            else if (link) {
                xmlElement = new String(ch, start, length);
                webPageContent += "<DIV id=\"link\"><A HREF=\"" + xmlElement + "\">" + titleString + "</A></DIV>";
                link = false;
            }
        }  //  end characters prsing

    }  //  end UserHandler (XML parsing)

}  //  end AnchorInteractivity



