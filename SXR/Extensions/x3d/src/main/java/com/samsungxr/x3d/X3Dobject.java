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
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;

import com.samsungxr.SXRExternalScene;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRScene;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.SXRShaderId;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.io.SXRControllerType;
import com.samsungxr.io.SXRInputManager;
import com.samsungxr.nodes.SXRVideoNode;
import com.samsungxr.nodes.SXRVideoNodePlayer;
import com.samsungxr.utility.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.samsungxr.script.SXRJavascriptScriptFile;
import com.samsungxr.script.javascript.SXRJavascriptV8File;
import com.samsungxr.x3d.data_types.MFString;
import com.samsungxr.x3d.data_types.SFBool;
import com.samsungxr.x3d.data_types.SFFloat;
import com.samsungxr.x3d.data_types.SFRotation;
import com.samsungxr.x3d.node.Appearance;
import com.samsungxr.x3d.node.Box;
import com.samsungxr.x3d.node.Cone;
import com.samsungxr.x3d.node.Coordinate;
import com.samsungxr.x3d.node.Cylinder;
import com.samsungxr.x3d.node.FontStyle;
import com.samsungxr.x3d.node.Geometry;
import com.samsungxr.x3d.node.ImageTexture;
import com.samsungxr.x3d.node.IndexedFaceSet;
import com.samsungxr.x3d.node.Material;
import com.samsungxr.x3d.node.MovieTexture;
import com.samsungxr.x3d.node.Normal;
import com.samsungxr.x3d.node.Proto;
import com.samsungxr.x3d.node.Shape;
import com.samsungxr.x3d.node.Sphere;
import com.samsungxr.x3d.node.Text;
import com.samsungxr.x3d.node.TextureCoordinate;
import com.samsungxr.x3d.node.TextureTransform;
import com.samsungxr.x3d.node.Transform;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.samsungxr.x3d.data_types.SFVec2f;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAssetLoader;
import com.samsungxr.SXRCamera;
import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDirectLight;
import com.samsungxr.SXRImportSettings;
import com.samsungxr.SXRIndexBuffer;
import com.samsungxr.SXRLODGroup;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRPerspectiveCamera;
import com.samsungxr.SXRPointLight;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRRenderData.SXRRenderMaskBit;
import com.samsungxr.SXRRenderData.SXRRenderingOrder;
import com.samsungxr.SXRRenderPass.SXRCullFaceEnum;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRSpotLight;
import com.samsungxr.SXRSwitch;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTextureParameters;
import com.samsungxr.SXRTextureParameters.TextureWrapType;
import com.samsungxr.SXRTransform;
import com.samsungxr.SXRVertexBuffer;
import com.samsungxr.nodes.SXRConeNode;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.nodes.SXRCylinderNode;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.nodes.SXRTextViewNode;

import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.EnumSet;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.AssetDataSource;
import com.google.android.exoplayer2.upstream.DataSource;


public class X3Dobject {

    /**
     * Allows developers to access the root of X3D scene graph
     * by calling: SXRNode.getNodeByName(X3D_ROOT_NODE);
     */
    public static final String X3D_ROOT_NODE = "x3d_root_node_";

    private static final String TAG = "X3DObject";

    public SXRShaderId x3DShader;

    // Like a C++ pre-compiler switch to select shaders.
    // Default is true to use Universal lights shader.
    public final static boolean UNIVERSAL_LIGHTS = true;

    private final static String JAVASCRIPT_IMPORT_PACKAGE = "importPackage(com.samsungxr.x3d.data_types)\nimportPackage(org.joml)\nimportPackage(com.samsungxr)";

    // Strings appended to SXRScene names when there are multiple
    // animations on the same <Transform> or SXRNode

    private static final String TRANSFORM_CENTER_ = "_Transform_Center_";
    private static final String TRANSFORM_NEGATIVE_CENTER_ = "_Transform_Neg_Center_";
    public static final String TRANSFORM_ROTATION_ = "_Transform_Rotation_";
    public static final String TRANSFORM_TRANSLATION_ = "_Transform_Translation_";
    public static final String TRANSFORM_SCALE_ = "_Transform_Scale_";
    private static final String TRANSFORM_SCALE_ORIENTATION_ = "_Transform_Scale_Orientation_";
    private static final String TRANSFORM_NEGATIVE_SCALE_ORIENTATION_ = "_Transform_Neg_Scale_Orientation_";

    // Append this incremented value to SXRScene names to insure unique
    // SXRNodes
    // when new SXRScene objects are generated to support animation
    private static int animationCount = 1;


    protected final static int verticesComponent = 1;
    protected final static int normalsComponent = 2;
    protected final static int textureCoordComponent = 3;
    protected final static int indexedFaceSetComponent = 4;
    protected final static int normalIndexComponent = 5;
    protected final static int textureIndexComponent = 6;
    protected final static int interpolatorKeyComponent = 7;
    protected final static int interpolatorKeyValueComponent = 8;
    protected final static int LODComponent = 9;
    protected final static int elevationGridHeight = 10;
    private boolean reorganizeVerts = false;

    private static final float CUBE_WIDTH = 20.0f; // used for cube maps

    //GearVR Multi-Texture settings:
    //  0:MULTIPLY; 1=for ADD; 2 for SUBTRACT; 3 for DIVIDE; 4=SMOOTH_ADD; 5=SIGNED_ADD
    public enum MultiTextureModes { MULTIPLY, ADD, SUBTRACT, DIVIDE,
        SMOOTH_ADD, SIGNED_ADD };

    private SXRAssetLoader.AssetRequest assetRequest = null;
    private SXRContext gvrContext = null;
    private Context activityContext = null;

    private SXRNode root = null;
    /**
     * Array list of DEFined items Clones objects with 'USE' parameter
     * As public, enables implementation of HTML5 DOM's
     * getElementByTagName() method.
     */
    public Vector<DefinedItem> mDefinedItems = new Vector<DefinedItem>();


    // When Translation object has multiple properties (center, scale, rotation
    // plus translation)
    // the mesh must be attached to the bottom of these multiply attached
    // SXRNode's but
    // the currentNode's parent must be with the original Transform's
    // parent.
    private SXRNode currentNode = null;

    // Since Script Object are made of multiple parts during parsing,
    // this variable will be the active ScriptObject being built
    private ScriptObject currentScriptObject = null;

    // Since SXRShapeObject contains LOD range, 'shapeLODSceneObj' is used only
    // when it's embedded into a Level-of-Detail
    private SXRNode shapeLODNode = null;

    // points to a sensor that wraps around other nodes.
    private Sensor currentSensor = null;

    private SXRNode meshAttachedNode = null;
    private SXRRenderData gvrRenderData = null;
    private SXRVertexBuffer gvrVertexBuffer = null;
    private SXRIndexBuffer gvrIndexBuffer = null;
    private SXRMaterial gvrMaterial = null;
    private boolean gvrMaterialUSEd = false; // for DEFine and USE gvrMaterial for
    // x3d APPEARANCE and MATERIAL nodes
    private boolean gvrRenderingDataUSEd = false; // for DEFine and USE
    // gvrRenderingData for x3d
    // SHAPE node
    private boolean gvrGroupingNodeUSEd = false; // for DEFine and USE
    // gvrNode for x3d
    // TRANSFORM and GROUP nodes


    private SXRTextureParameters gvrTextureParameters = null;
    private SXRTexture gvrTexture = null;
    private ArrayList<ScriptObject> scriptObjects = new ArrayList<ScriptObject>();

    protected Vector<Key> keys = new Vector<Key>();
    protected Vector<KeyValue> keyValues = new Vector<KeyValue>();
    protected Vector<Float> floatArray = new Vector<Float>();

    private Vector<TimeSensor> timeSensors = new Vector<TimeSensor>();
    private Vector<Interpolator> interpolators = new Vector<Interpolator>();

    private Vector<InlineObject> inlineObjects = new Vector<InlineObject>();
    private Utility utility = null;

    /**
     * public list of <Viewpoints> since camera position can be
     * changed in real-time
     */
    public Vector<Viewpoint> viewpoints = new Vector<Viewpoint>();

    /**
     * Array List of sensors can be accessed in real-time
     * such as in the onStep() function
     */
    public Vector<Sensor> sensors = new Vector<Sensor>();
    public Vector<EventUtility> eventUtilities = new Vector<EventUtility>();


    private ShaderSettings shaderSettings = null;
    private SXRTextViewNode gvrTextViewNode = null;

    private LODmanager lodManager = null;
    private SXRCameraRig cameraRigAtRoot = null;

    private AnimationInteractivityManager animationInteractivityManager = null;

    // set true in <SCRIPT> tag so SAX XML parser will parse JavaScript
    //    inside its characters function.
    private boolean parseJavaScript = false;
    // holds complete JavaScript code per <SCRIPT> tag
    private String javaScriptCode = "";

    // contains the directory structure for inlines to be appended in front of
    // references to texture map file names (plus their own sub-directory.
    private String inlineSubdirectory = "";

    private String indexedSetDEFName = "";
    private String indexedSetUSEName = "";
    //private enum proto_States {
    //    None, ProtoDeclare, ProtoInterface, ProtoBody }
    //private proto_States proto_State = proto_States.None;
    private ArrayList<Proto> protos = new ArrayList<Proto>();
    private Proto proto = null;
    private Proto protoInstance = null;

    // Internal settings from AssetRequest
    private boolean blockLighting = false;
    private boolean blockTexturing = false;




    // The Text_Font Params class and Reset() function handle
    // the values set in the <Text> and <FontStyle> nodes, which are
    // then passed to the SXRTextViewNode constructor
    private static class Text_FontParams {
        static float[] length = null;
        static float maxExtent = 0;
        static String nameTextAttribute = ""; // DEFind name associated with Text node
        static String string = ""; // the actual text to be shown in the scene
        static boolean solid = false;

        static String nameFontStyle = ""; // DEFind name associated with FontStyle node
        static String family = SXRTextViewNode.DEFAULT_FONT;
        static SXRTextViewNode.justifyTypes justify = SXRTextViewNode.justifyTypes.BEGIN;
        static float spacing = 0.0f;
        static float size = 10.0f;
        static SXRTextViewNode.fontStyleTypes style = SXRTextViewNode.fontStyleTypes.PLAIN;
    };

    private void Init_Text_FontParams() {
        Text_FontParams.maxExtent = 0;
        Text_FontParams.nameTextAttribute = ""; // DEFind name associated with Text node
        Text_FontParams.string = "";
        Text_FontParams.solid = false;

        Text_FontParams.nameFontStyle = ""; // DEFind name associated with FontStyle node
        Text_FontParams.family = SXRTextViewNode.DEFAULT_FONT;
        Text_FontParams.justify = SXRTextViewNode.justifyTypes.BEGIN;
        Text_FontParams.spacing = 0.0f;
        Text_FontParams.size = 10.0f;
        Text_FontParams.style = SXRTextViewNode.fontStyleTypes.PLAIN;
    }

    //Called after parsing </Shape>
    private void ShapePostParsing() {
        if (!gvrRenderingDataUSEd) {
            // SHAPE node not being USEd (shared) elsewhere

            // Shape containts Text
            if (gvrTextViewNode != null) {
                gvrTextViewNode.setTextColor((((0xFF << 8)
                        + (int) (shaderSettings.diffuseColor[0] * 255) << 8)
                        + (int) (shaderSettings.diffuseColor[1] * 255) << 8)
                        + (int) (shaderSettings.diffuseColor[2] * 255));
                gvrTextViewNode = null;
            }

            {
                // UNIVERSAL_LIGHTS

                if (!gvrMaterialUSEd) { // if SXRMaterial is NOT set by a USE statement.

                    if (meshAttachedNode == null) {
                        gvrMaterial = shaderSettings.material;
                        gvrRenderData.setMaterial(gvrMaterial);
                    } else {
                        // This SXRNode came with a SXRRenderData and SXRMaterial
                        // already attached.  Examples of this are Text or primitives
                        // such as the Box, Cone, Cylinder, Sphere

                        DefinedItem definedGRRenderingData = null;
                        if (gvrRenderData != null) {
                            // <Shape> node created an unused gvrRenderData
                            // Check if we had a DEF in Shape node so that we can point to
                            // the new gvrRenderData
                            for (DefinedItem definedItem : mDefinedItems) {
                                if (definedItem.getSXRRenderData() == gvrRenderData) {
                                    definedGRRenderingData = definedItem;
                                    break;
                                }
                            }
                        }
                        gvrRenderData = meshAttachedNode.getRenderData();
                        // reset the DEF item to now point to the shader
                        if (definedGRRenderingData != null)
                            definedGRRenderingData.setSXRRenderData(gvrRenderData);
                        gvrMaterial = gvrRenderData.getMaterial();
                    }
                    // X3D doesn't have an ambient color so need to do color
                    // calibration tests on how to set this.
                    gvrMaterial.setVec4("diffuse_color",
                            shaderSettings.diffuseColor[0],
                            shaderSettings.diffuseColor[1],
                            shaderSettings.diffuseColor[2],
                            (1.0f - shaderSettings.getTransparency()) );
                    gvrMaterial.setVec4("specular_color",
                            shaderSettings.specularColor[0],
                            shaderSettings.specularColor[1],
                            shaderSettings.specularColor[2], 1.0f);
                    gvrMaterial.setVec4("emissive_color",
                            shaderSettings.emissiveColor[0],
                            shaderSettings.emissiveColor[1],
                            shaderSettings.emissiveColor[2], 1.0f);
                    gvrMaterial.setFloat("specular_exponent",
                            128.0f * shaderSettings.shininess);

                    if (!shaderSettings.getMaterialName().isEmpty()) {
                        DefinedItem definedItem = new DefinedItem(
                                shaderSettings.getMaterialName());
                        definedItem.setSXRMaterial(gvrMaterial);
                        mDefinedItems.add(definedItem); // Add gvrMaterial to Array list
                        // of DEFined items Clones
                        // objects with USE
                    }

                    if ( shaderSettings.getMultiTexture()) {
                        if ( !shaderSettings.getMultiTextureName().isEmpty() ) {
                            DefinedItem definedItem = new DefinedItem(
                                    shaderSettings.getMultiTextureName() );
                            definedItem.setSXRMaterial(gvrMaterial);
                            mDefinedItems.add(definedItem); // Add gvrMaterial to Array list
                        }
                        gvrMaterial.setTexture("diffuseTexture", shaderSettings.getMultiTextureSXRTexture(0) );
                        gvrMaterial.setTexture("diffuseTexture1", shaderSettings.getMultiTextureSXRTexture(1) );
                        // 0:Mul; 1=for ADD; 2 for SUBTRACT; 3 for DIVIDE; 4=smooth add; 5=Signed add
                        gvrMaterial.setInt("diffuseTexture1_blendop", shaderSettings.getMultiTextureMode().ordinal());
                        gvrMaterial.setTexCoord("diffuseTexture", "a_texcoord", "diffuse_coord");
                        gvrMaterial.setTexCoord("diffuseTexture1", "a_texcoord", "diffuse_coord1");

                    }
                    else if (shaderSettings.texture != null) {
                        gvrMaterial.setTexture("diffuseTexture",
                                shaderSettings.texture);
                        // if the TextureMap is a DEFined item, then set the
                        // SXRMaterial to it as well to help if we set the
                        // in a SCRIPT node.
                        for (DefinedItem definedItem: mDefinedItems) {
                            if ( definedItem.getSXRTexture() == shaderSettings.texture) {
                                definedItem.setSXRMaterial(gvrMaterial);
                                break;
                            }
                        }
                    }

                    if ( !shaderSettings.movieTextures.isEmpty()) {
                        try {
                            SXRVideoNodePlayer<?> videoNodePlayer = null;
                            try {
                                videoNodePlayer = utility.makeExoPlayer(shaderSettings.movieTextures.get(0));
                            }
                            catch (Exception e) {
                                Log.e(TAG, "Exception getting videoNodePlayer: " + e);
                            }
                            videoNodePlayer.start();

                            SXRVideoNode gvrVideoNode =
                                    new SXRVideoNode(gvrContext, gvrRenderData.getMesh(), videoNodePlayer,
                                            SXRVideoNode.SXRVideoType.MONO);
                            currentNode.addChildObject(gvrVideoNode);
                            // Primitives such as Box, Cone, etc come with their own mesh
                            // so we need to remove these.
                            if ( meshAttachedNode != null) {
                                SXRNode primitiveParent = meshAttachedNode.getParent();
                                primitiveParent.removeChildObject(meshAttachedNode);
                            }
                            meshAttachedNode = gvrVideoNode;

                            if (shaderSettings.getMovieTextureName() != null) {
                                gvrVideoNode.setName(shaderSettings.getMovieTextureName());
                                DefinedItem item = new DefinedItem(shaderSettings.getMovieTextureName());
                                item.setSXRVideoNode(gvrVideoNode);
                                mDefinedItems.add(item);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "X3D MovieTexture Exception:\n" + e);
                        }
                    }  // end MovieTexture

                    // Texture Transform
                    // If DEFined iteam, add to the DeFinedItem list. Maay be interactive
                    // GearVR may not be doing texture transforms on primitives or text
                    // crash otherwise.

                    if (meshAttachedNode == null) {
                        if (!shaderSettings.getTextureTransformName().isEmpty()) {
                            DefinedItem definedItem = new DefinedItem(
                                    shaderSettings.getTextureTransformName());
                            definedItem.setSXRMaterial(gvrMaterial);
                            definedItem.setTextureTranslation(shaderSettings.getTextureTranslation());
                            definedItem.setTextureCenter(shaderSettings.getTextureCenter());
                            definedItem.setTextureScale(shaderSettings.getTextureScale());
                            definedItem.setTextureRotation(shaderSettings.getTextureRotation().getValue());
                            definedItem.setName(shaderSettings.getTextureTransformName());
                            mDefinedItems.add(definedItem); // Add gvrMaterial to Array list
                        }
                        // Texture Transform Matrix equation:
                        // TC' = -C * S * R * C * T * TC
                        //where TC' is the transformed texture coordinate
                        //   TC is the original Texture Coordinate
                        //   C = center, S = scale, R = rotation, T = translation
                        Matrix3f textureTransform = animationInteractivityManager.SetTextureTransformMatrix(
                                shaderSettings.getTextureTranslation(),
                                shaderSettings.getTextureCenter(),
                                shaderSettings.getTextureScale(),
                                shaderSettings.getTextureRotation());

                        shaderSettings.textureMatrix = textureTransform;
                        float[] texMtx = new float[9];
                        shaderSettings.textureMatrix.get(texMtx);
                        gvrMaterial.setFloatArray("texture_matrix", texMtx);
                    }

                    // Appearance node thus far contains properties of SXRMaterial
                    // node
                    if (!shaderSettings.getAppearanceName().isEmpty()) {
                        DefinedItem definedItem = new DefinedItem(
                                shaderSettings.getAppearanceName());
                        definedItem.setSXRMaterial(gvrMaterial);
                        mDefinedItems.add(definedItem);
                        // Add gvrMaterial to Array list
                        // of DEFined items Clones
                        // objects with USE
                    }

                    if ((shaderSettings.getTransparency() != 0) && (shaderSettings.getTransparency() != 1)) {
                        gvrRenderData.setRenderingOrder(SXRRenderingOrder.TRANSPARENT);
                    }

                } // end ! gvrMaterialUSEd

                gvrTexture = null;
            }
        } // end !gvrRenderingDataUSEd

        if (meshAttachedNode != null) {
            // gvrRenderData already attached to a SXRNode such as a
            // Cone or Cylinder or a Movie Texture
            meshAttachedNode = null;
        } else
            currentNode.attachRenderData(gvrRenderData);

        if (lodManager.shapeLODNode != null) {
            // if this Shape node was a direct child of a
            // Level-of-Detial (LOD),then restore the parent object
            // since we had to add a SXRNode to support
            // the Shape node's attachement to LOD.
            currentNode = currentNode.getParent();
            lodManager.shapeLODNode = null;
        }

        gvrMaterialUSEd = false; // for DEFine and USE, true if we encounter a
        // USE
        gvrRenderingDataUSEd = false; // for DEFine and USE gvrRenderingData for
        // x3d SHAPE node
        gvrRenderData = null;

    }  //  end ShapePostParsing

