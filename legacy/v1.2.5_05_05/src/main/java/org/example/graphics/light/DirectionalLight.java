package org.example.graphics.light;

import org.joml.Vector3f;
import org.joml.Matrix4f;

public class DirectionalLight {

    private Vector3f color;
    private Vector3f direction;
    private float intensity;

    private float shadowPosMult = 10.0f;
    private float orthoSize = 10.0f;

    public DirectionalLight(Vector3f color, Vector3f direction, float intensity) {
        this.color = color;
        this.direction = direction.normalize();
        this.intensity = intensity;
    }

    public Vector3f getColor() { return color; }
    public Vector3f getDirection() { return direction; }
    public float getIntensity() { return intensity; }

    public void setColor(Vector3f color) { this.color = color; }
    public void setDirection(Vector3f direction) { this.direction = direction.normalize(); }
    public void setIntensity(float intensity) { this.intensity = intensity; }

    public Matrix4f getLightProjectionMatrix() {
        return new Matrix4f().ortho(-orthoSize, orthoSize, -orthoSize, orthoSize, -20.0f, 20.0f);
    }

    public Matrix4f getLightViewMatrix() {
        Vector3f lightPos = new Vector3f(direction).mul(-shadowPosMult);
        Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        if (Math.abs(direction.y) > 0.99f) {
            up = new Vector3f(1.0f, 0.0f, 0.0f);
        }
        return new Matrix4f().lookAt(lightPos, target, up);
    }

    public Matrix4f getLightSpaceMatrix() {
        return new Matrix4f(getLightProjectionMatrix()).mul(getLightViewMatrix());
    }
}