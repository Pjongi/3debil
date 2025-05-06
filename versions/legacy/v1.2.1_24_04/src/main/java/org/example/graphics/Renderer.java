package org.example.graphics;

import org.example.core.Window;
import org.example.exception.ResourceLoadException;      // Import nowych wyjątków
import org.example.exception.ResourceNotFoundException; // Import nowych wyjątków
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.shadow.ShadowMap;
import org.example.scene.GameObject;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private ShaderProgram sceneShaderProgram;
    private ShaderProgram depthShaderProgram;
    private ShadowMap shadowMap;
    private final Window window;
    private Texture defaultTexture;

    public Renderer(Window window) { this.window = window; }

    /**
     * Inicjalizuje renderer, w tym mapę cieni, shadery i stan OpenGL.
     * @throws ResourceLoadException Jeśli wystąpi błąd podczas tworzenia zasobów GL lub ładowania shaderów.
     * @throws ResourceNotFoundException Jeśli pliki shaderów nie zostaną znalezione.
     */
    public void init() throws ResourceLoadException, ResourceNotFoundException {
        System.out.println("Renderer: Initializing...");
        initDefaultTexture();       // Może rzucić ResourceLoadException
        initShadowMap();            // Może rzucić ResourceLoadException
        initSceneShaderProgram();   // Może rzucić oba wyjątki
        initDepthShaderProgram();   // Może rzucić oba wyjątki
        setupOpenGLState();
        System.out.println("Renderer: Initialization complete.");
    }

    // --- Prywatne metody pomocnicze dla init ---

    private void initDefaultTexture() throws ResourceLoadException {
        System.out.println("Renderer: Initializing default texture...");
        ByteBuffer whitePixel = null;
        try {
            whitePixel = MemoryUtil.memAlloc(4);
            whitePixel.put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).flip();
            defaultTexture = new Texture(1, 1, whitePixel);
            System.out.println("Renderer: Default texture created.");
        } catch (Exception e) {
            System.err.println("Renderer: Failed to create default texture!");
            throw new ResourceLoadException("Failed to create default texture", e);
        } finally {
            if (whitePixel != null) MemoryUtil.memFree(whitePixel);
        }
    }

    private void initShadowMap() throws ResourceLoadException {
        System.out.println("Renderer: Initializing Shadow Map...");
        try {
            shadowMap = new ShadowMap();
        } catch (Exception e) {
            System.err.println("Renderer: Failed to create Shadow Map!");
            throw new ResourceLoadException("Failed to create Shadow Map", e);
        }
        System.out.println("Renderer: Shadow Map initialized.");
    }

    private void initSceneShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        System.out.println("Renderer: Initializing Scene Shader Program...");
        sceneShaderProgram = new ShaderProgram();
        sceneShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_vertex.glsl"));
        sceneShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_fragment.glsl"));
        sceneShaderProgram.link();
        // Tworzenie uniformów
        sceneShaderProgram.createUniform("projection"); sceneShaderProgram.createUniform("view");
        sceneShaderProgram.createUniform("model"); sceneShaderProgram.createUniform("textureSampler");
        sceneShaderProgram.createUniform("viewPos"); sceneShaderProgram.createUniform("dirLight.direction");
        sceneShaderProgram.createUniform("dirLight.color"); sceneShaderProgram.createUniform("dirLight.intensity");
        sceneShaderProgram.createUniform("lightSpaceMatrix"); sceneShaderProgram.createUniform("shadowMap");
        sceneShaderProgram.createUniform("shadowBias");
        System.out.println("Renderer: Scene Shader Program initialized successfully.");
    }

    private void initDepthShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        System.out.println("Renderer: Initializing Depth Shader Program...");
        depthShaderProgram = new ShaderProgram();
        depthShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_vertex.glsl"));
        depthShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_fragment.glsl"));
        depthShaderProgram.link();
        // Tworzenie uniformów
        depthShaderProgram.createUniform("model");
        depthShaderProgram.createUniform("lightSpaceMatrix");
        System.out.println("Renderer: Depth Shader Program initialized successfully.");
    }

    private void setupOpenGLState() {
        System.out.println("Renderer: Setting up OpenGL state...");
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        // glClearColor ustawiane w renderScene
        System.out.println("Renderer: OpenGL state set.");
    }

    // --- Metody Renderujące ---

    public void render(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        if (!isReady()) { // Sprawdź, czy renderer jest gotowy przed renderowaniem
            System.err.println("Renderer.render(): Renderer is not ready (initialization might have failed). Skipping frame.");
            return;
        }
        if (light == null) { System.err.println("Renderer.render(): Error - light is null. Cannot render shadows or lighting."); return; }

        renderDepthMap(gameObjects, light);
        renderScene(camera, gameObjects, light);
    }

    private void renderDepthMap(List<GameObject> gameObjects, DirectionalLight light) {
        // Sprawdzenie depthShaderProgram jest w isReady(), ale można dodać dla pewności
        if (depthShaderProgram == null || !depthShaderProgram.isLinked()) {
            System.err.println("Renderer.renderDepthMap(): Error - depthShaderProgram is not ready. Skipping depth pass.");
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, window.getWidth(), window.getHeight());
            return;
        }

        shadowMap.bindForWriting();
        depthShaderProgram.bind();
        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        depthShaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix);

        for (GameObject go : gameObjects) {
            if (go != null && go.getMesh() != null) {
                depthShaderProgram.setUniform("model", go.getModelMatrix());
                go.getMesh().render();
            }
        }
        depthShaderProgram.unbind();
        shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight());
    }

    private void renderScene(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        sceneShaderProgram.bind();
        setupScenePerFrameUniforms(camera, light); // Używa metody pomocniczej
        renderSceneObjects(gameObjects);           // Używa metody pomocniczej
        sceneShaderProgram.unbind();
    }

    // Metody pomocnicze dla renderScene (wydzielone wcześniej)
    private void setupScenePerFrameUniforms(Camera camera, DirectionalLight light) {
        // Ustaw uniformy kamery
        Matrix4f projMatrix = camera.getProjectionMatrix((float)window.getWidth() / window.getHeight());
        Matrix4f viewMatrix = camera.getViewMatrix();

        // DEBUG: Logowanie macierzy kamery
        // Możesz zakomentować te linie po zdiagnozowaniu problemu
        // System.out.println("DEBUG: Projection Matrix:\n" + projMatrix);
        // System.out.println("DEBUG: View Matrix:\n" + viewMatrix);

        sceneShaderProgram.setUniform("projection", projMatrix);
        sceneShaderProgram.setUniform("view", viewMatrix);
        sceneShaderProgram.setUniform("viewPos", camera.getPosition());

        // Ustaw uniformy światła
        sceneShaderProgram.setUniform("dirLight.direction", light.getDirection());
        sceneShaderProgram.setUniform("dirLight.color", light.getColor());
        sceneShaderProgram.setUniform("dirLight.intensity", light.getIntensity());

        // Ustaw uniformy cieni
        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        sceneShaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix);
        sceneShaderProgram.setUniform("shadowBias", 0.005f);

        // --- Ustawienie jednostek tekstur ---
        // Mapa cieni na jednostce 0
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthMapTexture());
        sceneShaderProgram.setUniform("shadowMap", 0);

        // Tekstura obiektu na jednostce 1
        // glActiveTexture(GL_TEXTURE1); // Niepotrzebne tutaj, ustawiane dla każdego obiektu
        sceneShaderProgram.setUniform("textureSampler", 1);
    }

    private void renderSceneObjects(List<GameObject> gameObjects) {
        // System.out.println("DEBUG: Rendering " + gameObjects.size() + " objects..."); // Logowanie liczby obiektów

        for (GameObject go : gameObjects) {
            if (go == null || go.getMesh() == null) continue;

            // System.out.println("DEBUG: Rendering GameObject at " + go.getPosition()); // Logowanie pozycji

            // Ustaw macierz modelu dla obiektu
            Matrix4f modelMatrix = go.getModelMatrix();
            // DEBUG: Logowanie macierzy modelu (np. tylko dla jednego obiektu)
            // if (gameObjects.indexOf(go) == 1) { // Przykład: loguj dla drugiego obiektu
            //     System.out.println("DEBUG: Model Matrix (Object " + gameObjects.indexOf(go) + "):\n" + modelMatrix);
            // }
            sceneShaderProgram.setUniform("model", modelMatrix);

            // Powiąż teksturę obiektu lub domyślną
            Texture texture = go.getTexture();
            Texture textureToBind = (texture != null) ? texture : defaultTexture;

            if (textureToBind != null) {
                // Aktywuj jednostkę 1 i powiąż teksturę
                // glActiveTexture(GL_TEXTURE1); // Ustawiane raz w setupScenePerFrameUniforms jest wystarczające, jeśli tylko jedna tekstura obiektu
                textureToBind.bind(1); // Wiąże teksturę z jednostką 1
            } else {
                // Odwiąż teksturę z jednostki 1, jeśli nie ma co wiązać
                glActiveTexture(GL_TEXTURE1);
                glBindTexture(GL_TEXTURE_2D, 0);
                System.err.println("Warning: Rendering GameObject without a texture (and default texture is null).");
            }

            // Renderuj siatkę obiektu
            go.getMesh().render();
        }
    }

    // --- Sprzątanie i Stan ---

    public void cleanup() {
        System.out.println("Renderer: Cleaning up...");
        if (defaultTexture != null) defaultTexture.cleanup();
        if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
        if (depthShaderProgram != null) depthShaderProgram.cleanup();
        if (shadowMap != null) shadowMap.cleanup();
        System.out.println("Renderer: Cleanup complete.");
    }

    public boolean isReady() {
        return sceneShaderProgram != null && sceneShaderProgram.isLinked() &&
                depthShaderProgram != null && depthShaderProgram.isLinked() &&
                shadowMap != null && defaultTexture != null;
    }
}