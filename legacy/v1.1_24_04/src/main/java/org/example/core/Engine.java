// src/main/java/org/example/core/Engine.java
package org.example.core;

import org.example.game.IEngineLogic; // Import interfejsu
import org.example.graphics.Camera;
import org.example.graphics.Renderer;
import org.joml.Vector3f;

// Główna klasa silnika, odpowiedzialna za zarządzanie cyklem życia aplikacji
// i koordynację podstawowych podsystemów.
public class Engine {

    private final Window window;
    private final Input input;
    private final Timer timer;
    private final Renderer renderer;
    private final Camera camera; // Kamera jest zarządzana przez silnik
    private final IEngineLogic gameLogic; // Logika specyficzna dla gry/dema

    private final String windowTitle;

    public Engine(String windowTitle, int width, int height, IEngineLogic gameLogic) throws Exception {
        this.windowTitle = windowTitle;
        this.gameLogic = gameLogic;

        // Inicjalizacja podstawowych komponentów silnika
        input = new Input();
        timer = new Timer();
        window = new Window(windowTitle, width, height);
        // Kamera tworzona tutaj, ale może być konfigurowana/kontrolowana przez gameLogic
        camera = new Camera(new Vector3f(0.0f, 1.0f, 5.0f), new Vector3f(0.0f, 1.0f, 0.0f));
        // Renderer musi być zainicjalizowany PO utworzeniu okna i kontekstu GL
        // ale PRZED wywołaniem gameLogic.init(), aby gameLogic mogło go użyć
        window.init(input); // Tworzy kontekst GL
        renderer = new Renderer(window);
        renderer.init(); // Ładuje shadery silnika, tworzy shadow map
        timer.init();

        // Inicjalizacja logiki gry - przekazujemy gotowe komponenty silnika
        gameLogic.init(window, renderer);
    }

    public void run() {
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

    private void loop() {
        while (!window.windowShouldClose()) {
            timer.update();
            float deltaTime = timer.getDeltaTime();

            // Przekazanie sterowania do logiki gry
            gameLogic.input(window, input, camera, deltaTime); // Logika gry obsługuje wejście
            gameLogic.update(deltaTime); // Logika gry aktualizuje swój stan
            gameLogic.render(window, camera, renderer); // Logika gry zleca renderowanie

            // Aktualizacje silnika na koniec klatki
            window.update(); // Zamiana buforów, obsługa zdarzeń GLFW
            input.update();  // Obliczenie delty myszy itp.
        }
    }

    private void cleanup() {
        System.out.println("Cleaning up " + windowTitle + "...");
        gameLogic.cleanup(); // Sprzątanie zasobów gry
        if (renderer != null) renderer.cleanup(); // Sprzątanie renderera (shadery, shadowmap)
        // Sprzątanie siatek jest teraz odpowiedzialnością gameLogic.cleanup()
        if (window != null) window.cleanup(); // Sprzątanie okna, GLFW, Input
        System.out.println("Cleanup complete.");
    }
}