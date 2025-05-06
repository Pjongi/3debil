package org.example.util;

import org.example.graphics.Mesh;

public class MeshLoader {

    // Dane pozycji, normalnych, UV i indeksów dla sześcianu
    private static final float[] CUBE_POSITIONS = { /* ... jak poprzednio ... */ -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f,-0.5f,0.5f,0.5f,-0.5f,-0.5f,-0.5f,-0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,-0.5f,-0.5f,-0.5f,0.5f,-0.5f,-0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f,-0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f,-0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f, 0.5f,-0.5f,0.5f,-0.5f,-0.5f,-0.5f,-0.5f,-0.5f,0.5f,-0.5f,0.5f,0.5f,-0.5f,0.5f,-0.5f };
    private static final float[] CUBE_NORMALS = { /* ... jak poprzednio ... */ 0,0,1,0,0,1,0,0,1,0,0,1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,1,0,0,1,0,0,1,0,0,1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,1,0,0,1,0,0,1,0,0,1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,0 };
    private static final float[] CUBE_UVS = {0,0,1,0,1,1,0,1,1,0,1,1,0,1,0,0,0,1,0,0,1,0,1,1,1,1,0,1,0,0,1,0,1,0,1,1,0,1,0,0,0,0,1,0,1,1,0,1};
    private static final int[] CUBE_INDICES = {0,1,2,0,2,3,4,5,6,4,6,7,8,9,10,8,10,11,12,13,14,12,14,15,16,17,18,16,18,19,20,21,22,20,22,23};

    public static Mesh createCube() {
        return new Mesh(CUBE_POSITIONS, CUBE_NORMALS, CUBE_UVS, CUBE_INDICES);
    }

    public static Mesh createPlane(float size) {
        float halfSize = size / 2.0f;
        float[] positions = {
                -halfSize, 0.0f,  halfSize, halfSize, 0.0f,  halfSize,
                halfSize, 0.0f, -halfSize, -halfSize, 0.0f, -halfSize,
        };
        float[] normals = {
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
        };
        // Proste UV mapujące całą teksturę na płaszczyznę
        float[] uvs = {
                0.0f, 0.0f, size, 0.0f, // Użyj 'size' do powtarzania tekstury lub 1.0f do rozciągnięcia
                size, size, 0.0f, size
        };
        int[] indices = { 0, 1, 2, 0, 2, 3 };
        return new Mesh(positions, normals, uvs, indices);
    }
}