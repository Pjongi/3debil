package org.example.graphics;

import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.shadow.ShadowMap;
import org.example.scene.GameObject;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private ShaderProgram sceneShaderProgram;
    private ShaderProgram depthShaderProgram;
    private ShadowMap shadowMap;
    private final Window window;
    private Texture defaultTexture; // Domyślna BIAŁA tekstura 1x1 (dla fallbacku map)
    private Material defaultMaterial; // Domyślny SZARY materiał (gdy obiekt nie ma własnego)

    public Renderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for Renderer");
        this.window = window;
        // Inicjalizacja pól na null
        this.sceneShaderProgram = null;
        this.depthShaderProgram = null;
        this.shadowMap = null;
        this.defaultTexture = null;
        this.defaultMaterial = null;
    }

    /**
     * Inicjalizuje renderer: tworzy domyślną teksturę i materiał, mapę cieni,
     * kompiluje i linkuje shadery oraz ustawia początkowy stan OpenGL.
     *
     * @throws ResourceLoadException Jeśli wystąpi błąd podczas tworzenia zasobów GL lub ładowania shaderów.
     * @throws ResourceNotFoundException Jeśli pliki shaderów nie zostaną znalezione.
     */
    public void init() throws ResourceLoadException, ResourceNotFoundException {
        System.out.println("Renderer: Initializing...");
        long startTime = System.nanoTime();

        try {
            initDefaultTexture();       // Najpierw tekstura (potrzebna dla materiału)
            initDefaultMaterial();      // Potem domyślny materiał
            initShadowMap();            // Mapa cieni
            initDepthShaderProgram();   // Shader dla cieni
            initSceneShaderProgram();   // Główny shader (musi być po initShadowMap)
            setupOpenGLState();         // Stan OpenGL
        } catch (ResourceLoadException | ResourceNotFoundException e) {
            System.err.println("Renderer: Initialization failed!");
            cleanupPartialInit(); // Spróbuj posprzątać
            throw e; // Rzuć błąd dalej
        }

        long endTime = System.nanoTime();
        System.out.println("Renderer: Initialization complete (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    // --- Prywatne metody pomocnicze dla init ---

    private void initDefaultTexture() throws ResourceLoadException {
        System.out.println("  Renderer: Initializing default texture...");
        ByteBuffer whitePixel = null;
        try {
            whitePixel = MemoryUtil.memAlloc(4);
            whitePixel.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip(); // Biały, nieprzezroczysty
            defaultTexture = new Texture(1, 1, whitePixel, false); // Bez mipmap
            System.out.println("  Renderer: Default texture created (ID: " + defaultTexture.getTextureId() + ").");
        } catch (Exception e) {
            System.err.println("  Renderer: Failed to create default texture!");
            throw new ResourceLoadException("Failed to create default texture", e);
        } finally {
            if (whitePixel != null) MemoryUtil.memFree(whitePixel);
        }
    }

    private void initDefaultMaterial() {
        System.out.println("  Renderer: Initializing default material...");
        // Użyj domyślnego konstruktora Material (tworzy szary materiał)
        defaultMaterial = new Material();
        System.out.println("  Renderer: Default material created.");
    }

    private void initShadowMap() throws ResourceLoadException {
        System.out.println("  Renderer: Initializing Shadow Map...");
        try {
            shadowMap = new ShadowMap();
        } catch (Exception e) {
            System.err.println("  Renderer: Failed to create Shadow Map!");
            throw new ResourceLoadException("Failed to create Shadow Map", e);
        }
        System.out.println("  Renderer: Shadow Map initialized (Texture ID: " + shadowMap.getDepthMapTexture() + ").");
    }

    private void initDepthShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        System.out.println("  Renderer: Initializing Depth Shader Program...");
        depthShaderProgram = new ShaderProgram();
        try {
            depthShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_vertex.glsl"));
            depthShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_fragment.glsl"));
            depthShaderProgram.link();
            depthShaderProgram.createUniform("model");
            depthShaderProgram.createUniform("lightSpaceMatrix");
        } catch (ResourceNotFoundException | ResourceLoadException | IllegalArgumentException e) {
            if (depthShaderProgram != null) depthShaderProgram.cleanup();
            depthShaderProgram = null;
            System.err.println("  Renderer: Failed to initialize Depth Shader Program!");
            throw e;
        }
        System.out.println("  Renderer: Depth Shader Program initialized successfully.");
    }

    private void initSceneShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        System.out.println("  Renderer: Initializing Scene Shader Program...");
        sceneShaderProgram = new ShaderProgram();
        try {
            sceneShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_vertex.glsl"));
            sceneShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_fragment.glsl"));
            sceneShaderProgram.link();

            // Uniformy sceny, kamery, światła, cieni
            sceneShaderProgram.createUniform("projection");
            sceneShaderProgram.createUniform("view");
            sceneShaderProgram.createUniform("model");
            sceneShaderProgram.createUniform("viewPos");
            sceneShaderProgram.createUniform("dirLight.direction");
            sceneShaderProgram.createUniform("dirLight.color");
            sceneShaderProgram.createUniform("dirLight.intensity");
            sceneShaderProgram.createUniform("lightSpaceMatrix");
            sceneShaderProgram.createUniform("shadowMapSampler"); // Zmieniono nazwę
            sceneShaderProgram.createUniform("shadowBias");

            // Uniformy materiału
            sceneShaderProgram.createUniform("material.ambient");
            sceneShaderProgram.createUniform("material.diffuse");
            sceneShaderProgram.createUniform("material.specular");
            sceneShaderProgram.createUniform("material.reflectance");
            sceneShaderProgram.createUniform("material.hasDiffuseMap");
            sceneShaderProgram.createUniform("material.hasSpecularMap");
            sceneShaderProgram.createUniform("diffuseSampler"); // Zmieniono nazwę
            sceneShaderProgram.createUniform("specularSampler");

        } catch (ResourceNotFoundException | ResourceLoadException | IllegalArgumentException e) {
            if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
            sceneShaderProgram = null;
            System.err.println("  Renderer: Failed to initialize Scene Shader Program!");
            throw e;
        }
        System.out.println("  Renderer: Scene Shader Program initialized successfully.");
    }

    private void setupOpenGLState() {
        System.out.println("  Renderer: Setting up OpenGL state...");
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        System.out.println("  Renderer: OpenGL state set.");
    }

    // --- Metody Renderujące ---

    /**
     * Główna metoda renderująca.
     * @param camera Kamera sceny.
     * @param gameObjects Lista obiektów do wyrenderowania (powinny być już przefiltrowane pod kątem widoczności).
     * @param light Kierunkowe źródło światła.
     */
    public void render(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        // Podstawowe sprawdzenia
        if (!isReady()) {
            System.err.println("Renderer.render(): Renderer is not ready. Skipping frame.");
            return;
        }
        if (camera == null || gameObjects == null || light == null) {
            System.err.println("Renderer.render(): Camera, gameObjects list, or light is null. Skipping frame.");
            return;
        }

        // 1. Renderowanie mapy głębokości (Depth Pass)
        renderDepthMap(gameObjects, light); // Przekazujemy listę potencjalnie widocznych obiektów

        // 2. Renderowanie sceny (Scene Pass)
        renderScene(camera, gameObjects, light);
    }

    /** Renderuje obiekty do mapy głębokości. */
    private void renderDepthMap(List<GameObject> gameObjects, DirectionalLight light) {
        shadowMap.bindForWriting();
        glClear(GL_DEPTH_BUFFER_BIT);
        depthShaderProgram.bind();
        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        depthShaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix);

        // Renderujemy tylko geometrię widocznych obiektów
        for (GameObject go : gameObjects) {
            if (go != null && go.getMesh() != null) { // Zakładamy, że lista zawiera już tylko widoczne obiekty
                depthShaderProgram.setUniform("model", go.getModelMatrix());
                go.getMesh().render();
            }
        }

        depthShaderProgram.unbind();
        shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight());
    }

    /** Renderuje główną scenę. */
    private void renderScene(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        // Czyszczenie domyślnego bufora
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Użycie głównego shadera
        sceneShaderProgram.bind();

        // Ustawienie uniformów per-frame (kamera, światło, samplery)
        setupScenePerFrameUniforms(camera, light);

        // Renderowanie obiektów z ich materiałami
        renderSceneObjects(gameObjects);

        // Odwiązanie shadera
        sceneShaderProgram.unbind();
    }

    /** Ustawia uniformy wspólne dla całej klatki sceny. */
    private void setupScenePerFrameUniforms(Camera camera, DirectionalLight light) {
        // Macierze kamery
        sceneShaderProgram.setUniform("projection", camera.getProjectionMatrix((float)window.getWidth() / window.getHeight()));
        sceneShaderProgram.setUniform("view", camera.getViewMatrix());
        sceneShaderProgram.setUniform("viewPos", camera.getPosition());

        // Światło kierunkowe
        sceneShaderProgram.setUniform("dirLight.direction", light.getDirection());
        sceneShaderProgram.setUniform("dirLight.color", light.getColor());
        sceneShaderProgram.setUniform("dirLight.intensity", light.getIntensity());

        // Cienie
        sceneShaderProgram.setUniform("lightSpaceMatrix", light.getLightSpaceMatrix());
        sceneShaderProgram.setUniform("shadowBias", 0.005f);
        // Powiąż teksturę mapy cieni z JEDNOSTKĄ 2
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthMapTexture());
        sceneShaderProgram.setUniform("shadowMapSampler", 2); // Powiedz shaderowi, że używa jednostki 2

        // --- Ustawienie JEDNOSTEK dla samplerów materiału ---
        // Te uniformy ustawiamy raz na klatkę
        sceneShaderProgram.setUniform("diffuseSampler", Material.DIFFUSE_MAP_TEXTURE_UNIT);   // = 0
        sceneShaderProgram.setUniform("specularSampler", Material.SPECULAR_MAP_TEXTURE_UNIT); // = 1
    }

    /** Renderuje poszczególne obiekty w scenie, używając ich materiałów. */
    private void renderSceneObjects(List<GameObject> gameObjects) {
        for (GameObject go : gameObjects) {
            // Pomiń obiekty null lub bez siatki (lista powinna zawierać tylko widoczne)
            if (go == null || go.getMesh() == null) continue;

            // Ustaw macierz modelu obiektu
            sceneShaderProgram.setUniform("model", go.getModelMatrix());

            // Pobierz materiał obiektu lub użyj domyślnego
            Material mat = go.getMaterial();
            Material materialToBind = (mat != null) ? mat : defaultMaterial;

            // Zwiąż materiał (ustawi uniformy i tekstury materiału)
            if (materialToBind != null) {
                // Przekazujemy domyślną teksturę jako fallback dla map materiału
                materialToBind.bind(sceneShaderProgram, defaultTexture);
            } else {
                // To nie powinno się zdarzyć, jeśli inicjalizacja przebiegła poprawnie
                System.err.println("Error: No material to bind for object and default material is null!");
                // Awaryjnie można by spróbować ustawić jakieś podstawowe uniformy
                // sceneShaderProgram.setUniform("material.ambient", new Vector3f(0.1f));
                // sceneShaderProgram.setUniform("material.diffuse", new Vector3f(0.5f));
                // ... itp.
                // Oraz odwiązać tekstury
                glActiveTexture(GL_TEXTURE0 + Material.DIFFUSE_MAP_TEXTURE_UNIT);
                glBindTexture(GL_TEXTURE_2D, 0);
                glActiveTexture(GL_TEXTURE0 + Material.SPECULAR_MAP_TEXTURE_UNIT);
                glBindTexture(GL_TEXTURE_2D, 0);
            }

            // Renderuj siatkę obiektu
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
        // Materiał domyślny nie zarządza teksturą, więc sprzątamy tylko teksturę
        if (defaultTexture != null) defaultTexture.cleanup();

        // Zerowanie referencji
        sceneShaderProgram = null;
        depthShaderProgram = null;
        shadowMap = null;
        defaultTexture = null;
        defaultMaterial = null;

        long endTime = System.nanoTime();
        System.out.println("Renderer: Cleanup complete (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void cleanupPartialInit() {
        System.out.println("Renderer: Cleaning up partially initialized resources due to error...");
        if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
        if (depthShaderProgram != null) depthShaderProgram.cleanup();
        if (shadowMap != null) shadowMap.cleanup();
        if (defaultTexture != null) defaultTexture.cleanup();
        // defaultMaterial nie wymaga cleanupu
        sceneShaderProgram = null;
        depthShaderProgram = null;
        shadowMap = null;
        defaultTexture = null;
        defaultMaterial = null;
        System.out.println("Renderer: Partial cleanup finished.");
    }

    public boolean isReady() {
        // Sprawdź wszystkie kluczowe komponenty, w tym domyślny materiał
        return sceneShaderProgram != null && sceneShaderProgram.isLinked() &&
                depthShaderProgram != null && depthShaderProgram.isLinked() &&
                shadowMap != null &&
                defaultTexture != null &&
                defaultMaterial != null;
    }
}