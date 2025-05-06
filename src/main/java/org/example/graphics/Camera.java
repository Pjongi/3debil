package org.example.graphics; // Upewnij się, że pakiet jest poprawny

import org.joml.*;
import org.example.core.Input; // Załóżmy, że Input jest w org.example.core
import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private Vector3f position;
    private Vector3f front;
    private Vector3f up;
    private Vector3f right;
    private final Vector3f worldUp;

    private float yaw;
    private float pitch;

    private final float movementSpeed;
    private final float mouseSensitivity;
    private float fov;

    public Camera(Vector3f position, Vector3f worldUp) {
        this.position = position;
        this.worldUp = new Vector3f(worldUp).normalize();
        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.yaw = -90.0f; // Patrzenie wzdłuż -Z
        this.pitch = 0.0f;
        this.movementSpeed = 2.5f;
        this.mouseSensitivity = 0.1f;
        this.fov = 45.0f;
        updateCameraVectors();
    }

    public Camera() {
        this(new Vector3f(0.0f, 0.0f, 3.0f), new Vector3f(0.0f, 1.0f, 0.0f));
    }

    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(position).add(front);
        return new Matrix4f().lookAt(position, center, up);
    }

    public Matrix4f getProjectionMatrix(float aspectRatio) {
        return new Matrix4f().perspective((float) java.lang.Math.toRadians(fov), aspectRatio, 0.1f, 100.0f);
    }

    public void processKeyboard(float deltaTime) {
        float velocity = movementSpeed * deltaTime;
        Vector3f frontMovement = new Vector3f(front).mul(velocity);
        Vector3f rightMovement = new Vector3f(right).mul(velocity);
        Vector3f upMovement = new Vector3f(worldUp).mul(velocity); // Używamy worldUp do ruchu góra/dół

        if (Input.isKeyDown(GLFW_KEY_W)) position.add(frontMovement);
        if (Input.isKeyDown(GLFW_KEY_S)) position.sub(frontMovement);
        if (Input.isKeyDown(GLFW_KEY_A)) position.sub(rightMovement);
        if (Input.isKeyDown(GLFW_KEY_D)) position.add(rightMovement);
        if (Input.isKeyDown(GLFW_KEY_SPACE)) position.add(upMovement);
        if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) position.sub(upMovement);
    }

    public void processMouseMovement(float xoffset, float yoffset, boolean constrainPitch) {
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        yaw += xoffset;
        pitch += yoffset;

        if (constrainPitch) {
            if (pitch > 89.0f) pitch = 89.0f;
            if (pitch < -89.0f) pitch = -89.0f;
        }
        updateCameraVectors();
    }

    private void updateCameraVectors() {
        Vector3f newFront = new Vector3f();
        newFront.x = (float) (java.lang.Math.cos(java.lang.Math.toRadians(yaw)) * java.lang.Math.cos(java.lang.Math.toRadians(pitch)));
        newFront.y = (float) java.lang.Math.sin(java.lang.Math.toRadians(pitch));
        newFront.z = (float) (java.lang.Math.sin(java.lang.Math.toRadians(yaw)) * java.lang.Math.cos(java.lang.Math.toRadians(pitch)));
        front = newFront.normalize();
        right = new Vector3f(front).cross(worldUp).normalize();
        up = new Vector3f(right).cross(front).normalize();
    }

    // --- Gettery ---
    public Vector3f getPosition() { return position; }
    public Vector3f getFront() { return front; }
    public Vector3f getUp() { return up; }
    public float getFov() { return fov; }

    // --- Settery ---
    public void setFov(float fov) { this.fov = fov; }

    /**
     * Zwraca pozycję kamery jako początek promienia.
     * @return Wektor pozycji.
     */
    public Vector3f getRayOrigin() {
        return new Vector3f(position); // Zwróć kopię, aby uniknąć modyfikacji
    }

    /**
     * Zwraca znormalizowany wektor kierunku patrzenia kamery.
     * @return Wektor kierunku (front).
     */
    public Vector3f getRayDirection() {
        return new Vector3f(front); // Zwróć kopię, aby uniknąć modyfikacji
    }
}