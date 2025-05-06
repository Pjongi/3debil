package org.example.core;

import org.example.audio.AudioManager; // Używamy teraz naszej wersji LWJGL
import org.example.game.IEngineLogic;
import org.example.graphics.Camera;
import org.example.graphics.Renderer;
import org.joml.Vector3f;

public class Engine {

    private final Window window;
    private final Input input;
    private final Timer timer;
    private final Renderer renderer;
    private final Camera camera;
    private final AudioManager audioManager; // Zmieniono typ z powrotem
    private final IEngineLogic gameLogic;
    private final String windowTitle;

    public Engine(String windowTitle, int width, int height, IEngineLogic gameLogic) throws Exception {
        this.windowTitle = windowTitle;
        this.gameLogic = gameLogic;

        input = new Input();
        timer = new Timer();
        window = new Window(windowTitle, width, height);
        camera = new Camera(new Vector3f(0.0f, 1.0f, 5.0f), new Vector3f(0.0f, 1.0f, 0.0f));

        // Inicjalizuj AudioManager (LWJGL)
        System.out.println("Engine: Initializing LWJGL Audio Manager...");
        audioManager = new AudioManager();
        audioManager.init();
        System.out.println("Engine: Audio Manager initialized.");

        // Inicjalizuj okno i kontekst GL
        window.init(input);

        // Inicjalizuj Renderer PO kontekście GL
        renderer = new Renderer(window);
        renderer.init();

        // Inicjalizuj Timer
        timer.init();

        // Inicjalizuj logikę gry, przekazując AudioManager
        System.out.println("Engine: Initializing game logic...");
        gameLogic.init(window, renderer, audioManager); // Przekazujemy poprawny typ
        System.out.println("Engine: Game logic initialized.");
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

            // Aktualizuj pozycję i orientację słuchacza OpenAL
            audioManager.getListener().setPosition(camera.getPosition());
            audioManager.getListener().setOrientation(camera.getFront(), camera.getUp()); // Używamy getUp()

            gameLogic.input(window, input, camera, deltaTime);
            gameLogic.update(deltaTime);
            gameLogic.render(window, camera, renderer);

            window.update();
            input.update();
        }
    }

    private void cleanup() {
        System.out.println("Cleaning up " + windowTitle + "...");
        gameLogic.cleanup();
        if (renderer != null) renderer.cleanup();
        if (audioManager != null) audioManager.cleanup(); // Sprzątnij AudioManager
        if (window != null) window.cleanup();
        System.out.println("Cleanup complete.");
    }
}