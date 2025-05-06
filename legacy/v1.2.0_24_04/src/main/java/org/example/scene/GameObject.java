package org.example.scene;

import org.example.graphics.Mesh;
import org.example.graphics.Texture; // Dodano import
import org.joml.*;

public class GameObject {

    private Mesh mesh;
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;
    private Texture texture; // Zastąpiono color przez texture

    public GameObject(Mesh mesh, Texture texture) { // Zaktualizowany konstruktor
        this.mesh = mesh;
        this.texture = texture;
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf().identity();
        this.scale = new Vector3f(1, 1, 1);
    }

    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f();
        model.translate(position);
        model.rotate(rotation);
        model.scale(scale);
        return model;
    }

    // Gettery i Settery
    public Mesh getMesh() { return mesh; }
    public Texture getTexture() { return texture; } // Dodano getter
    public Vector3f getPosition() { return position; }
    public Quaternionf getRotation() { return rotation; }
    public Vector3f getScale() { return scale; }

    public void setMesh(Mesh mesh) { this.mesh = mesh; }
    public void setTexture(Texture texture) { this.texture = texture; } // Dodano setter
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); }
    public void setRotation(float angleRad, float x, float y, float z) { this.rotation.fromAxisAngleRad(x, y, z, angleRad); }
    public void rotate(float angleRad, float x, float y, float z) { this.rotation.rotateAxis(angleRad, x, y, z); }
    public void setScale(float x, float y, float z) { this.scale.set(x, y, z); }
    public void setScale(float s) { this.scale.set(s, s, s); }
}