    /**
     * X3DObject parses and X3D file using Java SAX parser.
     * Constructor sets up camera rig structure and
     * enables getting to the root of the scene graph by
     * calling SXRNode.getNodeByName(X3D_ROOT_NODE);
     */
    /*********************************************/
    /********** X3Dobject Constructor ************/
    /*********************************************/
    public X3Dobject(SXRAssetLoader.AssetRequest assetRequest,
                     SXRNode root) {
        try {
            this.assetRequest = assetRequest;
            this.gvrContext = assetRequest.getContext();
            this.activityContext = gvrContext.getContext();
            this.root = root;
            x3DShader = gvrContext.getShaderManager().getShaderType(X3DShader.class);
            shaderSettings = new ShaderSettings(new SXRMaterial(gvrContext, x3DShader));

            EnumSet<SXRImportSettings> settings = assetRequest.getImportSettings();
            blockLighting = settings.contains(SXRImportSettings.NO_LIGHTING);
            blockTexturing = settings.contains(SXRImportSettings.NO_TEXTURING);

            utility = new Utility(this, gvrContext, settings);
            // Camera rig setup code based on SXRScene::init()
            SXRCamera leftCamera = new SXRPerspectiveCamera(gvrContext);
            leftCamera.setRenderMask(SXRRenderMaskBit.Left);

            SXRCamera rightCamera = new SXRPerspectiveCamera(gvrContext);
            rightCamera.setRenderMask(SXRRenderMaskBit.Right);

            SXRPerspectiveCamera centerCamera = new SXRPerspectiveCamera(gvrContext);
            centerCamera.setRenderMask(SXRRenderMaskBit.Left | SXRRenderMaskBit.Right);
            cameraRigAtRoot = SXRCameraRig.makeInstance(gvrContext);
            cameraRigAtRoot.getOwnerObject().setName("MainCamera");
            cameraRigAtRoot.attachLeftCamera(leftCamera);
            cameraRigAtRoot.attachRightCamera(rightCamera);
            cameraRigAtRoot.attachCenterCamera(centerCamera);
            gvrContext.getMainScene().setBackgroundColor(0, 0, 0, 1);  // black background default

            lodManager = new LODmanager(root);

            animationInteractivityManager = new AnimationInteractivityManager(
                    this, gvrContext, root, mDefinedItems, interpolators,
                    sensors, timeSensors, eventUtilities, scriptObjects,
                    viewpoints, this.assetRequest
            );
        } catch (Exception e) {
            Log.e(TAG, "X3Dobject constructor error: " + e);
        }
    } // end Constructor


    /*********************************************/
    /********** Utility Functions to *************/
    /************* Assist Parsing ****************/
    /*********************************************/


    protected void AddKeys(float key)

    {
        Key newKey = new Key(key);
        keys.add(newKey);
    }


    protected void AddKeyValues(float[] values)

    {
        KeyValue newKeyValue = new KeyValue(values);
        keyValues.add(newKeyValue);
    }


    /**
     * @author m1.williams
     *         Java SAX parser interface
     */
    class UserHandler extends DefaultHandler {

        String attributeValue = null;

        private void ReplicateSXRSceneObjStructure(String attributeValue) {
            // TODO: needs to complete implementation.  May instead
            // become a clone() or copy() function in SXRNode
            // Transform or Group node to be shared / re-used
            // Transform or Group constructs a SXRNode. DEF/USE 'shares'
            // that SXRNode. However, having a SXRNode as a child

            // of another SXRNode (which a <Transform USE="..." /> would do)
            // causes an infinite loop in the renderer.
            // Solution therefore is to duplicate SXRNode(s) including children
            // and share the SXRMesh and SXRMaterials.
            DefinedItem useItem = null;
            for (DefinedItem definedItem : mDefinedItems) {
                if (attributeValue.equals(definedItem.getName())) {
                    useItem = definedItem;
                    break;
                }
            }
            if (useItem != null) {
                // Get the SXRNode to replicate from the DEFinedItem list.
                SXRNode gvrNodeDEFitem = useItem.getSXRNode();
                String useItemName = useItem.getName();
                // need to parse through

                while (!gvrNodeDEFitem.hasMesh()) {
                    String name = gvrNodeDEFitem.getName();
                    String[] splitName = name.split(useItemName);
                    currentNode = AddSXRNode();
                    if (splitName.length > 1)
                        currentNode.setName("USE_" + useItemName + splitName[1]);
                    currentNode.getTransform()
                            .setPosition(gvrNodeDEFitem.getTransform().getPositionX(),
                                    gvrNodeDEFitem.getTransform().getPositionY(),
                                    gvrNodeDEFitem.getTransform().getPositionZ());
                    currentNode.getTransform()
                            .setRotation(gvrNodeDEFitem.getTransform().getRotationW(),
                                    gvrNodeDEFitem.getTransform().getRotationX(),
                                    gvrNodeDEFitem.getTransform().getRotationY(),
                                    gvrNodeDEFitem.getTransform().getRotationZ());
                    currentNode.getTransform()
                            .setScale(gvrNodeDEFitem.getTransform().getScaleX(),
                                    gvrNodeDEFitem.getTransform().getScaleY(),
                                    gvrNodeDEFitem.getTransform().getScaleZ());
                    gvrNodeDEFitem = gvrNodeDEFitem.getChildByIndex(0);
                }
                if (gvrNodeDEFitem.hasMesh()) {
                    String name = gvrNodeDEFitem.getName();
                    SXRRenderData gvrRenderDataDEFitem = gvrNodeDEFitem
                            .getRenderData();
                    currentNode = AddSXRNode();
                    currentNode.setName("USE_" + useItemName);
                    gvrRenderData = new SXRRenderData(gvrContext);
                    // we are backface culling by default

                    gvrRenderData.setCullFace(SXRCullFaceEnum.Back);
                    currentNode.attachRenderData(gvrRenderData);
                    gvrRenderData.setMaterial(gvrRenderDataDEFitem.getMaterial());
                    gvrRenderData.setMesh(gvrRenderDataDEFitem.getMesh());
                } else {
                    ;
                }
            }
        } // end ReplicateSXRSceneObjStructure


        private SXRNode AddSXRNode() {
            SXRNode newObject = new SXRNode(gvrContext);
            if (currentNode == null)
                root.addChildObject(newObject);
            else
                currentNode.addChildObject(newObject);
            return newObject;

        } // end AddSXRNode

        /**
         * Called by the Java SAX parser implementation
         * to parse the X3D nodes.
         */

        /*********************************************/
        /*********** Parse the X3D File **************/
        /*********************************************/
        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {

            /********** Transform **********/
            if (qName.equalsIgnoreCase("transform")) {

                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) {
                    ReplicateSXRSceneObjStructure(attributeValue);
                } // end USE Transform
                else {
                    // Not a 'Transform USE="..." node
                    // so initialize with default values

                    String name = "";
                    float[] center =
                            {
                                    0, 0, 0
                            };
                    float[] rotation =
                            {
                                    0, 0, 1, 0
                            };
                    float[] scaleOrientation =
                            {
                                    0, 0, 1, 0
                            };
                    float[] scale =
                            {
                                    1, 1, 1
                            };
                    float[] translation =
                            {
                                    0, 0, 0
                            };

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    // Order for Transformations:
                    // P' = T * C * R * SR * S * -SR * -C * P
                    // T=Translation, C=Center, R=Rotation, SR=ScaleOrientation, S=Scale,
                    // and P will the Point
                    // Parsing Center value must occur before Rotation
                    String translationAttribute = attributes.getValue("translation");
                    if (translationAttribute != null) {
                        translation = utility.parseFixedLengthFloatString(translationAttribute, 3,
                                false, false);
                    }
                    String centerAttribute = attributes.getValue("center");
                    if (centerAttribute != null) {
                        center = utility.parseFixedLengthFloatString(centerAttribute, 3, false,
                                false);
                    }
                    String rotationAttribute = attributes.getValue("rotation");
                    if (rotationAttribute != null) {
                        rotation = utility.parseFixedLengthFloatString(rotationAttribute, 4, false,
                                false);
                    }
                    String scaleOrientationAttribute = attributes
                            .getValue("scaleOrientation");
                    if (scaleOrientationAttribute != null) {
                        scaleOrientation = utility.parseFixedLengthFloatString(scaleOrientationAttribute,
                                4, false, false);
                    }
                    attributeValue = attributes.getValue("scale");
                    if (attributeValue != null) {
                        scale = utility.parseFixedLengthFloatString(attributeValue, 3, false, false);
                    }

                    currentNode = AddSXRNode();
                    if (name.isEmpty()) {
                        // There is no DEF, thus no animation or interactivity applied to
                        // this Transform.
                        // Therefore, just set the values in a single SXRNode
                        SXRTransform transform = currentNode.getTransform();
                        transform.setPosition(translation[0], translation[1],
                                translation[2]);
                        transform.rotateByAxisWithPivot((float) Math.toDegrees(rotation[3]),
                                rotation[0], rotation[1],
                                rotation[2], center[0], center[1],
                                center[2]);
                        transform.setScale(scale[0], scale[1], scale[2]);
                    } else {

                        // There is a 'DEF="...."' parameter so save SXRNode
                        // to the DefinedItem's array list in case it's referenced
                        // somewhere else in the X3D file.

                        // Array list of DEFined items
                        // Clones objects with USE
                        // This transform may be animated later, which means we must have
                        // separate SXRNodes
                        // for each transformation plus center and scaleOrientation if
                        // needed
                        // Order for Transformations:
                        // P' = T * C * R * SR * S * -SR * -C * P
                        // First add the translation
                        currentNode.getTransform()
                                .setPosition(translation[0], translation[1], translation[2]);
                        currentNode.setName(name + TRANSFORM_TRANSLATION_);
                        // now check if we have a center value.
                        if ((center[0] != 0) || (center[1] != 0) || (center[2] != 0)) {
                            currentNode = AddSXRNode();
                            currentNode.getTransform()
                                    .setPosition(center[0], center[1], center[2]);
                            currentNode.setName(name + TRANSFORM_CENTER_);
                        }
                        // add rotation
                        currentNode = AddSXRNode();
                        currentNode.getTransform()
                                .setRotationByAxis((float) Math.toDegrees(rotation[3]),
                                        rotation[0], rotation[1], rotation[2]);
                        currentNode.setName(name + TRANSFORM_ROTATION_);
                        // now check if we have a scale orientation value.
                        if ((scaleOrientation[0] != 0) || (scaleOrientation[1] != 0)
                                || (scaleOrientation[2] != 1) || (scaleOrientation[3] != 0)) {
                            currentNode = AddSXRNode();
                            currentNode.getTransform()
                                    .setRotationByAxis((float) Math
                                                    .toDegrees(scaleOrientation[3]), scaleOrientation[0],
                                            scaleOrientation[1], scaleOrientation[2]);
                            currentNode.setName(name + TRANSFORM_SCALE_ORIENTATION_);
                        }
                        // add rotation
                        currentNode = AddSXRNode();
                        currentNode.getTransform().setScale(scale[0], scale[1],
                                scale[2]);
                        currentNode.setName(name + TRANSFORM_SCALE_);
                        // if we had a scale orientation, now we have to negate it.
                        if ((scaleOrientation[0] != 0) || (scaleOrientation[1] != 0)
                                || (scaleOrientation[2] != 1) || (scaleOrientation[3] != 0)) {
                            currentNode = AddSXRNode();
                            currentNode.getTransform()
                                    .setRotationByAxis((float) Math
                                                    .toDegrees(-scaleOrientation[3]), scaleOrientation[0],
                                            scaleOrientation[1], scaleOrientation[2]);
                            currentNode
                                    .setName(name + TRANSFORM_NEGATIVE_SCALE_ORIENTATION_);
                        }
                        // now check if we have a center value.
                        if ((center[0] != 0) || (center[1] != 0) || (center[2] != 0)) {
                            currentNode = AddSXRNode();
                            currentNode.getTransform()
                                    .setPosition(-center[0], -center[1], -center[2]);
                            currentNode.setName(name + TRANSFORM_NEGATIVE_CENTER_);
                        }
                        // Actual object that will have SXRendering and SXRMesh attached
                        currentNode = AddSXRNode();
                        currentNode.setName(name);

                        // save the object for Interactivity, Animation and Scripts
                        // The AxisAngle is saved for Scripts which is how X3D describes
                        // rotations, not quaternions.
                        DefinedItem definedItem = new DefinedItem(name, rotation[3],
                                rotation[0], rotation[1], rotation[2]);
                        definedItem.setSXRNode(currentNode);
                        mDefinedItems.add(definedItem); // Array list of DEFined items
                    } // end if DEF name and thus possible animation / interactivity

                    // Check if there is an active Level-of-Detail (LOD)
                    // and if so add this currentNode if is
                    // a direct child of this LOD.
                    if (lodManager.isActive()) lodManager.AddLODNode( currentNode );

                    if ( proto != null) {
                        if ( proto.isProtoStateProtoBody()) {
                                if ( proto.getTransform() == null) {
                                Transform transform = new Transform(center, rotation,
                                        scale, scaleOrientation, translation, name );
                                proto.setTransform( transform );
                            }
                        }
                    }
                } // not a 'Transform USE="..."' node
            } // end <Transform> node


            /********** Group **********/
            else if (qName.equalsIgnoreCase("Group")) {
                attributeValue = attributes.getValue("USE");
                if (attributeValue != null)

                {

                    ReplicateSXRSceneObjStructure(attributeValue);
                } else {
                    // There is a 'DEF="...."' parameter so save it to the DefinedItem's
                    // array list

                    currentNode = AddSXRNode();
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        currentNode.setName(attributeValue);
                        DefinedItem definedItem = new DefinedItem(attributeValue);
                        definedItem.setSXRNode(currentNode);
                        mDefinedItems.add(definedItem); // Array list of DEFined items
                    }

                    // Check if there is an active Level-of-Detail (LOD)
                    // and add this currentNode if is
                    // a direct child of this LOD.
                    if (lodManager.isActive()) lodManager.AddLODNode( currentNode );
                }

            } // end <Group> node


            /********** Shape **********/
            else if (qName.equalsIgnoreCase("Shape")) {

                gvrRenderData = new SXRRenderData(gvrContext);
                gvrRenderData.setAlphaToCoverage(true);
                gvrRenderData.setRenderingOrder(SXRRenderingOrder.GEOMETRY);
                gvrRenderData.setCullFace(SXRCullFaceEnum.Back);
                shaderSettings.initializeTextureMaterial(new SXRMaterial(gvrContext, x3DShader));

                // Check if this Shape node is part of a Level-of-Detail
                // If there is an active Level-of-Detail (LOD)
                // add this Shape node to new SXRNode as a
                // a direct child of this LOD.
                if (lodManager.isActive()) {
                    if ( lodManager.transformLODNode == currentNode ) {
                        // <Shape> node not under a <Transform> inside a LOD node
                        // so we need to attach it to a SXRNode, and
                        // then attach it to the LOD
                        lodManager.shapeLODNode = AddSXRNode();
                        currentNode = lodManager.shapeLODNode;
                        lodManager.AddLODNode( currentNode );
                    }
                }

                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) { // Shape node to be shared / re-used
                    DefinedItem useItem = null;
                    for (DefinedItem definedItem : mDefinedItems) {
                        if (attributeValue.equals(definedItem.getName())) {
                            useItem = definedItem;
                            break;
                        }
                    }
                    if (useItem != null) {
                        // SXRRenderingData doesn't seem to be shared, but instead has an
                        // owner.  Thus share the SXRMesh and SXRMaterial attached to
                        // SXRRenderingData.
                        SXRRenderData gvrRenderDataDEFined = useItem.getSXRRenderData();
                        gvrRenderData.setMaterial(gvrRenderDataDEFined.getMaterial());
                        gvrRenderData.setMesh(gvrRenderDataDEFined.getMesh());
                        gvrRenderingDataUSEd = true;
                    }
                    else {
                        Log.e(TAG, "Error: Shape USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                    }
                } else {

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        DefinedItem definedItem = new DefinedItem(attributeValue);
                        definedItem.setSXRRenderData(gvrRenderData);
                        mDefinedItems.add(definedItem); // Array list of DEFined items
                        // Clones objects with USE
                    }
                    if ( proto != null) {
                        if ( proto.isProtoStateProtoBody()) {
                            if ( proto.getShape() == null) {
                                Shape shape = new Shape(proto, attributeValue);
                                proto.setShape( shape );
                                if ( proto.getTransform() != null) {
                                    proto.getTransform().setShape( shape );
                                }
                            }
                        }
                    }  //  end if proto != null
                }
            } // end <Shape> node


            /********** Appearance **********/
            else if (qName.equalsIgnoreCase("Appearance")) {
        /* This gives the X3D-only Shader */
                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) { // shared Appearance node, SXRMaterial
                    DefinedItem useItem = null;
                    for (DefinedItem definedItem : mDefinedItems) {
                        if (attributeValue.equals(definedItem.getName())) {
                            useItem = definedItem;
                            break;
                        }
                    }
                    if (useItem != null) {
                        gvrMaterial = useItem.getSXRMaterial();
                        gvrRenderData.setMaterial(gvrMaterial);
                        gvrMaterialUSEd = true; // for DEFine and USE, we encounter a USE,
                        // and thus have set the material
                    }
                    else {
                        Log.e(TAG, "Error: Appearance USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                    }
                } else {
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        shaderSettings.setAppearanceName(attributeValue);
                    }
                    if ( proto != null ) {
                        if ( proto.getAppearance() == null) {
                            Appearance appearance = new Appearance(attributeValue);
                            proto.setAppearance(appearance);
                        }
                    }
                }
            } // end <Appearance> node


            /********** Material **********/
            else if (qName.equalsIgnoreCase("material")) {
                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) {
                    DefinedItem useItem = null;
                    for (DefinedItem definedItem : mDefinedItems) {
                        if (attributeValue.equals(definedItem.getName())) {
                            useItem = definedItem;
                            break;
                        }
                    }
                    if (useItem != null) {
                        gvrMaterial = useItem.getSXRMaterial();
                        gvrRenderData.setMaterial(gvrMaterial);
                        gvrMaterialUSEd = true; // for DEFine and USE, we encounter a USE,
                        // and thus have set the material
                    }
                    else {
                        Log.e(TAG, "Error: Material USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                    }

                } else {
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        shaderSettings.setMaterialName(attributeValue);
                    }
                    String diffuseColorAttribute = attributes.getValue("diffuseColor");
                    if (diffuseColorAttribute != null) {
                        float diffuseColor[] = utility.parseFixedLengthFloatString(diffuseColorAttribute,
                                3, true, false);
                        shaderSettings.setDiffuseColor(diffuseColor);
                    }
                    String specularColorAttribute = attributes.getValue("specularColor");
                    if (specularColorAttribute != null) {
                        float specularColor[] = utility.parseFixedLengthFloatString(specularColorAttribute,
                                3, true, false);
                        shaderSettings.setSpecularColor(specularColor);
                    }
                    String emissiveColorAttribute = attributes.getValue("emissiveColor");
                    if (emissiveColorAttribute != null) {
                        float emissiveColor[] = utility.parseFixedLengthFloatString(emissiveColorAttribute,
                                3, true, false);
                        shaderSettings.setEmmissiveColor(emissiveColor);
                    }
                    String ambientIntensityAttribute = attributes
                            .getValue("ambientIntensity");
                    if (ambientIntensityAttribute != null) {
                        Log.e(TAG, "Material ambientIntensity currently not implemented.");
                        shaderSettings
                                .setAmbientIntensity(utility.parseSingleFloatString(ambientIntensityAttribute,
                                        true, false));
                    }
                    String shininessAttribute = attributes.getValue("shininess");
                    if (shininessAttribute != null) {
                        shaderSettings
                                .setShininess(utility.parseSingleFloatString(shininessAttribute, true,
                                        false));
                    }
                    String transparencyAttribute = attributes.getValue("transparency");
                    if (transparencyAttribute != null) {

                        shaderSettings
                                .setTransparency(utility.parseSingleFloatString(transparencyAttribute,
                                        true, false));
                    }
                    if ( proto != null ) {
                        if ( proto.getAppearance() != null) {
                            if ( proto.getAppearance().getMaterial() == null) {
                                Material material = new Material(shaderSettings.ambientIntensity,
                                            shaderSettings.diffuseColor, shaderSettings.emissiveColor,
                                            shaderSettings.shininess, shaderSettings.specularColor,
                                            shaderSettings.getTransparency());
                                proto.getAppearance().setMaterial(material);
                            }
                            else {
                                Log.e(TAG, "Proto error: Material not starting null inside Appearance node");
                            }
                        }
                        else {
                            Log.e(TAG, "Proto error: Material set without Appearance");
                        }
                    }
                } // end ! USE attribute
            } // end <Material> node


