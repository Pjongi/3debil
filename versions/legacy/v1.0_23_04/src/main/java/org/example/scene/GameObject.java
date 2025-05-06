package org.example.scene;

import org.example.graphics.Mesh;
import org.joml.*;

public class GameObject {

    private Mesh mesh;
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;
    private Vector3f color;

    public GameObject(Mesh mesh) {
        this.mesh = mesh;
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf().identity();
        this.scale = new Vector3f(1, 1, 1);
        this.color = new Vector3f(1.0f, 1.0f, 1.0f);
    }

    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f();
        model.translate(position);
        model.rotate(rotation);
        model.scale(scale);
        return model;
    }

    public Mesh getMesh() { return mesh; }
    public Vector3f getPosition() { return position; }
    public Quaternionf getRotation() { return rotation; }
    public Vector3f getScale() { return scale; }
    public Vector3f getColor() { return color; }

    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); }
    public void setRotation(float angleRad, float x, float y, float z) { this.rotation.fromAxisAngleRad(x, y, z, angleRad); }
    public void rotate(float angleRad, float x, float y, float z) { this.rotation.rotateAxis(angleRad, x, y, z); }
    public void setScale(float x, float y, float z) { this.scale.set(x, y, z); }
    public void setScale(float s) { this.scale.set(s, s, s); }
    public void setColor(float r, float g, float b) { this.color.set(r, g, b); }
}