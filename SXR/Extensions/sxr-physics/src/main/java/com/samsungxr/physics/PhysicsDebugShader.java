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

package com.samsungxr.physics;

import com.samsungxr.SXRShader;
import com.samsungxr.SXRShaderData;

/**
 * GLSL shader used for Bullet debug drawing.
 */
public class PhysicsDebugShader extends SXRShader
{
    final String vertex_shader =
            "precision mediump float;\n" +
            "layout (location = 0) in vec3 a_position;\n" +
            "layout (location = 1) in vec3 a_color;\n" +
            "layout (location = 0) out vec3 vertex_color;\n" +
            "uniform mat4 u_vp;\n" +
            "void main()\n" +
            "{\n" +
            "\tgl_Position = u_vp * vec4(a_position, 1);\n" +
            "\tvertex_color = a_color;\n" +
            "}";

    final String fragment_shader =
            "precision highp float;\n" +
            "layout (location = 0) in vec3 vertex_color;\n" +
            "layout (location = 0) out vec4 gl_FragColor;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_FragColor = vec4(vertex_color, 1);\n" +
            "}";

    public PhysicsDebugShader()
    {
        super("mat4 u_vp float line_width", "", "float3 a_position, float3 a_color", GLSLESVersion.V300);
        setSegment("VertexTemplate", vertex_shader);
        setSegment("FragmentTemplate", fragment_shader);
    }

    protected void setMaterialDefaults(SXRShaderData material)
    {
        material.setMat4("u_vp",
                         1, 0, 0, 0,
                         0, 1, 0, 0,
                         0, 0, 1, 0,
                         0, 0, 0, 1);
    }
};
