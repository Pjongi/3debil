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
    private Input input; // Przechowuje instancję Input

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void init(Input inputInstance) { // Przyjmuje Input jako argument
        this.input = inputInstance;
        if (this.input == null) {
            throw new IllegalStateException("Input object cannot be null for Window initialization.");
        }

        // Ustawienie error callback dla GLFW
        GLFWErrorCallback.createPrint(System.err).set();

        // Inicjalizacja GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Konfiguracja wskazówek dla okna
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Okno będzie niewidoczne aż do momentu pokazania
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3); // OpenGL 3.3
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Dla macOS

        // Utworzenie okna
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            glfwTerminate(); // Ważne, aby zwolnić zasoby GLFW w przypadku błędu
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Inicjalizacja Input z uchwytem okna (ważne dla glfwGetKey itp. w Input)
        this.input.init(windowHandle);

        // Ustawienie pozycji okna na środku ekranu
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
                System.err.println("Window: Could not get video mode for primary monitor. Positioning window at (100,100).");
                glfwSetWindowPos(windowHandle, 100, 100); // Domyślna pozycja
            }
        }

        // Ustawienie kontekstu OpenGL i synchronizacji pionowej
        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // Włącz VSync (1) lub wyłącz (0)

        // Pokaż okno
        glfwShowWindow(windowHandle);

        // Utworzenie capabilities OpenGL - to musi być po glfwMakeContextCurrent
        GL.createCapabilities();

        // Ustawienie początkowego koloru czyszczenia ekranu
        // Ten kolor będzie używany przez glClear, jeśli nie zostanie nadpisany później.
        // Renderer.setupOpenGLState() również ustawia glClearColor, więc to ustawienie
        // może być redundantne lub służyć jako bardzo wczesny domyślny.
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f); // Ciemnoszary/niebieskawy

        // Włączenie podstawowych testów OpenGL
        glEnable(GL_DEPTH_TEST); // Włącz test głębi
        glEnable(GL_CULL_FACE);  // Włącz odrzucanie tylnych ścianek
        glCullFace(GL_BACK);     // Odrzucaj tylne ścianki

        // Ustawienie viewportu na cały rozmiar okna
        // To jest viewport dla framebuffera, niekoniecznie logicznego rozmiaru okna (ważne dla HiDPI)
        // Potrzebujemy rozmiarów framebuffera tutaj.
        try (MemoryStack stack = stackPush()) {
            IntBuffer fbWidth = stack.mallocInt(1);
            IntBuffer fbHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(windowHandle, fbWidth, fbHeight);
            glViewport(0, 0, fbWidth.get(0), fbHeight.get(0));

            // Zapisz początkowe logiczne rozmiary, jeśli FramebufferSizeCallback jeszcze nie został wywołany
            // (choć zwykle jest wywoływany przynajmniej raz przy tworzeniu okna)
            this.width = fbWidth.get(0); // lub zostaw oryginalne width/height z konstruktora, callback to zaktualizuje
            this.height = fbHeight.get(0);
        }


        // Callback dla zmiany rozmiaru framebuffera (nie tylko okna)
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            if (w > 0 && h > 0) {
                glViewport(0, 0, w, h);
                // Aktualizuj przechowywane rozmiary okna, jeśli używasz ich do obliczeń proporcji itp.
                // Tutaj powinniśmy aktualizować this.width i this.height na podstawie rozmiaru framebuffera
                // lub mieć osobne pola dla rozmiaru framebuffera.
                // Na razie zakładamy, że Window.getWidth/Height zwraca logiczny rozmiar,
                // a glViewport zawsze używa rozmiaru framebuffera.
                // Jeśli chcesz, aby getWidth/getHeight zwracały rozmiar framebuffera, zaktualizuj je tutaj:
                // this.width = w;
                // this.height = h;
                // Jeśli chcesz, aby getWidth/getHeight zwracały rozmiar logiczny okna,
                // potrzebujesz osobnego callbacku glfwSetWindowSizeCallback.
                // Dla prostoty, na razie viewport jest głównym celem tego callbacku.
            }
        });
    }

    public boolean isKeyPressed(int keyCode) {
        return Input.isKeyDown(keyCode); // Delegacja do statycznej metody Input
    }

    public boolean windowShouldClose() {
        if (windowHandle == NULL) return true; // Jeśli okno nie istnieje, powinno się zamknąć
        return glfwWindowShouldClose(windowHandle);
    }

    /**
     * Metoda update okna. W poprzednich wersjach zawierała glfwPollEvents i glfwSwapBuffers.
     * glfwPollEvents jest teraz w Engine.loop().
     * glfwSwapBuffers jest również w Engine.loop().
     * Ta metoda może pozostać pusta lub zostać usunięta, jeśli nie ma innej logiki specyficznej dla okna na klatkę.
     */
    public void update() {
        // Jeśli potrzebujesz jakiejś logiki specyficznej dla okna na klatkę, dodaj ją tutaj.
        // Na przykład, aktualizacja tytułu okna o FPS (chociaż to lepiej robić rzadziej).
        // Na razie pusta, ponieważ kluczowe operacje (pollEvents, swapBuffers) są w Engine.loop().
    }

    public void cleanup() {
        System.out.println("Window: Cleaning up...");
        if (input != null) {
            input.cleanup(); // Input też może mieć zasoby do zwolnienia (callbacki)
            System.out.println("Window: Input cleanup called.");
        }

        // Zwolnij callbacki GLFW przypisane do tego okna
        glfwSetErrorCallback(null); // Najpierw globalny error callback
        if (windowHandle != NULL) {
            glfwFreeCallbacks(windowHandle); // Zwolnij wszystkie callbacki okna
            glfwSetFramebufferSizeCallback(windowHandle, null); // Jawne usunięcie, chociaż glfwFreeCallbacks powinno to zrobić
            // Usuń inne callbacki, jeśli były ustawiane bezpośrednio na windowHandle (np. key, mouse)
            // ale teraz są zarządzane przez Input i NuklearGui
        }


        // Zniszcz okno i zakończ GLFW
        if (windowHandle != NULL) {
            glfwDestroyWindow(windowHandle);
            System.out.println("Window: Window destroyed.");
            windowHandle = NULL; // Zapobiegaj wielokrotnemu użyciu
        }

        // glfwTerminate powinno być wywołane tylko raz na końcu działania aplikacji,
        // gdy wszystkie okna GLFW zostaną zniszczone.
        // Jeśli masz tylko jedno okno, to jest to dobre miejsce.
        glfwTerminate();
        System.out.println("Window: GLFW terminated.");

        // Zwolnij error callback, który został ustawiony na początku
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
            System.out.println("Window: GLFW error callback freed.");
        }
        System.out.println("Window: Cleanup complete.");
    }

    // --- Gettery ---
    public long getWindowHandle() { return windowHandle; }
    public int getWidth() { return width; } // Zwraca logiczną szerokość okna
    public int getHeight() { return height; } // Zwraca logiczną wysokość okna
    public String getTitle() { return title; }
    public Input getInput() { return input; } // Getter dla instancji Input
}