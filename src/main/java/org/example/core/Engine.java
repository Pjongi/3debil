package org.example.core;

import org.example.graphics.render.Renderer;
import org.example.audio.AudioManager;
import org.example.game.IEngineLogic;
import org.example.graphics.Camera;
import org.joml.Vector3f;

public class Engine {

    // Zainicjalizuj pola, których inicjalizacja jest w try-catch, wartością null
    private Window window = null;
    private Input input = null;
    private Timer timer = null;
    private Renderer renderer = null;
    private Camera camera = null;
    private AudioManager audioManager = null;

    // gameLogic jest final, więc MUSI być zainicjalizowany w konstruktorze (co robimy)
    private final IEngineLogic gameLogic;
    private final String windowTitle;
    private boolean initializedSuccessfully = false; // Flaga do śledzenia stanu inicjalizacji

    // Konstruktor pozostaje bez zmian w logice
    public Engine(String windowTitle, int width, int height, IEngineLogic gameLogic) {
        this.windowTitle = windowTitle;
        this.gameLogic = gameLogic; // Inicjalizacja finalnego pola

        try {
            // Inicjalizacje wewnątrz try
            input = new Input();
            timer = new Timer();
            window = new Window(windowTitle, width, height);
            camera = new Camera(new Vector3f(0.0f, 1.0f, 5.0f), new Vector3f(0.0f, 1.0f, 0.0f));

            System.out.println("Engine: Initializing LWJGL Audio Manager...");
            audioManager = new AudioManager();
            audioManager.init();
            System.out.println("Engine: Audio Manager initialized.");

            window.init(input);

            System.out.println("Engine: Initializing Renderer...");
            renderer = new Renderer(window);
            renderer.init();
            System.out.println("Engine: Renderer initialized.");

            timer.init();

            System.out.println("Engine: Initializing game logic...");
            gameLogic.init(window, renderer, audioManager);
            System.out.println("Engine: Game logic initialized.");

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
        } catch (Exception e) { // Złap nieoczekiwane błędy w pętli
            System.err.println("Error during game loop:");
            e.printStackTrace();
        } finally {
            cleanup(); // Zawsze sprzątaj
        }
    }

    private void loop() {
        while (!window.windowShouldClose()) {
            timer.update();
            float deltaTime = timer.getDeltaTime();

            // Aktualizuj pozycję i orientację słuchacza OpenAL (jeśli audioManager istnieje)
            if (audioManager != null && audioManager.getListener() != null) {
                audioManager.getListener().setPosition(camera.getPosition());
                audioManager.getListener().setOrientation(camera.getFront(), camera.getUp());
            }

            // Deleguj do logiki gry
            gameLogic.input(window, input, camera, deltaTime);
            gameLogic.update(deltaTime);
            gameLogic.render(window, camera, renderer); // Zakładamy, że renderer nie jest null, bo sprawdzono w run()

            window.update();
            input.update();
        }
    }

    // Metoda pomocnicza do sprzątania po częściowej inicjalizacji
    private void cleanupPartialInit() {
        System.out.println("Engine: Cleaning up after partial initialization due to error...");
        // Sprzątaj w odwrotnej kolejności, sprawdzając null
        if (gameLogic != null) { try { gameLogic.cleanup(); } catch (Exception e) { System.err.println("Error during partial gameLogic cleanup: "+e.getMessage());}}
        if (renderer != null) { try { renderer.cleanup(); } catch (Exception e) { System.err.println("Error during partial renderer cleanup: "+e.getMessage());}}
        if (audioManager != null) { try { audioManager.cleanup(); } catch (Exception e) { System.err.println("Error during partial audioManager cleanup: "+e.getMessage());}}
        if (window != null) { try { window.cleanup(); } catch (Exception e) { System.err.println("Error during partial window cleanup: "+e.getMessage());}}
        // Input jest sprzątany przez Window.cleanup()
        System.out.println("Engine: Partial cleanup finished.");
    }

    // Główna metoda sprzątająca
// W Engine.java

    private void cleanup() {
        // Sprzątaj tylko jeśli inicjalizacja się powiodła
        if (!initializedSuccessfully) {
            System.out.println("Engine Cleanup: Skipping cleanup as initialization failed.");
            return;
        }

        System.out.println("--- Starting Engine Cleanup ---");
        long cleanupStartTime = System.nanoTime(); // Zmierz czas trwania cleanup

        try {
            System.out.println("Engine Cleanup: Stage 1/4 - Calling gameLogic.cleanup()...");
            if (gameLogic != null) gameLogic.cleanup();
            System.out.println("Engine Cleanup: Stage 1/4 - gameLogic.cleanup() finished.");
        } catch (Exception e) {
            System.err.println("Error during game logic cleanup: " + e.getMessage());
            e.printStackTrace();
        }

        // Dodaj sprawdzenie błędów GL po cleanupie gry, na wszelki wypadek
        try {
        } catch (Exception e) {
            System.err.println("GL Error detected after gameLogic cleanup: " + e.getMessage());
        }

        try {
            System.out.println("Engine Cleanup: Stage 2/4 - Calling renderer.cleanup()...");
            if (renderer != null) renderer.cleanup(); // Sprząta zasoby OpenGL (shadery, tekstury, FBO)
            System.out.println("Engine Cleanup: Stage 2/4 - renderer.cleanup() finished.");
        } catch (Exception e) {
            System.err.println("Error during renderer cleanup: " + e.getMessage());
            e.printStackTrace();
        }

        // Dodaj sprawdzenie błędów GL po cleanupie renderera
        try {
        } catch (Exception e) {
            System.err.println("GL Error detected after renderer cleanup: " + e.getMessage());
        }


        try {
            System.out.println("Engine Cleanup: Stage 3/4 - Calling audioManager.cleanup()...");
            if (audioManager != null) audioManager.cleanup(); // Sprząta zasoby OpenAL (źródła, bufory, kontekst, urządzenie)
            System.out.println("Engine Cleanup: Stage 3/4 - audioManager.cleanup() finished.");
        } catch (Exception e) {
            System.err.println("Error during audioManager cleanup: " + e.getMessage());
            e.printStackTrace();
        }

        // Po audioManager.cleanup() kontekst AL jest zniszczony, nie można sprawdzać błędów AL

        try {
            System.out.println("Engine Cleanup: Stage 4/4 - Calling window.cleanup()...");
            if (window != null) window.cleanup(); // Sprząta Input, niszczy okno (i kontekst GL), terminacja GLFW
            System.out.println("Engine Cleanup: Stage 4/4 - window.cleanup() finished.");
        } catch (Exception e) {
            System.err.println("Error during window cleanup: " + e.getMessage());
            e.printStackTrace();
        }

        // Po window.cleanup() nie można już wywoływać funkcji GLFW ani OpenGL

        long cleanupEndTime = System.nanoTime();
        System.out.println("--- Engine Cleanup Finished (took " + (cleanupEndTime - cleanupStartTime) / 1_000_000 + " ms) ---");
    }
}