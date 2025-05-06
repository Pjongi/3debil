package org.example.graphics;

import org.example.core.Window;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.shadow.ShadowMap;
import org.example.scene.GameObject;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*; // Dla funkcji shaderów
import static org.lwjgl.opengl.GL30.*; // Dla Framebufferów itp.

public class Renderer {

    private ShaderProgram sceneShaderProgram;
    private ShaderProgram depthShaderProgram;
    private ShadowMap shadowMap;
    private final Window window;
    private Texture defaultTexture;

    public Renderer(Window window) {
        this.window = window;
    }

    public void init() throws Exception {
        System.out.println("Renderer: Initializing...");

        // Stwórz domyślną białą teksturę 1x1
        ByteBuffer whitePixel = null;
        try {
            whitePixel = MemoryUtil.memAlloc(4);
            whitePixel.put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).flip();
            defaultTexture = new Texture(1, 1, whitePixel);
            System.out.println("Renderer: Default texture created.");
        } catch (Exception e) {
            System.err.println("Renderer: Failed to create default texture!");
            throw e;
        } finally {
            if (whitePixel != null) MemoryUtil.memFree(whitePixel);
        }

        // Inicjalizacja ShadowMap
        System.out.println("Renderer: Initializing Shadow Map...");
        shadowMap = new ShadowMap();
        System.out.println("Renderer: Shadow Map initialized.");

        // Inicjalizacja Scene Shader Program
        System.out.println("Renderer: Initializing Scene Shader Program...");
        try {
            sceneShaderProgram = new ShaderProgram();
            sceneShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_vertex.glsl"));
            sceneShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_fragment.glsl"));
            sceneShaderProgram.link();

            // Tworzenie uniformów dla Scene Shader
            sceneShaderProgram.createUniform("projection");
            sceneShaderProgram.createUniform("view");
            sceneShaderProgram.createUniform("model");
            sceneShaderProgram.createUniform("textureSampler"); // Upewnij się, że shader tego używa!
            sceneShaderProgram.createUniform("viewPos");
            sceneShaderProgram.createUniform("dirLight.direction");
            sceneShaderProgram.createUniform("dirLight.color");
            sceneShaderProgram.createUniform("dirLight.intensity");
            sceneShaderProgram.createUniform("lightSpaceMatrix");
            sceneShaderProgram.createUniform("shadowMap");
            sceneShaderProgram.createUniform("shadowBias");
            System.out.println("Renderer: Scene Shader Program initialized successfully.");
        } catch (Exception e) {
            System.err.println("!!!!!!!! FAILED TO INITIALIZE SCENE SHADER PROGRAM !!!!!!!");
            e.printStackTrace();
            throw e; // Rzuć dalej, aby zatrzymać inicjalizację silnika
        }

        // Inicjalizacja Depth Shader Program (z dokładnym logowaniem błędów)
        System.out.println("Renderer: Initializing Depth Shader Program...");
        try {
            depthShaderProgram = new ShaderProgram();
            System.out.println("Renderer: Loading depth vertex shader...");
            depthShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_vertex.glsl"));
            System.out.println("Renderer: Loading depth fragment shader...");
            depthShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_fragment.glsl"));
            System.out.println("Renderer: Linking depth shader program...");
            depthShaderProgram.link();

            // Tworzenie uniformów dla Depth Shader
            System.out.println("Renderer: Creating depth shader uniforms...");
            depthShaderProgram.createUniform("model");
            depthShaderProgram.createUniform("lightSpaceMatrix");
            System.out.println("Renderer: Depth Shader Program initialized successfully.");

        } catch (Exception e) {
            System.err.println("!!!!!!!! FAILED TO INITIALIZE DEPTH SHADER PROGRAM !!!!!!!");
            e.printStackTrace();
            // depthShaderProgram pozostanie null
            // Rzuć dalej wyjątek, aby zatrzymać inicjalizację silnika
            throw new Exception("Failed to initialize depth shader program", e);
        }

