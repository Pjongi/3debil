package org.example.graphics.render;

import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.Camera;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.light.PointLight;
import org.example.graphics.light.SpotLight;
import org.example.scene.GameObject;

import java.util.List;

import static org.lwjgl.opengl.GL11.*; // Importy dla stanu OpenGL

/**
 * Główny orkiestrator procesu renderowania.
 * Inicjalizuje i koordynuje komponenty renderujące (ShaderManager,
 * ShadowRenderer, SceneRenderer, DefaultResourceManager) i zarządza
 * podstawowym stanem OpenGL.
 */
public class Renderer {

    // === Stałe (przeniesione tutaj dla centralizacji) ===
    public static final int MAX_POINT_LIGHTS = 4;
    public static final int MAX_SPOT_LIGHTS = 2;

    // === Zależności ===
    private final Window window;

    // === Komponenty renderujące ===
    private ShaderManager shaderManager;
    private ShadowRenderer shadowRenderer;
    private SceneRenderer sceneRenderer;
    private DefaultResourceManager defaultResourceManager;

    private boolean initialized = false;

    public Renderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for Renderer");
        this.window = window;
        // Inicjalizacja komponentów na null
        this.shaderManager = null;
        this.shadowRenderer = null;
        this.sceneRenderer = null;
        this.defaultResourceManager = null;
        this.initialized = false;
    }

    // --- Inicjalizacja ---

    public void init() throws ResourceLoadException, ResourceNotFoundException {
        if (initialized) {
            System.out.println("Renderer: Already initialized.");
            return;
        }
        System.out.println("Renderer: Initializing...");
        long startTime = System.nanoTime();

        try {
            // Utwórz instancje komponentów
            shaderManager = new ShaderManager();
            shadowRenderer = new ShadowRenderer(window); // Przekaż zależność Window
            sceneRenderer = new SceneRenderer(window);   // Przekaż zależność Window
            defaultResourceManager = new DefaultResourceManager();

            // Zainicjalizuj komponenty w odpowiedniej kolejności
            defaultResourceManager.init(); // Domyślne zasoby najpierw
            shaderManager.init();          // Potem shadery
            shadowRenderer.init();         // Potem mapa cieni

            // Ustaw zależności dla SceneRenderer po inicjalizacji pozostałych
            sceneRenderer.setupDependencies(
                    shaderManager.getSceneShaderProgram(),
                    defaultResourceManager.getDefaultTexture(),
                    defaultResourceManager.getDefaultMaterial(),
                    shadowRenderer.getShadowMapTextureId()
            );

            // Ustaw ogólny stan OpenGL
            setupOpenGLState();

            initialized = true; // Sukces

        } catch (ResourceLoadException | ResourceNotFoundException e) {
            System.err.println("Renderer: Initialization failed!");
            cleanupPartialInit(); throw e;
        } catch (Exception e) { // Złap inne nieoczekiwane wyjątki
            System.err.println("Renderer: Unexpected error during initialization!");
            e.printStackTrace(); // Wypisz stack trace dla nieoczekiwanych błędów
            cleanupPartialInit();
            throw new ResourceLoadException("Unexpected error during Renderer initialization", e);
        }

        long endTime = System.nanoTime();
        System.out.println("Renderer: Initialization complete (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void setupOpenGLState() {
        System.out.println("  Renderer: Setting up global OpenGL state...");
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f); // Kolor tła
        System.out.println("  Renderer: Global OpenGL state set.");
        // Uwaga: Viewport jest zarządzany przez ShadowRenderer i SceneRenderer
    }

    // --- Główna Metoda Renderująca ---

    public void render(Camera camera, List<GameObject> gameObjects,
                       DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {

        if (!isReady()) {
            System.err.println("Renderer.render(): Not ready. Skipping frame.");
            return;
        }

        // 1. Przebieg Cieni (Depth Pass) - delegacja do ShadowRenderer
        shadowRenderer.render(gameObjects, dirLight, shaderManager.getDepthShaderProgram());

        // 2. Przebieg Sceny (Scene Pass) - delegacja do SceneRenderer
        sceneRenderer.render(camera, gameObjects, dirLight, pointLights, spotLights);

        // Sprawdzenie błędów OpenGL na koniec klatki (opcjonalne, może wpływać na wydajność)
        // checkGLErrors("EndOfFrame");
    }

    // Opcjonalna metoda do sprawdzania błędów GL
    private void checkGLErrors(String context) {
        int error;
        while ((error = glGetError()) != GL_NO_ERROR) {
            System.err.println("OpenGL Error (" + context + "): " + error);
            // Można tu dodać mapowanie kodu błędu na string, jeśli potrzeba
        }
    }

    // --- Sprzątanie ---

    public void cleanup() {
        if (!initialized && shaderManager == null && shadowRenderer == null && sceneRenderer == null && defaultResourceManager == null) {
            System.out.println("Renderer: Cleanup skipped (already clean or never initialized).");
            return;
        }
        System.out.println("Renderer: Cleaning up...");
        long startTime = System.nanoTime();

        // Sprzątaj komponenty w odwrotnej kolejności inicjalizacji (lub logicznej)
        if (sceneRenderer != null) { // SceneRenderer nie ma zasobów GPU, ale można wywołać dla spójności
            sceneRenderer.cleanup();
            sceneRenderer = null;
        }
        if (shadowRenderer != null) {
            shadowRenderer.cleanup();
            shadowRenderer = null;
        }
        if (shaderManager != null) {
            shaderManager.cleanup();
            shaderManager = null;
        }
        if (defaultResourceManager != null) {
            defaultResourceManager.cleanup();
            defaultResourceManager = null;
        }

        initialized = false; // Zresetuj flagę

        long endTime = System.nanoTime();
        System.out.println("Renderer: Cleanup complete (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void cleanupPartialInit() {
        System.out.println("Renderer: Cleaning up partially initialized resources due to error...");
        // Wywołaj główną metodę cleanup, która sprawdzi nulle i posprząta co się da
        cleanup();
        System.out.println("Renderer: Partial cleanup finished.");
    }

    // --- Stan ---

    public boolean isReady() {
        // Renderer jest gotowy, jeśli został pomyślnie zainicjalizowany
        // i wszystkie jego kluczowe komponenty są gotowe (wewnętrznie sprawdzane przez ich gettery lub isReady)
        return initialized &&
                shaderManager != null && shaderManager.areShadersReady() &&
                shadowRenderer != null && shadowRenderer.getShadowMap() != null && // Proste sprawdzenie
                sceneRenderer != null && // SceneRenderer nie ma złożonego stanu "ready" poza ustawionymi zależnościami
                defaultResourceManager != null; // DefaultResourceManager nie ma złożonego stanu
    }
}