package org.example.util;

import org.example.graphics.Mesh;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil; // Potrzebny do zwalniania bufora

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer; // Potrzebny do bufora pliku
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class ModelLoader {

    // Metoda pomocnicza do wczytania zasobu jako ByteBuffer (identyczna jak w Texture/AudioManager)
    private static ByteBuffer ioResourceToByteBuffer(String resource) throws IOException {
        ByteBuffer buffer;
        try (InputStream source = ModelLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (source == null) {
                throw new IOException("Resource not found in classpath: " + resource);
            }
            byte[] bytes = source.readAllBytes();
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
        }
        return buffer;
    }


    public static Mesh loadMesh(String classpathResourcePath) {
        ByteBuffer fileData = null;
        AIScene aiScene = null;

        try {
            // Wczytaj plik modelu z classpath do bufora
            fileData = ioResourceToByteBuffer(classpathResourcePath);

            // Importuj scenę z pamięci
            aiScene = aiImportFileFromMemory(fileData,
                    aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_CalcTangentSpace,
                    ""); // Pusty hint dla formatu, Assimp powinien wykryć z danych

            if (aiScene == null || (aiScene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0 || aiScene.mRootNode() == null) {
                System.err.println("Could not load model from memory: " + classpathResourcePath + " - " + aiGetErrorString());
                return null;
            }

            PointerBuffer aiMeshes = aiScene.mMeshes();
            if (aiMeshes == null || aiMeshes.limit() == 0) {
                System.err.println("No meshes found in model: " + classpathResourcePath);
                return null;
            }
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(0));

            List<Float> vertices = new ArrayList<>();
            List<Float> normals = new ArrayList<>();
            List<Float> uvs = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();

            processVertices(aiMesh, vertices);
            processNormals(aiMesh, normals);
            processUVs(aiMesh, uvs);
            processIndices(aiMesh, indices);

            if (vertices.isEmpty() || normals.isEmpty() || indices.isEmpty()) {
                System.err.println("Mesh loaded from " + classpathResourcePath + " is incomplete.");
                return null;
            }
            if (uvs.isEmpty()) {
                System.out.println("Warning: Mesh loaded from " + classpathResourcePath + " has no UVs. Generating dummy UVs.");
                for (int i = 0; i < vertices.size() / 3; i++) { uvs.add(0.0f); uvs.add(0.0f); }
            }

            float[] verticesArr = toFloatArray(vertices);
            float[] normalsArr = toFloatArray(normals);
            float[] uvsArr = toFloatArray(uvs);
            int[] indicesArr = toIntArray(indices);

            System.out.println("Loaded mesh: " + classpathResourcePath);
            return new Mesh(verticesArr, normalsArr, uvsArr, indicesArr);

        } catch (IOException e) {
            System.err.println("IO Error loading model resource: " + classpathResourcePath + " - " + e.getMessage());
            return null;
        } finally {
            // Zwolnij zasoby Assimp i bufor pliku
            if (aiScene != null) {
                aiReleaseImport(aiScene);
            }
            if (fileData != null) {
                MemoryUtil.memFree(fileData);
            }
        }
    }

    private static void processVertices(AIMesh aiMesh, List<Float> vertices) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
            vertices.add(aiVertex.z());
        }
    }

    private static void processNormals(AIMesh aiMesh, List<Float> normals) {
        AIVector3D.Buffer aiNormals = aiMesh.mNormals();
        if (aiNormals == null) return; // Brak normalnych
        while (aiNormals.remaining() > 0) {
            AIVector3D aiNormal = aiNormals.get();
            normals.add(aiNormal.x());
            normals.add(aiNormal.y());
            normals.add(aiNormal.z());
        }
    }

    private static void processUVs(AIMesh aiMesh, List<Float> uvs) {
        AIVector3D.Buffer aiTextureCoords = aiMesh.mTextureCoords(0); // Pobierz pierwszy zestaw UV (0)
        if (aiTextureCoords == null) {
            System.out.println("Warning: Model mesh has no texture coordinates (UVs).");
            return;
        }
        while (aiTextureCoords.remaining() > 0) {
            AIVector3D texCoord = aiTextureCoords.get();
            uvs.add(texCoord.x());
            uvs.add(texCoord.y()); // Assimp może mieć 3D UV, bierzemy tylko x, y
        }
    }


    private static void processIndices(AIMesh aiMesh, List<Integer> indices) {
        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer aiFaces = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = aiFaces.get(i);
            IntBuffer buffer = aiFace.mIndices();
            // Zakładamy, że mamy trójkąty (dzięki aiProcess_Triangulate)
            while (buffer.remaining() > 0) {
                indices.add(buffer.get());
            }
        }
    }

    // Pomocnicze metody konwersji List na array
    private static float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}