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

import static com.samsungxr.utility.Assert.*;

import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.utility.Exceptions;
import com.samsungxr.utility.Log;

/**
 * Describes an indexed triangle mesh as a set of shared vertices with integer
 * indices for each triangle.
 * <p>
 * Usually each mesh vertex may have a position, normal and texture coordinate.
 * Skinned mesh vertices will also have bone weights and indices.
 * If the mesh uses a normal map for lighting, it will have tangents
 * and bitangents as well. These vertex components correspond to vertex
 * attributes in the OpenGL vertex shader.
 * <p>
 * The vertices for the mesh are stored in a {@link SXRVertexBuffer}
 * object and the indices for the faces are in a {@link SXRIndexBuffer}
 * object. Multiple meshes may share a single vertex or index buffer.
 * <p>
 * When a vertex buffer is constructed, a string descriptor is
 * supplied which describes the format of the vertices (the name and
 * type of each vertex attribute). Once the format has been established,
 * it cannot be subsequently changed - you cannot add new vertex
 * attributes. You can change the vertex or index buffer associated
 * with a mesh at any time.
 * <p>
 * Skinned meshes have bone weights and bone indices which designate
 * which bones affect each vertex and how much. The bones are supplied
 * as a list of {@link SXRBone} objects which have the name of the bone
 * and its associated matrices. The asset loader handles constructing
 * skinned meshes and their associated bones. GearVRF keeps the bones
 * for each mesh in a uniform buffer and skinning is performed by\
 * shaders on the GPU.
 * @see SXRVertexBuffer
 * @see SXRIndexBuffer
 * @see SXRBone
 * @see SXRAssetLoader
 * @see com.samsungxr.animation.SXRSkeleton
 */
public class SXRMesh extends SXRHybridObject implements PrettyPrint {
    static public final int MAX_BONES = 64;
    static public final int BONES_PER_VERTEX = 4;
    private static final String TAG = SXRMesh.class.getSimpleName();

    protected SXRVertexBuffer mVertices;
    protected SXRIndexBuffer mIndices;

    /**
     * Construct a mesh with default vertex layout
     * <i>float3 a_position float2 a_texcoord float3 a_normal</i>
     * @param gvrContext SXRContext to associate mesh with
     */
    public SXRMesh(SXRContext gvrContext) {
        this(gvrContext, "float3 " + KEY_POSITION + " float2 " + KEY_TEXCOORD + " float3 " + KEY_NORMAL);
    }

    /**
     * Construct a mesh with a specified vertex and index buffer.
     * @param vbuffer   {@link SXRVertexBuffer} with vertices for mesh.
     * @param ibuffer   {@link SXRIndexBuffer} with indices for mesh.
     * @see #setVertexBuffer(SXRVertexBuffer)
     * @see #setIndexBuffer(SXRIndexBuffer)
     */
    public SXRMesh(SXRVertexBuffer vbuffer, SXRIndexBuffer ibuffer)
    {
        super(vbuffer.getSXRContext(), NativeMesh.ctorBuffers(vbuffer.getNative(), (ibuffer != null) ? ibuffer.getNative() : 0L));
        mVertices = vbuffer;
        mIndices = ibuffer;
    }

    public SXRMesh(final SXRMesh srcMesh)
    {
        this(srcMesh.getVertexBuffer(), srcMesh.getIndexBuffer());
    }

    /**
     * Construct a mesh with specified vertex layout.
     * @param gvrContext SXRContext to associate mesh with
     * @param vertexDescriptor string describing vertex layout.
     *                         Each vertex attribute has a name and a type.
     *                         The types may be "int", "float" or "mat"
     *                         followed by an integer indicating vector size.
     * Vertex Descriptor Examples:
     * <ul>
     * <li>float3 a_position float2 a_texcoord float3 a_normal</li>
     * <li>float3 a_position, int4 a_bone_indices, float4 a_bone_weights</li>
     * </ul>
     */
    public SXRMesh(SXRContext gvrContext, String vertexDescriptor) {
        this(new SXRVertexBuffer(gvrContext, vertexDescriptor, 0), null);
    }

