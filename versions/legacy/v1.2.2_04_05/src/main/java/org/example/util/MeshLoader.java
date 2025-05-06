package org.example.util;

import org.example.graphics.Mesh;

public class MeshLoader {

    // Dane pozycji, normalnych, UV i indeksów dla sześcianu
    // Upewnij się, że liczba wierzchołków, normalnych i UV jest zgodna (24 wierzchołki)
    private static final float[] CUBE_POSITIONS = {
            // Front face (+Z)
            -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            // Back face (-Z)
            -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f,
            // Top face (+Y)
            -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f,
            // Bottom face (-Y)
            -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f,
            // Right face (+X)
            0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
            // Left face (-X)
            -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f
    };

    private static final float[] CUBE_NORMALS = {
            // Front face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            // Back face
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
            // Top face
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            // Bottom face
            0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
            // Right face
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            // Left face
            -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f
    };

    private static final float[] CUBE_UVS = {
            // Front face
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            // Back face
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            // Top face
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            // Bottom face
            1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            // Right face
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            // Left face
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f
    };

    // Indeksy odnoszą się do 24 wierzchołków zdefiniowanych powyżej
    private static final int[] CUBE_INDICES = {
            // Front face
            0, 1, 2, 0, 2, 3,
            // Back face
            4, 5, 6, 4, 6, 7,
            // Top face
            8, 9, 10, 8, 10, 11,
            // Bottom face
            12, 13, 14, 12, 14, 15,
            // Right face
            16, 17, 18, 16, 18, 19,
            // Left face
            20, 21, 22, 20, 22, 23
    };


    /**
     * Tworzy siatkę sześcianu.
     * @return Obiekt Mesh reprezentujący sześcian.
     */
    public static Mesh createCube() {
        return new Mesh(CUBE_POSITIONS, CUBE_NORMALS, CUBE_UVS, CUBE_INDICES);
    }

    /**
     * Tworzy siatkę płaszczyzny leżącej na płaszczyźnie XZ.
     * @param size Rozmiar boku płaszczyzny.
     * @param uvScale Skalowanie koordynatów UV (np. 1.0f dla całej tekstury, większe wartości dla powtarzania).
     * @return Obiekt Mesh reprezentujący płaszczyznę.
     */
    public static Mesh createPlane(float size, float uvScale) { // Wersja z dwoma argumentami
        float halfSize = size / 2.0f;
        float[] positions = {
                // x,    y,    z
                -halfSize, 0.0f,  halfSize, // lewy górny (z perspektywy +Y)
                halfSize, 0.0f,  halfSize, // prawy górny
                halfSize, 0.0f, -halfSize, // prawy dolny
                -halfSize, 0.0f, -halfSize, // lewy dolny
        };
        float[] normals = {
                // nx,   ny,   nz
                0.0f, 1.0f, 0.0f, // skierowana w górę
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
        };
        float[] uvs = {
                // u,      v
                0.0f,     0.0f,       // lewy górny tekstury
                uvScale,  0.0f,       // prawy górny
                uvScale,  uvScale,    // prawy dolny
                0.0f,     uvScale,    // lewy dolny
        };
        int[] indices = {
                0, 1, 2, // Pierwszy trójkąt
                0, 2, 3  // Drugi trójkąt
        };
        return new Mesh(positions, normals, uvs, indices);
    }

    /**
     * Tworzy siatkę płaszczyzny leżącej na płaszczyźnie XZ (domyślne skalowanie UV = 1.0).
     * @param size Rozmiar boku płaszczyzny.
     * @return Obiekt Mesh reprezentujący płaszczyznę.
     */
    public static Mesh createPlane(float size) { // Wersja z jednym argumentem
        return createPlane(size, 1.0f); // Wywołaj wersję z dwoma argumentami
    }
}