            /********** ImageTexture **********/
            else if (qName.equalsIgnoreCase("ImageTexture")) {
                if ( !blockTexturing ) {
                    attributeValue = attributes.getValue("USE");
                    if (attributeValue != null) {
                        DefinedItem useItem = null;
                        for (DefinedItem definedItem : mDefinedItems) {
                            if (attributeValue.equals(definedItem.getName())) {
                                useItem = definedItem;
                                break;
                            }
                        }
                        if (useItem != null) {
                            gvrTexture = useItem.getSXRTexture();
                            shaderSettings.setTexture(gvrTexture);
                        } else {
                            Log.e(TAG, "Error: ImageTexture USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                        }
                    } else {
                        gvrTextureParameters = new SXRTextureParameters(gvrContext);
                        gvrTextureParameters.setWrapSType(TextureWrapType.GL_REPEAT);
                        gvrTextureParameters.setWrapTType(TextureWrapType.GL_REPEAT);
                        gvrTextureParameters.setMinFilterType(SXRTextureParameters.TextureFilterType.GL_LINEAR_MIPMAP_NEAREST);

                        String urlAttribute = attributes.getValue("url");
                        if (urlAttribute != null) {
                            urlAttribute = urlAttribute.replace("\"", ""); // remove double and
                            // single quotes
                            urlAttribute = urlAttribute.replace("\'", "");

                            final String filename = urlAttribute;
                            String repeatSAttribute = attributes.getValue("repeatS");
                            if (repeatSAttribute != null) {
                                if (!utility.parseBooleanString(repeatSAttribute)) {
                                    gvrTextureParameters
                                            .setWrapSType(TextureWrapType.GL_CLAMP_TO_EDGE);
                                }
                            }
                            String repeatTAttribute = attributes.getValue("repeatT");
                            if (repeatTAttribute != null) {
                                if (!utility.parseBooleanString(repeatTAttribute)) {
                                    gvrTextureParameters
                                            .setWrapTType(TextureWrapType.GL_CLAMP_TO_EDGE);
                                }
                            }

                            final String defValue = attributes.getValue("DEF");
                            gvrTexture = new SXRTexture(gvrContext, gvrTextureParameters);
                            SXRAssetLoader.TextureRequest request = new SXRAssetLoader.TextureRequest(assetRequest, gvrTexture, (inlineSubdirectory + filename));
                            assetRequest.loadTexture(request);
                            shaderSettings.setTexture(gvrTexture);
                            if (defValue != null) {
                                DefinedItem item = new DefinedItem(defValue);
                                item.setSXRTexture(gvrTexture);
                                mDefinedItems.add(item);
                            }

                            if ( shaderSettings.getMultiTexture() ) {
                                shaderSettings.setMultiTextureSXRTexture( gvrTexture );
                            }
                        }  //  end url
                        if ( proto != null ) {
                            Appearance appearance = proto.getAppearance();
                            if (appearance != null) {
                                if (proto.getAppearance().getTexture() == null) {
                                    String[] urls = new String[1];
                                    urls[0] = urlAttribute;
                                    ImageTexture imageTexture = new ImageTexture(urls, true, true);
                                    proto.getAppearance().setTexture(imageTexture);
                                } else {
                                    Log.e(TAG, "Proto error: ImageTexture not starting null inside Appearance node");
                                }
                            } else {
                                Log.e(TAG, "PROTO:  <Appearance> not set.");
                            }  // end appearance != null
                        } // end proto != null
                    }  // end not USE
                }  //  end if ( !blockTexturing )
            } // end <ImageTexture> node


            /********** TextureTransform **********/
            else if (qName.equalsIgnoreCase("TextureTransform")) {
                attributeValue = attributes.getValue("DEF");
                if (attributeValue != null) {
                    shaderSettings.setTextureTransformName(attributeValue);
                }
                String centerAttribute = attributes.getValue("center");
                if (centerAttribute != null) {
                    float[] center = utility.parseFixedLengthFloatString(centerAttribute, 2,
                            false, false);
                    shaderSettings.setTextureCenter(center);
                }
                String rotationAttribute = attributes.getValue("rotation");
                if (rotationAttribute != null) {
                    float[] rotation = utility.parseFixedLengthFloatString(rotationAttribute, 1,
                            false, false);
                    shaderSettings.setTextureRotation( rotation[0] );
                }
                String scaleAttribute = attributes.getValue("scale");
                if (scaleAttribute != null) {
                    float[] scale = utility.parseFixedLengthFloatString(scaleAttribute, 2, false,
                            true);
                    shaderSettings.setTextureScale(scale);
                }
                String translationAttribute = attributes.getValue("translation");
                if (translationAttribute != null) {
                    float[] translation = utility.parseFixedLengthFloatString(translationAttribute,
                            2, false, false);
                    translation[1] = -translation[1];
                    shaderSettings.setTextureTranslation(translation);
                }
                if ( proto != null ) {
                    Appearance appearance = proto.getAppearance();
                    if (appearance != null) {
                        if (proto.getAppearance().getTextureTransform() == null) {
                            TextureTransform textureTransform = new TextureTransform(
                                    shaderSettings.getTextureCenter().getValue(), shaderSettings.getTextureRotation().getValue(),
                                    shaderSettings.getTextureScale().getValue(), shaderSettings.getTextureTranslation().getValue()
                            );
                            proto.getAppearance().setTextureTransform(textureTransform);
                        } else {
                            Log.e(TAG, "Proto error: TextureTransform not starting null inside Appearance node");
                        }
                    } else {
                        Log.e(TAG, "PROTO:  <Appearance> not set.");
                    }  // end appearance != null
                } // end proto != null


            }  // end TextureTransform

            /********** IndexedFaceSet **********/

            //TODO: eventually include IndexedLineSet **********/

            else if (qName.equalsIgnoreCase("IndexedFaceSet")) {
                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) { // shared SXRIndexBuffer / SXRMesh
                    indexedSetUSEName = attributeValue;
                } else {
                    gvrIndexBuffer = new SXRIndexBuffer(gvrContext, 4, 0);
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        indexedSetDEFName = attributeValue;
                    }
                    attributeValue = attributes.getValue("solid");
                    if (attributeValue != null) {
                        if ( !utility.parseBooleanString(attributeValue)) {
                            Log.e(TAG, "IndexedFaceSet solid=false not implemented. ");
                        }
                    }
                    attributeValue = attributes.getValue("ccw");
                    if (attributeValue != null) {
                        if ( !utility.parseBooleanString(attributeValue)) {
                            Log.e(TAG, "IndexedFaceSet ccw=false attribute not implemented. ");
                        }
                    }
                    attributeValue = attributes.getValue("colorPerVertex");
                    if (attributeValue != null) {

                        Log.e(TAG,
                                "IndexedFaceSet colorPerVertex attribute not implemented. ");

                    }
                    attributeValue = attributes.getValue("normalPerVertex");
                    if (attributeValue != null) {

                        if ( !utility.parseBooleanString(attributeValue)) {
                            Log.e(TAG,
                                    "IndexedFaceSet normalPerVertex=false attribute not implemented. ");
                        }

                    }
                    String coordIndexAttribute = attributes.getValue("coordIndex");
                    if (coordIndexAttribute != null) {
                        utility.parseNumbersString(coordIndexAttribute,
                                X3Dobject.indexedFaceSetComponent, 3);
                        reorganizeVerts = true;
                    }
                    String normalIndexAttribute = attributes.getValue("normalIndex");
                    if (normalIndexAttribute != null) {
                        utility.parseNumbersString(normalIndexAttribute,
                                X3Dobject.normalIndexComponent, 3);
                    }
                    String texCoordIndexAttribute = attributes.getValue("texCoordIndex");
                    if (texCoordIndexAttribute != null) {
                        utility.parseNumbersString(texCoordIndexAttribute,
                                X3Dobject.textureIndexComponent, 3);
                    }
                }
                if ( proto != null ) {
                    Geometry geometry = proto.getGeometry();
                    if (geometry == null) {
                        geometry = new Geometry();
                        proto.setGeometry(geometry);
                    }
                    IndexedFaceSet indexedFaceSet = new IndexedFaceSet();
                    geometry.setIndexedFaceSet( indexedFaceSet );

                    indexedFaceSet.setCoordIndex( utility.meshCreator.mPositionIndices.array() );
                    indexedFaceSet.setTexCoordIndex( utility.meshCreator.mTexcoordIndices.array() );
                    indexedFaceSet.setNormalIndex( utility.meshCreator.mNormalIndices.array() );

                } // end proto != null
            } // end <IndexedFaceSet> node