    /**
     * Get the 3D vertices of the mesh. Each vertex is represented as a packed
     * {@code float} triplet:
     * <p>
     * <code>
     *     { x0, y0, z0, x1, y1, z1, x2, y2, z2, ... }
     * </code>
     * This function retrieves the <i>a_position</i> vertex attribute.
     * @return Array with the packed vertex data.
     * @see SXRVertexBuffer#getFloatVec(String)
     */
    public float[] getVertices() {
        return mVertices.getFloatArray(KEY_POSITION);
    }

    /**
     * @see SXRMesh#getVertices()
     */
    public FloatBuffer getVerticesAsFloatBuffer()
    {
        return mVertices.getFloatVec(KEY_POSITION).asReadOnlyBuffer();
    }

    /**
     * Sets the 3D vertices of the mesh. Each vertex is represented as a packed
     * {@code float} triplet:
     * <p>
     * <code>{ x0, y0, z0, x1, y1, z1, x2, y2, z2, ...}</code>
     * This function updates the <i>a_position</i> vertex attribute.
     * @param vertices
     *            Array containing the packed vertex data.
     * @see SXRVertexBuffer#setFloatArray(String, float[])
     */
    public void setVertices(float[] vertices) {
        mVertices.setFloatArray(KEY_POSITION, vertices);
    }

    /**
     * Get the vertex buffer object with the vertices for this mesh.
     * <p>
     * Vertex buffers may be shared across meshes. You can change which
     * vertex buffer a mesh uses at any time with {@link #setVertexBuffer(SXRVertexBuffer)}
     * </p>
     * @returns SXRVertexBuffer used by this mesh
     * @see #getVertices()
     */
    public SXRVertexBuffer getVertexBuffer() { return mVertices; }

    /**
     * Get the index buffer object with the face indices for this mesh.
     * <p>
     * Index buffers may be shared across meshes. You can change which
     * index buffer a mesh uses at any time with {@link #setIndexBuffer(SXRIndexBuffer)}.
     * </p>
     * @returns SXRIndexBuffer used by this mesh
     */
    public SXRIndexBuffer getIndexBuffer() { return mIndices; }

    /**
     * Changes the vertex buffer associated with this mesh.
     * @param vbuf new vertex buffer to use
     * @see #setVertices(float[])
     * @see #getVertexBuffer()
     * @see #getVertices()
     */
    public void setVertexBuffer(SXRVertexBuffer vbuf)
    {
        if (vbuf == null)
        {
            throw new IllegalArgumentException("Vertex buffer cannot be null");
        }
        mVertices = vbuf;
        NativeMesh.setVertexBuffer(getNative(), vbuf.getNative());
    }

    /**
     * Changes the index buffer associated with this mesh.
     * @param ibuf new index buffer to use
     * @see #setIndices(int[])
     * @see #getIndexBuffer()
     * @see #getIntIndices()
     */
    public void setIndexBuffer(SXRIndexBuffer ibuf)
    {
        mIndices = ibuf;
        NativeMesh.setIndexBuffer(getNative(), (ibuf != null) ? ibuf.getNative() : 0L);
    }

    /**
     * Get the normal vectors of the mesh. Each normal vector is represented as
     * a packed {@code float} triplet:
     * <p>
     * <code>{ x0, y0, z0, x1, y1, z1, x2, y2, z2, ...}</code>
     * This function retrieves the <i>a_normal</i> vertex attribute.
     * @return Array with the packed normal data.
     * @see SXRVertexBuffer#getFloatArray(String)
     */
    public float[] getNormals() {
        return mVertices.getFloatArray(KEY_NORMAL);
    }

    /**
     * @see SXRMesh#getNormals()
     */
    public FloatBuffer getNormalsAsFloatBuffer() {
        return mVertices.getFloatVec(KEY_NORMAL).asReadOnlyBuffer();
    }

    /**
     * Sets the normal vectors of the mesh. Each normal vector is represented as
     * a packed {@code float} triplet:
     * <p>
     * <code>{ x0, y0, z0, x1, y1, z1, x2, y2, z2, ...}</code>
     * This function updates the <i>a_normal</i> vertex attribute.
     * @param normals
     *            Array containing the packed normal data.
     * @see SXRVertexBuffer#setFloatArray(String, float[])
     */
    public void setNormals(float[] normals) {
        mVertices.setFloatArray(KEY_NORMAL, normals);
    }

