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
        FloatBuffer uvBuffer = null;
        IntBuffer indicesBuffer = null;
        try {
            vertexCount = indices.length;

            vaoId = glGenVertexArrays(); // (OpenGL) Poproś OpenGL o ID dla nowego VAO
            glBindVertexArray(vaoId);   // (OpenGL) Aktywuj ten VAO

            // --- VBO Pozycje ---
            posVboId = glGenBuffers(); // (OpenGL) Poproś o ID dla nowego bufora (VBO)
            posBuffer = MemoryUtil.memAllocFloat(positions.length); // (Java) Zaalokuj bufor w pamięci natywnej
            posBuffer.put(positions).flip(); // (Java) Wypełnij bufor danymi pozycji i przygotuj do odczytu
            glBindBuffer(GL_ARRAY_BUFFER, posVboId); // (OpenGL) Aktywuj ten VBO jako bufor tablicy wierzchołków
            // (OpenGL) Prześlij dane z bufora Javy (posBuffer) do aktywnego VBO na GPU
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            // (OpenGL) Poinformuj OpenGL, jak interpretować dane w VBO dla atrybutu wierzchołka 0 (pozycja)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0); // (OpenGL) Włącz ten atrybut wierzchołka

            // --- VBO Normalne (analogicznie) ---
            normalVboId = glGenBuffers();
            normalBuffer = MemoryUtil.memAllocFloat(normals.length);
            normalBuffer.put(normals).flip();
            glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
            glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);

            // --- VBO UV (analogicznie) ---
            uvVboId = glGenBuffers();
            uvBuffer = MemoryUtil.memAllocFloat(uvs.length);
            uvBuffer.put(uvs).flip();
            glBindBuffer(GL_ARRAY_BUFFER, uvVboId);
            glBufferData(GL_ARRAY_BUFFER, uvBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0); // 2 komponenty dla UV
            glEnableVertexAttribArray(2);

            // --- EBO Indeksy (analogicznie, ale dla GL_ELEMENT_ARRAY_BUFFER) ---
            idxVboId = glGenBuffers();
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, idxVboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            // Odwiązanie buforów (dobra praktyka)
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            // Odwiązanie GL_ELEMENT_ARRAY_BUFFER dzieje się automatycznie, gdy odwiązujemy VAO

        } finally {
            // (Java) Zwolnij pamięć natywną buforów użytych do transferu
            if (posBuffer != null) MemoryUtil.memFree(posBuffer);
            if (normalBuffer != null) MemoryUtil.memFree(normalBuffer);
            if (uvBuffer != null) MemoryUtil.memFree(uvBuffer);
            if (indicesBuffer != null) MemoryUtil.memFree(indicesBuffer);
        }
    }

    public void render() {
        glBindVertexArray(vaoId); // (OpenGL) Aktywuj VAO tej siatki
        // (OpenGL) Wydaj polecenie rysowania: użyj aktualnie związanego VAO, EBO (wewnątrz VAO),
        // aktywnego shadera i ustawionych uniformów, aby narysować 'vertexCount' wierzchołków
        // jako trójkąty (GL_TRIANGLES), używając indeksów typu GL_UNSIGNED_INT.
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0); //<-- Główne polecenie renderowania przekazywane OpenGL
        glBindVertexArray(0); // Odwiąż VAO
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