        // Ustawienia OpenGL
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        System.out.println("Renderer: Initialization complete.");
    }

    public void render(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        // Podstawowe sprawdzenia, czy wszystko jest gotowe
        if (light == null) {
            System.err.println("Renderer.render(): Error - light is null. Cannot render.");
            return;
        }
        if (sceneShaderProgram == null || !sceneShaderProgram.isLinked()) {
            System.err.println("Renderer.render(): Error - sceneShaderProgram is not ready. Cannot render scene.");
            return;
        }
        // Sprawdzenie dla depthShaderProgram jest teraz wewnątrz renderDepthMap
        // dla pewności można je zostawić też tutaj

        // DEBUG: Sprawdzenie stanu depthShaderProgram tuż przed użyciem
        System.out.println("DEBUG: Checking depthShaderProgram before renderDepthMap. Is null? " + (this.depthShaderProgram == null));
        if(this.depthShaderProgram != null) {
            System.out.println("DEBUG: depthShaderProgram is linked? " + this.depthShaderProgram.isLinked());
        }


        // Renderuj mapę cieni
        renderDepthMap(gameObjects, light);

        // Renderuj główną scenę
        renderScene(camera, gameObjects, light);
    }

    private void renderDepthMap(List<GameObject> gameObjects, DirectionalLight light) {
        // Sprawdź, czy depth shader jest gotowy przed użyciem
        if (depthShaderProgram == null || !depthShaderProgram.isLinked()) {
            System.err.println("Renderer.renderDepthMap(): Error - depthShaderProgram is not ready. Skipping depth pass.");
            // Można by przywrócić domyślny framebuffer, ale unbindAfterWriting to zrobi
            glBindFramebuffer(GL_FRAMEBUFFER, 0); // Upewnij się, że nie renderujemy do FBO cienia
            glViewport(0, 0, window.getWidth(), window.getHeight());
            return; // Nie kontynuuj rysowania do mapy cieni
        }

        shadowMap.bindForWriting(); // Wiąże FBO i ustawia viewport
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
        shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight()); // Odwiązuje FBO i przywraca viewport
    }

    private void renderScene(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        // Ustawienia dla głównej sceny (viewport ustawiony przez unbindAfterWriting)
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        sceneShaderProgram.bind();

        // Ustaw uniformy kamery i widoku
        sceneShaderProgram.setUniform("projection", camera.getProjectionMatrix((float)window.getWidth() / window.getHeight()));
        sceneShaderProgram.setUniform("view", camera.getViewMatrix());
        sceneShaderProgram.setUniform("viewPos", camera.getPosition());

        // Ustaw uniformy światła kierunkowego
        sceneShaderProgram.setUniform("dirLight.direction", light.getDirection());
        sceneShaderProgram.setUniform("dirLight.color", light.getColor());
        sceneShaderProgram.setUniform("dirLight.intensity", light.getIntensity());

        // Ustaw uniformy związane z cieniami
        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        sceneShaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix);
        sceneShaderProgram.setUniform("shadowBias", 0.005f);

        // Aktywuj i powiąż teksturę mapy cieni z jednostką 0
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthMapTexture());
        sceneShaderProgram.setUniform("shadowMap", 0); // Powiedz shaderowi, żeby użył jednostki 0

        // Ustaw jednostkę tekstur dla tekstur obiektów (np. 1)
        sceneShaderProgram.setUniform("textureSampler", 1); // Powiedz shaderowi, żeby użył jednostki 1

        // Renderuj każdy obiekt
        for (GameObject go : gameObjects) {
            if (go == null || go.getMesh() == null) continue;

            sceneShaderProgram.setUniform("model", go.getModelMatrix());

            // Aktywuj jednostkę 1 i powiąż teksturę obiektu (lub domyślną)
            Texture texture = go.getTexture();
            Texture textureToBind = (texture != null) ? texture : defaultTexture;

            if (textureToBind != null) {
                textureToBind.bind(1); // Powiąż z jednostką 1
            } else {
                // Sytuacja awaryjna
                glActiveTexture(GL_TEXTURE1);
                glBindTexture(GL_TEXTURE_2D, 0);
                System.err.println("Warning: Rendering GameObject without a texture (and default texture is null).");
            }

            // Renderuj siatkę obiektu
            go.getMesh().render();
        }

        sceneShaderProgram.unbind();
    }

    public void cleanup() {
        System.out.println("Renderer: Cleaning up...");
        if (defaultTexture != null) defaultTexture.cleanup();
        if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
        if (depthShaderProgram != null) depthShaderProgram.cleanup();
        if (shadowMap != null) shadowMap.cleanup();
        System.out.println("Renderer: Cleanup complete.");
    }

    // Metoda sprawdzająca, czy renderer jest gotowy (opcjonalnie)
    public boolean isReady() {
        return sceneShaderProgram != null && sceneShaderProgram.isLinked() &&
                depthShaderProgram != null && depthShaderProgram.isLinked() &&
                shadowMap != null && defaultTexture != null;
    }
}