    /**
     * Retrieves a set of texture coordinates from the mesh.
     * <p>
     * A mesh may have multiple sets of texture coordinates
     * Each texture coordinate is represented as a packed {@code float} pair:
     * <p>
     * <code>{ u0, v0, u1, v1, u2, v2, ...}</code>
     * This function retrieves the <i>a_texcoordN</i> vertex attribute where N is
     * the value of the <i>index</i> argument.
     * @param index
     *          0-based index indicating which set of texture coordinates to get.
     * @see #getTexCoords()
     * @see SXRVertexBuffer#getFloatArray(String)
     */
    public float[] getTexCoords(int index)
    {
        final String key = (index > 0) ? (KEY_TEXCOORD + index) : KEY_TEXCOORD;
        return mVertices.getFloatArray(key);
    }

    /**
     * @see SXRMesh#getTexCoords(int)
     */
    public FloatBuffer getTexCoordsAsFloatBuffer(int index)
    {
        final String key = (index > 0) ? (KEY_TEXCOORD + index) : KEY_TEXCOORD;
        return mVertices.getFloatVec(key).asReadOnlyBuffer();
    }

    /**
     * Get the u,v texture coordinates for the mesh. Each texture coordinate is
     * represented as a packed {@code float} pair:
     * <p>
     * <code>{ u0, v0, u1, v1, u2, v2, ...}</code>
     * This function retrieves the <i>a_texcoord</i> vertex attribute.
     * @return Array with the packed texture coordinate data.
     * @see SXRVertexBuffer#getFloatArray(String)
     */
    public float[] getTexCoords() {
        return mVertices.getFloatArray(KEY_TEXCOORD);
    }

    /**
     * @see SXRMesh#getTexCoords()
     */
    public FloatBuffer getTexCoordsAsFloatBuffer() {
        return mVertices.getFloatVec(KEY_TEXCOORD).asReadOnlyBuffer();
    }

    /**
     * Sets the first set of texture coordinates for the mesh. Each texture coordinate is
     * represented as a packed {@code float} pair:
     * <p>
     * <code>{ u0, v0, u1, v1, u2, v2, ...}</code>
     * <p>This function updates the <i>a_texcoord</i> vertex attribute.
     * @param texCoords
     *            Array containing the packed texture coordinate data.
     * @see SXRVertexBuffer#setFloatArray(String, float[])
     */
    public void setTexCoords(float[] texCoords)
    {
        setTexCoords(texCoords, 0);
    }

    /**
     * Populates a set of texture coordinates for the mesh.
     * <p>
     * A mesh may have multiple sets of texture coordinates
     * Each texture coordinate is represented as a packed {@code float} pair:
     * <p>
     * <code>{ u0, v0, u1, v1, u2, v2, ...}</code>
     * This function updates the <i>a_texcoordN</i> vertex attribute where N is
     * the value of the <i>index</i> argument.
     * @param texCoords
     *          Array containing the packed texture coordinate data.
     * @param index
     *          0-based index indicating which set of texture coordinates to update.
     * @see #getTexCoords()
     * @see SXRVertexBuffer#setFloatArray(String, float[])
     */
    public void setTexCoords(float [] texCoords, int index)
    {
        String key = (index > 0) ? (KEY_TEXCOORD + index) : KEY_TEXCOORD;
        mVertices.setFloatArray(key, texCoords);
    }

    /**
     * Get the triangle vertex indices of the mesh. The indices for each
     * triangle are represented as a packed {@code int} triplet, where
     * {@code t0} is the first triangle, {@code t1} is the second, etc.:
     * <p>
     * <code>
     * { t0[0], t0[1], t0[2], t1[0], t1[1], t1[2], ...}
     * </code>
     * <p>
     * Face indices may also be {@code char}. If you have specified
     * char indices, this function will throw an exception.
     * @return int array with the packed triangle index data.
     * @see #setCharIndices(char[])
     * @see #getIntIndices()
     * @throws IllegalArgumentException if index buffer is not <i>int</i>
     */
    public char[] getCharIndices() {
        return (mIndices != null) ? mIndices.asCharArray() : null;
    }

