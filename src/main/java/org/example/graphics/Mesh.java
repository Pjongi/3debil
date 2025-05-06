package org.example.graphics;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {

    private int vaoId;
    private int posVboId;
    private int normalVboId;
    private int uvVboId; // Dodano VBO dla UV
    private int idxVboId;
    private int vertexCount;

    public Mesh(float[] positions, float[] normals, float[] uvs, int[] indices) {
        FloatBuffer posBuffer = null;
        FloatBuffer normalBuffer = null;
        FloatBuffer uvBuffer = null; // Dodano
        IntBuffer indicesBuffer = null;
        try {
            vertexCount = indices.length;

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // VBO Pozycje (Attrib 0)
            posVboId = glGenBuffers();
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, posVboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);

            // VBO Normalne (Attrib 1)
            normalVboId = glGenBuffers();
            normalBuffer = MemoryUtil.memAllocFloat(normals.length);
            normalBuffer.put(normals).flip();
            glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
            glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);

            // VBO UV (Attrib 2) - Dodano
            uvVboId = glGenBuffers();
            uvBuffer = MemoryUtil.memAllocFloat(uvs.length);
            uvBuffer.put(uvs).flip();
            glBindBuffer(GL_ARRAY_BUFFER, uvVboId);
            glBufferData(GL_ARRAY_BUFFER, uvBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0); // 2 komponenty
            glEnableVertexAttribArray(2);

            // EBO Indeksy
            idxVboId = glGenBuffers();
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, idxVboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);

        } finally {
            if (posBuffer != null) MemoryUtil.memFree(posBuffer);
            if (normalBuffer != null) MemoryUtil.memFree(normalBuffer);
            if (uvBuffer != null) MemoryUtil.memFree(uvBuffer); // Dodano
            if (indicesBuffer != null) MemoryUtil.memFree(indicesBuffer);
        }
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2); // Dodano

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(posVboId);
        glDeleteBuffers(normalVboId);
        glDeleteBuffers(uvVboId); // Dodano
        glDeleteBuffers(idxVboId);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}