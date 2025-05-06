package org.example.util;

import org.example.graphics.Mesh;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.util.ResourceLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class ModelLoader {

    /**
     * Ładuje pierwszą siatkę (mesh) z pliku modelu w classpath.
     * @param classpathResourcePath Ścieżka do pliku modelu.
     * @return Obiekt Mesh.
     * @throws ResourceNotFoundException Jeśli plik modelu nie zostanie znaleziony.
     * @throws ResourceLoadException Jeśli wystąpi błąd podczas ładowania lub przetwarzania modelu.
     */
    public static Mesh loadMesh(String classpathResourcePath)
            throws ResourceNotFoundException, ResourceLoadException {

        ByteBuffer fileData = null;
        AIScene aiScene = null;

        try {
            // 1. Wczytaj plik modelu używając ResourceLoader
            fileData = ResourceLoader.ioResourceToByteBuffer(classpathResourcePath);
            // System.out.println("DEBUG: Loaded model file data for: " + classpathResourcePath);

            // 2. Importuj scenę z pamięci używając Assimp
            aiScene = aiImportFileFromMemory(fileData,
                    aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_CalcTangentSpace | aiProcess_GenSmoothNormals,
                    "");

            if (aiScene == null || (aiScene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0 || aiScene.mRootNode() == null) {
                throw new ResourceLoadException("Could not load model from memory using Assimp: " + classpathResourcePath + " - " + aiGetErrorString());
            }

            // 3. Przetwarzanie pierwszej siatki
            PointerBuffer aiMeshes = aiScene.mMeshes();
            if (aiMeshes == null || aiMeshes.limit() == 0) {
                throw new ResourceLoadException("No meshes found in model: " + classpathResourcePath);
            }
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(0));

            List<Float> vertices = new ArrayList<>(); List<Float> normals = new ArrayList<>(); List<Float> uvs = new ArrayList<>(); List<Integer> indices = new ArrayList<>();
            processVertices(aiMesh, vertices);
            processNormals(aiMesh, normals);
            processUVs(aiMesh, uvs);
            processIndices(aiMesh, indices);

            // Walidacja i generowanie dummy UVs
            if (vertices.isEmpty() || normals.isEmpty() || indices.isEmpty()) {
                throw new ResourceLoadException("Mesh loaded from " + classpathResourcePath + " is incomplete (missing vertices, normals, or indices).");
            }
            if (uvs.isEmpty()) {
                System.out.println("Warning: Mesh loaded from " + classpathResourcePath + " has no UVs. Generating dummy UVs.");
                for (int i = 0; i < vertices.size() / 3; i++) { uvs.add(0.0f); uvs.add(0.0f); }
            }

            // Konwersja list na tablice
            float[] verticesArr = toFloatArray(vertices); float[] normalsArr = toFloatArray(normals); float[] uvsArr = toFloatArray(uvs); int[] indicesArr = toIntArray(indices);

            System.out.println("Loaded mesh: " + classpathResourcePath);
            return new Mesh(verticesArr, normalsArr, uvsArr, indicesArr);

        } catch (IOException e) { // Złap IO z ResourceLoader (inny niż NotFound)
            // Jeśli to ResourceNotFoundException, rzuć dalej
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            }
            throw new ResourceLoadException("IO error loading model resource: " + classpathResourcePath, e);
        } catch (Exception e) { // Złap inne błędy (np. z Assimp, przetwarzania list)
            throw new ResourceLoadException("Failed to load or process model: " + classpathResourcePath, e);
        } finally {
            // 4. Zwolnij zasoby Assimp
            if (aiScene != null) {
                aiReleaseImport(aiScene);
            }
            // 5. ZAWSZE zwalniaj bufor wczytany przez ResourceLoader
            if (fileData != null) {
                MemoryUtil.memFree(fileData);
            }
        }
    }

    // --- Metody pomocnicze do przetwarzania danych Assimp ---
    private static void processVertices(AIMesh aiMesh, List<Float> vertices) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) { AIVector3D aiVertex = aiVertices.get(); vertices.add(aiVertex.x()); vertices.add(aiVertex.y()); vertices.add(aiVertex.z()); }
    }
    private static void processNormals(AIMesh aiMesh, List<Float> normals) {
        AIVector3D.Buffer aiNormals = aiMesh.mNormals(); if (aiNormals == null) return;
        while (aiNormals.remaining() > 0) { AIVector3D aiNormal = aiNormals.get(); normals.add(aiNormal.x()); normals.add(aiNormal.y()); normals.add(aiNormal.z()); }
    }
    private static void processUVs(AIMesh aiMesh, List<Float> uvs) {
        AIVector3D.Buffer aiTextureCoords = aiMesh.mTextureCoords(0); if (aiTextureCoords == null) return;
        while (aiTextureCoords.remaining() > 0) { AIVector3D texCoord = aiTextureCoords.get(); uvs.add(texCoord.x()); uvs.add(texCoord.y()); }
    }
    private static void processIndices(AIMesh aiMesh, List<Integer> indices) {
        int numFaces = aiMesh.mNumFaces(); AIFace.Buffer aiFaces = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) { AIFace aiFace = aiFaces.get(i); IntBuffer buffer = aiFace.mIndices(); while (buffer.remaining() > 0) { indices.add(buffer.get()); } }
    }

    // --- Metody pomocnicze do konwersji list na tablice ---
    private static float[] toFloatArray(List<Float> list) {
        int size = list.size();
        float[] array = new float[size];
        for (int i = 0; i < size; i++) {
            array[i] = list.get(i); // Autoboxing/unboxing Float -> float
        }
        return array; // Przywrócony return
    }

    private static int[] toIntArray(List<Integer> list) {
        int size = list.size();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = list.get(i); // Autoboxing/unboxing Integer -> int
        }
        return array; // Przywrócony return
    }
}