            /********** Coordinate **********/
            else if (qName.equalsIgnoreCase("Coordinate")) {
                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) { // Coordinate node to be shared / re-used
                    DefinedItem useItem = null;
                    for (DefinedItem definedItem : mDefinedItems) {
                        if (attributeValue.equals(definedItem.getName())) {
                            useItem = definedItem;
                            break;
                        }
                    }
                    if (useItem != null) {

                        // 'useItem' points to SXRMesh who's useItem.getSXRMesh Coordinates
                        // were DEFined earlier. We don't want to share the entire SXRMesh
                        // since the 2 meshes may have different Normals and
                        // Texture Coordinates.  So as an alternative, copy the vertices.
                        gvrVertexBuffer = useItem.getVertexBuffer();
                        reorganizeVerts = false;
                    }
                } // end USE Coordinate
                else {
                    // Not a 'Coordinate USE="..." node
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        DefinedItem definedItem = new DefinedItem(attributeValue);
                            utility.meshCreator.defineVertexBuffer(definedItem);
                        // Array list of DEFined items clones objects with USE
                        mDefinedItems.add(definedItem);
                    }
                    String pointAttribute = attributes.getValue("point");
                    if (pointAttribute != null) {
                        utility.parseNumbersString(pointAttribute, X3Dobject.verticesComponent, 3);
                    }
                    if ( proto != null ) {
                        Geometry geometry = proto.getGeometry();
                        if (geometry != null) {
                            IndexedFaceSet indexedFaceSet = geometry.getIndexedFaceSet();
                            if (indexedFaceSet != null) {
                                Coordinate coordinate = new Coordinate();
                                indexedFaceSet.setCoord( coordinate );
                                float[] coordinateValues = utility.meshCreator.mInputPositions.array();

                                coordinate.setMeshCreatorInputPositions( coordinateValues);

                            }
                            else {
                                Log.e(TAG, "PROTO: <Coordinate> not inside <IndexedFaceSet>");
                            }
                        }
                        else {
                            Log.e(TAG, "PROTO: <Coordinate> not inside <IndexedFaceSet>");
                        }  // end geometry != null
                    }  // end proto != null
                } // end NOT a USE Coordinates condition
            } // end <Coordinate> node


            /********** TextureCoordinate **********/
            else if (qName.equalsIgnoreCase("TextureCoordinate")) {
                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) { // Coordinate node to be shared / re-used
                    DefinedItem useItem = null;
                    for (DefinedItem definedItem : mDefinedItems) {
                        if (attributeValue.equals(definedItem.getName())) {
                            useItem = definedItem;
                            break;
                        }
                    }
                    if (useItem != null) {

                        // 'useItem' points to SXRVertexBuffer who's useItem.getVertexBuffer
                        // TextureCoordinates were DEFined earlier.
                        // We don't want to share the entire SXRVertexBuffer since the
                        // the 2 meshes may have different Normals and Positions
                        // So as an alternative, copy the texture coordinates.
                        gvrVertexBuffer.setFloatArray("a_texcoord", useItem.getVertexBuffer().getFloatArray("a_texcoord"));
                        reorganizeVerts = false;
                    }
                } // end USE TextureCoordinate
                else {
                    // Not a 'TextureCoordinate USE="..." node
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        // This is a 'TextureCoordinate DEF="..." case, so save the item
                        DefinedItem definedItem = new DefinedItem(attributeValue);
                            definedItem.setVertexBuffer(gvrVertexBuffer);
                        // Array list of DEFined items clones objects with USE
                        mDefinedItems.add(definedItem);
                    }
                    // Have to flip the y texture coordinates because the image will be
                    // upside down
                    String pointAttribute = attributes.getValue("point");
                    if (pointAttribute != null) {
                        utility.parseNumbersString(pointAttribute, X3Dobject.textureCoordComponent, 2);
                    }
                    if ( proto != null ) {
                        Geometry geometry = proto.getGeometry();
                        if (geometry != null) {
                            IndexedFaceSet indexedFaceSet = geometry.getIndexedFaceSet();
                            if (indexedFaceSet != null) {
                                TextureCoordinate textureCoordinate = new TextureCoordinate();
                                indexedFaceSet.setTetureCoordinate( textureCoordinate );
                                float[] textureCoordinateValues = utility.meshCreator.mInputTexCoords.array();
                                textureCoordinate.setMeshCreatorInputTexCoords( textureCoordinateValues );
                            }
                            else {
                                Log.e(TAG, "PROTO: <TextureCoordinate> not inside <IndexedFaceSet>");
                            }
                        }
                        else {
                            Log.e(TAG, "PROTO: <TextureCoordinate> not inside <IndexedFaceSet>");
                        }  // end geometry != null
                    }  // end proto != null

                } // end NOT a USE TextureCoordinate condition
            } // end <TextureCoordinate> node


            /********** Normal **********/
            else if (qName.equalsIgnoreCase("Normal")) {
                attributeValue = attributes.getValue("USE");
                if (attributeValue != null) { // Coordinate node to be shared / re-used
                    DefinedItem useItem = null;
                    for (DefinedItem definedItem : mDefinedItems) {
                        if (attributeValue.equals(definedItem.getName())) {
                            useItem = definedItem;
                            break;
                        }
                    }
                    if (useItem != null) {

                        // 'useItem' points to SXRVertexBuffer who's useItem.getVertexBuffer Coordinates
                        // were DEFined earlier. We don't want to share the entire vertex buffer since
                        // the 2 vertex buffers may have different Normals and Texture Coordinates
                        // So as an alternative, copy the normals.
                        gvrVertexBuffer.setFloatArray("a_normal", useItem.getVertexBuffer().getFloatArray("a_normal"));
                        reorganizeVerts = false;
                    }
                } // end USE Normal
                else {
                    // Not a 'Normal USE="..." node
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        // This is a 'Normal DEF="..." case, so save the item
                        DefinedItem definedItem = new DefinedItem(attributeValue);
                            definedItem.setVertexBuffer(gvrVertexBuffer);
                        // Array list of DEFined items clones objects with USE
                        mDefinedItems.add(definedItem);
                    }
                    String vectorAttribute = attributes.getValue("vector");
                    if (vectorAttribute != null) {
                        utility.parseNumbersString(vectorAttribute, X3Dobject.normalsComponent, 3);
                    }
                    if ( proto != null ) {
                        Geometry geometry = proto.getGeometry();
                        if (geometry != null) {
                            IndexedFaceSet indexedFaceSet = geometry.getIndexedFaceSet();
                            if (indexedFaceSet != null) {
                                Normal normal = new Normal();
                                indexedFaceSet.setNormal( normal );
                                float[] normalValues = utility.meshCreator.mInputNormals.array();
                                normal.setMeshCreatorInputNormals( normalValues );
                            }
                            else {
                                Log.e(TAG, "PROTO: <Normal> not inside <IndexedFaceSet>");
                            }
                        }
                        else {
                            Log.e(TAG, "PROTO: <Normal> not inside <IndexedFaceSet>");
                        }  // end geometry != null
                    }
                } // end NOT a USE Normals condition
            } // end <Normal> node


            /********** LIGHTS **********/
            /********** PointLight **********/
            else if (qName.equalsIgnoreCase("PointLight")) {
                if (UNIVERSAL_LIGHTS && !blockLighting) {
                    attributeValue = attributes.getValue("USE");
                    if (attributeValue != null) { // shared PointLight
                        DefinedItem useItem = null;
                        for (DefinedItem definedItem : mDefinedItems) {
                            if (attributeValue.equals(definedItem.getName())) {
                                useItem = definedItem;
                                break;
                            }
                        }
                        if (useItem != null) {
                            // SXRf does not allow a light attached at two places
                            // so copy the attributes of the original light into the second
                            // light
                            SXRNode definedNode = useItem.getSXRNode();
                            SXRPointLight definedPtLight = (SXRPointLight) definedNode
                                    .getLight();
                            SXRTransform definedTransform = definedPtLight.getTransform();

                            SXRNode newPtLightSceneObj = AddSXRNode();
                            newPtLightSceneObj.getTransform()
                                    .setPosition(definedTransform.getPositionX(),
                                            definedTransform.getPositionY(),
                                            definedTransform.getPositionZ());
                            // attachLight does not allow a light to be shared so we need to
                            // just create a new light and share its attributes
                            SXRPointLight newPtLight = new SXRPointLight(gvrContext);
                            newPtLightSceneObj.attachLight(newPtLight);
                            float[] attribute = definedPtLight.getAmbientIntensity();
                            newPtLight.setAmbientIntensity(attribute[0], attribute[1],
                                    attribute[2], attribute[3]);
                            attribute = definedPtLight.getDiffuseIntensity();
                            newPtLight.setDiffuseIntensity(attribute[0], attribute[1],
                                    attribute[2], 1);
                            attribute = definedPtLight.getSpecularIntensity();
                            newPtLight.setSpecularIntensity(attribute[0], attribute[1],
                                    attribute[2], 1);
                            newPtLight
                                    .setAttenuation(definedPtLight.getAttenuationConstant(),
                                            definedPtLight.getAttenuationLinear(),
                                            definedPtLight.getAttenuationQuadratic());
                            newPtLight.enable();
                        }
                        else {
                            Log.e(TAG, "Error: PointLight USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                        }
                    } // end reuse a PointLight
                    else {
                        // add a new PointLight
                        float ambientIntensity = 0;
                        float[] attenuation =
                                {
                                        1, 0, 0
                                };
                        float[] color =
                                {
                                        1, 1, 1
                                };
                        boolean global = true;
                        float intensity = 1;
                        float[] location =
                                {
                                        0, 0, 0
                                };
                        boolean on = true;
                        float radius = 100;

                        SXRNode newPtLightSceneObj = AddSXRNode();
                        SXRPointLight newPtLight = new SXRPointLight(gvrContext);
                        newPtLightSceneObj.attachLight(newPtLight);

                        attributeValue = attributes.getValue("DEF");
                        if (attributeValue != null) {
                            newPtLightSceneObj.setName(attributeValue);
                            DefinedItem definedItem = new DefinedItem(attributeValue);
                            definedItem.setSXRNode(newPtLightSceneObj);
                            mDefinedItems.add(definedItem); // Array list of DEFined items
                            // Clones objects with USE
                        }
                        attributeValue = attributes.getValue("ambientIntensity");
                        if (attributeValue != null) {
                            ambientIntensity = utility.parseSingleFloatString(attributeValue, true,
                                    false);
                        }
                        attributeValue = attributes.getValue("attenuation");
                        if (attributeValue != null) {
                            attenuation = utility.parseFixedLengthFloatString(attributeValue, 3,
                                    false, true);
                            if ((attenuation[0] == 0) && (attenuation[1] == 0)
                                    && (attenuation[2] == 0))
                                attenuation[0] = 1;
                        }
                        attributeValue = attributes.getValue("color");
                        if (attributeValue != null) {
                            color = utility.parseFixedLengthFloatString(attributeValue, 3, true,
                                    false);
                        }
                        attributeValue = attributes.getValue("global");
                        if (attributeValue != null) {
                            global = utility.parseBooleanString(attributeValue); // NOT IMPLEMENTED
                            Log.e(TAG, "Point Light global attribute not implemented. ");
                        }
                        attributeValue = attributes.getValue("intensity");
                        if (attributeValue != null) {
                            intensity = utility.parseSingleFloatString(attributeValue, true, false);
                        }
                        attributeValue = attributes.getValue("location");
                        if (attributeValue != null) {
                            location = utility.parseFixedLengthFloatString(attributeValue, 3, false,
                                    false);
                        }
                        attributeValue = attributes.getValue("on");
                        if (attributeValue != null) {
                            on = utility.parseBooleanString(attributeValue);
                        }
                        attributeValue = attributes.getValue("radius");
                        if (attributeValue != null) {
                            radius = utility.parseSingleFloatString(attributeValue, false, true);
                        }
                        // In x3d, ambientIntensity is only 1 value, not 3.
                        newPtLight.setAmbientIntensity(ambientIntensity, ambientIntensity,
                                ambientIntensity, 1);
                        newPtLight.setDiffuseIntensity(color[0] * intensity,
                                color[1] * intensity,
                                color[2] * intensity, 1);
                        // x3d doesn't have an equivalent for specular intensity
                        newPtLight.setSpecularIntensity(color[0] * intensity,
                                color[1] * intensity,
                                color[2] * intensity, 1);
                        newPtLight.setAttenuation(attenuation[0], attenuation[1],
                                attenuation[2]);
                        if (on)
                            newPtLight.enable();
                        else
                            newPtLight.disable();

                        SXRTransform newPtLightSceneObjTransform = newPtLightSceneObj
                                .getTransform();
                        newPtLightSceneObjTransform.setPosition(location[0], location[1],
                                location[2]);
                    } // end a new PointLight
                } // end if UNIVERSAL_LIGHTS

            } // end <PointLight> node


            /********** DirectionalLight **********/
            else if (qName.equalsIgnoreCase("DirectionalLight")) {
                if (UNIVERSAL_LIGHTS && !blockLighting) {
                    attributeValue = attributes.getValue("USE");
                    if (attributeValue != null) { // shared PointLight
                        DefinedItem useItem = null;
                        for (DefinedItem definedItem : mDefinedItems) {
                            if (attributeValue.equals(definedItem.getName())) {
                                useItem = definedItem;
                                break;
                            }
                        }
                        if (useItem != null) {
                            // SXRf does not allow a light attached at two places
                            // so copy the attributes of the original light into the second
                            // light
                            SXRNode definedNode = useItem.getSXRNode();
                            SXRDirectLight definedDirectLight = (SXRDirectLight) definedNode
                                    .getLight();
                            SXRTransform definedTransform = definedDirectLight.getTransform();

                            SXRNode newDirectLightSceneObj = AddSXRNode();
                            newDirectLightSceneObj.getTransform()
                                    .setRotation(definedTransform.getRotationW(),
                                            definedTransform.getRotationX(),
                                            definedTransform.getRotationY(),
                                            definedTransform.getRotationZ());
                            // attachLight does not allow a light to be shared so we need to
                            // just create a new light and share its attributes
                            SXRDirectLight newDirectLight = new SXRDirectLight(gvrContext);
                            newDirectLightSceneObj.attachLight(newDirectLight);
                            float[] attribute = definedDirectLight.getAmbientIntensity();
                            newDirectLight.setAmbientIntensity(attribute[0], attribute[1],
                                    attribute[2], attribute[3]);
                            attribute = definedDirectLight.getDiffuseIntensity();
                            newDirectLight.setDiffuseIntensity(attribute[0], attribute[1],
                                    attribute[2], 1);
                            attribute = definedDirectLight.getSpecularIntensity();
                            newDirectLight.setSpecularIntensity(attribute[0], attribute[1],
                                    attribute[2], 1);
                            newDirectLight.enable();
                        }
                        else {
                            Log.e(TAG, "Error: DirectionalLight USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                        }
                    } // end reuse a DirectionalLight
                    else {
                        // add a new DirectionalLight
                        float ambientIntensity = 0;
                        float[] color =
                                {
                                        1, 1, 1
                                };
                        float[] direction =
                                {
                                        0, 0, -1
                                };
                        boolean global = true;
                        float intensity = 1;
                        boolean on = true;

                        SXRNode newDirectionalLightSceneObj = AddSXRNode();
                        SXRDirectLight newDirectionalLight = new SXRDirectLight(gvrContext);
                        newDirectionalLightSceneObj.attachLight(newDirectionalLight);
                        DefinedItem definedItem = null;

                        attributeValue = attributes.getValue("DEF");
                        if (attributeValue != null) {
                            newDirectionalLightSceneObj.setName(attributeValue);
                            definedItem = new DefinedItem(attributeValue);
                            definedItem.setSXRNode(newDirectionalLightSceneObj);
                            mDefinedItems.add(definedItem); // Array list of DEFined items
                            // Clones objects with USE
                        }
                        attributeValue = attributes.getValue("ambientIntensity");
                        if (attributeValue != null) {
                            ambientIntensity = utility.parseSingleFloatString(attributeValue, true,
                                    false);
                        }
                        attributeValue = attributes.getValue("color");
                        if (attributeValue != null) {
                            color = utility.parseFixedLengthFloatString(attributeValue, 3, true,
                                    false);
                        }
                        attributeValue = attributes.getValue("direction");
                        if (attributeValue != null) {
                            direction = utility.parseFixedLengthFloatString(attributeValue, 3, false,
                                    false);
                        }
                        attributeValue = attributes.getValue("global");
                        if (attributeValue != null) {
                            Log.e(TAG,
                                    "DirectionalLight global attribute not currently implemented. ");
                        }
                        attributeValue = attributes.getValue("intensity");
                        if (attributeValue != null) {
                            intensity = utility.parseSingleFloatString(attributeValue, true, false);
                        }
                        attributeValue = attributes.getValue("on");
                        if (attributeValue != null) {
                            on = utility.parseBooleanString(attributeValue);
                        }

                        newDirectionalLight.setAmbientIntensity(1, 1, 1, 1);
                        newDirectionalLight.setDiffuseIntensity(color[0] * intensity,
                                color[1] * intensity,
                                color[2] * intensity, 1);
                        newDirectionalLight.setSpecularIntensity(1, 1, 1, 1);

                        if (on)
                            newDirectionalLight.enable();
                        else
                            newDirectionalLight.disable();

                        if (definedItem != null) definedItem.setDirection(direction);

                        Quaternionf q = animationInteractivityManager.ConvertDirectionalVectorToQuaternion(
                                new Vector3f(direction[0], direction[1], direction[2]));
                        // set direction in the Light's SXRScene
                        SXRTransform newDirectionalLightSceneObjTransform = newDirectionalLightSceneObj
                                .getTransform();
                        newDirectionalLightSceneObjTransform.setRotation(q.w, q.x, q.y,
                                q.z);
                    } // end if adding new Directional Light
                } // end if Universal Lights

            } // end <Directional Light> node


            /********** SpotLight **********/
            else if (qName.equalsIgnoreCase("SpotLight")) {
                if (UNIVERSAL_LIGHTS && !blockLighting) {
                    attributeValue = attributes.getValue("USE");
                    if (attributeValue != null) { // shared PointLight
                        DefinedItem useItem = null;
                        for (DefinedItem definedItem : mDefinedItems) {
                            if (attributeValue.equals(definedItem.getName())) {
                                useItem = definedItem;
                                break;
                            }
                        }
                        if (useItem != null) {
                            // SXRf does not allow a light attached at two places
                            // so copy the attributes of the original light into the second
                            // light
                            SXRNode definedNode = useItem.getSXRNode();
                            SXRSpotLight definedSpotLight = (SXRSpotLight) definedNode
                                    .getLight();
                            SXRTransform definedTransform = definedSpotLight.getTransform();

                            SXRNode newSpotLightSceneObj = AddSXRNode();
                            newSpotLightSceneObj.getTransform()
                                    .setPosition(definedTransform.getPositionX(),
                                            definedTransform.getPositionY(),
                                            definedTransform.getPositionZ());
                            newSpotLightSceneObj.getTransform()
                                    .setRotation(definedTransform.getRotationW(),
                                            definedTransform.getRotationX(),
                                            definedTransform.getRotationY(),
                                            definedTransform.getRotationZ());
                            // attachLight does not allow a light to be shared so we need to
                            // just create a new light and share its attributes
                            SXRSpotLight newSpotLight = new SXRSpotLight(gvrContext);
                            newSpotLightSceneObj.attachLight(newSpotLight);
                            float[] attribute = definedSpotLight.getAmbientIntensity();
                            newSpotLight.setAmbientIntensity(attribute[0], attribute[1],
                                    attribute[2], attribute[3]);
                            attribute = definedSpotLight.getDiffuseIntensity();
                            newSpotLight.setDiffuseIntensity(attribute[0], attribute[1],
                                    attribute[2], 1);
                            attribute = definedSpotLight.getSpecularIntensity();
                            newSpotLight.setSpecularIntensity(attribute[0], attribute[1],
                                    attribute[2], 1);
                            newSpotLight
                                    .setAttenuation(definedSpotLight.getAttenuationConstant(),
                                            definedSpotLight.getAttenuationLinear(),
                                            definedSpotLight.getAttenuationQuadratic());
                            newSpotLight
                                    .setInnerConeAngle(definedSpotLight.getInnerConeAngle());
                            newSpotLight
                                    .setOuterConeAngle(definedSpotLight.getOuterConeAngle());
                            newSpotLight.enable();
                        }
                        else {
                            Log.e(TAG, "Error: SpotLight USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                        }
                    } // end reuse a SpotLight
                    else {
                        // add a new SpotLight
                        float ambientIntensity = 0;
                        float[] attenuation =
                                {
                                        1, 0, 0
                                };
                        float beamWidth = (float) Math.PI / 4; // range is 0 to PI
                        float[] color =
                                {
                                        1, 1, 1
                                };
                        float cutOffAngle = (float) Math.PI / 2; // range is 0 to PI
                        float[] direction =
                                {
                                        0, 0, -1
                                };
                        boolean global = true;
                        float intensity = 1;
                        float[] location =
                                {
                                        0, 0, 0
                                };
                        boolean on = true;
                        float radius = 100;

                        SXRNode newSpotLightSceneObj = AddSXRNode();
                        SXRSpotLight newSpotLight = new SXRSpotLight(gvrContext);
                        newSpotLightSceneObj.attachLight(newSpotLight);

                        DefinedItem definedItem = null;

                        attributeValue = attributes.getValue("DEF");
                        if (attributeValue != null) {
                            newSpotLightSceneObj.setName(attributeValue);
                            definedItem = new DefinedItem(attributeValue);
                            definedItem.setSXRNode(newSpotLightSceneObj);
                            mDefinedItems.add(definedItem); // Array list of DEFined items
                            // Clones objects with USE
                        }
                        attributeValue = attributes.getValue("ambientIntensity");
                        if (attributeValue != null) {
                            ambientIntensity = utility.parseSingleFloatString(attributeValue, true,
                                    false);
                        }
                        attributeValue = attributes.getValue("attenuation");
                        if (attributeValue != null) {
                            attenuation = utility.parseFixedLengthFloatString(attributeValue, 3,
                                    false, true);
                            if ((attenuation[0] == 0) && (attenuation[1] == 0)
                                    && (attenuation[2] == 0))
                                attenuation[0] = 1;
                        }
                        attributeValue = attributes.getValue("beamWidth");
                        if (attributeValue != null) {
                            beamWidth = utility.parseSingleFloatString(attributeValue, false, true);
                            if (beamWidth > (float) Math.PI / 2) {
                                beamWidth = (float) Math.PI / 2;
                                Log.e(TAG, "Spot Light beamWidth cannot exceed PI/2.");
                            }
                        }
                        attributeValue = attributes.getValue("color");
                        if (attributeValue != null) {
                            color = utility.parseFixedLengthFloatString(attributeValue, 3, true,
                                    false);
                        }
                        attributeValue = attributes.getValue("cutOffAngle");
                        if (attributeValue != null) {
                            cutOffAngle = utility.parseSingleFloatString(attributeValue, false, true);
                            if (cutOffAngle > (float) Math.PI / 2) {
                                cutOffAngle = (float) Math.PI / 2;
                                Log.e(TAG, "Spot Light cutOffAngle cannot exceed PI/2.");
                            }
                        }
                        attributeValue = attributes.getValue("direction");
                        if (attributeValue != null) {
                            direction = utility.parseFixedLengthFloatString(attributeValue, 3, false,
                                    false);
                        }
                        attributeValue = attributes.getValue("global");
                        if (attributeValue != null) {
                            Log.e(TAG,
                                    "Spot Light global attribute not currently implemented. ");
                        }
                        attributeValue = attributes.getValue("intensity");
                        if (attributeValue != null) {
                            intensity = utility.parseSingleFloatString(attributeValue, true, false);
                        }
                        attributeValue = attributes.getValue("location");
                        if (attributeValue != null) {
                            location = utility.parseFixedLengthFloatString(attributeValue, 3, false,
                                    false);
                        }
                        attributeValue = attributes.getValue("on");
                        if (attributeValue != null) {
                            on = utility.parseBooleanString(attributeValue);
                        }
                        attributeValue = attributes.getValue("radius");
                        if (attributeValue != null) {
                            radius = utility.parseSingleFloatString(attributeValue, false, true);
                        }
                        // x3d only has a single value for ambient intensity
                        newSpotLight.setAmbientIntensity(ambientIntensity, ambientIntensity,
                                ambientIntensity, 1);
                        newSpotLight.setDiffuseIntensity(color[0] * intensity,
                                color[1] * intensity,
                                color[2] * intensity, 1);
                        newSpotLight.setSpecularIntensity(0, 0, 0, 1);
                        newSpotLight.setAttenuation(attenuation[0], attenuation[1],
                                attenuation[2]);

                        if (on)
                            newSpotLight.enable();
                        else
                            newSpotLight.disable();
                        newSpotLight.setInnerConeAngle(beamWidth * 180 / (float) Math.PI);
                        newSpotLight.setOuterConeAngle(cutOffAngle * 180 / (float) Math.PI);

                        if (definedItem != null) definedItem.setDirection(direction);
                        Quaternionf q = animationInteractivityManager.ConvertDirectionalVectorToQuaternion(
                                new Vector3f(direction[0], direction[1], direction[2]));
                        // set position and direction in the SpotLight's SXRScene
                        SXRTransform newSpotLightSceneObjTransform = newSpotLightSceneObj
                                .getTransform();
                        newSpotLightSceneObjTransform.setPosition(location[0], location[1],
                                location[2]);
                        newSpotLightSceneObjTransform.setRotation(q.w, q.x, q.y, q.z);
                    } // end adding a new SpotLight

                } // end if UNIVERSAL_LIGHTS

            } // end <SpotLight> node


            /********** TimeSensor **********/
            else if (qName.equalsIgnoreCase("TimeSensor")) {
                String name = null;
                float cycleInterval = 1;
                boolean enabled = true;
                boolean loop = false;
                float pauseTime = 0;
                float resumeTime = 0;
                float startTime = 0;
                float stopTime = 0;

                attributeValue = attributes.getValue("DEF");
                if (attributeValue != null) {
                    name = attributeValue;
                }
                attributeValue = attributes.getValue("cycleInterval");
                if (attributeValue != null) {
                    cycleInterval = utility.parseSingleFloatString(attributeValue, false, true);
                }
                attributeValue = attributes.getValue("enabled");
                if (attributeValue != null) {
                    enabled = utility.parseBooleanString(attributeValue);
                }
                attributeValue = attributes.getValue("loop");
                if (attributeValue != null) {
                    loop = utility.parseBooleanString(attributeValue);
                }
                attributeValue = attributes.getValue("pauseTime");
                if (attributeValue != null) {
                    pauseTime = utility.parseSingleFloatString(attributeValue, false, true);
                    Log.e(TAG, "Timer pauseTime not currently implemented. ");
                }
                attributeValue = attributes.getValue("resumeTime");
                if (attributeValue != null) {
                    resumeTime = utility.parseSingleFloatString(attributeValue, false, true);
                    Log.e(TAG, "Timer resumeTime not currently implemented. ");
                }
                attributeValue = attributes.getValue("startTime");
                if (attributeValue != null) {
                    startTime = utility.parseSingleFloatString(attributeValue, false, true);
                    Log.e(TAG, "Timer startTime not currently implemented. ");
                }
                attributeValue = attributes.getValue("stopTime");
                if (attributeValue != null) {
                    stopTime = utility.parseSingleFloatString(attributeValue, false, true);
                    Log.e(TAG, "Timer stopTime not currently implemented. ");
                }

                TimeSensor newTimeSensor = new TimeSensor(name, cycleInterval, enabled,
                        loop, pauseTime, resumeTime, startTime, stopTime);
                timeSensors.add(newTimeSensor);

            } // end <TimeSensor> node


            /********** ROUTE **********/
            else {
                if (qName.equalsIgnoreCase("ROUTE")) {
                    String fromNode = null;
                    String fromField = null;
                    String toNode = null;
                    String toField = null;
                    attributeValue = attributes.getValue("fromNode");
                    if (attributeValue != null) {
                        fromNode = attributeValue;
                    }
                    attributeValue = attributes.getValue("fromField");
                    if (attributeValue != null) {
                        fromField = attributeValue;
                    }
                    attributeValue = attributes.getValue("toNode");
                    if (attributeValue != null) {
                        toNode = attributeValue;
                    }
                    attributeValue = attributes.getValue("toField");
                    if (attributeValue != null) {
                        toField = attributeValue;
                    }

                    animationInteractivityManager.buildInteractiveObject(fromNode, fromField, toNode, toField);
                } // end <ROUTE> node


                /********** PositionInterpolator **********/
                else if (qName.equalsIgnoreCase("PositionInterpolator")) {
                    String name = null;
                    float[] keysList = null;
                    float[] keyValuesList = null;

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("key");
                    if (attributeValue != null) {
                        utility.parseNumbersString(attributeValue, X3Dobject.interpolatorKeyComponent,
                                1);

                        keysList = new float[keys.size()];
                        for (int i = 0; i < keysList.length; i++) {
                            Key keyObject = keys.get(i);
                            keysList[i] = keyObject.key;
                        }
                        keys.clear();
                    }
                    attributeValue = attributes.getValue("keyValue");
                    if (attributeValue != null) {
                        utility.parseNumbersString(attributeValue,
                                X3Dobject.interpolatorKeyValueComponent, 3);

                        keyValuesList = new float[keyValues.size() * 3];
                        for (int i = 0; i < keyValues.size(); i++) {
                            KeyValue keyValueObject = keyValues.get(i);
                            for (int j = 0; j < 3; j++) {
                                keyValuesList[i * 3 + j] = keyValueObject.keyValues[j];
                            }
                        }
                        keyValues.clear();
                    }
                    Interpolator newInterporlator = new Interpolator(name, keysList,
                            keyValuesList);
                    interpolators.add(newInterporlator);

                } // end <PositionInterpolator> node


                /********** OrientationInterpolator **********/
                else if (qName.equalsIgnoreCase("OrientationInterpolator")) {
                    String name = null;
                    float[] keysList = null;
                    float[] keyValuesList = null;

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("key");
                    if (attributeValue != null) {
                        utility.parseNumbersString(attributeValue, X3Dobject.interpolatorKeyComponent,
                                1);

                        keysList = new float[keys.size()];
                        for (int i = 0; i < keysList.length; i++) {
                            Key keyObject = keys.get(i);
                            keysList[i] = keyObject.key;
                        }
                        keys.clear();
                    }
                    attributeValue = attributes.getValue("keyValue");
                    if (attributeValue != null) {
                        utility.parseNumbersString(attributeValue,
                                X3Dobject.interpolatorKeyValueComponent, 4);

                        keyValuesList = new float[keyValues.size() * 4];
                        for (int i = 0; i < keyValues.size(); i++) {
                            KeyValue keyValueObject = keyValues.get(i);
                            for (int j = 0; j < 4; j++) {
                                keyValuesList[i * 4 + j] = keyValueObject.keyValues[j];
                            }
                        }
                        keyValues.clear();
                    }
                    Interpolator newInterporlator = new Interpolator(name, keysList,
                            keyValuesList);
                    interpolators.add(newInterporlator);

                } // end <OrientationInterpolator> node


                /********** Box **********/
                else if (qName.equalsIgnoreCase("Box")) {
                    float[] size =
                            {
                                    2, 2, 2
                            };
                    boolean solid = true; // box visible from inside

                    attributeValue = attributes.getValue("size");
                    if (attributeValue != null) {
                        size = utility.parseFixedLengthFloatString(attributeValue, 3, false, true);
                    }
                    attributeValue = attributes.getValue("solid");
                    if (attributeValue != null) {
                        solid = utility.parseBooleanString(attributeValue);
                    }
                    Vector3f sizeVector = new Vector3f(size[0], size[1], size[2]);
                    SXRCubeNode gvrCubeNode = new SXRCubeNode(
                            gvrContext, solid, sizeVector);
                    gvrCubeNode.getRenderData().setMaterial(new SXRMaterial(gvrContext, x3DShader));

                    if ( proto != null ) {
                        if (proto.getGeometry() == null) {
                            Geometry geometry = new Geometry();
                            proto.setGeometry( geometry );
                            Box boxObj = new Box();
                            geometry.setBox( boxObj );
                        }
                    }
                    else currentNode.addChildObject(gvrCubeNode);

                    meshAttachedNode = gvrCubeNode;
                } // end <Box> node


                /********** Cone **********/
                else if (qName.equalsIgnoreCase("Cone")) {
                    boolean bottom = true;
                    float bottomRadius = 1;
                    float height = 2;
                    boolean side = true;
                    boolean solid = true; // cone visible from inside

                    attributeValue = attributes.getValue("bottom");
                    if (attributeValue != null) {
                        bottom = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("bottomRadius");
                    if (attributeValue != null) {
                        bottomRadius = utility.parseSingleFloatString(attributeValue, false, true);
                    }
                    attributeValue = attributes.getValue("height");
                    if (attributeValue != null) {
                        height = utility.parseSingleFloatString(attributeValue, false, true);
                    }
                    attributeValue = attributes.getValue("side");
                    if (attributeValue != null) {
                        side = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("solid");
                    if (attributeValue != null) {
                        solid = utility.parseBooleanString(attributeValue);
                    }
                    SXRCylinderNode.CylinderParams params = new SXRCylinderNode.CylinderParams();
                    params.BottomRadius = bottomRadius;
                    params.TopRadius = 0;
                    params.Height = height;
                    params.FacingOut = solid;
                    params.HasTopCap = false;
                    params.HasBottomCap = bottom;
                    params.Material = new SXRMaterial(gvrContext, x3DShader);
                    SXRCylinderNode gvrConeNode = new SXRCylinderNode(gvrContext,
                            params);

                    if ( proto != null ) {
                        if (proto.getGeometry() == null) {
                            Geometry geometry = new Geometry();
                            proto.setGeometry( geometry );
                            Cone coneObj = new Cone();
                            geometry.setCone( coneObj );
                        }
                    }
                    else currentNode.addChildObject(gvrConeNode);
                    //currentNode.addChildObject(cone);
                    //meshAttachedNode = cone;
                    meshAttachedNode = gvrConeNode;
                }  // end <Cone> node


                /********** Cylinder **********/
                else if (qName.equalsIgnoreCase("Cylinder")) {
                    boolean bottom = true;
                    float height = 2;
                    float radius = 1;
                    boolean side = true;
                    boolean solid = true; // cylinder visible from inside
                    boolean top = true;

                    attributeValue = attributes.getValue("bottom");
                    if (attributeValue != null) {
                        bottom = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("height");
                    if (attributeValue != null) {
                        height = utility.parseSingleFloatString(attributeValue, false, true);
                    }
                    attributeValue = attributes.getValue("radius");
                    if (attributeValue != null) {
                        radius = utility.parseSingleFloatString(attributeValue, false, true);
                    }
                    attributeValue = attributes.getValue("side");
                    if (attributeValue != null) {
                        side = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("solid");
                    if (attributeValue != null) {
                        solid = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("top");
                    if (attributeValue != null) {
                        top = utility.parseBooleanString(attributeValue);
                    }
                    SXRCylinderNode.CylinderParams params = new SXRCylinderNode.CylinderParams();
                    params.BottomRadius = radius;
                    params.TopRadius = radius;
                    params.Height = height;
                    params.HasBottomCap = bottom;
                    params.HasTopCap = top;
                    params.FacingOut = solid;
                    params.Material = new SXRMaterial(gvrContext, x3DShader);
                    SXRCylinderNode gvrCylinderNode = new SXRCylinderNode(
                            gvrContext, params);
                    if ( proto != null ) {
                        if (proto.getGeometry() == null) {
                            Geometry geometry = new Geometry();
                            proto.setGeometry( geometry );
                            Cylinder cylinder = new Cylinder();
                            geometry.setCylinder( cylinder );
                        }
                    }
                    else currentNode.addChildObject(gvrCylinderNode);
                    meshAttachedNode = gvrCylinderNode;

                } // end <Cylinder> node


                /********** Sphere **********/
                else if (qName.equalsIgnoreCase("Sphere")) {
                    float radius = 1;
                    boolean solid = true; // sphere visible from inside
                    attributeValue = attributes.getValue("radius");
                    if (attributeValue != null) {
                        radius = utility.parseSingleFloatString(attributeValue, false, true);
                    }
                    attributeValue = attributes.getValue("solid");
                    if (attributeValue != null) {
                        solid = utility.parseBooleanString(attributeValue);
                    }
                    SXRSphereNode gvrSphereNode = new SXRSphereNode(
                            gvrContext, solid, new SXRMaterial(gvrContext, x3DShader), radius);

                    if ( proto != null ) {
                         if (proto.getGeometry() == null) {
                            Geometry geometry = new Geometry();
                            proto.setGeometry( geometry );
                            Sphere sphere = new Sphere();
                            geometry.setSphere( sphere );
                        }
                    }
                    else currentNode.addChildObject(gvrSphereNode);
                    meshAttachedNode = gvrSphereNode;

                } // end <Sphere> node

                // Less frequent commands and thus moved to end of if-then-else.

                /********** Viewpoint **********/
                else if (qName.equalsIgnoreCase("Viewpoint")) {
                    float[] centerOfRotation =
                            {
                                    0, 0, 0
                            };
                    String description = "";
                    float fieldOfView = (float) Math.PI / 4;
                    boolean jump = true;
                    String name = "";
                    float[] orientation =
                            {
                                    0, 0, 1, 0
                            };
                    float[] position =
                            {
                                    0, 0, 10
                            };
                    boolean retainUserOffsets = false;

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("centerOfRotation");
                    if (attributeValue != null) {
                        centerOfRotation = utility.parseFixedLengthFloatString(attributeValue, 3,
                                false, false);
                        Log.e(TAG, "X3D Viewpoint centerOfRotation not implemented in GearVR.");
                    }
                    attributeValue = attributes.getValue("description");
                    if (attributeValue != null) {
                        description = attributeValue;
                    }
                    attributeValue = attributes.getValue("fieldOfView");
                    if (attributeValue != null) {
                        fieldOfView = utility.parseSingleFloatString(attributeValue, false, true);
                        if (fieldOfView > (float) Math.PI)
                            fieldOfView = (float) Math.PI;
                        Log.e(TAG, "X3D Viewpoint fieldOfView not implemented in GearVR. ");
                    }
                    attributeValue = attributes.getValue("jump");
                    if (attributeValue != null) {
                        jump = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("orientation");
                    if (attributeValue != null) {
                        orientation = utility.parseFixedLengthFloatString(attributeValue, 4, false,
                                false);
                    }
                    attributeValue = attributes.getValue("position");
                    if (attributeValue != null) {
                        position = utility.parseFixedLengthFloatString(attributeValue, 3, false,
                                false);
                    }
                    attributeValue = attributes.getValue("retainUserOffsets");
                    if (attributeValue != null) {
                        retainUserOffsets = utility.parseBooleanString(attributeValue);
                        Log.e(TAG, "Viewpoint retainUserOffsets attribute not implemented. ");
                    }
                    // Add viewpoint to the list.
                    // Since viewpoints can be under a Transform, save the parent.
                    Viewpoint viewpoint = new Viewpoint(centerOfRotation, description,
                            fieldOfView, jump, name, orientation, position, retainUserOffsets,
                            currentNode);
                    viewpoints.add(viewpoint);

                    if ( !name.equals("") ) {
                        DefinedItem definedItem = new DefinedItem(name);
                        definedItem.setViewpoint(viewpoint);
                        mDefinedItems.add(definedItem); // Array list of DEFined items
                    }


                } // end <Viewpoint> node


                /********** Text **********/
                else if (qName.equalsIgnoreCase("Text")) {
                    String[] string = null;
                    float[] length = {0};
                    Init_Text_FontParams();
                    attributeValue = attributes.getValue("USE");
                    if (attributeValue != null) {
                        Log.e(TAG, "Text node USE name not currently implemented.");
                    }

                   attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        Text_FontParams.nameTextAttribute = attributeValue;
                    }
                    attributeValue = attributes.getValue("length");
                    if (attributeValue != null) {
                        // reusing the keys parsing here cause it works
                        utility.parseNumbersString(attributeValue, X3Dobject.interpolatorKeyComponent,
                                1);
                        length = new float[keys.size()];
                        for (int i = 0; i < length.length; i++) {
                            Key keyObject = keys.get(i);
                            length[i] = keyObject.key;
                        }
                        keys.clear();
                        Log.e(TAG, "Text 'length' attribute currently not implemented.");
                    }
                    attributeValue = attributes.getValue("maxExtent");
                    if (attributeValue != null) {
                        Text_FontParams.maxExtent = utility.parseSingleFloatString(attributeValue, false, true);
                        Log.e(TAG, "Text 'maxExtent' attribute currently not implemented. ");
                    }
                    attributeValue = attributes.getValue("string");
                    if (attributeValue != null) {
                        string = utility.parseMFString(attributeValue);
                        String text = "";
                        for (int i = 0; i < string.length; i++) {
                            if (i > 0) text += "\n";
                            text += string[i];
                        }
                        Text_FontParams.string = text;
                    }
                    attributeValue = attributes.getValue("solid");
                    if (attributeValue != null) {
                        Text_FontParams.solid = utility.parseBooleanString(attributeValue);
                        Log.e(TAG, "Text 'solid' attribute currently not implemented. ");
                    }

                    if ( proto != null ) {
                        if (proto.getGeometry() == null) {
                            Geometry geometry = new Geometry();
                            proto.setGeometry( geometry );
                            Text textObj = new Text(null, length[0], Text_FontParams.maxExtent,
                                    string, Text_FontParams.solid, Text_FontParams.nameTextAttribute);
                            geometry.setText( textObj );
                            FontStyle fontStyle = new FontStyle();
                            textObj.setFontStyle( fontStyle );
                        }
                    }
                } // end <Text> node


                /********** FontStyle **********/
                else if (qName.equalsIgnoreCase("FontStyle")) {
                    attributeValue = attributes.getValue("USE");
                    if (attributeValue != null) { // shared FontStyle
                        // copy the values from a defined style type
                        SXRNode definedNode = root.getNodeByName(attributeValue);
                        if ( definedNode.getClass().equals(SXRTextViewNode.class) ) {
                            SXRTextViewNode gvrTextViewNode = (SXRTextViewNode) definedNode;
                            Text_FontParams.family = gvrTextViewNode.getFontFamily();
                            Text_FontParams.justify = gvrTextViewNode.getJustification();
                            Text_FontParams.spacing = gvrTextViewNode.getLineSpacing();
                            Text_FontParams.size = gvrTextViewNode.getSize();
                            Text_FontParams.style = gvrTextViewNode.getStyleType();
                        }
                        else {
                            Log.e(TAG, "Error: FontStyle USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                        }
                    }
                    else {
                        String[] justifyMFString = null;

                        attributeValue = attributes.getValue("DEF");
                        if (attributeValue != null) {
                            Text_FontParams.nameFontStyle = attributeValue;
                        }
                        attributeValue = attributes.getValue("family");
                        if (attributeValue != null) {
                            String[] family = utility.parseMFString(attributeValue);
                            // handle spaces in the font name
                            if (family.length > 1) {
                                for (int i = 1; i < family.length; i++) {
                                    family[0] += (" " + family[i]);
                                }
                            }
                            Text_FontParams.family = family[0]; // we only accept one family per string
                        }
                        attributeValue = attributes.getValue("horizontal");
                        if (attributeValue != null) {
                            boolean horizontal = utility.parseBooleanString(attributeValue);
                            Log.e(TAG, "horizontal feature of FontStyle not implemented");
                        }
                        attributeValue = attributes.getValue("justify");
                        if (attributeValue != null) {
                            //String[] justifyMFString = utility.parseMFString(attributeValue);
                            justifyMFString = utility.parseMFString(attributeValue);
                            SXRTextViewNode.justifyTypes[] justify = new SXRTextViewNode.justifyTypes[justifyMFString.length];
                            for (int i = 0; i < justify.length; i++) {
                                if (justifyMFString[i].equalsIgnoreCase("END"))
                                    justify[i] = SXRTextViewNode.justifyTypes.END;
                                else if (justifyMFString[i].equalsIgnoreCase("FIRST"))
                                    justify[i] = SXRTextViewNode.justifyTypes.FIRST;
                                else if (justifyMFString[i].equalsIgnoreCase("MIDDLE"))
                                    justify[i] = SXRTextViewNode.justifyTypes.MIDDLE;
                                else justify[i] = SXRTextViewNode.justifyTypes.BEGIN;
                            }
                            Text_FontParams.justify = justify[0]; // we only accept one justification per string
                        }
                        attributeValue = attributes.getValue("language");
                        if (attributeValue != null) {
                            String language = attributeValue;
                            Log.e(TAG, "language feature of FontStyle not implemented");
                        }
                        attributeValue = attributes.getValue("leftToRight");
                        if (attributeValue != null) {
                            boolean leftToRight = utility.parseBooleanString(attributeValue);
                            Log.e(TAG, "leftToRight feature of FontStyle not implemented");
                        }
                        attributeValue = attributes.getValue("spacing");
                        if (attributeValue != null) {
                            Text_FontParams.spacing = 10.0f * (utility.parseSingleFloatString(attributeValue, false, true) - 1);
                        }
                        attributeValue = attributes.getValue("size");
                        if (attributeValue != null) {
                            Text_FontParams.size = 10.0f * utility.parseSingleFloatString(attributeValue, false, true);
                        }
                        attributeValue = attributes.getValue("style");
                        if (attributeValue != null) {
                            if (attributeValue.equalsIgnoreCase("BOLD")) {
                                Text_FontParams.style = SXRTextViewNode.fontStyleTypes.BOLD;
                            } else if (attributeValue.equalsIgnoreCase("ITALIC")) {
                                Text_FontParams.style = SXRTextViewNode.fontStyleTypes.ITALIC;
                            } else if (attributeValue.equalsIgnoreCase("BOLDITALIC")) {
                                Text_FontParams.style = SXRTextViewNode.fontStyleTypes.BOLDITALIC;
                            } else {
                                Text_FontParams.style = SXRTextViewNode.fontStyleTypes.PLAIN;
                            }
                        }
                        attributeValue = attributes.getValue("topToBottom");
                        if (attributeValue != null) {
                            boolean topToBottom = utility.parseBooleanString(attributeValue);
                            Log.e(TAG, "topToBottom feature of FontStyle not implemented");
                        }

                        if ( proto != null ) {
                            if (proto.getGeometry() != null) {
                                if ( proto.getGeometry().getText() != null) {
                                    Text text = proto.getGeometry().getText();
                                    FontStyle fontStyle = text.getFontStyle();
                                    if (justifyMFString != null) fontStyle.setJustify( justifyMFString );
                                }
                            }
                        }

                    } // not re-USE FontStyle
                } // end <FontStyle> node

                /********** Billboard **********/
                else if (qName.equalsIgnoreCase("Billboard")) {
                    Log.e(TAG, "X3D Billboard currently not implemented. ");
                    //TODO: Billboard not currently implemented
                    String name = "";
                    float[] axisOfRotation =
                            {
                                    0, 1, 0
                            };
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("axisOfRotation");
                    if (attributeValue != null) {
                        axisOfRotation = utility.parseFixedLengthFloatString(attributeValue, 3, true,
                                false);
                    }

                } // end <Billboard> node


                /********** Inline **********/
                else if (qName.equalsIgnoreCase("Inline")) {
                    // Inline data saved, and added after the inital .x3d program is parsed
                    String name = "";
                    String[] url = new String[1];
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("url");
                    if (attributeValue != null) {
                        url[0] = attributeValue;
                        SXRNode inlineSXRNode = currentNode; // preserve
                        // the
                        // currentNode
                        if (lodManager.isActive()  &&
                                (inlineSXRNode.getComponent(SXRLODGroup.getComponentType()) != null)) {
                            inlineSXRNode = AddSXRNode();
                            inlineSXRNode.setName("inlineSXRNode"
                                    + lodManager.getCurrentRangeIndex());
                            final SXRNode parent = inlineSXRNode.getParent();
                            if (null == parent.getComponent(SXRLODGroup.getComponentType())) {
                                parent.attachComponent(new SXRLODGroup(gvrContext));
                            }
                            final SXRLODGroup lodGroup = (SXRLODGroup) parent.getComponent(SXRLODGroup.getComponentType());
                            lodGroup.addRange(lodManager.getMinRange(), inlineSXRNode);
                            lodManager.increment();
                        }
                        InlineObject inlineObject = new InlineObject(inlineSXRNode,
                                url);
                        inlineObjects.add(inlineObject);
                    }

                    // LOD has it's own SXRNode which has a
                    // SXRLODGroup component attached
                    if (lodManager.isActive() && lodManager.transformLODNode == null) {
                    //if (lodManager.transformLODNode == null) {
                        lodManager.transformLODNode = AddSXRNode();
                        lodManager.transformLODNode.attachComponent(new SXRLODGroup(gvrContext));
                        currentNode = lodManager.transformLODNode;
                    }
                } // end <Inline> node


                /********** LOD **********/
                else if (qName.equalsIgnoreCase("LOD")) {
                    String name = "";
                    float[] center =
                            {
                                    0, 0, 0
                            };
                    float[] range = null;
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("center");
                    if (attributeValue != null) {
                        center = utility.parseFixedLengthFloatString(attributeValue, 3, false, false);
                    }
                    attributeValue = attributes.getValue("range");
                    if (attributeValue != null) {
                        utility.parseNumbersString(attributeValue, X3Dobject.LODComponent, 1);
                        range = new float[keys.size() + 2];
                        range[0] = 0;
                        for (int i = 0; i < keys.size(); i++) {
                            Key keyObject = keys.get(i);
                            range[i + 1] = keyObject.key;
                        }
                        range[range.length - 1] = Float.MAX_VALUE;
                        keys.clear();
                    }
                    lodManager.set(range, center);

                    // LOD has it's own SXRNode which has a
                    // SXRLODGroup component attached
                    if (lodManager.transformLODNode == null) {
                        lodManager.transformLODNode = AddSXRNode();
                        lodManager.transformLODNode.attachComponent(new SXRLODGroup(gvrContext));
                        currentNode = lodManager.transformLODNode;
                    }

                } // end <LOD> Level-of-Detail node


                /********** Switch **********/
                else if (qName.equalsIgnoreCase("Switch")) {
                    String name = "";
                    int whichChoice = -1;

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("whichChoice");
                    if (attributeValue != null) {
                        whichChoice = utility.parseIntegerString(attributeValue);
                    }
                    currentNode = AddSXRNode();
                    currentNode.setName( name );

                    SXRSwitch gvrSwitch = new SXRSwitch( gvrContext );
                    gvrSwitch.setSwitchIndex( whichChoice );
                    currentNode.attachComponent(gvrSwitch);

                    DefinedItem definedItem = new DefinedItem(currentNode.getName());
                    definedItem.setSXRNode(currentNode);
                    mDefinedItems.add(definedItem); // Array list of DEFined items in the X3D scene
                } // end <Switch> node


                /********** Anchor **********/
                else if (qName.equalsIgnoreCase("Anchor")) {
                    String name = "";
                    String description = "";
                    String[] parameter = null;
                    String url = "";
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("description");
                    if (attributeValue != null) {
                        description = attributeValue;
                    }
                    attributeValue = attributes.getValue("parameter");
                    if (attributeValue != null) {
                        parameter = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("url");
                    if (attributeValue != null) {

                        // TODO: issues with parsing

                        // multiple strings with special chars
                        url = attributeValue;
                    }
                    // Set the currentSensor pointer so that child objects will be added
                    // to the list of eye pointer objects.
                    currentNode = AddSXRNode();
                    currentNode.setName(name);
                    Sensor sensor = new Sensor(name, Sensor.Type.ANCHOR,
                            currentNode, true);
                    sensor.setAnchorURL(url);
                    sensors.add(sensor);
                    animationInteractivityManager.BuildInteractiveObjectFromAnchor(sensor, url);
                } // end <Anchor> node


                /********** TouchSensor **********/
                else if (qName.equalsIgnoreCase("TouchSensor")) {
                    String name = "";
                    String description = "";
                    boolean enabled = true;
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("description");
                    if (attributeValue != null) {
                        description = attributeValue;
                    }
                    attributeValue = attributes.getValue("enabled");
                    if (attributeValue != null) {
                        enabled = utility.parseBooleanString(attributeValue);
                    }

                    Sensor sensor = new Sensor(name, Sensor.Type.TOUCH, currentNode, enabled);
                    sensors.add(sensor);
                    // add colliders to all objects under the touch sensor
                    currentNode.attachCollider(new SXRMeshCollider(gvrContext, true));
                } // end <TouchSensor> node


                /********** PlaneSensor **********/
                else if (qName.equalsIgnoreCase("PlaneSensor")) {
                    String name = "";
                    String description = "";
                    boolean enabled = true;
                    SFVec2f minPosition = new SFVec2f(0, 0);
                    SFVec2f maxPosition = new SFVec2f(-1, -1);
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("description");
                    if (attributeValue != null) {
                        description = attributeValue;
                    }
                    attributeValue = attributes.getValue("enabled");
                    if (attributeValue != null) {
                        enabled = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("maxPosition");
                    if (attributeValue != null) {
                        float[] maxValues = utility.parseFixedLengthFloatString(attributeValue, 2, false, false);
                        maxPosition.setValue(maxValues[0], maxValues[1]);
                    }
                    attributeValue = attributes.getValue("minPosition");
                    if (attributeValue != null) {
                        float[] minValues = utility.parseFixedLengthFloatString(attributeValue, 2, false, false);
                        minPosition.setValue(minValues[0], minValues[1]);
                    }

                    Sensor sensor = new Sensor(name, Sensor.Type.PLANE, currentNode, enabled);
                    sensor.setMinMaxValues(minPosition, maxPosition);
                    sensors.add(sensor);
                    // add colliders to all objects under the touch sensor
                    currentNode.attachCollider(new SXRMeshCollider(gvrContext, true));
                } // end <PlaneSensor> node


                /********** CylinderSensor **********/
                else if (qName.equalsIgnoreCase("CylinderSensor")) {
                    String name = "";
                    String description = "";
                    SFRotation axisRotation = new SFRotation(0, 1, 0, 0);
                    SFBool enabled = new SFBool(true);
                    SFFloat diskAngle = new SFFloat( (float)Math.PI/12.0f);
                    SFFloat minAngle = new SFFloat(0);
                    SFFloat maxAngle = new SFFloat(-1);
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("description");
                    if (attributeValue != null) {
                        description = attributeValue;
                    }
                    attributeValue = attributes.getValue("axisRotation");
                    if (attributeValue != null) {
                        axisRotation.setValue( utility.parseFixedLengthFloatString(attributeValue, 4, true, true) );
                        Log.e(TAG, "CylinderSensor axisRotation not implemented");
                    }

                    attributeValue = attributes.getValue("enabled");
                    if (attributeValue != null) {
                        enabled.setValue( utility.parseBooleanString(attributeValue) );
                    }
                    attributeValue = attributes.getValue("diskAngle");
                    if (attributeValue != null) {
                        Log.e(TAG, "CylinderSensor diskAngle not implemented");
                    }
                    attributeValue = attributes.getValue("maxAngle");
                    if (attributeValue != null) {
                        float maxValue = utility.parseSingleFloatString(attributeValue, false, false);
                        if (maxValue > Math.PI*2) maxValue = (float)Math.PI*2;
                        else if (maxValue < -Math.PI*2) maxValue = -(float)Math.PI*2;
                        maxAngle.setValue(maxValue);
                    }
                    attributeValue = attributes.getValue("minAngle");
                    if (attributeValue != null) {
                        float minValue = utility.parseSingleFloatString(attributeValue, false, false);
                        if (minValue > Math.PI*2) minValue = (float)Math.PI*2;
                        else if (minValue < -Math.PI*2) minValue = -(float)Math.PI*2;
                        minAngle.setValue(minValue);
                    }

                    Sensor sensor = new Sensor(name, Sensor.Type.CYLINDER, currentNode, enabled.getValue() );
                    sensor.setMinMaxAngle(minAngle, maxAngle);
                    sensor.setAxisRotation( axisRotation );
                    sensors.add(sensor);
                    // add colliders to all objects under the touch sensor
                    currentNode.attachCollider(new SXRMeshCollider(gvrContext, true));
                } // end <CylinderSensor> node


                /********** SphereSensor **********/
                else if (qName.equalsIgnoreCase("SphereSensor")) {
                    String name = "";
                    String description = "";
                    boolean enabled = true;
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("description");
                    if (attributeValue != null) {
                        description = attributeValue;
                    }
                    attributeValue = attributes.getValue("enabled");
                    if (attributeValue != null) {
                        enabled = utility.parseBooleanString(attributeValue);
                    }

                    Sensor sensor = new Sensor(name, Sensor.Type.SPHERE, currentNode, enabled);
                    sensors.add(sensor);
                    // add colliders to all objects under the touch sensor
                    currentNode.attachCollider(new SXRMeshCollider(gvrContext, true));
                } // end <SphereSensor> node


                /********** ProximitySensor **********/
                else if (qName.equalsIgnoreCase("ProximitySensor")) {
                    Log.e(TAG, "ProximitySensor currently not implemented. ");
                    //TODO Proximity Sensor not currently implemented
                    String name = "";
                    String description = "";
                    String[] parameter;
                    String[] url;
                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("url");
                    if (attributeValue != null) {
                        url = utility.parseMFString(attributeValue);
                    }
                }  //  end <ProximitySensor> node


                /********** Script **********/
                else if (qName.equalsIgnoreCase("Script")) {
                    String name = "";
                    Boolean directOutput = false;
                    Boolean mustEvaluate = false;
                    String[] url = null;

                    // The EcmaScript / JavaScript will be parsed inside
                    // SAX's characters method
                    parseJavaScript = true;
                    //reset.  This will hold complete JavaScript function(s)
                    javaScriptCode = "";

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("url");
                    if (attributeValue != null) {
                        url = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("directOutput");
                    if (attributeValue != null) {
                        directOutput = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("mustEvaluate");
                    if (attributeValue != null) {
                        mustEvaluate = utility.parseBooleanString(attributeValue);
                    }
                    currentScriptObject = new ScriptObject(name, directOutput, mustEvaluate, url);
                }  //  end <Script> node


                /******* field (embedded inside <Script>) node *******/
                else if (qName.equalsIgnoreCase("field")) {

                    String name = "";
                    ScriptObject.AccessType accessType = ScriptObject.AccessType.INPUT_OUTPUT;
                    String type = "";
                    String value = "";

                    attributeValue = attributes.getValue("accessType");
                    if (attributeValue != null) {
                        if (attributeValue.equals("inputOnly")) {
                            accessType = ScriptObject.AccessType.INPUT_ONLY;
                        } else if (attributeValue.equals("outputOnly")) {
                            accessType = ScriptObject.AccessType.OUTPUT_ONLY;
                        } else if (attributeValue.equals("inputOutput")) {
                            accessType = ScriptObject.AccessType.INPUT_OUTPUT;
                        } else if (attributeValue.equals("initializeOnly")) {
                            accessType = ScriptObject.AccessType.INITIALIZE_ONLY;
                        }
                    }
                    attributeValue = attributes.getValue("name");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("type");
                    if (attributeValue != null) {
                        type = attributeValue;
                    }
                    attributeValue = attributes.getValue("value");
                    if (attributeValue != null) {
                        value = attributeValue;
                    }
                    if (currentScriptObject != null) {
                        currentScriptObject.addField(name, accessType, type);
                    }
                    else if ( proto != null ) {
                        if ( proto.isProtoStateProtoInterface() ) {
                            // Add this field to the list of Proto field's
                            proto.AddField(accessType, name, type, value);
                        }
                    }
                }  //  end <field> node

                /********** MovieTexture **********/
                else if (qName.equalsIgnoreCase("MovieTexture")) {
                    attributeValue = attributes.getValue("USE");
                    if (attributeValue != null) {
                        DefinedItem useItem = null;
                        for (DefinedItem definedItem : mDefinedItems) {
                            if (attributeValue.equals(definedItem.getName())) {
                                useItem = definedItem;
                                break;
                            }
                        }
                        if (useItem != null) {
                            Log.e(TAG, "MovieTexture USE not implemented");
                            gvrTexture = useItem.getSXRTexture();
                            shaderSettings.setTexture(gvrTexture);
                        }
                        else {
                            Log.e(TAG, "Error: MovieTexture USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                        }
                    } else {
                        String description = "";
                        boolean loop = false;

                        String urlAttribute = attributes.getValue("url");
                        if (urlAttribute != null) {
                            String[] urlsString = utility.parseMFString(urlAttribute);

                            for (int i = 0; i < urlsString.length; i++) {
                                shaderSettings.movieTextures.add(urlsString[i]);
                            }
                        }
                        attributeValue = attributes.getValue("loop");
                        if (attributeValue != null) {
                            shaderSettings.setMovieTextureLoop(utility.parseBooleanString(attributeValue) );
                        }
                        String repeatSAttribute = attributes.getValue("repeatS");
                        if (repeatSAttribute != null) {
                                if (!utility.parseBooleanString(repeatSAttribute)) {
                                    //TODO: gvrTextureParameters.setWrapSType(TextureWrapType.GL_CLAMP_TO_EDGE);
                                }
                        }
                        String repeatTAttribute = attributes.getValue("repeatT");
                        if (repeatTAttribute != null) {
                                if (!utility.parseBooleanString(repeatTAttribute)) {
                                    //TODO: gvrTextureParameters.setWrapTType(TextureWrapType.GL_CLAMP_TO_EDGE);
                                }
                        }
                        shaderSettings.setMovieTextureName(attributes.getValue("DEF") );

                        if ( proto != null ) {
                            if ( proto.getAppearance() != null) {

                                if ( proto.getAppearance().getMovieTexture() == null) {
                                    String[] movieTextures = new String[1];
                                    movieTextures[0] = shaderSettings.movieTextures.get(0);
                                    MovieTexture movieTexture = new MovieTexture(loop, 1.0f, 1.0f,
                                            movieTextures);
                                    proto.getAppearance().setMovieTexture( movieTexture );
                                }
                                else {
                                    Log.e(TAG, "Proto error: MovieTexture not starting null inside Appearance node");
                                }
                            }
                            else {
                                Log.e(TAG, "Proto error: MovieTexture set without Appearance");
                            }
                        }

                    }
                } // end <MovieTexture> node


                /********** MultiTexture **********/
                else if (qName.equalsIgnoreCase("MultiTexture")) {
                    String name = "";
                    float alpha = 1;
                    float[] color = { 1, 1, 1 };
                    String[] function = {""};
                    MFString mode = new MFString("MODULATE");
                    String[] source = {""};

                    shaderSettings.setMultiTexture( true );

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        shaderSettings.setMultiTextureName( attributeValue );
                    }
                    attributeValue = attributes.getValue("alpha");
                    if (attributeValue != null) {
                        alpha = utility.parseSingleFloatString(attributeValue, true,
                                true);
                        Log.e(TAG, "MultiTexture alpha not implemented");
                    }
                    attributeValue = attributes.getValue("color");
                    if (attributeValue != null) {
                        color = utility.parseFixedLengthFloatString(attributeValue, 3, true,
                                false);
                        Log.e(TAG, "MultiTexture color not implemented");
                    }
                    attributeValue = attributes.getValue("function");
                    if (attributeValue != null) {
                        function = utility.parseMFString(attributeValue);
                        Log.e(TAG, "MultiTexture function not implemented");
                    }
                    attributeValue = attributes.getValue("mode");
                    if (attributeValue != null) {
                        String[] modeString = utility.parseMFString(attributeValue);
                        mode.setValue(modeString.length, modeString);
                    }
                    attributeValue = attributes.getValue("source");
                    if (attributeValue != null) {
                        source = utility.parseMFString(attributeValue);
                        Log.e(TAG, "MultiTexture source not implemented");
                    }

                    shaderSettings.setMultiTextureMode( mode );
                }  //  end <MultiTexture> node


                /********** BooleanToggle **********/
                else if (qName.equalsIgnoreCase("BooleanToggle")) {
                    String name = "";
                    boolean toggle = false;

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("toggle");
                    if (attributeValue != null) {
                        toggle = utility.parseBooleanString(attributeValue);
                    }
                    EventUtility eventUtility = new EventUtility(name, EventUtility.DataType.BOOLEAN, EventUtility.Type.TOGGLE, toggle);
                    eventUtilities.add(eventUtility);
                }  //  end <BooleanToggle> node


                /********** ElevationGrid **********/
                else if (qName.equalsIgnoreCase("ElevationGrid")) {
                    Log.e(TAG, "X3D ElevationGrid not currently implemented. ");
                } // end <ElevationGrid> node


                /********** Navigation Info **********/
                else if (qName.equalsIgnoreCase("NavigationInfo")) {
                    String name = "";
                    float[] avatarSize =
                            {
                                    0.25f, 1.6f, 0.75f
                            };
                    boolean headlight = true;
                    float speed = 1;
                    float transitionTime = 1;
                    float visibilityLimit = 0;

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        name = attributeValue;
                    }
                    attributeValue = attributes.getValue("avatarSize");
                    if (attributeValue != null) {
                        avatarSize = utility.parseFixedLengthFloatString(attributeValue, 3, false,
                                true);
                        Log.e(TAG, "NavigationInfo avatarSize attribute not implemented. ");
                    }
                    attributeValue = attributes.getValue("headlight");
                    if (attributeValue != null) {
                        headlight = utility.parseBooleanString(attributeValue);
                    }
                    attributeValue = attributes.getValue("speed");
                    if (attributeValue != null) {
                        speed = utility.parseSingleFloatString(attributeValue, false, true);
                        Log.e(TAG, "NavigationInfo speed attribute not implemented. ");
                    }
                    attributeValue = attributes.getValue("transitionTime");
                    if (attributeValue != null) {
                        transitionTime = utility.parseSingleFloatString(attributeValue, false, true);
                        Log.e(TAG,
                                "NavigationInfo transitionTime attribute not implemented. ");
                    }
                    attributeValue = attributes.getValue("type");
                    if (attributeValue != null) {
                        Log.e(TAG, "NavigationInfo type attribute not implemented. ");
                    }
                    attributeValue = attributes.getValue("visibilityLimit");
                    if (attributeValue != null) {
                        visibilityLimit = utility.parseSingleFloatString(attributeValue, false, true);
                        Log.e(TAG,
                                "NavigationInfo visibilityLimit attribute not implemented. ");
                    }
                    if (headlight) {
                        SXRNode headlightNode = new SXRNode(gvrContext);
                        SXRDirectLight headLight = new SXRDirectLight(gvrContext);
                        headlightNode.attachLight(headLight);
                        headLight.setDiffuseIntensity(1, 1, 1, 1);
                        headlightNode.setName("HeadLight");
                        SXRNode cameraHeadTransform = cameraRigAtRoot.getHeadTransformObject();
                        SXRPerspectiveCamera gvrCenterCamera = cameraRigAtRoot.getCenterCamera();
                        cameraHeadTransform.attachLight(headLight);
                    }

                } // end <NavigationInfo> node


                /********** Background **********/
                else if (qName.equalsIgnoreCase("Background")) {
                    float[] skycolor =
                            {
                                    0, 0, 0
                            };
                    String[] backUrl = {};
                    String[] bottomUrl = {};
                    String[] frontUrl = {};
                    String[] leftUrl = {};
                    String[] rightUrl = {};
                    String[] topUrl = {};
                    float transparency = 0;
                    float groundAngle = 0;

                    attributeValue = attributes.getValue("DEF");
                    if (attributeValue != null) {
                        Log.e(TAG, "Background DEF attribute not implemented. ");
                    }
                    attributeValue = attributes.getValue("groundColor");
                    if (attributeValue != null) {
                        Log.e(TAG, "Background groundColor attribute not implemented. ");
                    }
                    attributeValue = attributes.getValue("skyColor");
                    if (attributeValue != null) {
                        skycolor = utility.parseFixedLengthFloatString(attributeValue, 3, true,
                                false);
                    }
                    attributeValue = attributes.getValue("backUrl");
                    if (attributeValue != null) {
                        backUrl = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("bottomUrl");
                    if (attributeValue != null) {
                        bottomUrl = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("frontUrl");
                    if (attributeValue != null) {
                        frontUrl = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("leftUrl");
                    if (attributeValue != null) {
                        leftUrl = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("rightUrl");
                    if (attributeValue != null) {
                        rightUrl = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("topUrl");
                    if (attributeValue != null) {
                        topUrl = utility.parseMFString(attributeValue);
                    }
                    attributeValue = attributes.getValue("transparency");
                    if (attributeValue != null) {
                        transparency = utility.parseSingleFloatString(attributeValue, true, false);
                        Log.e(TAG, "Background transparency attribute not implemented. ");
                    }
                    attributeValue = attributes.getValue("groundAngle");
                    if (attributeValue != null) {
                        Log.e(TAG, "Background groundAngle attribute not implemented. ");
                        groundAngle = utility.parseSingleFloatString(attributeValue, false, true);
                        if (groundAngle > (float) Math.PI / 2) {
                            groundAngle = (float) Math.PI / 2;
                            Log.e(TAG, "Background groundAngle cannot exceed PI/2.");
                        }
                    }

                    // if url's defined, use cube mapping for the background
                    if ((backUrl.length > 0) && (bottomUrl.length > 0)
                            && (frontUrl.length > 0) && (leftUrl.length > 0)
                            && (rightUrl.length > 0) && (topUrl.length > 0)) {

                        ArrayList<SXRTexture> textureList = new ArrayList<SXRTexture>(6);
                        SXRAssetLoader loader = gvrContext.getAssetLoader();
                        String urlAttribute = backUrl[0].substring(0,
                                backUrl[0].indexOf("."));
                        int assetID = activityContext.getResources()
                                .getIdentifier(urlAttribute, "drawable",
                                        activityContext.getPackageName());
                        if (assetID != 0) {
                            textureList
                                    .add(loader.loadTexture(new SXRAndroidResource(
                                            gvrContext, assetID)));
                        }

                        urlAttribute = rightUrl[0].substring(0, rightUrl[0].indexOf("."));
                        assetID = activityContext.getResources()
                                .getIdentifier(urlAttribute, "drawable",
                                        activityContext.getPackageName());
                        if (assetID != 0) {
                            textureList
                                    .add(loader.loadTexture(new SXRAndroidResource(
                                            gvrContext, assetID)));
                        }

                        urlAttribute = frontUrl[0].substring(0, frontUrl[0].indexOf("."));
                        assetID = activityContext.getResources()
                                .getIdentifier(urlAttribute, "drawable",
                                        activityContext.getPackageName());
                        if (assetID != 0) {
                            textureList
                                    .add(loader.loadTexture(new SXRAndroidResource(
                                            gvrContext, assetID)));
                        }

                        urlAttribute = leftUrl[0].substring(0, leftUrl[0].indexOf("."));
                        assetID = activityContext.getResources()
                                .getIdentifier(urlAttribute, "drawable",
                                        activityContext.getPackageName());
                        if (assetID != 0) {
                            textureList
                                    .add(loader.loadTexture(new SXRAndroidResource(
                                            gvrContext, assetID)));
                        }

                        urlAttribute = topUrl[0].substring(0, topUrl[0].indexOf("."));
                        assetID = activityContext.getResources()
                                .getIdentifier(urlAttribute, "drawable",
                                        activityContext.getPackageName());
                        if (assetID != 0) {
                            textureList
                                    .add(loader.loadTexture(new SXRAndroidResource(
                                            gvrContext, assetID)));
                        }

                        urlAttribute = bottomUrl[0].substring(0, bottomUrl[0].indexOf("."));
                        assetID = activityContext.getResources()
                                .getIdentifier(urlAttribute, "drawable",
                                        activityContext.getPackageName());
                        if (assetID != 0) {
                            textureList
                                    .add(loader.loadTexture(new SXRAndroidResource(
                                            gvrContext, assetID)));
                        }

                        SXRCubeNode mCubeEvironment = new SXRCubeNode(
                                gvrContext, false, textureList);
                        mCubeEvironment.getRenderData().setMaterial(new SXRMaterial(gvrContext, x3DShader));
                        mCubeEvironment.getTransform().setScale(CUBE_WIDTH, CUBE_WIDTH,
                                CUBE_WIDTH);

                        root.addChildObject(mCubeEvironment);
                    } else {
                        // Not cubemapping, then set default skyColor
                        gvrContext.getMainScene().setBackgroundColor(skycolor[0], skycolor[1], skycolor[2], 1);
                    }

                } // end <Background> node

                /********** PROTO Node: ProtoDeclare **********/
                else if (qName.equalsIgnoreCase("ProtoDeclare")) {
                    proto = new Proto( X3Dobject.this );
                    proto.setProtoStateProtoDeclare();
                    attributeValue = attributes.getValue("name");
                    if (attributeValue != null) {
                        proto.setName( attributeValue );
                    }
                }
                /********** PROTO Node: ProtoInterface **********/
                else if (qName.equalsIgnoreCase("ProtoInterface")) {
                    if ( proto != null ) {
                        if ( proto.isProtoStateProtoDeclare()) {
                            proto.setProtoStateProtoInterface();
                        }
                    }
                }
                /********** PROTO Node: ProtoBody **********/
                else if (qName.equalsIgnoreCase("ProtoBody")) {
                    if ( proto != null ) {
                        if ( proto.isProtoStateProtoDeclare()) {
                            proto.setProtoStateProtoBody();
                        }
                    }
                }
                /********** PROTO Node: IS **********/
                else if (qName.equalsIgnoreCase("IS")) {
                    if ( proto != null ) {
                        if ( proto.isProtoStateProtoBody()) {
                            proto.setProtoStateProtoIS();
                        }
                    }
                }
                /********** PROTO Node: connect **********/
                else if (qName.equalsIgnoreCase("connect")) {
                    if ( proto != null ) {
                        if ( proto.isProtoStateProtoIS()) {
                            attributeValue = attributes.getValue("protoField");
                            if (attributeValue != null) {
                                // relate the protoField variable name to the item's property.
                                Proto.Field field = proto.getField(attributeValue);
                                if (field == null) {
                                    Log.e(TAG, "Error: possibly undefined Proto Interface value: " + attributeValue);
                                }
                                else {
                                    attributeValue = attributes.getValue("nodeField");
                                    if (attributeValue != null) {
                                        proto.setNodeField(field, attributeValue);
                                    }
                                    if (proto.getAppearance() != null) {
                                        if (proto.getAppearance().getTexture() != null) {
                                            if (proto.getNodeField(field).equalsIgnoreCase("url")) {
                                                proto.getAppearance().getTexture().setUrl(proto.getField_MFString(field));
                                            }
                                        }
                                        if (proto.getAppearance().getTextureTransform() != null) {
                                            if (proto.getNodeField(field).equalsIgnoreCase("scale")) {
                                                proto.getAppearance().getTextureTransform().setScale(proto.getField_SFVec2f(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("rotation")) {
                                                proto.getAppearance().getTextureTransform().setRotation(proto.getField_SFFloat(field)[0]);
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("translation")) {
                                                proto.getAppearance().getTextureTransform().setTranslation(proto.getField_SFVec2f(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("center")) {
                                                proto.getAppearance().getTextureTransform().setCenter(proto.getField_SFVec2f(field));
                                            }
                                        }  //  end if TextureTransform
                                        if (proto.getAppearance().getMovieTexture() != null) {
                                            if (proto.getNodeField(field).equalsIgnoreCase("url")) {
                                                proto.getAppearance().getMovieTexture().setUrl(proto.getField_MFString(field));
                                            }
                                        }  //  end if MovieTexture
                                    } // end proto.getAppearance() != null
                                    if (proto.getGeometry() != null) {
                                        Box box = proto.getGeometry().getBox();
                                        if (box != null) {
                                            if (proto.getNodeField(field).equalsIgnoreCase("size")) {
                                                box.setSize(proto.getField_SFVec3f(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("solid")) {
                                                box.setSolid(proto.getField_SFBool(field));
                                            }
                                        } // end Box
                                        Cone cone = proto.getGeometry().getCone();
                                        if (cone != null) {
                                            if (proto.getNodeField(field).equalsIgnoreCase("bottom")) {
                                                cone.setBottom(proto.getField_SFBool(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("bottomRadius")) {
                                                cone.setBottomRadius(proto.getField_SFFloat(field)[0]);
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("height")) {
                                                cone.setHeight(proto.getField_SFFloat(field)[0]);
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("side")) {
                                                cone.setSide(proto.getField_SFBool(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("solid")) {
                                                cone.setSolid(proto.getField_SFBool(field));
                                            }
                                        }  // end if cone != null
                                        Cylinder cylinder = proto.getGeometry().getCylinder();
                                        if (cylinder != null) {
                                            if (proto.getNodeField(field).equalsIgnoreCase("bottom")) {
                                                cylinder.setBottom(proto.getField_SFBool(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("height")) {
                                                cylinder.setHeight(proto.getField_SFFloat(field)[0]);
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("radius")) {
                                                cylinder.setRadius(proto.getField_SFFloat(field)[0]);
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("side")) {
                                                cylinder.setSide(proto.getField_SFBool(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("solid")) {
                                                cylinder.setSolid(proto.getField_SFBool(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("top")) {
                                                cylinder.setTop(proto.getField_SFBool(field));
                                            }
                                        } // end Cylinder
                                        Sphere sphere = proto.getGeometry().getSphere();
                                        if (sphere != null) {
                                            if (proto.getNodeField(field).equalsIgnoreCase("radius")) {
                                                sphere.setRadius(proto.getField_SFFloat(field)[0]);
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("solid")) {
                                                sphere.setSolid(proto.getField_SFBool(field));
                                            }
                                        } // end Sphere
                                        IndexedFaceSet indexedFaceSet = proto.getGeometry().getIndexedFaceSet();
                                        if (indexedFaceSet != null) {
                                            Log.e(TAG, "<connect> <IndexedFaceSet> not allowing fields to be set");
                                        } // end indexedFaceSet
                                        Text text = proto.getGeometry().getText();
                                        if (text != null) {
                                            FontStyle fontStyle = text.getFontStyle();
                                            if (proto.getNodeField(field).equalsIgnoreCase("string")) {
                                                text.setString(proto.getField_MFString(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("family")) {
                                                if (fontStyle != null)
                                                    fontStyle.setFamily(proto.getField_MFString(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("justify")) {
                                                if (fontStyle != null)
                                                    fontStyle.setJustify(proto.getField_MFString(field));
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("size")) {
                                                if (fontStyle != null)
                                                    fontStyle.setSize(proto.getField_SFFloat(field)[0] * 10);
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("spacing")) {
                                                if (fontStyle != null)
                                                    fontStyle.setSpacing( proto.getField_SFFloat(field)[0] );
                                            } else if (proto.getNodeField(field).equalsIgnoreCase("style")) {
                                                if (fontStyle != null)
                                                    fontStyle.setStyle(proto.getField_SFString(field));
                                            }
                                        } // end text
                                    }  // end proto.getGeometry() != null
                                }
                            }  //  end if (attributeValue != null)
                        }  //  end if ( proto.isProtoStateProtoIS())
                    }  //  end if proto != null
                }  // end else if Proto connect

                /********** PROTO Node: ProtoInstance **********/
                else if (qName.equalsIgnoreCase("ProtoInstance")) {
                    attributeValue = attributes.getValue("name");
                    if (attributeValue != null) {
                        for (Proto _proto : protos) {
                            if (_proto.getName().equalsIgnoreCase(attributeValue)) {
                                protoInstance = _proto;
                                Geometry geometryInstance = new Geometry();
                                Box box = _proto.getGeometry().getBox();
                                Cone cone = _proto.getGeometry().getCone();
                                Cylinder cylinder = _proto.getGeometry().getCylinder();
                                Sphere sphere = _proto.getGeometry().getSphere();
                                IndexedFaceSet indexedFaceSet = _proto.getGeometry().getIndexedFaceSet();
                                Text text = _proto.getGeometry().getText();
                                if ( box != null ) {
                                    try {
                                        Box cloneBox = (Box) box.clone();
                                        geometryInstance.setBox( cloneBox );
                                    }
                                    catch (CloneNotSupportedException ex) {
                                        Log.e(TAG, "Proto <Box> exception: " + ex);
                                    }
                                    catch (Exception ex) {
                                        Log.e(TAG, "Proto <Box> exception: " + ex);
                                    }
                                }
                                else if ( cone != null ) {
                                    try {
                                        Cone cloneCone = (Cone) cone.clone();
                                        geometryInstance.setCone( cloneCone );
                                    }
                                    catch (CloneNotSupportedException ex) {
                                        Log.e(TAG, "Proto <Cone> exception: " + ex);
                                    }
                                    catch (Exception ex) {
                                        Log.e(TAG, "Proto <Cone> exception: " + ex);
                                    }
                                }
                                else if ( cylinder != null ) {
                                    try {
                                        Cylinder cloneCylinder = (Cylinder) cylinder.clone();
                                        geometryInstance.setCylinder( cloneCylinder );
                                    }
                                    catch (CloneNotSupportedException ex) {
                                        Log.e(TAG, "Proto <Cylinder> exception: " + ex);
                                    }
                                    catch (Exception ex) {
                                        Log.e(TAG, "Proto <Cylinder> exception: " + ex);
                                    }
                                }
                                else if ( sphere != null ) {
                                    try {
                                        Sphere cloneSphere = (Sphere) sphere.clone();
                                        geometryInstance.setSphere( cloneSphere );
                                    }
                                    catch (CloneNotSupportedException ex) {
                                        Log.e(TAG, "Proto <Sphere> exception: " + ex);
                                    }
                                    catch (Exception ex) {
                                        Log.e(TAG, "Proto <Sphere> exception: " + ex);
                                    }
                                }
                                else if ( indexedFaceSet != null ) {
                                    try {
                                        IndexedFaceSet cloneIndexedFaceSet = (IndexedFaceSet) indexedFaceSet.clone();
                                        geometryInstance.setIndexedFaceSet( cloneIndexedFaceSet );
                                    }
                                    catch (CloneNotSupportedException ex) {
                                        Log.e(TAG, "Proto <IndexedFaceSet> Clone exception: " + ex);
                                    }
                                    catch (Exception ex) {
                                        Log.e(TAG, "Proto <IndexedFaceSet> exception: " + ex);
                                    }
                                }
                                else if ( text != null ) {
                                    try {
                                        FontStyle fontStyle = text.getFontStyle();
                                        FontStyle cloneFontStyle = (FontStyle) fontStyle.clone();
                                        Text cloneText = (Text) text.clone();
                                        cloneText.setFontStyle( cloneFontStyle );
                                        geometryInstance.setText( cloneText );
                                    }
                                    catch (CloneNotSupportedException ex) {
                                        Log.e(TAG, "Proto <Text> exception: " + ex);
                                    }
                                    catch (Exception ex) {
                                        Log.e(TAG, "Proto <Text> exception: " + ex);
                                    }
                                }

                                protoInstance.setGeometryInstance( geometryInstance );

                                // Set the default material values
                                if (protoInstance.getShape() != null ) {
                                    if (gvrRenderData == null) gvrRenderData = new SXRRenderData(gvrContext);
                                    gvrRenderData.setAlphaToCoverage(true);
                                    gvrRenderData.setRenderingOrder(SXRRenderingOrder.GEOMETRY);
                                    gvrRenderData.setCullFace(SXRCullFaceEnum.Back);
                                    shaderSettings.initializeTextureMaterial(new SXRMaterial(gvrContext, x3DShader));

                                    if (protoInstance.getShape().getAppearance() != null ) {
                                        Material material = protoInstance.getAppearance().getMaterial();
                                        ImageTexture imageTexture = protoInstance.getAppearance().getTexture();
                                        TextureTransform textureTransform = protoInstance.getAppearance().getTextureTransform();
                                        MovieTexture movieTexture = protoInstance.getAppearance().getMovieTexture();
                                        if (material != null) {
                                            shaderSettings.ambientIntensity = material.getAmbientIntensity();
                                            shaderSettings.diffuseColor = material.getDiffuseColor();
                                            shaderSettings.emissiveColor = material.getEmissiveColor();
                                            shaderSettings.shininess = material.getShininess();
                                            shaderSettings.specularColor = material.getSpecularColor();
                                            shaderSettings.setTransparency(material.getTransparency());
                                        }
                                        if ( imageTexture != null ) {
                                            gvrTextureParameters = new SXRTextureParameters(gvrContext);
                                            gvrTextureParameters.setWrapSType(TextureWrapType.GL_REPEAT);
                                            gvrTextureParameters.setWrapTType(TextureWrapType.GL_REPEAT);
                                            gvrTextureParameters.setMinFilterType(SXRTextureParameters.TextureFilterType.GL_LINEAR_MIPMAP_NEAREST);

                                            SXRTexture gvrTexture = new SXRTexture(gvrContext, gvrTextureParameters);
                                            SXRAssetLoader.TextureRequest request = new SXRAssetLoader.TextureRequest(assetRequest, gvrTexture, imageTexture.getUrl()[0]);

                                            assetRequest.loadTexture(request);
                                            shaderSettings.setTexture(gvrTexture);
                                        }
                                        if (textureTransform != null ){
                                            shaderSettings.setTextureCenter( textureTransform.getCenter() );
                                            shaderSettings.setTextureRotation( textureTransform.getRotation() );
                                            shaderSettings.setTextureScale( textureTransform.getScale() );
                                            shaderSettings.setTextureTranslation( textureTransform.getTranslation() );
                                        }
                                        if (movieTexture != null ){
                                            Log.e(TAG, "   <Proto> <MovieTexture> not currently supported.");
                                            shaderSettings.movieTextures.add(movieTexture.getUrl()[0]);
                                        }
                                    }
                                    else {
                                        Log.e(TAG, "Appearance missing from ProtoInstance");
                                    }
                                }
                                else {
                                    Log.e(TAG, "Shape missing from ProtoInstance");
                               }
                            }
                        }
                        if ( protoInstance == null ) {
                            Log.e(TAG, "<ProtoInstance name='" + attributeValue + "'> not matched with a <ProtoDeclare> ");
                        }
                    }
                    else {
                        Log.e(TAG, "<ProtoInstance> does not contain a name.");
                    }
                }  // end if PROTO Node: ProtoInstance

                /********** PROTO Node: fieldValue **********/
                else if (qName.equalsIgnoreCase("fieldValue")) {
                    attributeValue = attributes.getValue("name");
                    if (attributeValue != null) {
                        Proto.Field field = protoInstance.getField( attributeValue );

                        if ( protoInstance.getData_type(field) == Proto.data_types.SFNode) {
                            if ( protoInstance.getAppearance() != null ) {
                                if ( protoInstance.getAppearance().getMaterial() != null) {
                                    //TODO: Handle SFNode
                                }
                            }
                        }
                        attributeValue = attributes.getValue("value");
                        if (attributeValue != null) {
                            if ( protoInstance.getAppearance() != null) {
                                Appearance appearance = protoInstance.getAppearance();
                                if ( appearance.getTexture() != null ) {
                                    if (  protoInstance.getNodeField(field).equalsIgnoreCase("url")) {
                                        gvrTextureParameters = new SXRTextureParameters(gvrContext);
                                        gvrTextureParameters.setWrapSType(TextureWrapType.GL_REPEAT);
                                        gvrTextureParameters.setWrapTType(TextureWrapType.GL_REPEAT);
                                        gvrTextureParameters.setMinFilterType(SXRTextureParameters.TextureFilterType.GL_LINEAR_MIPMAP_NEAREST);

                                        SXRTexture gvrTexture = new SXRTexture(gvrContext, gvrTextureParameters);
                                        SXRAssetLoader.TextureRequest request = new SXRAssetLoader.TextureRequest(assetRequest, gvrTexture, attributeValue);

                                        assetRequest.loadTexture(request);
                                        shaderSettings.setTexture(gvrTexture);
                                    }
                                }
                                if ( appearance.getMaterial() != null) {
                                    Material material = appearance.getMaterial();
                                    if (  protoInstance.getNodeField(field).equalsIgnoreCase("ambientIntensity"))
                                        shaderSettings.ambientIntensity = utility.parseSingleFloatString(attributeValue, true, false);
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("diffuseColor"))
                                        shaderSettings.diffuseColor = utility.parseFixedLengthFloatString(attributeValue, 3, true, false);
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("emissiveColor"))
                                        shaderSettings.emissiveColor = utility.parseFixedLengthFloatString(attributeValue, 3, true, false);
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("shininess"))
                                        shaderSettings.shininess = utility.parseSingleFloatString(attributeValue, true, false);
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("specularColor"))
                                        shaderSettings.specularColor = utility.parseFixedLengthFloatString(attributeValue, 3, true, false);
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("transparency"))
                                        shaderSettings.setTransparency(utility.parseSingleFloatString(attributeValue, true, false) );
                                }
                                if ( appearance.getTextureTransform() != null ) {
                                    TextureTransform textureTransform = appearance.getTextureTransform();
                                    if (  protoInstance.getNodeField(field).equalsIgnoreCase("center"))
                                        shaderSettings.setTextureCenter( utility.parseFixedLengthFloatString(attributeValue, 2, false, false) );
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("rotation"))
                                        shaderSettings.setTextureRotation( utility.parseSingleFloatString(attributeValue, false, false) );
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("scale"))
                                        shaderSettings.setTextureScale( utility.parseFixedLengthFloatString(attributeValue, 2, false, false) );
                                    else if (  protoInstance.getNodeField(field).equalsIgnoreCase("translation"))
                                        shaderSettings.setTextureTranslation( utility.parseFixedLengthFloatString(attributeValue, 2, false, false) );
                                }
                            }  //  protoInstance.getAppearance() != null

                            if ( protoInstance.getGeometryInstance() != null) {
                                Box box = protoInstance.getGeometryInstance().getBox();
                                Cone cone = protoInstance.getGeometryInstance().getCone();
                                Cylinder cylinder = protoInstance.getGeometryInstance().getCylinder();
                                Sphere sphere = protoInstance.getGeometryInstance().getSphere();
                                IndexedFaceSet indexedFaceSet = protoInstance.getGeometryInstance().getIndexedFaceSet();
                                Text text = protoInstance.getGeometryInstance().getText();
                                if ( box != null ) {
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("size")) {
                                        box.setSize(
                                                utility.parseFixedLengthFloatString(attributeValue, 3, false, false)
                                        );
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("solid")) {
                                        box.setSolid(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                }  //  end Box
                                if ( cone != null ) {
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("bottom")) {
                                        cone.setBottom(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("bottomRadius")) {
                                        cone.setBottomRadius(
                                                utility.parseSingleFloatString(attributeValue, false, true));
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("height")) {
                                        cone.setHeight(
                                                utility.parseSingleFloatString(attributeValue, false, true));
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("side")) {
                                        cone.setSide(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("solid")) {
                                        cone.setSolid(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                }  //  end Cone
                                if ( cylinder != null ) {
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("bottom")) {
                                        cylinder.setBottom(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("height")) {
                                        cylinder.setHeight(
                                                utility.parseSingleFloatString(attributeValue, false, true));
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("radius")) {
                                        cylinder.setRadius(
                                                utility.parseSingleFloatString(attributeValue, false, true));
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("side")) {
                                        cylinder.setSide(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("solid")) {
                                        cylinder.setSolid(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("top")) {
                                        cylinder.setTop(
                                                utility.parseBooleanString( attributeValue ) );
                                    }
                                }  //  end Cylinder
                                if ( sphere != null ) {
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("radius")) {
                                        sphere.setRadius(utility.parseSingleFloatString(attributeValue, false, true));
                                    }
                                    else if (protoInstance.getNodeField(field).equalsIgnoreCase("solid")) {
                                        sphere.setSolid( utility.parseBooleanString( attributeValue ) );
                                    }
                                }  //  end Sphere
                                if ( indexedFaceSet != null ) {
                                    //TODO: <PROTO> <fieldValue> for <IndexedFaceSet> field not implemented.
                                }
                                if ( text != null ) {
                                    FontStyle fontStyle = text.getFontStyle();
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("string")) {
                                        String[] strArray = utility.parseMFString(attributeValue);
                                        text.setString( strArray );
                                    }
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("family")) {
                                        String[] strArray = utility.parseMFString(attributeValue);
                                        if (fontStyle != null) fontStyle.setFamily( strArray );
                                    }
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("justify")) {
                                        String[] strArray = utility.parseMFString(attributeValue);
                                        if (fontStyle != null) fontStyle.setJustify( strArray );
                                    }
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("size")) {
                                        float size = utility.parseSingleFloatString(attributeValue, false, true);
                                        if (fontStyle != null) fontStyle.setSize( size*10);
                                    }
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("spacing")) {
                                        float spacing = utility.parseSingleFloatString(attributeValue, false, true);
                                        if (fontStyle != null) fontStyle.setSpacing( spacing );
                                    }
                                    if (protoInstance.getNodeField(field).equalsIgnoreCase("style")) {
                                        String[] strArray = utility.parseMFString(attributeValue);
                                        if (fontStyle != null) fontStyle.setStyle(strArray[0]);
                                    }
                                }  // end if text != null
                            } // end if geometry !null
                        }
                    }
                }  //  end if PROTO Node: fieldValue

                // These next few nodes are used once per file and thus moved
                //  to the end of the parsing's if-then-else statement

                /********** X3D **********/
                else if (qName.equalsIgnoreCase("x3d")) {
                    attributeValue = attributes.getValue("version");
                    if (attributeValue != null) {
                        // currently, we don't do anything with the version information
                    }
                    attributeValue = attributes.getValue("profile");
                    if (attributeValue != null) {
                        // currently, we don't do anything with the profile information
                    }

                }  //  end <X3D> node

                /********** Scene **********/
                else if (qName.equalsIgnoreCase("scene")) {
                    ;

                }  //  end <Sene> node

                /***** end of parsing the nodes currently parsed *****/
                else {
                    Log.e(TAG, "X3D node '" + qName + "' not implemented.");
                }
            }  // end 'else { if stmt' at ROUTE, which should be deleted
        }  //  end startElement

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (qName.equalsIgnoreCase("Transform")) {
                if (!gvrGroupingNodeUSEd) {
                    if (currentNode.getParent() == root)
                        currentNode = null;
                    else {
                        String name = currentNode.getName();
                        currentNode = currentNode.getParent();
                        while (currentNode.getName()
                                .equals(name + TRANSFORM_ROTATION_)
                                || currentNode.getName()
                                .equals(name + TRANSFORM_TRANSLATION_)
                                || currentNode.getName().equals(name + TRANSFORM_SCALE_)
                                || currentNode.getName().equals(name + TRANSFORM_CENTER_)
                                || currentNode.getName()
                                .equals(name + TRANSFORM_NEGATIVE_CENTER_)
                                || currentNode.getName()
                                .equals(name + TRANSFORM_SCALE_ORIENTATION_)
                                || currentNode.getName()
                                .equals(name + TRANSFORM_NEGATIVE_SCALE_ORIENTATION_)) {
                            currentNode = currentNode.getParent();
                        }
                    }
                }
                gvrGroupingNodeUSEd = false;
            } // end </Transform> parsing
            else if (qName.equalsIgnoreCase("Group")) {
                if (currentNode.getParent() == root)
                    currentNode = null;
                else
                    currentNode = currentNode.getParent();
            } else if (qName.equalsIgnoreCase("Shape")) {
                if (proto == null) ShapePostParsing();
            } // end of ending Shape node
            else if (qName.equalsIgnoreCase("Appearance")) {
                ;
            } else if (qName.equalsIgnoreCase("Material")) {
                ;
            } else if (qName.equalsIgnoreCase("ImageTexture")) {
                ;
            } else if (qName.equalsIgnoreCase("TextureTransform")) {
                ;
            } else if (qName.equalsIgnoreCase("IndexedFaceSet")) {
                if (indexedSetUSEName.length() > 0) {
                    //Using previously defined mesh
                    DefinedItem useItem = null;
                    for (DefinedItem definedItem : mDefinedItems) {
                        if (indexedSetUSEName.equals(definedItem.getName())) {
                            useItem = definedItem;
                            break;
                        }
                    }
                    if (useItem != null) {
                        gvrRenderData.setMesh( useItem.getSXRMesh() );
                    }
                    else {
                        Log.e(TAG, "Error: IndexedFaceSet USE='" + attributeValue + "'; No matching DEF='" + attributeValue + "'.");
                    }
                }
                else {
                    if (reorganizeVerts) {
                        gvrVertexBuffer = utility.meshCreator.organizeVertices(gvrIndexBuffer);
                        reorganizeVerts = false;
                    }
                    SXRMesh mesh = new SXRMesh(gvrContext, gvrVertexBuffer.getDescriptor());
                    if (indexedSetDEFName.length() > 0) {
                        // Save SXRMesh since it may be reused later.
                        DefinedItem definedItem = new DefinedItem(indexedSetDEFName);
                        definedItem.setSXRMesh(mesh);
                        mDefinedItems.add(definedItem); // Array list of DEFined items
                    }
                    gvrRenderData.setMesh(mesh);
                    mesh.setIndexBuffer(gvrIndexBuffer);
                    mesh.setVertexBuffer(gvrVertexBuffer);
                }
                gvrVertexBuffer = null;
                gvrIndexBuffer = null;
                indexedSetDEFName = "";
                indexedSetUSEName = "";
            } else if (qName.equalsIgnoreCase("Coordinate")) {
                // vertices.clear(); // clean up this Vector<Vertex> list.
            } else if (qName.equalsIgnoreCase("TextureCoordinate")) {
                // textureCoord.clear(); // clean up this Vector<TextureValues> list.
            } else if (qName.equalsIgnoreCase("Normal")) {
                // vertexNormal.clear(); // clean up this Vector<VertexNormal> list.
            } else if (qName.equalsIgnoreCase("DirectionalLight")) {
                ;
            } else if (qName.equalsIgnoreCase("PointLight")) {
                ;
            } else if (qName.equalsIgnoreCase("SpotLight")) {
                ;
            } else if (qName.equalsIgnoreCase("TimeSensor")) {
                ;
            } else if (qName.equalsIgnoreCase("PositionInterpolator")) {
                ;
            } else if (qName.equalsIgnoreCase("OrientationInterpolator")) {
                ;
            } else if (qName.equalsIgnoreCase("ROUTE")) {
                ;
            } else if (qName.equalsIgnoreCase("TouchSensor")) {
                ;
            } else if (qName.equalsIgnoreCase("PlaneSensor")) {
                ;
            } else if (qName.equalsIgnoreCase("CylinderSensor")) {
                ;
            } else if (qName.equalsIgnoreCase("SphereSensor")) {
                ;
            } else if (qName.equalsIgnoreCase("ProximitySensor")) {
                ;
            } else if (qName.equalsIgnoreCase("Text")) {
                if ( proto == null ) {
                    gvrTextViewNode = new SXRTextViewNode(gvrContext,
                            Text_FontParams.nameFontStyle,
                            Text_FontParams.string, Text_FontParams.family, Text_FontParams.justify,
                            Text_FontParams.spacing, Text_FontParams.size, Text_FontParams.style);

                    SXRRenderData gvrRenderData = gvrTextViewNode.getRenderData();
                    gvrRenderData.setRenderingOrder(SXRRenderData.SXRRenderingOrder.TRANSPARENT);


                    if (!Text_FontParams.nameTextAttribute.equals("")) {
                        // add it to the list of DEFined objects
                        DefinedItem definedItem = new DefinedItem(Text_FontParams.nameTextAttribute);
                        definedItem.setSXRTextViewNode(gvrTextViewNode);
                        mDefinedItems.add(definedItem); // Array list of DEFined items
                    }
                    if (!Text_FontParams.nameFontStyle.equals("")) {
                        // add FontStyle to the list of DEFined objects
                        DefinedItem definedItem = new DefinedItem(Text_FontParams.nameFontStyle);
                        definedItem.setSXRTextViewNode(gvrTextViewNode);
                        mDefinedItems.add(definedItem); // Array list of DEFined items
                    }

                    gvrTextViewNode.setTextColor(Color.WHITE); // default
                    gvrTextViewNode.setBackgroundColor(Color.TRANSPARENT); // default
                    currentNode.addChildObject(gvrTextViewNode);
                }
            } else if (qName.equalsIgnoreCase("FontStyle")) {
                ;
            } else if (qName.equalsIgnoreCase("Billboard")) {
                ;
            } else if (qName.equalsIgnoreCase("Anchor")) {
                if (currentNode.getParent() == root)
                    currentNode = null;
                else
                    currentNode = currentNode.getParent();
            } else if (qName.equalsIgnoreCase("Inline")) {
                ;
            } else if (qName.equalsIgnoreCase("LOD")) {
                // End of LOD so go to the parent of the current
                // SXRNode which was added to support LOD
                if (currentNode == lodManager.transformLODNode) {
                    currentNode = currentNode.getParent();
                }
                lodManager.transformLODNode = null;
            } else if (qName.equalsIgnoreCase("Switch")) {
                // Verify the Switch index is between 0 and (max number of children - 1)
                // if it is not, then no object should appear per the X3D spec.
                SXRSwitch gvrSwitch = (SXRSwitch)currentNode.getComponent(SXRSwitch.getComponentType());
                if ( gvrSwitch != null) {
                    if ( (gvrSwitch.getSwitchIndex() < 0) || (gvrSwitch.getSwitchIndex() >= currentNode.getChildrenCount()) ) {
                        // if the switch index is outside the array of possible children (which is legal in X3D)
                        // then no object should appear, which occurs when SXRSwitch is set higher than possilble # children.
                        gvrSwitch.setSwitchIndex( currentNode.getChildrenCount() );
                    }
                }
                currentNode = currentNode.getParent();
            } else if (qName.equalsIgnoreCase("Box")) {
                ;
            } else if (qName.equalsIgnoreCase("Cone")) {
                ;
            } else if (qName.equalsIgnoreCase("Cylinder")) {
                ;
            } else if (qName.equalsIgnoreCase("Sphere")) {
                ;
            }

            /*********
             * Less frequently used commands and thus moved to end of a long
             * if-then-else.
             ********/
            else if (qName.equalsIgnoreCase("Viewpoint")) {
                ;
            } else if (qName.equalsIgnoreCase("Script")) {
                javaScriptCode = JAVASCRIPT_IMPORT_PACKAGE + '\n' + javaScriptCode  + '\n';
                currentScriptObject.setJavaScriptCode(javaScriptCode);
                if ( animationInteractivityManager.V8JavaScriptEngine) {
                    SXRJavascriptV8File gvrJavascriptV8File = new SXRJavascriptV8File(gvrContext, javaScriptCode);
                    currentScriptObject.setSXRJavascriptV8File( gvrJavascriptV8File );
                }
                else {
                    // using Mozila Rhino js engine
                    SXRJavascriptScriptFile gvrJavascriptScriptFile = new SXRJavascriptScriptFile(gvrContext, javaScriptCode);
                    currentScriptObject.setSXRJavascriptScriptFile(gvrJavascriptScriptFile);
                }
                scriptObjects.add(currentScriptObject);

                parseJavaScript = false;
                currentScriptObject = null;
            } else if (qName.equalsIgnoreCase("field")) {
                ; // embedded inside a <SCRIPT> node
            } else if (qName.equalsIgnoreCase("MultiTexture")) {
                ;
            } else if (qName.equalsIgnoreCase("BooleanToggle")) {
                ;
            } else if (qName.equalsIgnoreCase("NavigationInfo")) {
                ;
            } else if (qName.equalsIgnoreCase("Background")) {
                ;
            } else if (qName.equalsIgnoreCase("MovieTexture")) {
                ;
            } else if (qName.equalsIgnoreCase("ElevationGrid")) {
                ;
            }
            else if (qName.equalsIgnoreCase("ProtoDeclare")) {
                if (proto != null) {
                    proto.setProtoStateNone();
                    protos.add(proto);
                }
                else Log.e(TAG, "Error with </ProtoDeclare>");
                proto = null;
            }
            else if (qName.equalsIgnoreCase("ProtoInterface")) {
                if (proto != null) proto.setProtoStateProtoDeclare();
                else Log.e(TAG, "Error with </ProtoInterface>");
            }
            else if (qName.equalsIgnoreCase("ProtoBody")) {
                if (proto != null) proto.setProtoStateProtoDeclare();
                else Log.e(TAG, "Error with </ProtoBody>");
            }
            else if (qName.equalsIgnoreCase("IS")) {
                if (proto != null) proto.setProtoStateProtoBody();
                else {
                    Log.e(TAG, "Error with Proto </IS>");
                }
            }
            else if (qName.equalsIgnoreCase("connect")) {
                ;
            }
            else if (qName.equalsIgnoreCase("ProtoInstance")) {
                if ( protoInstance.getGeometryInstance() != null) {
                    Box box = protoInstance.getGeometryInstance().getBox();
                    Cylinder cylinder = protoInstance.getGeometryInstance().getCylinder();
                    Cone cone = protoInstance.getGeometryInstance().getCone();
                    Sphere sphere = protoInstance.getGeometryInstance().getSphere();
                    IndexedFaceSet indexedFaceSet = protoInstance.getGeometryInstance().getIndexedFaceSet();
                    Text text = protoInstance.getGeometryInstance().getText();
                    if ( box != null ) {
                        float[] size = box.getSize();
                        Vector3f sizeVector = new Vector3f(size[0], size[1], size[2]);
                        SXRCubeNode gvrCubeNode = new SXRCubeNode(
                                gvrContext, box.getSolid(), sizeVector);
                        gvrCubeNode.getRenderData().setMaterial(new SXRMaterial(gvrContext, x3DShader));
                        currentNode.addChildObject(gvrCubeNode);
                        meshAttachedNode = gvrCubeNode;
                    }
                    if ( cylinder != null ) {
                        SXRCylinderNode.CylinderParams params = new SXRCylinderNode.CylinderParams();
                        params.BottomRadius = cylinder.getRadius();
                        params.TopRadius = cylinder.getRadius();
                        params.Height = cylinder.getHeight();
                        params.HasBottomCap = cylinder.getBottom();
                        params.HasTopCap = cylinder.getTop();
                        params.FacingOut = cylinder.getSolid();

                        params.Material = new SXRMaterial(gvrContext, x3DShader);
                        SXRCylinderNode gvrCylinderNode = new SXRCylinderNode(
                                gvrContext, params);
                        currentNode.addChildObject(gvrCylinderNode);
                        meshAttachedNode = gvrCylinderNode;
                    }
                    if ( cone != null ) {
                        SXRCylinderNode.CylinderParams params = new SXRCylinderNode.CylinderParams();
                        params.BottomRadius = cone.getBottomRadius();
                        params.TopRadius = 0;
                        params.Height = cone.getHeight();
                        params.HasBottomCap = cone.getBottom();
                        params.HasTopCap = false;
                        params.FacingOut = cone.getSolid();

                        params.Material = new SXRMaterial(gvrContext, x3DShader);
                        SXRCylinderNode gvrCylinderNode = new SXRCylinderNode(
                                gvrContext, params);
                        currentNode.addChildObject(gvrCylinderNode);
                        meshAttachedNode = gvrCylinderNode;
                    }
                    if ( sphere != null ) {
                        SXRSphereNode gvrSphereNode = new SXRSphereNode(
                                gvrContext, sphere.getSolid(),
                                new SXRMaterial(gvrContext, x3DShader), sphere.getRadius());
                        currentNode.addChildObject(gvrSphereNode);
                        meshAttachedNode = gvrSphereNode;
                    }
                    if ( indexedFaceSet != null ) {

                        int[] coordIndex = indexedFaceSet.getCoordIndex();
                        for (int i = 0; i < coordIndex.length; i++) {
                            utility.meshCreator.mPositionIndices.add( coordIndex[i] );
                        }

                        int[] texCoordIndex = indexedFaceSet.getTexCoordIndex();
                        for (int i = 0; i < texCoordIndex.length; i++) {
                            utility.meshCreator.mTexcoordIndices.add( texCoordIndex[i] );
                        }

                        int[] normalIndex = indexedFaceSet.getNormalIndex();
                        for (int i = 0; i < normalIndex.length; i++) {
                            utility.meshCreator.mNormalIndices.add( normalIndex[i] );
                        }

                        gvrIndexBuffer = new SXRIndexBuffer(gvrContext, 4, 0);
                        float[] coords = indexedFaceSet.getCoord().getMeshCreatorInputPositions();
                        float[] normals = indexedFaceSet.getNormal().getMeshCreatorInputNormals();
                        float[] texCoords = indexedFaceSet.getTexCoord().getMeshCreatorInputTexCoords();

                        utility.meshCreator.addInputPosition( coords );
                        utility.meshCreator.addInputNormal(normals);
                        utility.meshCreator.addInputTexcoord(texCoords);
                        gvrVertexBuffer = utility.meshCreator.organizeVertices(gvrIndexBuffer);

                        SXRMesh mesh = new SXRMesh(gvrContext, gvrVertexBuffer.getDescriptor());

                        // set up of SXRRenderDate from <Shape>
                        // Need to test if this remains here are OK when we have a <Shape> node

                        if (gvrRenderData == null) gvrRenderData = new SXRRenderData(gvrContext);
                        gvrRenderData.setAlphaToCoverage(true);
                        gvrRenderData.setRenderingOrder(SXRRenderingOrder.GEOMETRY);
                        gvrRenderData.setCullFace(SXRCullFaceEnum.Back);

                        gvrRenderData.setMesh(mesh);
                        mesh.setIndexBuffer(gvrIndexBuffer);
                        mesh.setVertexBuffer(gvrVertexBuffer);

                        gvrRenderData.setMesh( mesh );
                        SXRNode gvrNode = new SXRNode(gvrContext);
                        gvrNode.attachRenderData(gvrRenderData);
                        currentNode.addChildObject(gvrNode);
                    }
                    if ( text != null ) {
                        Init_Text_FontParams();

                        FontStyle fontStyle = text.getFontStyle();
                        shaderSettings.initializeTextureMaterial(new SXRMaterial(gvrContext, x3DShader));

                        String textString = "";
                        for (int i = 0; i < text.getString().length; i++) {
                            textString += text.getString()[i];
                            if (i != (text.getString().length - 1) ) {
                                textString += '\n';
                            }
                        }

                        SXRTextViewNode.justifyTypes jutifyType = SXRTextViewNode.justifyTypes.BEGIN;
                        if ( fontStyle.getJustify()[0].equalsIgnoreCase("MIDDLE")) jutifyType = SXRTextViewNode.justifyTypes.MIDDLE;
                        else if ( fontStyle.getJustify()[0].equalsIgnoreCase("END")) jutifyType = SXRTextViewNode.justifyTypes.END;

                        String fontFamily = Text_FontParams.family;
                        if (fontStyle.getFamily() != null) fontFamily = fontStyle.getFamily()[0];

                        SXRTextViewNode.fontStyleTypes fontStyleType = SXRTextViewNode.fontStyleTypes.PLAIN;
                        if ( fontStyle.getStyle().equalsIgnoreCase("BOLD")) fontStyleType = SXRTextViewNode.fontStyleTypes.BOLD;
                        else if ( fontStyle.getStyle().equalsIgnoreCase("ITALIC")) fontStyleType = SXRTextViewNode.fontStyleTypes.ITALIC;
                        else if ( fontStyle.getStyle().equalsIgnoreCase("BOLDITALIC")) fontStyleType = SXRTextViewNode.fontStyleTypes.BOLDITALIC;

                        gvrTextViewNode = new SXRTextViewNode(gvrContext,
                                "PROTOtext",
                                textString,
                                fontFamily,
                                jutifyType,
                                fontStyle.getSpacing(),
                                fontStyle.getSize(),
                                fontStyleType);

                        gvrTextViewNode.setTextColor(Color.WHITE); // default
                        gvrTextViewNode.setBackgroundColor(Color.TRANSPARENT); // default

                        if (currentNode == null) root.addChildObject(gvrTextViewNode);
                        else currentNode.addChildObject(gvrTextViewNode);

                        gvrRenderData.setRenderingOrder(SXRRenderData.SXRRenderingOrder.TRANSPARENT);

                    }  //  end text != null
                }  //  end if ( protoInstance.getGeometryInstance() != null)

                protoInstance = null;
                ShapePostParsing();
            }  //  end ProtoInstance
            else if (qName.equalsIgnoreCase("fieldValue")) {
                ;
            }

            /*********
             * These are once per file commands and thus moved to the end of the
             * if-then-else statement
             ********/
            else if (qName.equalsIgnoreCase("scene")) {
                // Now that the scene is over, we can set construct the animations since
                // we now have all the ROUTES, and set up either the default or an actual
                // camera based on a <Viewpoint> in the scene.

                // First, set up the camera / Viewpoint
                // The camera rig is indirectly attached to the root

                if (cameraRigAtRoot != null) {

                    SXRCameraRig mainCameraRig = gvrContext.getMainScene().getMainCameraRig();

                    float[] cameraPosition = {0, 0, 10}; // X3D's default camera position
                    if ( !viewpoints.isEmpty()) {

                        // X3D file contained a <Viewpoint> node.
                        // Per X3D spec., when there is 1 or more Viewpoints in the
                        // X3D file, init with the first viewpoint in the X3D file
                        Viewpoint viewpoint = viewpoints.firstElement();
                        viewpoint.setIsBound(true);
                        cameraPosition = viewpoint.getPosition();
                    } // <Viewpoint> node existed
                    mainCameraRig.getTransform().setPosition(cameraPosition[0], cameraPosition[1], cameraPosition[2]);
                    SXRCursorController gazeController = null;
                    SXRInputManager inputManager = gvrContext.getInputManager();

                    // Set up cursor based on camera position
                    List<SXRCursorController> controllerList = inputManager.getCursorControllers();

                    for(SXRCursorController controller: controllerList){
                        if(controller.getControllerType() == SXRControllerType.GAZE);
                        {
                            gazeController = controller;
                            gazeController.setCursorControl(SXRCursorController.CursorControl.PROJECT_CURSOR_ON_SURFACE);
                            break;
                        }
                    }
                    if ( gazeController != null) {
                        gazeController.setOrigin(cameraPosition[0], cameraPosition[1], cameraPosition[2]);
                    }
                } // end setting based on new camera rig

            } // end </scene>
            else if (qName.equalsIgnoreCase("x3d")) {
                ;
            } // end </x3d>
            else {
                Log.e(TAG, "Not parsing ending '" + qName + "' tag.");
                ;
            } // end </x3d>
        }  // end endElement


        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            if (parseJavaScript) {
                // Each JavaScript must start with the importPackage line
                // to include the X3D Data types like SFColor
                String js = "";
                boolean leadingNonprintChars = true;
                for (int i = start; i < length; i++) {
                    if ((ch[i] == ' ') || (ch[i] == '\t')) {
                        if (!leadingNonprintChars && (ch[i] == ' ')) {
                            js += ch[i];
                        }
                    } else {
                        js += ch[i];
                        leadingNonprintChars = false;
                    }
                }
                javaScriptCode += js;
            }
        }  //  end characters method

    } // end UserHandler

    public void Parse(InputStream inputStream, ShaderSettings shaderSettings) {
        try {
            this.shaderSettings = shaderSettings;

            // Parse the initial X3D file
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            UserHandler userhandler = new UserHandler();
            saxParser.parse(inputStream, userhandler);

            // parse the Inline files
            if (inlineObjects.size() != 0) {
                for (int i = 0; i < inlineObjects.size(); i++) {
                    InlineObject inlineObject = inlineObjects.get(i);
                    String[] urls = inlineObject.getURL();
                    for (int j = 0; j < urls.length; j++) {
                        SXRAndroidResource gvrAndroidResource = null;
                        SXRResourceVolume gvrResourceVolume = null;
                        try {
                            String filename = urls[j];
                            inlineSubdirectory = "";
                            int lastIndex = urls[j].lastIndexOf('/');
                            if (lastIndex != -1) {
                                inlineSubdirectory = urls[j].substring(0, urls[j].lastIndexOf('/')+1);
                                filename = urls[j].substring(urls[j].lastIndexOf('/')+1, urls[j].length());
                            }

                            // Get rid of any single or double quotes surrounding the filename.
                            // this happens when the url = '"filename.x3d"' for example
                            if ( (filename.indexOf("\"") == 0) || (filename.indexOf("\'") == 0) ) {
                                filename = filename.substring(1, filename.length());
                            }
                            if ( (filename.indexOf("\"") == (filename.length()-1)) || (filename.indexOf("\'") == (filename.length()-1)) ) {
                                filename = filename.substring(0, filename.length()-1);
                            }

                            gvrResourceVolume = new SXRResourceVolume(gvrContext, urls[j]);
                            gvrAndroidResource = gvrResourceVolume.openResource( filename );

                            if ( filename.toLowerCase().endsWith(".x3d")) {
                                inputStream = gvrAndroidResource.getStream();
                                currentNode = inlineObject.getInlineSXRNode();
                                saxParser.parse(inputStream, userhandler);
                            }
                            else {
                                SXRExternalScene gvrExternalScene = new SXRExternalScene(gvrContext, urls[j], false);
                                currentNode = inlineObject.getInlineSXRNode();
                                if (currentNode == null) root.attachComponent(gvrExternalScene);
                                else currentNode.attachComponent(gvrExternalScene);
                                SXRScene gvrScene = gvrContext.getMainScene();
                                gvrExternalScene.load(gvrScene);
                                SXRAnimator gvrAnimator = gvrExternalScene.getAnimator();
                            }
                        } catch (FileNotFoundException e) {
                            Log.e(TAG,
                                    "Inline file reading: File Not Found: url " + urls[j] + ", Exception "
                                            + e);
                        } catch (IOException ioException) {
                            Log.e(TAG,
                                    "Inline file reading url " + urls[j]);
                            Log.e(TAG, "IOException: " + ioException.toString());
                        } catch (Exception exception) {
                            Log.e(TAG, "Inline file reading error: Exception "
                                    + exception);
                        }
                    }
                }
            }

            try {
                animationInteractivityManager.initAnimationsAndInteractivity();
                // Need to build a JavaScript function that constructs the
                // X3D data type objects used with a SCRIPT.
                // Scripts can also have an initialize() method.
                animationInteractivityManager.InitializeScript();
            }
            catch (Exception exception) {
                Log.e(TAG, "Error initialing X3D <ROUTE> or <Script> node related to Animation or Interactivity.");
            }

        } catch (Exception exception) {

            Log.e(TAG, "X3D/XML Parsing Exception = " + exception);
        }

    } // end Parse
}
