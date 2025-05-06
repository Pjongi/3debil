package org.example.util;

import org.example.graphics.Mesh;

public class MeshLoader {

    public static Mesh createCube() {
        float[] positions = {
                -0.5f, -0.5f,  0.5f, 0.5f, -0.5f,  0.5f, 0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,
                -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f, 0.5f,  0.5f, -0.5f, 0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f, -0.5f,  0.5f,  0.5f, 0.5f,  0.5f,  0.5f, 0.5f,  0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f,  0.5f, -0.5f, -0.5f,  0.5f,
                0.5f, -0.5f, -0.5f, 0.5f,  0.5f, -0.5f, 0.5f,  0.5f,  0.5f, 0.5f, -0.5f,  0.5f,
                -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f, -0.5f,
        };

        float[] normals = {
                0.0f,  0.0f,  1.0f, 0.0f,  0.0f,  1.0f, 0.0f,  0.0f,  1.0f, 0.0f,  0.0f,  1.0f,
                0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f,
                0.0f,  1.0f,  0.0f, 0.0f,  1.0f,  0.0f, 0.0f,  1.0f,  0.0f, 0.0f,  1.0f,  0.0f,
                0.0f, -1.0f,  0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,  0.0f,
                1.0f,  0.0f,  0.0f, 1.0f,  0.0f,  0.0f, 1.0f,  0.0f,  0.0f, 1.0f,  0.0f,  0.0f,
                -1.0f,  0.0f,  0.0f, -1.0f,  0.0f,  0.0f, -1.0f,  0.0f,  0.0f, -1.0f,  0.0f,  0.0f,
        };

        int[] indices = {
                0,  1,  2,   0,  2,  3,  4,  5,  6,   4,  6,  7,  8,  9, 10,   8, 10, 11,
                12, 13, 14,  12, 14, 15, 16, 17, 18,  16, 18, 19, 20, 21, 22,  20, 22, 23,
        };
        return new Mesh(positions, normals, indices);
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
        int[] indices = { 0, 1, 2, 0, 2, 3 };
        return new Mesh(positions, normals, indices);
    }
}