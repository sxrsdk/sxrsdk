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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Opaque type that specifies a material shader.
 */
public class SXRShaderId {
    final Class<? extends SXRShader> ID;
    protected SXRShader mShaderTemplate;
    protected int mNativeShader;

    public SXRShaderId(Class<? extends SXRShader> id)
    {
        ID = id;
        mShaderTemplate = null;
        mNativeShader = 0;
    }

    protected SXRShaderId(SXRShader template)
    {
        ID = template.getClass();
        mShaderTemplate = template;
    }

    /**
     * Gets the string describing the uniforms used by shaders of this type.
     * @param ctx GVFContext shader is associated with
     * @return uniform descriptor string
     * @see #getTemplate(SXRContext) SXRShader#getUniformDescriptor()
     */
    public String getUniformDescriptor(SXRContext ctx)
    {
        if (mShaderTemplate == null)
        {
            mShaderTemplate = makeTemplate(ID, ctx);
            ctx.getShaderManager().addShaderID(this);
        }
        return mShaderTemplate.getUniformDescriptor();
    }

    /**
     * Gets the string describing the textures used by shaders of this type.
     * @param ctx GVFContext shader is associated with
     * @return texture descriptor string
     * @see #getTemplate(SXRContext) SXRShader#getTextureDescriptor()
     */
    public String getTextureDescriptor(SXRContext ctx)
    {
        if (mShaderTemplate == null)
        {
            mShaderTemplate = makeTemplate(ID, ctx);
            ctx.getShaderManager().addShaderID(this);
        }
        return mShaderTemplate.getTextureDescriptor();
    }

    /**
     * Gets the Java subclass of SXRShader which implements
     * this shader type.
     * @param ctx SXRContext shader is associated with
     * @return SXRShader class implementing the shader type
     */
    public SXRShader getTemplate(SXRContext ctx)
    {
        if (mShaderTemplate == null)
        {
            mShaderTemplate = makeTemplate(ID, ctx);
            ctx.getShaderManager().addShaderID(this);
        }
        return mShaderTemplate;
    }

    /**
     * Links a specific SXRShader Java class to this shader ID.
     * This should only ever be called once.
     * @param shader Java shader class implementing this shader type
     */
    void setTemplate(SXRShader shader)
    {
        mShaderTemplate = shader;
    }

    /**
     * Instantiates an instance of input Java shader class,
     * which must be derived from SXRShader or SXRShaderTemplate.
     * @param id        Java class which implements shaders of this type.
     * @param ctx       SXRContext shader belongs to
     * @return SXRShader subclass which implements this shader type
     */
    SXRShader makeTemplate(Class<? extends SXRShader> id, SXRContext ctx)
    {
        try
        {
            Constructor<? extends SXRShader> maker = id.getDeclaredConstructor(SXRContext.class);
            return maker.newInstance(ctx);
        }
        catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex)
        {
            try
            {
                Constructor<? extends SXRShader> maker = id.getDeclaredConstructor();
                return maker.newInstance();
            }
            catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex2)
            {
                ctx.getEventManager().sendEvent(ctx, IErrorEvents.class, "onError", new Object[] {ex2.getMessage(), this});
                return null;
            }
        }
    }
}