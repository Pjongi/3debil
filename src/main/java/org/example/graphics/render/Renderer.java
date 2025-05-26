package org.example.graphics.render;

import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.Camera;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.light.PointLight;
import org.example.graphics.light.SpotLight;
import org.example.scene.GameObject;
import org.lwjgl.opengl.GL11; // Dla glGetError
import static org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION;

import java.util.List;

import static org.lwjgl.opengl.GL11.*; // Importy dla stanu OpenGL

public class Renderer {

    public static final int MAX_POINT_LIGHTS = 4;
    public static final int MAX_SPOT_LIGHTS = 2; // Zakładając, że tyle obsługujesz w shaderze sceny

    private final Window window;

    private ShaderManager shaderManager;
    private ShadowRenderer shadowRenderer; // Dla cieni kierunkowych
    private SceneRenderer sceneRenderer;
    private DefaultResourceManager defaultResourceManager;
    private SpotLightShadowRenderer spotLightShadowRenderer; // Dla cieni reflektorowych (jeśli używasz)

    private boolean initialized = false;

    public Renderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for Renderer");
        this.window = window;
        this.shaderManager = null;
        this.shadowRenderer = null;
        this.sceneRenderer = null;
        this.defaultResourceManager = null;
        this.spotLightShadowRenderer = null; // Inicjalizacja
        this.initialized = false;
    }

    public void init() throws ResourceLoadException, ResourceNotFoundException {
        if (initialized) {
            System.out.println("Renderer: Already initialized.");
            return;
        }
        System.out.println("Renderer: Initializing...");
        long startTime = System.nanoTime();

        try {
            shaderManager = new ShaderManager();
            shadowRenderer = new ShadowRenderer(window);
            sceneRenderer = new SceneRenderer(window);
            defaultResourceManager = new DefaultResourceManager();
            spotLightShadowRenderer = new SpotLightShadowRenderer(window); // Utwórz instancję

            defaultResourceManager.init();
            shaderManager.init(); // Shadery muszą być pierwsze, jeśli inne komponenty z nich korzystają
            shadowRenderer.init();
            spotLightShadowRenderer.init(); // Inicjalizuj renderer cieni reflektorowych

            // Przekaż zależności do SceneRenderer
            // Upewnij się, że przekazujesz poprawne ID tekstur cieni, jeśli używasz obu typów
            sceneRenderer.setupDependencies(
                    shaderManager.getSceneShaderProgram(),
                    defaultResourceManager.getDefaultTexture(),
                    defaultResourceManager.getDefaultMaterial(),
                    shadowRenderer.getShadowMapTextureId()
                    // Jeśli shader sceny obsługuje też cienie reflektorowe, potrzebujesz sposobu
                    // na przekazanie ID tekstury cube mapy z spotLightShadowRenderer
                    // np. dodając kolejny argument do setupDependencies lub osobną metodę.
                    // Na razie zakładam, że shader sceny używa tylko cieni kierunkowych.
            );

            setupOpenGLState();
            initialized = true;

        } catch (ResourceLoadException | ResourceNotFoundException e) {
            System.err.println("Renderer: Initialization failed!");
            cleanupPartialInit();
            throw e;
        } catch (Exception e) {
            System.err.println("Renderer: Unexpected error during initialization!");
            e.printStackTrace();
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
        // Kolor tła ustawiany tutaj jest domyślnym kolorem czyszczenia.
        // SceneRenderer wywołuje glClear z tym kolorem.
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        System.out.println("  Renderer: Global OpenGL state set.");
    }

    public void render(Camera camera, List<GameObject> gameObjects,
                       DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {

        if (!isReady()) {
            System.err.println("Renderer.render(): Not ready. Skipping frame.");
            return;
        }

        // 1. Przebieg Cieni Kierunkowych (jeśli jest światło kierunkowe)
        if (dirLight != null && shadowRenderer != null && shaderManager.getDepthShaderProgram() != null) {
            shadowRenderer.render(gameObjects, dirLight, shaderManager.getDepthShaderProgram());
        }

        // 2. Przebieg Cieni Reflektorowych (dla każdego reflektora rzucającego cień)
        //    To jest uproszczenie; w praktyce chciałbyś iterować po spotLights i renderować mapę cieni
        //    dla tych, które mają włączone cienie. Shader sceny musiałby potem używać odpowiedniej mapy.
        //    Na razie zakładamy, że jest jeden główny spotLight rzucający cień, jeśli w ogóle.
        //    Jeśli DemoGame ma tylko jeden flashlight (spotLight.get(0)), to:
        if (spotLights != null && !spotLights.isEmpty() && spotLightShadowRenderer != null && shaderManager.getSpotLightDepthShaderProgram() != null) {
            // Załóżmy, że chcemy cienie tylko od pierwszego reflektora dla uproszczenia
            SpotLight shadowCastingSpotLight = spotLights.get(0);
            if (shadowCastingSpotLight != null) { // Upewnij się, że istnieje
                // Potrzebujesz shadera głębi dla cube mapy (może być inny niż dla 2D mapy cieni)
                spotLightShadowRenderer.render(gameObjects, shadowCastingSpotLight, shaderManager.getSpotLightDepthShaderProgram());
                // SceneRenderer musiałby wiedzieć, jak użyć tej mapy cieni (spotLightShadowRenderer.getDepthCubeMapTextureId())
            }
        }


        // 3. Główny Przebieg Sceny
        //    SceneRenderer jest odpowiedzialny za glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        //    przed rysowaniem sceny.
        if (sceneRenderer != null) {
            // Jeśli shader sceny używa cieni reflektorowych, musisz przekazać ID tekstury cube mapy
            // do SceneRenderer (np. przez nową metodę lub rozszerzenie setupDependencies)
            // i SceneRenderer musi ją zbindować i użyć w shaderze.
            // np. sceneRenderer.setSpotLightShadowMapTexture(spotLightShadowRenderer.getDepthCubeMapTextureId());
            sceneRenderer.render(camera, gameObjects, dirLight, pointLights, spotLights);
        }

        checkGLErrors("EndOfFrame_Renderer");
    }

    private void checkGLErrors(String context) {
        int error;
        while ((error = GL11.glGetError()) != GL_NO_ERROR) {
            System.err.println("OpenGL Error (" + context + "): " + error + " - " + getGLErrorString(error));
        }
    }

    // Prosta metoda pomocnicza do konwersji kodów błędów GL na stringi
    private String getGLErrorString(int error) {
        switch (error) {
            case GL_INVALID_ENUM: return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE: return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION: return "GL_INVALID_OPERATION";
            case GL_STACK_OVERFLOW: return "GL_STACK_OVERFLOW";
            case GL_STACK_UNDERFLOW: return "GL_STACK_UNDERFLOW";
            case GL_OUT_OF_MEMORY: return "GL_OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION: return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default: return "Unknown GL error";
        }
    }

    public void cleanup() {
        if (!initialized && shaderManager == null && shadowRenderer == null && sceneRenderer == null && defaultResourceManager == null && spotLightShadowRenderer == null) {
            System.out.println("Renderer: Cleanup skipped (already clean or never initialized).");
            return;
        }
        System.out.println("Renderer: Cleaning up...");
        long startTime = System.nanoTime();

        if (sceneRenderer != null) {
            sceneRenderer.cleanup(); // Głównie zeruje referencje
            sceneRenderer = null;
        }
        if (shadowRenderer != null) {
            shadowRenderer.cleanup();
            shadowRenderer = null;
        }
        if (spotLightShadowRenderer != null) { // Sprzątanie renderera cieni reflektorowych
            spotLightShadowRenderer.cleanup();
            spotLightShadowRenderer = null;
        }
        if (shaderManager != null) {
            shaderManager.cleanup();
            shaderManager = null;
        }
        if (defaultResourceManager != null) {
            defaultResourceManager.cleanup();
            defaultResourceManager = null;
        }

        initialized = false;

        long endTime = System.nanoTime();
        System.out.println("Renderer: Cleanup complete (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void cleanupPartialInit() {
        System.out.println("Renderer: Cleaning up partially initialized resources due to error...");
        cleanup(); // Wywołaj główną metodę cleanup
        System.out.println("Renderer: Partial cleanup finished.");
    }

    public boolean isReady() {
        return initialized &&
                shaderManager != null && shaderManager.areShadersReady() &&
                shadowRenderer != null && shadowRenderer.getShadowMap() != null &&
                spotLightShadowRenderer != null && // Sprawdzenie gotowości
                sceneRenderer != null &&
                defaultResourceManager != null;
    }

    // Opcjonalny getter, jeśli potrzebny
    public ShaderManager getShaderManager() {
        return shaderManager;
    }
}