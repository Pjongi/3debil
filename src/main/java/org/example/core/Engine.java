package org.example.core;

import org.example.audio.AudioManager;
import org.example.graphics.Camera;
import org.example.graphics.render.Renderer;
import org.example.game.IEngineLogic;
import org.example.ui.NuklearGui;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11; // Import dla testowego rysowania
import org.lwjgl.opengl.GL15; // Import dla testowego rysowania
import org.lwjgl.opengl.GL20; // Import dla testowego rysowania
import org.lwjgl.opengl.GL30; // Import dla testowego rysowania
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class Engine {

    private Window window = null;
    private Input input = null;
    private Timer timer = null;
    private Renderer renderer = null;
    private Camera camera = null;
    private AudioManager audioManager = null;
    private NuklearGui nuklearGui;

    private final IEngineLogic gameLogic;
    private final String windowTitle;

    private boolean initializedSuccessfully = false;
    private boolean isPaused = false;
    private boolean escKeyPressed = false;

    // Zmienne dla testowego shadera (jeśli zdecydujesz się go użyć)
    // private int testQuadProgram;
    // private int testQuadVao;

    public Engine(String windowTitle, int width, int height, IEngineLogic gameLogic) {
        this.windowTitle = windowTitle;
        this.gameLogic = gameLogic;

        try {
            input = new Input();
            timer = new Timer();
            window = new Window(windowTitle, width, height);
            camera = new Camera(new Vector3f(0.0f, 1.0f, 5.0f), new Vector3f(0.0f, 1.0f, 0.0f));

            System.out.println("Engine: Initializing LWJGL Audio Manager...");
            audioManager = new AudioManager();
            audioManager.init();
            System.out.println("Engine: Audio Manager initialized.");

            window.init(input);

            System.out.println("Engine: Initializing Renderer (from org.example.graphics.render)...");
            renderer = new Renderer(window);
            renderer.init();
            System.out.println("Engine: Renderer initialized.");

            System.out.println("Engine: Initializing Nuklear GUI...");
            nuklearGui = new NuklearGui(window);
            nuklearGui.init();
            System.out.println("Engine: Nuklear GUI initialized.");

            timer.init();

            System.out.println("Engine: Initializing game logic...");
            gameLogic.init(window, renderer, audioManager, this.nuklearGui);
            System.out.println("Engine: Game logic initialized.");

            input.setActiveCallbacks();

            // Inicjalizacja testowego shadera (opcjonalnie, jeśli chcesz go mieć gotowego)
            // initTestQuadShader();


            initializedSuccessfully = true;

        } catch (Exception e) {
            System.err.println("FATAL: Engine initialization failed!");
            e.printStackTrace();
            cleanupPartialInit();
            initializedSuccessfully = false;
        }
    }

    /*
    // Opcjonalna metoda do inicjalizacji testowego shadera raz
    private void initTestQuadShader() {
        String testVertSrc = "#version 330 core\n" +
                             "layout (location = 0) in vec2 aPos;\n" +
                             "void main() { gl_Position = vec4(aPos.x, aPos.y, 0.0, 1.0); }";
        String testFragSrc = "#version 330 core\n" +
                             "out vec4 FragColor;\n" +
                             "uniform vec4 u_Color;\n" +
                             "void main() { FragColor = u_Color; }";

        testQuadProgram = GL20.glCreateProgram();
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, testVertSrc);
        GL20.glCompileShader(vs);
        if (GL20.glGetShaderi(vs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) System.err.println("Test VS fail: " + GL20.glGetShaderInfoLog(vs));
        GL20.glAttachShader(testQuadProgram, vs);

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, testFragSrc);
        GL20.glCompileShader(fs);
        if (GL20.glGetShaderi(fs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) System.err.println("Test FS fail: " + GL20.glGetShaderInfoLog(fs));
        GL20.glAttachShader(testQuadProgram, fs);

        GL20.glLinkProgram(testQuadProgram);
        if (GL20.glGetProgrami(testQuadProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) System.err.println("Test Prog Link fail: " + GL20.glGetProgramInfoLog(testQuadProgram));

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        float[] vertices = {
            -1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f
        };
        testQuadVao = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers(); // vbo może być lokalne
        GL30.glBindVertexArray(testQuadVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        // Nie usuwamy vbo tutaj, jeśli jest częścią VAO
    }
    */

    public void run() {
        if (!initializedSuccessfully) {
            System.err.println("Cannot run engine, initialization failed.");
            return;
        }
        System.out.println("Starting " + windowTitle + "...");
        System.out.println("LWJGL " + org.lwjgl.Version.getVersion() + "!");
        try {
            loop();
        } catch (Exception e) {
            System.err.println("Error during game loop:");
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public void setPaused(boolean paused) {
        if (this.isPaused == paused) {
            return;
        }
        System.out.println("Engine.setPaused(" + paused + ") called. Previous isPaused=" + this.isPaused);

        this.isPaused = paused;
        if (window == null || input == null || nuklearGui == null) {
            System.err.println("Engine.setPaused: Critical component is null.");
            return;
        }

        if (paused) {
            System.out.println("Engine: Game Paused - Activating GUI Input. Cursor set to NORMAL.");
            input.clearCallbacks();
            nuklearGui.setActiveCallbacks();
            GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        } else {
            System.out.println("Engine: Game Resumed - Activating Game Input. Cursor set to DISABLED.");
            nuklearGui.clearCallbacks();
            input.setActiveCallbacks();
            input.resetMouseDelta();
        }
    }

    private void loop() {
        while (window != null && !window.windowShouldClose()) {
            timer.update();
            float deltaTime = timer.getDeltaTime();

            if (nuklearGui != null) {
                nuklearGui.beginInput();
            }

            GLFW.glfwPollEvents();

            if (isPaused && nuklearGui != null) {
                buildPauseMenuAndHandleActions();
            }

            if (nuklearGui != null) {
                nuklearGui.endInput();
            }

            boolean escCurrentlyPressed = (window != null && glfwGetKey(window.getWindowHandle(), GLFW_KEY_ESCAPE) == GLFW_PRESS);
            if (escCurrentlyPressed && !escKeyPressed) {
                setPaused(!isPaused);
            }
            escKeyPressed = escCurrentlyPressed;

            if (audioManager != null && audioManager.getListener() != null && camera != null) {
                audioManager.getListener().setPosition(camera.getPosition());
                audioManager.getListener().setOrientation(camera.getFront(), camera.getUp());
            }

            if (!isPaused) {
                if (gameLogic != null) {
                    gameLogic.input(window, input, camera, deltaTime);
                    gameLogic.update(deltaTime);
                }
                if (input != null) input.update();
            }

            // 7. Renderowanie sceny gry
            // To powinno zawierać glClear dla sceny 3D (realizowane w SceneRenderer.render)
            if (renderer != null && camera != null && gameLogic != null) {
                // System.out.println("ENGINE: Calling gameLogic.render()"); // LOG (odkomentuj do testów)
                gameLogic.render(window, camera, renderer);
            }

            // TESTOWE RYSOANIE - ODkomentuj poniższy blok do testów
            /*
            {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                GL20.glUseProgram(testQuadProgram);
                // Ustaw kolor na półprzezroczysty zielony
                int colorLoc = GL20.glGetUniformLocation(testQuadProgram, "u_Color");
                GL20.glUniform4f(colorLoc, 0.0f, 1.0f, 0.0f, 0.3f); // RGBA

                GL30.glBindVertexArray(testQuadVao);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
                GL30.glBindVertexArray(0);
                GL20.glUseProgram(0);

                // Przywróć stan (jeśli potrzebne przed Nuklearem, ale Nuklear sam ustawia swój stan)
                // GL11.glEnable(GL11.GL_DEPTH_TEST);
                // GL11.glDisable(GL_BLEND);
                System.out.println("ENGINE LOOP: Drew test green quad.");
            }
            */


            // 8. Renderowanie GUI (Nuklear)
            if (nuklearGui != null) {
                // System.out.println("ENGINE: Calling nuklearGui.renderGUI()"); // LOG (odkomentuj do testów)
                nuklearGui.renderGUI(Nuklear.NK_ANTI_ALIASING_ON, deltaTime);
            }

            if (window != null) {
                GLFW.glfwSwapBuffers(window.getWindowHandle());
            }
        }
    }

    private void buildPauseMenuAndHandleActions() { // Bez zmian...
        NkContext ctx = nuklearGui.getContext();
        if (ctx == null) return;

        String menuTitle = "Pause Menu";

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NkRect rect = NkRect.malloc(stack);
            float menuWidth = 220;
            float menuHeight = 100;
            float x = (window.getWidth() - menuWidth) / 2;
            float y = (window.getHeight() - menuHeight) / 2;

            if (Nuklear.nk_begin(ctx, menuTitle, Nuklear.nk_rect(x, y, menuWidth, menuHeight, rect),
                    Nuklear.NK_WINDOW_BORDER | Nuklear.NK_WINDOW_MOVABLE | Nuklear.NK_WINDOW_TITLE | Nuklear.NK_WINDOW_NO_SCROLLBAR | Nuklear.NK_WINDOW_CLOSABLE )) {

                Nuklear.nk_layout_row_dynamic(ctx, 30, 1);
                if (Nuklear.nk_button_label(ctx, "Resume Game")) {
                    System.out.println("Engine: 'Resume Game' button LOGIC TRIGGERED!");
                    setPaused(false);
                }

                Nuklear.nk_layout_row_dynamic(ctx, 30, 1);
                if (Nuklear.nk_button_label(ctx, "Exit to Desktop")) {
                    System.out.println("Engine: 'Exit to Desktop' button LOGIC TRIGGERED!");
                    if (window != null) {
                        GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
                    }
                }
            } else {
                if (isPaused) {
                    setPaused(false);
                }
            }
            Nuklear.nk_end(ctx);
        }
    }

    private void cleanupPartialInit() { // Bez zmian...
        System.out.println("Engine: Cleaning up after partial initialization due to error...");
        if (gameLogic != null) { try { gameLogic.cleanup(); } catch (Exception e) { System.err.println("Error during partial gameLogic cleanup: "+e.getMessage()); e.printStackTrace();}}
        if (nuklearGui != null) { try { nuklearGui.cleanup(); } catch (Exception e) { System.err.println("Error during partial nuklearGui cleanup: "+e.getMessage()); e.printStackTrace();}}
        if (renderer != null) { try { renderer.cleanup(); } catch (Exception e) { System.err.println("Error during partial renderer cleanup: "+e.getMessage()); e.printStackTrace();}}
        if (audioManager != null) { try { audioManager.cleanup(); } catch (Exception e) { System.err.println("Error during partial audioManager cleanup: "+e.getMessage()); e.printStackTrace();}}
        if (window != null) { try { window.cleanup(); } catch (Exception e) { System.err.println("Error during partial window cleanup: "+e.getMessage()); e.printStackTrace();}}
        System.out.println("Engine: Partial cleanup finished.");
    }

    private void cleanup() { // Bez zmian...
        // Opcjonalne czyszczenie testowego shadera, jeśli był inicjalizowany
        // if (testQuadProgram != 0) GL20.glDeleteProgram(testQuadProgram);
        // if (testQuadVao != 0) GL30.glDeleteVertexArrays(testQuadVao);
        // (VBO dla testowego kwadratu jest usuwane, jeśli jest tworzone lokalnie w initTestQuadShader lub nie jest potrzebne po usunięciu VAO)


        if (!initializedSuccessfully) { System.out.println("Engine Cleanup: Skipping cleanup as initialization failed."); return; }
        System.out.println("--- Starting Engine Cleanup ---");
        long cleanupStartTime = System.nanoTime();
        try {
            System.out.println("Engine Cleanup: Stage 1/5 - Calling gameLogic.cleanup()...");
            if (gameLogic != null) gameLogic.cleanup();
            System.out.println("Engine Cleanup: Stage 1/5 - gameLogic.cleanup() finished.");
        } catch (Exception e) { System.err.println("Error during game logic cleanup: " + e.getMessage()); e.printStackTrace(); }
        try {
            System.out.println("Engine Cleanup: Stage 2/5 - Calling nuklearGui.cleanup()...");
            if (nuklearGui != null) nuklearGui.cleanup();
            System.out.println("Engine Cleanup: Stage 2/5 - nuklearGui.cleanup() finished.");
        } catch (Exception e) { System.err.println("Error during Nuklear GUI cleanup: " + e.getMessage()); e.printStackTrace(); }
        try {
            System.out.println("Engine Cleanup: Stage 3/5 - Calling renderer.cleanup()...");
            if (renderer != null) renderer.cleanup();
            System.out.println("Engine Cleanup: Stage 3/5 - renderer.cleanup() finished.");
        } catch (Exception e) { System.err.println("Error during renderer cleanup: " + e.getMessage()); e.printStackTrace(); }
        try {
            System.out.println("Engine Cleanup: Stage 4/5 - Calling audioManager.cleanup()...");
            if (audioManager != null) audioManager.cleanup();
            System.out.println("Engine Cleanup: Stage 4/5 - audioManager.cleanup() finished.");
        } catch (Exception e) { System.err.println("Error during audioManager cleanup: " + e.getMessage()); e.printStackTrace(); }
        try {
            System.out.println("Engine Cleanup: Stage 5/5 - Calling window.cleanup()...");
            if (window != null) window.cleanup();
            System.out.println("Engine Cleanup: Stage 5/5 - window.cleanup() finished.");
        } catch (Exception e) { System.err.println("Error during window cleanup: " + e.getMessage()); e.printStackTrace(); }
        long cleanupEndTime = System.nanoTime();
        System.out.println("--- Engine Cleanup Finished (took " + (cleanupEndTime - cleanupStartTime) / 1_000_000 + " ms) ---");
    }

    public NuklearGui getNuklearGui() {
        return nuklearGui;
    }
}