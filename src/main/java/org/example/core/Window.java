package org.example.core;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import java.nio.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {

    private long windowHandle;
    private int width;
    private int height;
    private final String title;
    private Input input;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void init(Input inputInstance) {
        this.input = inputInstance;
        if (this.input == null) {
            throw new IllegalStateException("Input object cannot be null for Window initialization.");
        }

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            glfwTerminate();
            throw new RuntimeException("Failed to create the GLFW window");
        }

        this.input.init(windowHandle); // Input tylko inicjalizuje swoje stany

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(windowHandle, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(
                        windowHandle,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2
                );
            } else {
                System.err.println("Could not get video mode for primary monitor.");
                glfwSetWindowPos(windowHandle, 100, 100);
            }
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1);
        glfwShowWindow(windowHandle);
        GL.createCapabilities();
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glViewport(0, 0, width, height);
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            if (w > 0 && h > 0) {
                glViewport(0, 0, w, h);
                this.width = w;
                this.height = h;
            }
        });
    }

    public boolean isKeyPressed(int keyCode) { return Input.isKeyDown(keyCode); }
    public boolean windowShouldClose() { return glfwWindowShouldClose(windowHandle); }

    /**
     * Metoda update została uproszczona - glfwPollEvents jest teraz w Engine.loop()
     */
    public void update() {
        if (windowHandle != NULL) {
            // Usunięto glfwPollEvents() stąd
            glfwSwapBuffers(windowHandle);
        }
    }

    public void cleanup() {
        System.out.println("Window: Cleaning up...");
        if (input != null) {
            input.cleanup();
            System.out.println("Window: Input cleanup called.");
        }
        glfwSetFramebufferSizeCallback(windowHandle, null);

        if (windowHandle != NULL) {
            glfwDestroyWindow(windowHandle);
            System.out.println("Window: Window destroyed.");
            windowHandle = NULL;
        }
        glfwTerminate();
        System.out.println("Window: GLFW terminated.");
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
            System.out.println("Window: GLFW error callback freed.");
        }
        System.out.println("Window: Cleanup complete.");
    }

    public long getWindowHandle() { return windowHandle; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getTitle() { return title; }
    public Input getInput() { return input; }
}