    /**
     * Get the triangle vertex indices of the mesh. The indices for each
     * triangle are represented as a packed {@code int} triplet, where
     * {@code t0} is the first triangle, {@code t1} is the second, etc.:
     * <p>
     * <code>
     * { t0[0], t0[1], t0[2], t1[0], t1[1], t1[2], ...}
     * </code>
     * <p>
     * Face indices may also be {@code char}. If you have specified
     * char indices, this function will throw an exception.
     * @return int array with the packed triangle index data.
     * @see #setCharIndices(char[])
     * @see #getIntIndices()
     * @throws IllegalArgumentException if index buffer is not <i>int</i>
     */
    public char[] getIntIndices() {
        return (mIndices != null) ? mIndices.asCharArray() : null;
    }

    /**
     * Sets the vertex indices of the mesh as {@code int} values.
     * <p>
     * If no index buffer exists, a new {@link SXRIndexBuffer} is
     * constructed with {@code int} indices. Otherwise, the existing
     * index buffer is updated. If that index buffer has been
     * already constructed with {@code char} indices, this function
     * will throw an exception.
     * @param indices
     *            int array containing the packed index data or null.
     *            If null is specified, the index buffer is destroyed
     *            and the mesh will have only vertices.
     * @see #setCharIndices(char[])
     * @see #setIndexBuffer(SXRIndexBuffer)
     * @see #getCharIndices()
     * @see #getIndexBuffer()
     *@throws IllegalArgumentException if index buffer is not <i>int</i>
     */
    public void setIndices(int[] indices)
    {
        if (indices != null)
        {
            if (mIndices == null)
            {
                setIndexBuffer(new SXRIndexBuffer(getSXRContext(), 4, indices.length));
            }
            mIndices.setIntVec(indices);
        }
        else
        {
            mIndices = null;
            NativeMesh.setIndexBuffer(getNative(), 0L);
        }
    }

    /**
     * Sets the vertex indices of the mesh as {@code char} values.
     * <p>
     * If no index buffer exists, a new {@link SXRIndexBuffer} is
     * constructed with {@code char} indices. Otherwise, the existing
     * index buffer is updated. If that index buffer has been
     * already constructed with {@code int} indices, this function
     * will throw an exception.
     * @param indices
     *            char array containing the packed index data or null.
     *            If null is specified, the index buffer is destroyed
     *            and the mesh will have only vertices.
     * @see #setIndexBuffer(SXRIndexBuffer)
     * @see #getCharIndices()
     * @see #getIndexBuffer()
     * @throws IllegalArgumentException if index buffer is not <i>char</i>
     */
    public void setCharIndices(char[] indices)
    {
        if (indices != null)
        {
            if (mIndices == null)
            {
                setIndexBuffer(new SXRIndexBuffer(getSXRContext(), 2, indices.length));
            }
            mIndices.setShortVec(indices);
        }
        else
        {
            mIndices = null;
            NativeMesh.setIndexBuffer(getNative(), 0L);
        }
    }
    /**
     * Sets the vertex indices of the mesh as {@code int} values.
     * <p>
     * If no index buffer exists, a new {@link SXRIndexBuffer} is
     * constructed with {@code int} indices. Otherwise, the existing
     * index buffer is updated. If that index buffer has been
     * already constructed with {@code char} indices, this function
     * will throw an exception.
     * @param indices
     *            int array containing the packed index data or null.
     *            If null is specified, the index buffer is destroyed
     *            and the mesh will have only vertices.
     * @see #setIndexBuffer(SXRIndexBuffer)
     * @see #getIntIndices()
     * @see #getIndexBuffer()
     *@throws IllegalArgumentException if index buffer is not <i>int</i>
     */
    public void setIntIndices(int[] indices)
    {
        if (indices != null)
        {
            if (mIndices == null)
            {
                setIndexBuffer(new SXRIndexBuffer(getSXRContext(), 4, indices.length));
            }
            mIndices.setIntVec(indices);
        }
        else
        {
            mIndices = null;
            NativeMesh.setIndexBuffer(getNative(), 0L);
        }
    }

