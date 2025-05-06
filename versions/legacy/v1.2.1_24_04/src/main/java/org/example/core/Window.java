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
    private final String title; // title jest final
    private Input input;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void init(Input input) {
        this.input = input;

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

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (this.input != null) {
                this.input.getKeyboardCallback().invoke(window, key, scancode, action, mods);
            }
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        glfwSetCursorPosCallback(windowHandle, this.input.getMouseMoveCallback());
        glfwSetMouseButtonCallback(windowHandle, this.input.getMouseButtonCallback());

        this.input.init(windowHandle);

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
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glViewport(0, 0, width, height);
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            if (w > 0 && h > 0) {
                glViewport(0, 0, w, h);
                this.width = w;
                this.height = h;
            }
        });
    }

    public boolean isKeyPressed(int keyCode) {
        return Input.isKeyDown(keyCode);
    }

    public boolean windowShouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    public void cleanup() {
        System.out.println("Window: Cleaning up..."); // Dodaj logowanie dla pewności

        // 1. Zwolnij callbacki związane z inputem (przez Input.cleanup)
        if (input != null) {
            input.cleanup();
            System.out.println("Window: Input callbacks cleaned up.");
        }

        // 2. Zwolnij pozostałe callbacki okna
        //glfwFreeCallbacks(windowHandle);
        //System.out.println("Window: Window callbacks freed.");

        // 3. Zniszcz okno (i powiązany kontekst GL)
        if (windowHandle != NULL) {
            glfwDestroyWindow(windowHandle);
            System.out.println("Window: Window destroyed.");
            windowHandle = NULL; // Ustaw na NULL po zniszczeniu
        }

        // --- ZMIANA KOLEJNOŚCI ---
        // 4. Zwolnij error callback PRZED glfwTerminate()
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
            System.out.println("Window: Error callback freed.");
        } else {
            System.out.println("Window: No active error callback to free.");
        }

        // 5. Zakończ działanie GLFW
        glfwTerminate();
        System.out.println("Window: GLFW terminated.");
        // --- KONIEC ZMIANY KOLEJNOŚCI ---

        System.out.println("Window: Cleanup complete.");
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getTitle() {
        return title;
    }

    public Input getInput() {
        return input;
    }
}