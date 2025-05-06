package org.example.core;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Timer {

    private double lastLoopTime;
    private float deltaTime;

    public void init() {
        lastLoopTime = getTime();
    }

    public double getTime() {
        return glfwGetTime();
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    public void update() {
        double time = getTime();
        deltaTime = (float) (time - lastLoopTime);
        lastLoopTime = time;
    }
}