    /**
     * Sets the vertex indices of the mesh as {@code char} values.
     * <p>
     * If no index buffer exists, a new {@link SXRIndexBuffer} is
     * constructed with {@code char} indices. Otherwise, the existing
     * index buffer is updated. If that index buffer has been
     * already constructed with {@code int} indices, this function
     * will throw an exception.
     * @param indices
     *            char array containing the packed index data or null.
     *            If null is specified, the index buffer is destroyed
     *            and the mesh will have only vertices.
     * @see #setIntIndices(int[])
     * @see #setIndexBuffer(SXRIndexBuffer)
     * @see #getIntIndices()
     * @see #getIndexBuffer()
     * @throws IllegalArgumentException if index buffer is not <i>char</i>
     */
    public void setIndices(char[] indices)
    {
        if (indices != null)
        {
            if (mIndices == null)
            {
                setIndexBuffer(new SXRIndexBuffer(getSXRContext(), 2, indices.length));
            }
            mIndices.setShortVec(indices);
        }
        else
        {
            mIndices = null;
            NativeMesh.setIndexBuffer(getNative(), 0L);
        }
    }

    /**
     * Sets the vertex indices of the mesh as {@code char} values.
     * <p>
     * If no index buffer exists, a new {@link SXRIndexBuffer} is
     * constructed with {@code char} indices. Otherwise, the existing
     * index buffer is updated. If that index buffer has been
     * already constructed with {@code int} indices, this function
     * will throw an exception.
     * @param indices
     *            CharBuffer containing the packed index data or null.
     *            If null is specified, the index buffer is destroyed
     *            and the mesh will have only vertices.
     * @see #setIntIndices(int[])
     * @see #setIndexBuffer(SXRIndexBuffer)
     * @see #getIntIndices()
     * @see #getIndexBuffer()
     * @throws IllegalArgumentException if index buffer is not <i>char</i>
     */
    public void setIndices(CharBuffer indices)
    {
        if (indices != null)
        {
            if (mIndices == null)
            {
                setIndexBuffer(new SXRIndexBuffer(getSXRContext(), 2, indices.capacity() / 2));
            }
            mIndices.setShortVec(indices);
        }
        else
        {
            NativeMesh.setIndexBuffer(getNative(), 0L);
        }
    }

    /**
     * Get the array of {@code float} values associated with the vertex attribute
     * {@code key}.
     * <p>
     * @param key   Name of the shader attribute
     * @return Array of {@code float} values containing the vertex data for the named channel.
     * @throws IllegalArgumentException if vertex attribute is not <i>float</i>
     */
    public float[] getFloatArray(String key)
    {
        return mVertices.getFloatArray(key);
    }

    /**
     * Get the array of {@code integer} values associated with the vertex attribute
     * {@code key}.
     *
     * @param key   Name of the shader attribute
     * @return Array of {@code integer} values containing the vertex data for the named channel.
     */
    public int[] getIntArray(String key)
    {
        return mVertices.getIntArray(key);
    }

    /**
     * Bind an array of {@code int} values to the vertex attribute
     * {@code key}.
     *
     * @param key      Name of the vertex attribute
     * @param arr      Data to bind to the shader attribute.
     * @throws IllegalArgumentException if int array is wrong size
     */
    public void setIntArray(String key, int[] arr)
    {
        mVertices.setIntArray(key, arr);
    }

    /**
     * Bind a buffer of {@code float} values to the vertex attribute
     * {@code key}.
     *
     * @param key   Name of the vertex attribute
     * @param buf   Data buffer to bind to the shader attribute.
     * @throws IllegalArgumentException if attribute name not in descriptor or float buffer is wrong size
     */
    public void setIntVec(String key, IntBuffer buf)
    {
        mVertices.setIntVec(key, buf);
    }

    /**
     * Bind an array of {@code float} values to the vertex attribute
     * {@code key}.
     *
     * @param key   Name of the vertex attribute
     * @param arr   Data to bind to the shader attribute.
     * @throws IllegalArgumentException if attribute name not in descriptor or float array is wrong size
     */
    public void setFloatArray(String key, float[] arr)
    {
        mVertices.setFloatArray(key, arr);
    }

    /**
     * Bind a buffer of {@code float} values to the vertex attribute
     * {@code key}.
     *
     * @param key   Name of the vertex attribute
     * @param buf   Data buffer to bind to the shader attribute.
     * @throws IllegalArgumentException if attribute name not in descriptor or float buffer is wrong size
     */
    public void setFloatVec(String key, FloatBuffer buf)
    {
        mVertices.setFloatVec(key, buf);
    }

    /**
     * Calculate a bounding sphere from the mesh vertices.
     * @param sphere        float[4] array to get center of sphere and radius;
     *                      sphere[0] = center.x, sphere[1] = center.y, sphere[2] = center.z, sphere[3] = radius
     */
    public void getSphereBound(float[] sphere)
    {
        mVertices.getSphereBound(sphere);
    }

