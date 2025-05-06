package org.example.game;

import org.example.audio.AudioManager;
import org.example.audio.SoundSource;
import org.example.core.Input;
import org.example.core.Window;
import org.example.exception.ResourceLoadException;      // Importuj nowe wyjątki
import org.example.exception.ResourceNotFoundException; // Importuj nowe wyjątki
import org.example.graphics.Camera;
import org.example.graphics.Mesh;
import org.example.graphics.Renderer;
import org.example.graphics.Texture;
import org.example.graphics.light.DirectionalLight;
import org.example.scene.GameObject;
import org.example.util.MeshLoader;
import org.example.util.ModelLoader;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoGame implements IEngineLogic {

    private List<GameObject> gameObjects;
    private Map<String, Mesh> meshes;
    private Map<String, Texture> textures;
    private DirectionalLight directionalLight;
    private AudioManager audioManager;
    private SoundSource backgroundMusicSource;
    private SoundSource stepSoundSource;
    private int stepSoundBuffer = -1; // Nadal potrzebne ID bufora
    private float stepSoundCooldown = 0.3f;
    private float timeSinceLastStep = 0f;

    @Override
    // Usunięto 'throws Exception', bo łapiemy specyficzne wyjątki wewnątrz
    public void init(Window window, Renderer renderer, AudioManager audioManager) {
        this.audioManager = audioManager;
        gameObjects = new ArrayList<>();
        meshes = new HashMap<>();
        textures = new HashMap<>();

        System.out.println("DemoGame: Initializing resources...");
        // Ładuj zasoby z obsługą błędów
        loadTexturesSafe();
        loadMeshesSafe();
        loadSoundsSafe(); // Teraz używa poprawionej logiki

        // Twórz obiekty tylko jeśli podstawowe zasoby się załadowały
        if (!textures.isEmpty() && !meshes.isEmpty()) {
            createGameObjects();
        } else {
            System.err.println("DemoGame: Skipping GameObject creation due to missing essential textures or meshes.");
        }
        createLights();
        System.out.println("DemoGame: Resources initialization attempt finished.");

        if (backgroundMusicSource != null) {
            backgroundMusicSource.play();
            System.out.println("DemoGame: Playing background music.");
        }
    }

    private void loadTexturesSafe() {
        System.out.println("DemoGame: Loading textures...");
        try {
            textures.put("stone", new Texture("textures/stone.png"));
        } catch (ResourceNotFoundException e) {
            System.err.println("DemoGame: ERROR - Texture file not found: " + e.getMessage());
        } catch (ResourceLoadException e) {
            System.err.println("DemoGame: ERROR - Failed to load stone texture: " + e.getMessage());
        }
        try {
            textures.put("wood", new Texture("textures/wood.png"));
        } catch (ResourceNotFoundException e) {
            System.err.println("DemoGame: ERROR - Texture file not found: " + e.getMessage());
        } catch (ResourceLoadException e) {
            System.err.println("DemoGame: ERROR - Failed to load wood texture: " + e.getMessage());
        }
        try {
            textures.put("grass", new Texture("textures/grass.png"));
            System.out.println("DemoGame: Custom textures loaded (or attempted).");
        } catch (ResourceNotFoundException e) {
            System.err.println("DemoGame: ERROR - Texture file not found: " + e.getMessage());
        } catch (ResourceLoadException e) {
            System.err.println("DemoGame: ERROR - Failed to load grass texture: " + e.getMessage());
        }

        // Zawsze twórz domyślną teksturę (zakładamy, że to się uda)
        try {
            textures.put("default", createDefaultTexture());
            System.out.println("DemoGame: Default texture created.");
        } catch (ResourceLoadException e) {
            System.err.println("DemoGame: FATAL ERROR - Failed to create default texture: " + e.getMessage());
            // Można rozważyć przerwanie działania aplikacji tutaj, jeśli domyślna tekstura jest krytyczna
        }
    }

    private void loadMeshesSafe() {
        System.out.println("DemoGame: Loading meshes...");
        try {
            meshes.put("cube", MeshLoader.createCube());
            meshes.put("plane", MeshLoader.createPlane(10.0f));
            System.out.println("DemoGame: Basic meshes created.");
        } catch (Exception e) { // MeshLoader nie rzuca specyficznych wyjątków
            System.err.println("DemoGame: ERROR - Failed to create basic meshes: " + e.getMessage());
        }
        try {
            Mesh bunnyMesh = ModelLoader.loadMesh("models/bunny.obj"); // Może rzucić wyjątki
            meshes.put("bunny", bunnyMesh);
            System.out.println("DemoGame: Bunny mesh loaded.");
        } catch (ResourceNotFoundException e) {
            System.err.println("DemoGame: ERROR - Model file not found: " + e.getMessage());
        } catch (ResourceLoadException e) {
            System.err.println("DemoGame: ERROR - Failed to load bunny model: " + e.getMessage());
        }
    }

    private void loadSoundsSafe() {
        System.out.println("DemoGame: Loading sounds...");
        int musicBufferId = -1;
        try {
            musicBufferId = audioManager.loadSound("audio/music.wav"); // Może rzucić wyjątki
            backgroundMusicSource = audioManager.createSource(true, true); // Twórz tylko jeśli ładowanie się powiodło
            backgroundMusicSource.setBuffer(musicBufferId);
            backgroundMusicSource.setGain(0.3f);
            System.out.println("DemoGame: Background music loaded and source created.");
        } catch (ResourceNotFoundException e) {
            System.err.println("DemoGame: ERROR - Music file not found: " + e.getMessage());
            backgroundMusicSource = null;
        } catch (ResourceLoadException | RuntimeException e) { // Złap też RuntimeException z OpenAL
            System.err.println("DemoGame: ERROR - Failed to load/process music: " + e.getMessage());
            // e.printStackTrace(); // Opcjonalnie
            backgroundMusicSource = null;
        }

        stepSoundBuffer = -1; // Zresetuj ID
        try {
            stepSoundBuffer = audioManager.loadSound("audio/step.wav"); // Może rzucić wyjątki
            stepSoundSource = audioManager.createSource(false, false); // Twórz tylko jeśli ładowanie się powiodło
            stepSoundSource.setBuffer(stepSoundBuffer);
            stepSoundSource.setPosition(new Vector3f(0, 0, 0)); // Ustaw pozycję, nawet jeśli nie jest odtwarzany od razu
            stepSoundSource.setGain(0.8f);
            System.out.println("DemoGame: Step sound loaded and source created.");
        } catch (ResourceNotFoundException e) {
            System.err.println("DemoGame: ERROR - Step sound file not found: " + e.getMessage());
            stepSoundSource = null;
        } catch (ResourceLoadException | RuntimeException e) { // Złap też RuntimeException z OpenAL
            System.err.println("DemoGame: ERROR - Failed to load/process step sound: " + e.getMessage());
            stepSoundSource = null;
        }
    }

    private void createGameObjects() {
        System.out.println("DemoGame: Creating game objects...");
        Texture defaultTex = textures.get("default");
        if (defaultTex == null) { System.err.println("DemoGame: FATAL - Default texture is missing. Cannot create objects."); return; }

        Mesh planeMesh = meshes.get("plane");
        if (planeMesh != null) {
            GameObject floor = new GameObject(planeMesh, textures.getOrDefault("grass", defaultTex));
            floor.setPosition(0.0f, -0.5f, 0.0f); gameObjects.add(floor);
        } else { System.err.println("DemoGame: Plane mesh missing."); }

        Mesh cubeMesh = meshes.get("cube");
        if (cubeMesh != null) {
            GameObject cube1 = new GameObject(cubeMesh, textures.getOrDefault("stone", defaultTex));
            cube1.setPosition(-1.5f, 0.0f, -1.5f); gameObjects.add(cube1);
            GameObject cube2 = new GameObject(cubeMesh, textures.getOrDefault("wood", defaultTex));
            cube2.setPosition(1.5f, 0.5f, 0.0f); cube2.setScale(0.75f); gameObjects.add(cube2);
            GameObject cube3 = new GameObject(cubeMesh, textures.getOrDefault("stone", defaultTex));
            cube3.setPosition(0.0f, 0.2f, 1.8f); cube3.setScale(0.5f); gameObjects.add(cube3);
        } else { System.err.println("DemoGame: Cube mesh missing."); }

        Mesh bunnyMesh = meshes.get("bunny");
        if (bunnyMesh != null) {
            GameObject bunny = new GameObject(bunnyMesh, textures.getOrDefault("stone", defaultTex));
            bunny.setPosition(0.0f, -0.45f, 0.5f); bunny.setScale(0.7f); gameObjects.add(bunny);
            System.out.println("DemoGame: Bunny GameObject created.");
        } else { System.err.println("DemoGame: Bunny mesh missing."); }

        System.out.println("DemoGame: Game objects creation attempt finished.");
    }

    private void createLights() {
        System.out.println("DemoGame: Creating lights...");
        directionalLight = new DirectionalLight( new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(-0.5f, -1.0f, -0.3f), 1.0f );
        System.out.println("DemoGame: Directional light created.");
    }

    @Override
    public void input(Window window, Input input, Camera camera, float deltaTime) {
        camera.processKeyboard(deltaTime);
        Vector2f mouseDelta = Input.getMouseDelta();
        if (Math.abs(mouseDelta.x) < 1000 && Math.abs(mouseDelta.y) < 1000) { // Proste sanity check
            camera.processMouseMovement(mouseDelta.x, -mouseDelta.y, true);
        }

        boolean isMoving = Input.isKeyDown(GLFW.GLFW_KEY_W) || Input.isKeyDown(GLFW.GLFW_KEY_A) ||
                Input.isKeyDown(GLFW.GLFW_KEY_S) || Input.isKeyDown(GLFW.GLFW_KEY_D);

        timeSinceLastStep += deltaTime;
        if (isMoving && stepSoundSource != null && !stepSoundSource.isPlaying() && timeSinceLastStep >= stepSoundCooldown) {
            stepSoundSource.setPosition(camera.getPosition()); // Aktualizuj pozycję przed odtworzeniem
            stepSoundSource.play();
            timeSinceLastStep = 0f;
        }
    }

    @Override
    public void update(float deltaTime) {
        // Bardziej odporny sposób na obracanie obiektów
        for (GameObject go : gameObjects) {
            // Zakładamy, że tylko obiekty z siatką "cube" mają się obracać w różny sposób
            if (go.getMesh() == meshes.get("cube")) {
                // Można by dodać tagi lub nazwy do GameObject, aby je rozróżnić
                // Na razie użyjemy pozycji jako prostego rozróżnienia
                if (Math.abs(go.getPosition().x - 1.5f) < 0.1f) { // Kostka 2
                    go.rotate(deltaTime * 0.5f, 0.0f, 1.0f, 0.0f);
                } else if (Math.abs(go.getPosition().z - 1.8f) < 0.1f) { // Kostka 3
                    go.rotate(deltaTime * 0.8f, 1.0f, 0.3f, 0.5f);
                }
            }
        }
    }

    @Override
    public void render(Window window, Camera camera, Renderer renderer) {
        if (directionalLight != null) {
            renderer.render(camera, gameObjects, directionalLight);
        } else {
            System.err.println("DemoGame.render(): Warning - directionalLight is null. Skipping render.");
        }
    }

    @Override
    public void cleanup() {
        System.out.println("DemoGame: Cleaning up resources...");
        // Dźwięki są sprzątane przez AudioManager.cleanup() wywoływane przez Engine

        System.out.println("DemoGame: Cleaning up meshes...");
        for (Mesh mesh : meshes.values()) {
            if (mesh != null) mesh.cleanup();
        }
        meshes.clear();

        System.out.println("DemoGame: Cleaning up textures...");
        for (Texture texture : textures.values()) {
            if (texture != null) texture.cleanup();
        }
        textures.clear();
        System.out.println("DemoGame: Resource cleanup finished.");
    }

    // Metoda pomocnicza do tworzenia domyślnej tekstury
    private Texture createDefaultTexture() throws ResourceLoadException {
        ByteBuffer defaultPixel = null;
        try {
            defaultPixel = MemoryUtil.memAlloc(4);
            defaultPixel.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
            return new Texture(1, 1, defaultPixel); // Użyj konstruktora przyjmującego ByteBuffer
        } catch(Exception e) { // Złap ogólny na wszelki wypadek
            System.err.println("Failed to create default texture: " + e.getMessage());
            throw new ResourceLoadException("Failed to create default texture", e);
        } finally {
            if (defaultPixel != null) {
                MemoryUtil.memFree(defaultPixel);
            }
        }
    }
}