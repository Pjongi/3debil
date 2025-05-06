package org.example.graphics;

import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.light.PointLight;
import org.example.graphics.light.SpotLight;
import org.example.graphics.shadow.ShadowMap;
import org.example.scene.GameObject;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Główna klasa renderująca. Zarządza shaderami, mapą cieni,
 * domyślnymi zasobami i orkiestruje proces renderowania sceny.
 */
public class Renderer {

    // Stałe
    public static final int MAX_POINT_LIGHTS = 4;
    public static final int MAX_SPOT_LIGHTS = 2;

    // Zależności
    private final Window window;

    // Zarządzane zasoby
    private ShaderProgram sceneShaderProgram;
    private ShaderProgram depthShaderProgram;
    private ShadowMap shadowMap;
    private Texture defaultTexture;
    private Material defaultMaterial;

    public Renderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for Renderer");
        this.window = window;
        // Inicjalizacja pól na null
        this.sceneShaderProgram = null; this.depthShaderProgram = null; this.shadowMap = null;
        this.defaultTexture = null; this.defaultMaterial = null;
    }

    // --- Inicjalizacja ---

    public void init() throws ResourceLoadException, ResourceNotFoundException {
        System.out.println("Renderer: Initializing...");
        long startTime = System.nanoTime();
        try {
            initDefaultResources();
            initShadowMapping();
            initShaders();
            setupOpenGLState();
        } catch (ResourceLoadException | ResourceNotFoundException e) {
            System.err.println("Renderer: Initialization failed!");
            cleanupPartialInit(); throw e;
        }
        long endTime = System.nanoTime();
        System.out.println("Renderer: Initialization complete (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void initDefaultResources() throws ResourceLoadException {
        System.out.println("  Renderer: Initializing default resources...");
        // Tekstura
        ByteBuffer whitePixel = null;
        try {
            whitePixel = MemoryUtil.memAlloc(4).put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).flip();
            defaultTexture = new Texture(1, 1, whitePixel, false);
            System.out.println("    Default texture created (ID: " + defaultTexture.getTextureId() + ").");
        } catch (Exception e) { throw new ResourceLoadException("Failed to create default texture", e); }
        finally { if (whitePixel != null) MemoryUtil.memFree(whitePixel); }
        // Materiał
        defaultMaterial = new Material();
        System.out.println("    Default material created.");
    }

    private void initShadowMapping() throws ResourceLoadException {
        System.out.println("  Renderer: Initializing shadow mapping...");
        try {
            shadowMap = new ShadowMap();
            System.out.println("    Shadow Map initialized (Texture ID: " + shadowMap.getDepthMapTexture() + ").");
        } catch (Exception e) { throw new ResourceLoadException("Failed to create Shadow Map", e); }
    }

    private void initShaders() throws ResourceNotFoundException, ResourceLoadException {
        System.out.println("  Renderer: Initializing shaders...");
        try {
            initDepthShaderProgram();
            initSceneShaderProgram();
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            System.err.println("  Renderer: Shader initialization failed!");
            // cleanupPartialInit wywoła cleanup shaderów jeśli istnieją
            throw e;
        }
        System.out.println("  Renderer: Shaders initialized successfully.");
    }


    private void initDepthShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        depthShaderProgram = new ShaderProgram();
        depthShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_vertex.glsl"));
        depthShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_fragment.glsl"));
        depthShaderProgram.link();
        depthShaderProgram.createUniform("model");
        depthShaderProgram.createUniform("lightSpaceMatrix");
    }

    private void initSceneShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        sceneShaderProgram = new ShaderProgram();
        sceneShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_vertex.glsl"));
        sceneShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_fragment.glsl"));
        sceneShaderProgram.link();

        // Create uniforms (scene, camera, light, shadow, material, samplers)
        String[] uniformNames = {
                "projection", "view", "model", "viewPos",
                "dirLight.direction", "dirLight.color", "dirLight.intensity",
                "lightSpaceMatrix", "shadowMapSampler", "shadowBias",
                "material.ambient", "material.diffuse", "material.specular", "material.reflectance",
                "material.hasDiffuseMap", "material.hasSpecularMap",
                "diffuseSampler", "specularSampler",
                "numPointLights", "numSpotLights"
        };
        for (String name : uniformNames) sceneShaderProgram.createUniform(name);

        // Create array uniforms for point lights
        for (int i = 0; i < MAX_POINT_LIGHTS; i++) {
            String base = "pointLights[" + i + "].";
            sceneShaderProgram.createUniform(base + "position");
            sceneShaderProgram.createUniform(base + "color");
            sceneShaderProgram.createUniform(base + "intensity");
            sceneShaderProgram.createUniform(base + "att.constant");
            sceneShaderProgram.createUniform(base + "att.linear");
            sceneShaderProgram.createUniform(base + "att.quadratic");
        }
        // Create array uniforms for spot lights
        for (int i = 0; i < MAX_SPOT_LIGHTS; i++) {
            String base = "spotLights[" + i + "].";
            sceneShaderProgram.createUniform(base + "pl.position");
            sceneShaderProgram.createUniform(base + "pl.color");
            sceneShaderProgram.createUniform(base + "pl.intensity");
            sceneShaderProgram.createUniform(base + "pl.att.constant");
            sceneShaderProgram.createUniform(base + "pl.att.linear");
            sceneShaderProgram.createUniform(base + "pl.att.quadratic");
            sceneShaderProgram.createUniform(base + "direction");
            sceneShaderProgram.createUniform(base + "cutOffCos");
            sceneShaderProgram.createUniform(base + "outerCutOffCos");
        }
    }


    private void setupOpenGLState() {
        System.out.println("  Renderer: Setting up OpenGL state...");
        glEnable(GL_DEPTH_TEST);    // Włącz test głębokości
        glEnable(GL_CULL_FACE);     // Włącz odrzucanie niewidocznych ścian
        glCullFace(GL_BACK);        // Odrzucaj ściany tylne
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f); // Kolor tła
        System.out.println("  Renderer: OpenGL state set.");
    }

    // --- Główna Metoda Renderująca ---

    public void render(Camera camera, List<GameObject> gameObjects,
                       DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {

        if (!isReady()) { System.err.println("Renderer.render(): Not ready."); return; }
        if (camera == null || gameObjects == null) { System.err.println("Renderer.render(): Null arguments."); return; }

        // 1. Depth Pass (dla cieni kierunkowych)
        renderDepthPass(gameObjects, dirLight);

        // 2. Scene Pass (główne renderowanie z oświetleniem)
        renderScenePass(camera, gameObjects, dirLight, pointLights, spotLights);
    }

    // --- Przebiegi Renderowania ---

    private void renderDepthPass(List<GameObject> gameObjects, DirectionalLight dirLight) {
        if (dirLight == null) { // Jeśli nie ma światła kierunkowego, nie generujemy mapy cieni
            shadowMap.bindForWriting(); // Wciąż musimy związać i wyczyścić, aby mapa była pusta
            glClear(GL_DEPTH_BUFFER_BIT);
            shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight());
            return;
        }

        shadowMap.bindForWriting();
        glClear(GL_DEPTH_BUFFER_BIT);
        depthShaderProgram.bind();
        depthShaderProgram.setUniform("lightSpaceMatrix", dirLight.getLightSpaceMatrix());

        for (GameObject go : gameObjects) {
            // Renderuj tylko widoczne obiekty do mapy cieni
            if (go != null && go.getMesh() != null && go.getProperties().isVisible()) {
                depthShaderProgram.setUniform("model", go.getModelMatrix());
                go.getMesh().render();
            }
        }

        depthShaderProgram.unbind();
        shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight());
    }

    private void renderScenePass(Camera camera, List<GameObject> gameObjects,
                                 DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {
        // Ustaw viewport na rozmiar okna (shadow map mogła go zmienić)
        glViewport(0, 0, window.getWidth(), window.getHeight());
        // Czyszczenie domyślnego bufora ramki
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Użycie głównego shadera
        sceneShaderProgram.bind();

        // Ustawienie uniformów per-frame
        setCameraUniforms(camera);
        setLightingUniforms(dirLight, pointLights, spotLights);
        setSamplerUniforms(); // Przypisanie jednostek do samplerów

        // Renderowanie obiektów
        renderSceneObjects(gameObjects);

        // Odwiązanie shadera
        sceneShaderProgram.unbind();
    }

    // --- Ustawianie Uniformów ---

    private void setCameraUniforms(Camera camera) {
        sceneShaderProgram.setUniform("projection", camera.getProjectionMatrix((float)window.getWidth() / window.getHeight()));
        sceneShaderProgram.setUniform("view", camera.getViewMatrix());
        sceneShaderProgram.setUniform("viewPos", camera.getPosition());
    }

    private void setLightingUniforms(DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {
        // Światło Kierunkowe + Cienie
        if (dirLight != null) {
            sceneShaderProgram.setUniform("dirLight.direction", dirLight.getDirection());
            sceneShaderProgram.setUniform("dirLight.color", dirLight.getColor());
            sceneShaderProgram.setUniform("dirLight.intensity", dirLight.getIntensity());
            sceneShaderProgram.setUniform("lightSpaceMatrix", dirLight.getLightSpaceMatrix());
            sceneShaderProgram.setUniform("shadowBias", 0.005f);
            glActiveTexture(GL_TEXTURE2); // Jednostka 2 dla mapy cieni
            glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthMapTexture());
        } else {
            sceneShaderProgram.setUniform("dirLight.intensity", 0.0f); // Wyłącz światło kierunkowe
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, 0); // Odwiąż teksturę cienia
        }

        // Światła Punktowe
        int numPoints = pointLights != null ? Math.min(pointLights.size(), MAX_POINT_LIGHTS) : 0;
        sceneShaderProgram.setUniform("numPointLights", numPoints);
        for (int i = 0; i < numPoints; i++) setPointLightUniforms(pointLights.get(i), i);

        // Światła Reflektorowe
        int numSpots = spotLights != null ? Math.min(spotLights.size(), MAX_SPOT_LIGHTS) : 0;
        sceneShaderProgram.setUniform("numSpotLights", numSpots);
        for (int i = 0; i < numSpots; i++) setSpotLightUniforms(spotLights.get(i), i);
    }

    private void setPointLightUniforms(PointLight pl, int index) {
        String base = "pointLights[" + index + "].";
        sceneShaderProgram.setUniform(base + "position", pl.position);
        sceneShaderProgram.setUniform(base + "color", pl.color);
        sceneShaderProgram.setUniform(base + "intensity", pl.intensity);
        sceneShaderProgram.setUniform(base + "att.constant", pl.attenuation.constant);
        sceneShaderProgram.setUniform(base + "att.linear", pl.attenuation.linear);
        sceneShaderProgram.setUniform(base + "att.quadratic", pl.attenuation.quadratic);
    }

    private void setSpotLightUniforms(SpotLight sl, int index) {
        String base = "spotLights[" + index + "].";
        // Użyj setPointLightUniforms dla części wspólnej, dostosowując nazwę bazy
        // (trochę mniej czytelne, ale unika powtórzeń)
        String plBase = base + "pl.";
        sceneShaderProgram.setUniform(plBase + "position", sl.pointLight.position);
        sceneShaderProgram.setUniform(plBase + "color", sl.pointLight.color);
        sceneShaderProgram.setUniform(plBase + "intensity", sl.pointLight.intensity);
        sceneShaderProgram.setUniform(plBase + "att.constant", sl.pointLight.attenuation.constant);
        sceneShaderProgram.setUniform(plBase + "att.linear", sl.pointLight.attenuation.linear);
        sceneShaderProgram.setUniform(plBase + "att.quadratic", sl.pointLight.attenuation.quadratic);
        // Część specyficzna dla SpotLight
        sceneShaderProgram.setUniform(base + "direction", sl.direction);
        sceneShaderProgram.setUniform(base + "cutOffCos", sl.getCutOffCos());
        sceneShaderProgram.setUniform(base + "outerCutOffCos", sl.getOuterCutOffCos());
    }

    private void setSamplerUniforms() {
        // Przypisz jednostki teksturujące do uniformów samplerów w shaderze
        sceneShaderProgram.setUniform("diffuseSampler", Material.DIFFUSE_MAP_TEXTURE_UNIT);   // = 0
        sceneShaderProgram.setUniform("specularSampler", Material.SPECULAR_MAP_TEXTURE_UNIT); // = 1
        sceneShaderProgram.setUniform("shadowMapSampler", 2);                                // = 2
    }

    // --- Renderowanie Obiektów ---

    private void renderSceneObjects(List<GameObject> gameObjects) {
        for (GameObject go : gameObjects) {
            if (go == null || go.getMesh() == null) continue; // Pomiń nieprawidłowe

            // Ustaw macierz modelu
            sceneShaderProgram.setUniform("model", go.getModelMatrix());

            // Pobierz i zwiąż materiał (lub domyślny)
            Material materialToBind = go.getMaterial() != null ? go.getMaterial() : defaultMaterial;
            if (materialToBind != null) {
                materialToBind.bind(sceneShaderProgram, defaultTexture);
            } else {
                System.err.println("Error: Missing material and default material!");
                // Awaryjne odwiązanie
                glActiveTexture(GL_TEXTURE0 + Material.DIFFUSE_MAP_TEXTURE_UNIT); glBindTexture(GL_TEXTURE_2D, 0);
                glActiveTexture(GL_TEXTURE0 + Material.SPECULAR_MAP_TEXTURE_UNIT); glBindTexture(GL_TEXTURE_2D, 0);
            }

            // Renderuj siatkę
            go.getMesh().render();
        }
    }

    // --- Sprzątanie i Stan ---

    public void cleanup() {
        System.out.println("Renderer: Cleaning up...");
        long startTime = System.nanoTime();
        if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
        if (depthShaderProgram != null) depthShaderProgram.cleanup();
        if (shadowMap != null) shadowMap.cleanup();
        if (defaultTexture != null) defaultTexture.cleanup();
        sceneShaderProgram = null; depthShaderProgram = null; shadowMap = null;
        defaultTexture = null; defaultMaterial = null;
        long endTime = System.nanoTime();
        System.out.println("Renderer: Cleanup complete (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void cleanupPartialInit() {
        System.out.println("Renderer: Cleaning up partially initialized resources...");
        if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
        if (depthShaderProgram != null) depthShaderProgram.cleanup();
        if (shadowMap != null) shadowMap.cleanup();
        if (defaultTexture != null) defaultTexture.cleanup();
        sceneShaderProgram = null; depthShaderProgram = null; shadowMap = null;
        defaultTexture = null; defaultMaterial = null;
        System.out.println("Renderer: Partial cleanup finished.");
    }

    public boolean isReady() {
        return sceneShaderProgram != null && sceneShaderProgram.isLinked() &&
                depthShaderProgram != null && depthShaderProgram.isLinked() &&
                shadowMap != null &&
                defaultTexture != null &&
                defaultMaterial != null;
    }
}