    /**
     * Calculate a bounding box from the mesh vertices.
     * @param bounds        float[6] array to get corners of box;
     *                      bounds[0,1,2] = minimum X,Y,Z and bounds[3,4,6] = maximum X,Y,Z
     */
    public void getBoxBound(float[] bounds)
    {
        mVertices.getBoxBound(bounds);
    }

    /**
     * Determine if a named attribute exists in this mesh.
     * @param key Name of the shader attribute
     * @return true if attribute exists, false if not
     */
    public boolean hasAttribute(String key) {
        return mVertices.hasAttribute(key);
    }

    /**
     * Constructs a {@link SXRMesh mesh} that contains this mesh.
     * <p>
     * This is primarily useful with the {@link SXRPicker}, which does
     * "ray casting" to detect which node you're pointing to. Ray
     * casting is computationally expensive, and you generally want to limit the
     * number of {@linkplain SXRCollider triangles to check.} A simple
     * {@linkplain SXRContext#createQuad(float, float) quad} is cheap enough,
     * but with complex meshes you will probably want to cut search time by
     * registering the object's bounding box, not the whole mesh.
     *
     * @return A {@link SXRMesh} of the bounding box.
     */
    public SXRMesh getBoundingBox()
    {
        SXRMesh meshbox = new SXRMesh(getSXRContext(), "float3 " + KEY_POSITION);
        float[] bbox = new float[6];

        getBoxBound(bbox);
        float min_x = bbox[0];
        float min_y = bbox[1];
        float min_z = bbox[2];
        float max_x = bbox[3];
        float max_y = bbox[4];
        float max_z = bbox[5];
        float[] positions = {
                min_x, min_y, min_z,
                max_x, min_y, min_z,
                min_x, max_y, min_z,
                max_x, max_y, min_z,
                min_x, min_y, max_z,
                max_x, min_y, max_z,
                min_x, max_y, max_z,
                max_x, max_y, max_z
        };
        char indices[] = {
                0, 2, 1, 1, 2, 3, 1, 3, 7, 1, 7, 5, 4, 5, 6, 5, 7, 6, 0, 6, 2, 0, 4, 6, 0, 1, 5, 0,
                5, 4, 2, 7, 3, 2, 6, 7
        };
        meshbox.setVertices(positions);
        meshbox.setIndices(indices);
        return meshbox;
    }

    /**
     * Sets the contents of this mesh to be a quad consisting of two triangles,
     * with the specified width and height. If the mesh descriptor allows for
     * normals and/or texture coordinates, they are added.
     *
     * @param width
     *            the quad's width
     * @param height
     *            the quad's height
     */
    public void createQuad(float width, float height)
    {
        String vertexDescriptor = getVertexBuffer().getDescriptor();
        float[] vertices = { width * -0.5f, height * 0.5f, 0.0f, width * -0.5f,
                height * -0.5f, 0.0f, width * 0.5f, height * 0.5f, 0.0f,
                width * 0.5f, height * -0.5f, 0.0f };
        setVertices(vertices);

        if (vertexDescriptor.contains("normal"))
        {
            final float[] normals = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f};
            setNormals(normals);
        }

