package org.example.core;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import static org.lwjgl.glfw.GLFW.*;

public class Input {

    private static final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] buttons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private static double mouseX, mouseY;
    private static double prevMouseX, prevMouseY;
    private static final Vector2f mouseDelta = new Vector2f();

    private final GLFWKeyCallback keyboardCallback;
    private final GLFWCursorPosCallback mouseMoveCallback;
    private final GLFWMouseButtonCallback mouseButtonCallback;

    public Input() {
        keyboardCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key <= GLFW_KEY_LAST) {
                    keys[key] = (action != GLFW_RELEASE);
                }
            }
        };

        mouseMoveCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;
            }
        };

        mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button <= GLFW_MOUSE_BUTTON_LAST) {
                    buttons[button] = (action != GLFW_RELEASE);
                }
            }
        };
    }

    public static boolean isKeyDown(int keycode) {
        if (keycode < 0 || keycode > GLFW_KEY_LAST) {
            return false;
        }
        return keys[keycode];
    }

    public static boolean isMouseButtonDown(int button) {
        if (button < 0 || button > GLFW_MOUSE_BUTTON_LAST) {
            return false;
        }
        return buttons[button];
    }

    public static double getMouseX() {
        return mouseX;
    }

    public static double getMouseY() {
        return mouseY;
    }

    public static Vector2f getMouseDelta() {
        return mouseDelta;
    }

    public void update() {
        mouseDelta.x = (float)(mouseX - prevMouseX);
        mouseDelta.y = (float)(mouseY - prevMouseY);
        prevMouseX = mouseX;
        prevMouseY = mouseY;
    }

    public void init(long windowHandle) {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowHandle, x, y);
        mouseX = x[0];
        mouseY = y[0];
        prevMouseX = mouseX;
        prevMouseY = mouseY;
        mouseDelta.set(0, 0);

        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        if (glfwRawMouseMotionSupported()) {
            glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }
    }

    public void cleanup() {
        if (keyboardCallback != null) keyboardCallback.free();
        if (mouseMoveCallback != null) mouseMoveCallback.free();
        if (mouseButtonCallback != null) mouseButtonCallback.free();
    }

    public GLFWKeyCallback getKeyboardCallback() {
        return keyboardCallback;
    }

    public GLFWCursorPosCallback getMouseMoveCallback() {
        return mouseMoveCallback;
    }

    public GLFWMouseButtonCallback getMouseButtonCallback() {
        return mouseButtonCallback;
    }
}