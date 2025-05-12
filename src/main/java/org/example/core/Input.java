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
    private static boolean firstMouse = true;

    private final GLFWKeyCallback keyboardCallback;
    private final GLFWCursorPosCallback mouseMoveCallback;
    private final GLFWMouseButtonCallback mouseButtonCallback;

    private long windowHandleForInput = 0;

    public Input() {
        keyboardCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                // +++ DODANY LOG DEBUGUJĄCY +++
                System.out.println("Input.java keyboardCallback: key=" + key +
                        " (" + glfwGetKeyName(key, scancode) + ")" +
                        ", action=" + action +
                        " (PRESS=" + (action == GLFW_PRESS) +
                        ", RELEASE=" + (action == GLFW_RELEASE) +
                        ", REPEAT=" + (action == GLFW_REPEAT) + ")");

                if (key >= 0 && key <= GLFW_KEY_LAST) {
                    keys[key] = (action != GLFW_RELEASE);
                }
            }
        };

        mouseMoveCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                if (firstMouse || (windowHandleForInput != 0 && glfwGetInputMode(windowHandleForInput, GLFW_CURSOR) == GLFW_CURSOR_NORMAL)) {
                    prevMouseX = xpos;
                    prevMouseY = ypos;
                }
                firstMouse = false;
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
        if (keycode < 0 || keycode > GLFW_KEY_LAST) return false;
        return keys[keycode];
    }

    public static boolean isMouseButtonDown(int button) {
        if (button < 0 || button > GLFW_MOUSE_BUTTON_LAST) return false;
        return buttons[button];
    }

    public static double getMouseX() { return mouseX; }
    public static double getMouseY() { return mouseY; }
    public static Vector2f getMouseDelta() { return mouseDelta; }

    public void update() {
        if (windowHandleForInput != 0 && glfwGetInputMode(windowHandleForInput, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
            mouseDelta.x = (float)(mouseX - prevMouseX);
            mouseDelta.y = (float)(mouseY - prevMouseY);
        } else {
            mouseDelta.x = 0;
            mouseDelta.y = 0;
            firstMouse = true;
        }
        prevMouseX = mouseX;
        prevMouseY = mouseY;
    }

    public void init(long windowHandle) {
        this.windowHandleForInput = windowHandle;
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowHandle, x, y);
        mouseX = x[0];
        mouseY = y[0];
        prevMouseX = mouseX;
        prevMouseY = mouseY;
        mouseDelta.set(0, 0);
        firstMouse = true;
    }

    public void setActiveCallbacks() {
        if (windowHandleForInput == 0) {
            System.err.println("Input.setActiveCallbacks: windowHandleForInput is not set!");
            return;
        }
        System.out.println("Input: Attempting to set game key callback."); // DEBUG LOG
        GLFWKeyCallback prevKeyCb = glfwSetKeyCallback(windowHandleForInput, keyboardCallback);
        if (prevKeyCb != null && prevKeyCb != keyboardCallback) {
            System.out.println("Input.setActiveCallbacks: Overwrote a previous different key callback during game key callback setup.");
            prevKeyCb.free();
        }

        System.out.println("Input: Attempting to set game cursor pos callback."); // DEBUG LOG
        GLFWCursorPosCallback prevCursorCb = glfwSetCursorPosCallback(windowHandleForInput, mouseMoveCallback);
        if (prevCursorCb != null && prevCursorCb != mouseMoveCallback) {
            System.out.println("Input.setActiveCallbacks: Overwrote a previous different cursor callback during game cursor callback setup.");
            prevCursorCb.free();
        }

        System.out.println("Input: Attempting to set game mouse button callback."); // DEBUG LOG
        GLFWMouseButtonCallback prevMouseBtnCb = glfwSetMouseButtonCallback(windowHandleForInput, mouseButtonCallback);
        if (prevMouseBtnCb != null && prevMouseBtnCb != mouseButtonCallback) {
            System.out.println("Input.setActiveCallbacks: Overwrote a previous different mouse button callback during game mouse button callback setup.");
            prevMouseBtnCb.free();
        }


        glfwSetInputMode(windowHandleForInput, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported()) {
            glfwSetInputMode(windowHandleForInput, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }
        System.out.println("Input: Game input callbacks and cursor mode (DISABLED) set.");
    }

    public void clearCallbacks() {
        if (windowHandleForInput == 0) return;
        System.out.println("Input: Clearing game key callback."); // DEBUG LOG
        glfwSetKeyCallback(windowHandleForInput, null);
        System.out.println("Input: Clearing game cursor pos callback."); // DEBUG LOG
        glfwSetCursorPosCallback(windowHandleForInput, null);
        System.out.println("Input: Clearing game mouse button callback."); // DEBUG LOG
        glfwSetMouseButtonCallback(windowHandleForInput, null);
        System.out.println("Input: Game input callbacks cleared.");
    }

    public void resetMouseDelta() {
        firstMouse = true;
        mouseDelta.set(0, 0);
        if (windowHandleForInput != 0) {
            double[] x = new double[1];
            double[] y = new double[1];
            glfwGetCursorPos(windowHandleForInput, x, y);
            prevMouseX = x[0];
            prevMouseY = y[0];
            mouseX = x[0];
            mouseY = y[0];
        }
        System.out.println("Input: Mouse delta state reset.");
    }

    public void cleanup() {
        if (keyboardCallback != null) keyboardCallback.free();
        if (mouseMoveCallback != null) mouseMoveCallback.free();
        if (mouseButtonCallback != null) mouseButtonCallback.free();
        System.out.println("Input: Callback objects freed.");
    }
}