        if (vertexDescriptor.contains("texcoord"))
        {
            final float[] texCoords = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f};
            setTexCoords(texCoords);
        }

        char[] triangles = { 0, 1, 2, 1, 3, 2 };
        setIndices(triangles);
    }

    /**
     * Creates a mesh whose vertices describe a quad consisting of two triangles,
     * with the specified width and height. If the vertex descriptor allows for
     * normals and/or texture coordinates, they are added.
     *
     * @param ctx         SXRContext to use for creating mesh.
     * @param vertexDesc  String describing vertex format of {@link SXRVertexBuffer}
     * @param width       the quad's width
     * @param height      the quad's height
     * @return A 2D, rectangular mesh with four vertices and two triangles
     */
    public static SXRMesh createQuad(SXRContext ctx, String vertexDesc, float width, float height)
    {
        SXRMesh mesh = new SXRMesh(ctx, vertexDesc);
        mesh.createQuad(width, height);
        return mesh;
    }


    @Override
    public void prettyPrint(StringBuffer sb, int indent)
    {
        mVertices.prettyPrint(sb, indent);
        if (mIndices != null)
        {
            mIndices.prettyPrint(sb, indent);
        }
    }

    /**
     * A static method to generate a curved mesh along an arc.
     *
     * Note the width and height arguments are used only as a means to
     * get the width:height ratio.
     *
     * @param gvrContext    the current context
     * @param width         a number representing the width
     * @param height        a number representing the height
     * @param centralAngle  the central angle of the arc in degrees
     * @param radius        the radius of the circle
     * @return
     */
    public static SXRMesh createCurvedMesh(SXRContext gvrContext, int width, int height, float centralAngle, float radius)
    {
        SXRMesh mesh = new SXRMesh(gvrContext);
        final float MAX_DEGREES_PER_SUBDIVISION = 10f;
        float ratio = (float)width/(float)height;
        int subdivisions = (int) Math.ceil(centralAngle / MAX_DEGREES_PER_SUBDIVISION);
        float degreesPerSubdivision = centralAngle/subdivisions;
        // Scale the number of subdivisions with the central angle size
        // Let each subdivision represent a constant number of degrees on the arc
        double startDegree = -centralAngle/2.0;
        float h = (float) (radius * Math.toRadians(centralAngle))/ratio;
        float yTop = h/2;
        float yBottom = -yTop;
        float[] vertices = new float[(subdivisions+1)*6];
        float[] normals = new float[(subdivisions+1)*6];
        float[] texCoords= new float[(subdivisions+1)*4];
        char[] triangles = new char[subdivisions*6];

        /*
         * The following diagram illustrates the construction method
         * Let s be the number of subdivisions, then we create s pairs of vertices
         * like so
         *
         * {0}  {2}  {4} ... {2s-1}
         *                             |y+
         * {1}  {3}  {5} ... {2s}      |___x+
         *                          z+/
         */
        for(int i = 0; i <= subdivisions; i++)
        {
            double angle = Math.toRadians(-90+startDegree + degreesPerSubdivision*i);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            float x = (float) (radius * cos);
            float z = (float) ((radius * sin) + radius);
            vertices[6*i] = x;
            vertices[6*i + 1] = yTop;
            vertices[6*i + 2] = z;
            normals[6*i] = (float)-cos;
            normals[6*i + 1] = 0.0f;
            normals[6*i + 2] = (float)-sin;
            texCoords[4*i] = (float)i/subdivisions;
            texCoords[4*i + 1] = 0.0f;

            vertices[6*i + 3] = x;
            vertices[6*i + 4] = yBottom;
            vertices[6*i + 5] = z;
            normals[6*i + 3] = (float)-cos;
            normals[6*i + 4] = 0.0f;
            normals[6*i + 5] = (float)-sin;
            texCoords[4*i + 2] = (float)i/subdivisions;
            texCoords[4*i + 3] = 1.0f;
        }

        /*
         * Referring to the diagram above, we create two triangles
         * for each pair of consecutive pairs of vertices
         * (e.g. we create two triangles with {0, 1} and {2, 3}
         *  and two triangles with {2, 3} and {4, 5})
         *
         * {0}--{2}--{4}-...-{2s-1}
         *  | ＼  | ＼ |        |       |y+
         * {1}--{3}--{5}-...-{2s}      |___x+
         *                          z+/
         */
        for (int i = 0; i < subdivisions; i++)
        {
            triangles[6*i] = (char)(2*(i+1)+1);
            triangles[6*i+1] = (char) (2*(i));
            triangles[6*i+2] = (char) (2*(i)+1);
            triangles[6*i+3] = (char) (2*(i+1)+1);
            triangles[6*i+4] = (char) (2*(i+1));
            triangles[6*i+5] = (char) (2*(i));
        }
        mesh.setVertices(vertices);
        mesh.setNormals(normals);
        mesh.setTexCoords(texCoords);
        mesh.setIndices(triangles);
        return mesh;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

    final static String KEY_TEXCOORD = "a_texcoord";
    final static String KEY_NORMAL = "a_normal";
    final static String KEY_POSITION = "a_position";
}

class NativeMesh
{
    static native long ctorBuffers(long vertexBuffer, long indexBuffer);

    static native void setIndexBuffer(long mesh, long ibuf);

    static native void setVertexBuffer(long mesh, long vbuf);
}
