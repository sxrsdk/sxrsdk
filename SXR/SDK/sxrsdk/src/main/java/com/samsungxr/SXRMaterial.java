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

import com.samsungxr.shaders.SXRColorBlendShader;
import com.samsungxr.shaders.SXRColorShader;
import com.samsungxr.shaders.SXRCubemapReflectionShader;
import com.samsungxr.shaders.SXRCubemapShader;
import com.samsungxr.shaders.SXRHorizontalFlipShader;
import com.samsungxr.shaders.SXRLightmapShader;
import com.samsungxr.shaders.SXROESHorizontalStereoShader;
import com.samsungxr.shaders.SXROESShader;
import com.samsungxr.shaders.SXROESVerticalStereoShader;
import com.samsungxr.shaders.SXRPhongLayeredShader;
import com.samsungxr.shaders.SXRPhongShader;
import com.samsungxr.shaders.SXRTextureShader;
import com.samsungxr.shaders.SXRVerticalFlipShader;
import com.samsungxr.utility.Colors;
import com.samsungxr.utility.Log;

import android.graphics.Color;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapculates data to be sent to a vertex or fragment shader.
 * It contains a list of key / value pairs which can specify arbitrary length
 * float or int vectors. It also has key / value pairs for the texture
 * samplers to be used with the shader.
 * <p>
 * Visible scene objects must have render data
 * {@linkplain SXRNode#attachRenderData(SXRRenderData)} attached. Each
 * {@link SXRRenderData} has a {@link SXRMesh} that defines its
 * geometry, and a {@link SXRMaterial} that defines its surface.
 * <p>
 * Each {@link SXRMaterial} contains two main things:
 * <ul>
 * <li>The id of a shader, which is used to draw the mesh. See
 * {@link SXRShaderType} and {@link SXRContext#getMaterialShaderManager()}.
 * 
 * <li>Data to pass to the shader. This usually - but not always - means a
 * {@link SXRTexture} and can include other named values to pass to the shader.
 * </ul>
 * 
 * <p>
 * The simplest way to create a {@link SXRMaterial} is to call the
 * {@linkplain SXRMaterial#SXRMaterial(SXRContext)} constructor that takes only a
 * SXRContext. Then you just {@link SXRMaterial#setTexture(String, SXRTexture)}
 * and you're ready to draw with the default shader, which is
 * called 'unlit' because it simply drapes the texture over the mesh, without
 * any lighting or reflection effects.
 * 
 * <pre>
 * {@code}
 * // for example
 * SXRMaterial material = new SXRMaterial(gvrContext);
 * material.setTexture("u_texture", texture);
 * }
 * </pre>
 */
public class SXRMaterial extends  SXRShaderData
{

    private static final String TAG = Log.tag(SXRMaterial.class);
    private String mMainTextureName = null;

    /** Pre-built shader ids. */
    public abstract static class SXRShaderType {
        public abstract static class Color {
            public static final SXRShaderId ID = new SXRShaderId(SXRColorShader.class);
        }

        public abstract static class UnlitHorizontalStereo {
            public static final SXRShaderId ID = new SXRShaderId(SXRUnlitHorizontalStereoShader.class);
        }

        public abstract static class UnlitVerticalStereo {
            public static final SXRShaderId ID = new SXRShaderId(SXRUnlitVerticalStereoShader.class);
        }

        public abstract static class OES {
            public static final SXRShaderId ID = new SXRShaderId(SXROESShader.class);
        }

        public abstract static class OESHorizontalStereo {
            public static final SXRShaderId ID = new SXRShaderId(SXROESHorizontalStereoShader.class);
        }

        public abstract static class OESVerticalStereo {
            public static final SXRShaderId ID = new SXRShaderId(SXROESVerticalStereoShader.class);
        }

        public abstract static class Cubemap {
            public static final SXRShaderId ID = new SXRShaderId(SXRCubemapShader.class);
        }

        public abstract static class CubemapReflection {
            public static final SXRShaderId ID = new SXRShaderId(SXRCubemapReflectionShader.class);
        }

        public abstract static class Texture {
            public static final SXRShaderId ID = new SXRShaderId(SXRTextureShader.class);
        }

        public abstract static class Phong {
            public static final SXRShaderId ID = new SXRShaderId(SXRPhongShader.class);
        }

        public abstract static class UnlitFBO {
            public static final SXRShaderId ID = new SXRShaderId(SXRUnlitFBOShader.class);
        }

        public abstract static class LightMap {
            public static final SXRShaderId ID = new SXRShaderId(SXRLightmapShader.class);
        }

        public abstract static class PhongLayered {
            public static final SXRShaderId ID = new SXRShaderId(SXRPhongLayeredShader.class);
        }

        public abstract static class VerticalFlip {
            public static final SXRShaderId ID = new SXRShaderId(SXRVerticalFlipShader.class);
        }

        public abstract static class HorizontalFlip {
            public static final SXRShaderId ID = new SXRShaderId(SXRHorizontalFlipShader.class);
        }

        public abstract static class ColorBlend {
            public static final SXRShaderId ID = new SXRShaderId(SXRColorBlendShader.class);
        }

    };

    /**
     * Create a new holder for a shader's uniforms.
     * 
     * @param gvrContext
     *            Current {@link SXRContext}
     * @param shaderId
     *            Id of a {@linkplain SXRShaderType stock} or
     *            {@linkplain SXRShaderManager custom} shader.
     */
    public SXRMaterial(SXRContext gvrContext, SXRShaderId shaderId) {
        super(gvrContext, shaderId);
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]+) ([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(getTextureDescriptor());
        if (matcher.find(0))
        {
            mMainTextureName = matcher.group(2);
        }
    }

    /**
     * A convenience overload: builds a {@link SXRMaterial} that uses the most
     * common stock shader, the {@linkplain SXRShaderType.Texture 'texture'} shader.
     * 
     * @param gvrContext
     *            Current {@link SXRContext}
     */
    public SXRMaterial(SXRContext gvrContext) {
        this(gvrContext, SXRShaderType.Texture.ID);
    }

    /**
     * Create a new holder for a shader's uniforms from an existing material.
     *
     * @param src   SXRMaterial to copy from
     * @param shaderId  shader ID
      */
    public SXRMaterial(SXRMaterial src, SXRShaderId shaderId)
    {
        super(src, shaderId);
        if (hasTexture(mMainTextureName))
        {
            mMainTextureName = src.mMainTextureName;
        }
    }

    /**
     * The {@link SXRTexture texture} currently bound to the
     * {@code u_texture} shader uniform.
     *
     * With most built-in shaders, this is the texture that is actually displayed.
     *
     * @return The {@linkplain SXRTexture main texture}
     */
    public SXRTexture getMainTexture()
    {
        if (mMainTextureName != null)
        {
            return getTexture(mMainTextureName);
        }
        throw new IllegalArgumentException("Cannot get main texture if shader has no textures");
    }

    /**
     * Bind a different {@link SXRTexture texture} to the {@code u_texture}
     * shader uniform.
     *
     * @param texture
     *            The {@link SXRTexture} to bind.
     */
    public void setMainTexture(SXRTexture texture)
    {
        if (mMainTextureName != null)
        {
            setTexture(mMainTextureName, texture);
            return;
        }
        throw new IllegalArgumentException("Cannot set main texture if shader has no textures");
    }


    /**
     * Set the baked light map texture, the <i>lightmap_texture</i> uniform.
     *
     * @param texture
     *            Texture with baked light map
     */
    public void setLightMapTexture(SXRTexture texture) {
        setTexture("lightmap_texture", texture);
    }

    /**
     * Set the light map information(offset and scale) at UV space to
     * map the light map texture to the mesh.
     *
     * @param lightMapInformation
     *            Atlas information object with the offset and scale
     * at UV space necessary to map the light map texture to the mesh.
     */
    public void setLightMapInfo(SXRAtlasInformation lightMapInformation) {
        setTextureAtlasInfo("lightmap", lightMapInformation);
    }

    /**
     * Set the light map information(offset and scale) at UV space to
     * map the light map texture to the mesh.
     *
     * @param key
     *            Prefix name of the uniform at light map shader:
     *            ([key]_texture, [key]_offset and [key]_scale.
     * @param atlasInformation
     *            Atlas information object with the offset and scale
     * at UV space necessary to map the light map texture to the mesh.
     */
    public void setTextureAtlasInfo(String key, SXRAtlasInformation atlasInformation) {
        setTextureAtlasInfo(key, atlasInformation.getOffset(), atlasInformation.getScale());
    }

    /**
     * Set the light map information(offset and scale) at UV space to
     * map the light map texture to the mesh.
     *
     * @param key
     *            Prefix name of the uniform at light map shader:
     *            ([key]_texture, [key]_offset and [key]_scale.
     * @param offset
     *            Array with x and y offset values at UV space
     *            to map the 2D texture to the mesh.
     * @param scale
     *            Array with x and y scale values at UV space
     *            to map the 2D texture to the mesh.
     */
    public void setTextureAtlasInfo(String key, float[] offset, float[] scale) {
        setTextureOffset(key, offset);
        setTextureScale(key, scale);
    }

    /**
     * Returns the placement offset of texture {@code key}}
     * @param key Texture name. A common name is "main",
     *            "lightmap", etc.
     * @return    The vector of x and y at uv space.
     */
    public float[] getTextureOffset(String key) {
        return getVec2(key + "_offset");
    }

    /**
     * Set the placement offset of texture {@code key}}
     * @param key Texture name. A common name is "main",
     *            "lightmap", etc.
     */
    public void setTextureOffset(String key, float[] offset) {
        setVec2(key + "_offset", offset[0], offset[1]);
    }

    /**
     * Returns the placement scale of texture {@code key}}
     * @param key Texture name. A common name is "main",
     *            "lightmap", etc.
     * @return    The vector of x and y at uv space.
     */
    public float[] getTextureScale(String key) {
        return getVec2(key + "_scale");
    }

    /**
     * Set the placement scale of texture {@code key}}
     * @param key Texture name. A common name is "main",
     *            "lightmap", etc.
     */
    public void setTextureScale(String key, float[] scale) {
        setVec2(key + "_scale", scale[0], scale[1]);
    }

    /**
     * Get the {@code u_color} uniform.
     *
     * By convention, some of the SXRF shaders can use a {@code vec3} uniform named
     * {@code u_color}. With the default {@linkplain SXRShaderType.Texture 'texture'
     * shader,} this allows you to modulate the texture with a color.
     * @return The current {@code vec3 u_color} as a three-element array
     */
    public float[] getColor() {
        return getVec3("u_color");
    }

    /**
     * A convenience method that wraps {@link #getColor()} and returns an
     * Android {@link Color}
     *
     * @return An Android {@link Color}
     */
    public int getRgbColor() {
        return Colors.toColor(getColor());
    }

    /**
     * Set the {@code u_color} uniform.
     *
     * By convention, SXRF shaders can use a {@code vec3} uniform named
     * {@code color}. With the default {@linkplain SXRShaderType.Texture 'texture'
     * shader,} this allows you to modulate the texture with a color.
     * Values are between {@code 0.0f} and {@code 1.0f}, inclusive.
     *
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     */
    public void setColor(float r, float g, float b) {
        setVec3("u_color", r, g, b);
    }

    /**
     * A convenience overload of {@link #setColor(float, float, float)} that
     * lets you use familiar Android {@link Color} values.
     *
     * @param color
     *            Any Android {@link Color}; the alpha byte is ignored.
     */
    public void setColor(int color) {
        setColor(Colors.byteToGl(Color.red(color)), //
                Colors.byteToGl(Color.green(color)), //
                Colors.byteToGl(Color.blue(color)));
    }

    /**
     * Get the {@code ambient_color} uniform.
     *
     * By convention, SXRF shaders can use a {@code vec4} uniform named
     * {@code ambient_color}. With the default {@linkplain SXRShaderType.Texture 'texture'
     * shader,} this allows you to modulate the texture with a color.
     *
     * @return The current {@code vec4 ambient_color} as a four-element
     *         array
     */
    public float[] getAmbientColor() {
        return getVec4("ambient_color");
    }

    /**
     * Set the {@code ambient_color} uniform for lighting.
     *
     * By convention, SXRF shaders can use a {@code vec4} uniform named
     * {@code ambient_color}. With the {@linkplain SXRShaderType.Texture
     * shader,} this allows you to add an overlay ambient light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 1.0f},
     * inclusive.
     *
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     * @param a
     *            Alpha
     */
    public void setAmbientColor(float r, float g, float b, float a) {
        setVec4("ambient_color", r, g, b, a);
    }

    /**
     * Get the {@code diffuse_color} uniform.
     *
     * By convention, SXRF shaders can use a {@code vec4} uniform named
     * {@code diffuse_color}. With the {@linkplain SXRShaderType.Texture
     *  shader,} this allows you to add an overlay color on top of the
     * texture.
     *
     * @return The current {@code vec4 diffuse_color} as a four-element
     *         array
     */
    public float[] getDiffuseColor() {
        return getVec4("diffuse_color");
    }

    /**
     * Set the {@code diffuse_color} uniform for lighting.
     *
     * By convention, SXRF shaders can use a {@code vec4} uniform named
     * {@code diffuse_color}. With the {@linkplain SXRShaderType.Texture
     * shader,} this allows you to add an overlay diffuse light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 1.0f},
     * inclusive.
     *
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     * @param a
     *            Alpha
     */
    public void setDiffuseColor(float r, float g, float b, float a) {
        setVec4("diffuse_color", r, g, b, a);
    }

    /**
     * Get the {@code specular_color} uniform.
     *
     * By convention, SXRF shaders can use a {@code vec4} uniform named
     * {@code specular_color}. With the {@linkplain SXRShaderType.Texture
     * shader,} this allows you to add an overlay color on top of the
     * texture.
     *
     * @return The current {@code vec4 specular_color} as a four-element
     *         array
     */
    public float[] getSpecularColor() {
        return getVec4("specular_color");
    }

    /**
     * Set the {@code specular_color} uniform for lighting.
     *
     * By convention, SXRF shaders can use a {@code vec4} uniform named
     * {@code specular_color}. With the {@linkplain SXRShaderType.Texture
     * hader,} this allows you to add an overlay specular light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 1.0f},
     * inclusive.
     *
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     * @param a
     *            Alpha
     */
    public void setSpecularColor(float r, float g, float b, float a) {
        setVec4("specular_color", r, g, b, a);
    }

    /**
     * Get the {@code specular_exponent} uniform.
     *
     * By convention, SXRF shaders can use a {@code float} uniform named
     * {@code specular_exponent}. With the {@linkplain SXRShaderType.Texture
     * shader,} this allows you to add an overlay color on top of the
     * texture.
     *
     * @return The current {@code float specular_exponent} as a float
     *         value.
     */
    public float getSpecularExponent() {
        return getFloat("specular_exponent");
    }

    /**
     * Set the {@code specular_exponent} uniform for lighting.
     *
     * By convention, SXRF shaders can use a {@code float} uniform named
     * {@code specular_exponent}. With the {@linkplain SXRShaderType.Texture
     * shader,} this allows you to add an overlay specular light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 128.0f},
     * inclusive.
     *
     * @param exp
     *            Specular exponent
     */
    public void setSpecularExponent(float exp) {
        setFloat("specular_exponent", exp);
    }

    /**
     * Get the {@code u_opacity} uniform.
     * <p>
     * The {@linkplain #setOpacity(float) setOpacity() documentation} explains
     * what the {@code u_opacity} uniform does.
     *
     * @return The {@code u_opacity} uniform used to render this material
     */
    public float getOpacity() {
        return getFloat("u_opacity");
    }

    /**
     * Set the opacity ({@code u_opacity uniform}), in a complicated way.
     *
     * There are two things you need to know, how opacity is applied, and how
     * opacity is implemented.
     * <p>
     * First, SXRF does not sort by distance every object it can see, then draw
     * from back to front. Rather, it sorts every object by
     * {@linkplain SXRRenderData#getRenderingOrder() render order,} then draws
     * the {@linkplain SXRScene scene graph} in traversal order. So, if you want
     * to see a scene object through another scene object, you have to
     * explicitly {@linkplain SXRRenderData#setRenderingOrder(int) set the
     * rendering order} so that the translucent object draws after the opaque
     * object. You can use any integer values you like, but SXRF supplies
     * {@linkplain SXRRenderData.SXRRenderingOrder four standard values;} the
     * {@linkplain SXRRenderData#getRenderingOrder() default value} is
     * {@linkplain SXRRenderData.SXRRenderingOrder#GEOMETRY GEOMETRY.}
     * <p>
     * Second, technically all this method does is set the {@code opacity}
     * uniform. What this does depends on the actual shader. If you don't
     * specify a shader (or you specify the
     * {@linkplain SXRMaterial.SXRShaderType.Texture} shader) setting
     * {@code u_opacity} does exactly what you expect; you only have to worry
     * about the render order. However, it is totally up to a custom shader
     * whether or how it will handle opacity.
     *
     * @param opacity
     *            Value between {@code 0.0f} and {@code 1.0f}, inclusive.
     */
    public void setOpacity(float opacity) {
        setFloat("u_opacity", opacity);
    }


    /**
     * Gets the line width for line drawing.
     *
     * @see SXRRenderData#setDrawMode(int)
     */
    public float getLineWidth() {
        return getFloat("line_width");
    }

    /**
     * Sets the line width for line drawing.
     *
     * By default, the line width is 1. It is applied when the
     * draw mode is GL_LINES, GL_LINE_STRIP or GL_LINE_LOOP.
     *
     * @param lineWidth new line width.
     * @see SXRRenderData#setDrawMode(int)
     */
    public void setLineWidth(float lineWidth) {
        setFloat("line_width", lineWidth);
    }
}

