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
import org.lwjgl.system.MemoryStack;

// Potrzebne importy dla glfwGetKey
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
    private boolean escKeyPressed = false; // Służy do wykrywania zbocza narastającego naciśnięcia ESC

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
            gameLogic.init(window, renderer, audioManager);
            System.out.println("Engine: Game logic initialized.");

            input.setActiveCallbacks();

            initializedSuccessfully = true;

        } catch (Exception e) {
            System.err.println("FATAL: Engine initialization failed!");
            e.printStackTrace();
            cleanupPartialInit();
            initializedSuccessfully = false;
        }
    }

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
            // System.out.println("Engine.setPaused: No change in pause state (" + paused + ")");
            return;
        }
        System.out.println("Engine.setPaused(" + paused + ") called. Previous isPaused=" + this.isPaused); // LOG

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

            // Sugestia: Zakomentuj poniższy blok `if (nuklearGui.getContext() != null)`
            // jeśli samo `nk_begin` w `buildPauseMenuAndHandleActions` jest wystarczające do pokazania okna.
            // To może pomóc uniknąć konfliktów w zarządzaniu widocznością okna Nuklear.
            /*
            if (nuklearGui.getContext() != null) {
                System.out.println("Engine.setPaused: Calling nk_window_show for 'Pause Menu' to NK_SHOWN."); // LOG
                Nuklear.nk_window_show(nuklearGui.getContext(), "Pause Menu", Nuklear.NK_SHOWN);
            }
            */

        } else {
            System.out.println("Engine: Game Resumed - Activating Game Input. Cursor set to DISABLED.");
            nuklearGui.clearCallbacks();
            input.setActiveCallbacks();
            input.resetMouseDelta(); // Ważne, aby zresetować deltę myszy po wyjściu z GUI
        }
    }

    private void loop() {
        while (window != null && !window.windowShouldClose()) {
            timer.update();
            float deltaTime = timer.getDeltaTime();

            // 1. Rozpocznij input dla Nukleara, JEŚLI jest pauza
            if (isPaused && nuklearGui != null) {
                nuklearGui.beginInput();
            }

            // 2. Przetwórz zdarzenia systemowe (mysz, klawiatura itp.)
            GLFW.glfwPollEvents(); // To wywoła callbacki (Nukleara lub Input.java)

            // 3. JEŚLI jest pauza: Zdefiniuj UI i sprawdź interakcje, a następnie zakończ input Nukleara
            if (isPaused && nuklearGui != null) {
                buildPauseMenuAndHandleActions(); // Definiuje UI i odpytuje stan przycisków
                nuklearGui.endInput();            // Finalizuje input Nukleara dla tej ramki
            }

            // 4. Logika globalna (ESC do pauzy)
            // Użyj glfwGetKey dla niezawodnego odczytu stanu fizycznego klawisza
            // System.out.println("Engine.loop() - Top: isPaused=" + isPaused + ", escKeyPressedBeforeCheck=" + escKeyPressed); // LOG

            boolean escCurrentlyPressed = (window != null && glfwGetKey(window.getWindowHandle(), GLFW_KEY_ESCAPE) == GLFW_PRESS);
            // System.out.println("Engine.loop() - escCurrentlyPressed=" + escCurrentlyPressed); // LOG

            if (escCurrentlyPressed && !escKeyPressed) {
                // System.out.println("Engine.loop() - ESC TOGGLE! Current isPaused=" + isPaused + ", new isPaused=" + !isPaused); // LOG
                setPaused(!isPaused);
            }
            escKeyPressed = escCurrentlyPressed; // Aktualizuj stan poprzedniego wciśnięcia
            // System.out.println("Engine.loop() - Bottom: isPaused=" + isPaused + ", escKeyPressedAfterUpdate=" + escKeyPressed); // LOG


            // 5. Aktualizacja słuchacza audio
            if (audioManager != null && audioManager.getListener() != null && camera != null) {
                audioManager.getListener().setPosition(camera.getPosition());
                audioManager.getListener().setOrientation(camera.getFront(), camera.getUp());
            }

            // 6. Logika gry
            if (!isPaused) {
                if (gameLogic != null) {
                    gameLogic.input(window, input, camera, deltaTime);
                    gameLogic.update(deltaTime);
                }
                if (input != null) input.update(); // Input.update() głównie oblicza mouseDelta
            }
            // else: Jeśli jest pauza, logika menu jest już obsłużona w kroku 3

            // 7. Renderowanie
            if (renderer != null && camera != null && gameLogic != null) {
                gameLogic.render(window, camera, renderer);
            }

            if (isPaused && nuklearGui != null) {
                nuklearGui.renderGUI(Nuklear.NK_ANTI_ALIASING_ON);
            }

            // 8. Zamiana buforów okna
            if (window != null) {
                GLFW.glfwSwapBuffers(window.getWindowHandle());
            }
        }
    }

    private void buildPauseMenuAndHandleActions() {
        NkContext ctx = nuklearGui.getContext();
        String menuTitle = "Pause Menu";

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NkRect rect = NkRect.malloc(stack);
            float menuWidth = 220;
            float menuHeight = 100; // Zwiększono, aby zmieścić przyciski
            float x = (window.getWidth() - menuWidth) / 2;
            float y = (window.getHeight() - menuHeight) / 2;

            // Upewnij się, że okno Nuklear jest "otwarte" jeśli gra jest spauzowana
            // Jeśli używasz NK_WINDOW_CLOSABLE, to zamknięcie okna przez X ustawi jego stan na NK_HIDDEN.
            // Tutaj możemy chcieć je pokazać ponownie, jeśli gra jest w stanie pauzy.
            // Alternatywnie, można by nie używać NK_WINDOW_CLOSABLE i polegać tylko na przyciskach.
            // nk_window_show(ctx, menuTitle, Nuklear.NK_SHOWN); // Opcjonalne, jeśli zarządzasz widocznością inaczej

            if (Nuklear.nk_begin(ctx, menuTitle, Nuklear.nk_rect(x, y, menuWidth, menuHeight, rect),
                    Nuklear.NK_WINDOW_BORDER | Nuklear.NK_WINDOW_MOVABLE | Nuklear.NK_WINDOW_TITLE | Nuklear.NK_WINDOW_NO_SCROLLBAR | Nuklear.NK_WINDOW_CLOSABLE )) {

                Nuklear.nk_layout_row_dynamic(ctx, 30, 1);
                if (Nuklear.nk_button_label(ctx, "Resume Game")) {
                    System.out.println("Engine: 'Resume Game' button LOGIC TRIGGERED!");
                    setPaused(false); // Wznowienie gry
                }

                Nuklear.nk_layout_row_dynamic(ctx, 30, 1);
                if (Nuklear.nk_button_label(ctx, "Exit to Desktop")) {
                    System.out.println("Engine: 'Exit to Desktop' button LOGIC TRIGGERED!");
                    if (window != null) {
                        GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
                    }
                }
            } else {
                // nk_begin zwróciło false, co oznacza, że okno Nuklear jest "zamknięte"
                // (np. przez kliknięcie 'X' jeśli NK_WINDOW_CLOSABLE jest ustawione).
                // W takiej sytuacji chcemy wznowić grę.
                // System.out.println("Engine.buildPauseMenu: nk_begin for '" + menuTitle + "' returned false. Current isPaused: " + isPaused); // LOG
                if (isPaused) { // Dodatkowe sprawdzenie, czy faktycznie byliśmy w pauzie
                    // System.out.println("Engine.buildPauseMenu: Resuming game because Nuklear window was closed (nk_begin returned false)."); // LOG
                    setPaused(false);
                }
            }
            Nuklear.nk_end(ctx);
        }
    }

    private void cleanupPartialInit() {
        System.out.println("Engine: Cleaning up after partial initialization due to error...");
        if (gameLogic != null) { try { gameLogic.cleanup(); } catch (Exception e) { System.err.println("Error during partial gameLogic cleanup: "+e.getMessage());}}
        if (nuklearGui != null) { try { nuklearGui.cleanup(); } catch (Exception e) { System.err.println("Error during partial nuklearGui cleanup: "+e.getMessage());}}
        if (renderer != null) { try { renderer.cleanup(); } catch (Exception e) { System.err.println("Error during partial renderer cleanup: "+e.getMessage());}}
        if (audioManager != null) { try { audioManager.cleanup(); } catch (Exception e) { System.err.println("Error during partial audioManager cleanup: "+e.getMessage());}}
        if (window != null) { try { window.cleanup(); } catch (Exception e) { System.err.println("Error during partial window cleanup: "+e.getMessage());}}
        System.out.println("Engine: Partial cleanup finished.");
    }

    private void cleanup() {
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
}