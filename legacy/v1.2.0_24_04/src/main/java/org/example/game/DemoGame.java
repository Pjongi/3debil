package org.example.game;

import org.example.audio.AudioManager;
import org.example.audio.SoundSource;
import org.example.core.Input;
import org.example.core.Window;
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
    private int stepSoundBuffer = -1;
    private float stepSoundCooldown = 0.3f;
    private float timeSinceLastStep = 0f;


    @Override
    public void init(Window window, Renderer renderer, AudioManager audioManager) throws Exception {
        this.audioManager = audioManager;
        gameObjects = new ArrayList<>();
        meshes = new HashMap<>();
        textures = new HashMap<>();

        System.out.println("DemoGame: Initializing resources...");
        loadTextures();
        loadMeshes();
        loadSounds(); // Ładuje teraz pliki .wav
        createGameObjects();
        createLights();
        System.out.println("DemoGame: Resources initialized.");

        if (backgroundMusicSource != null) {
            backgroundMusicSource.play();
            System.out.println("DemoGame: Playing background music.");
        }
    }

    private void loadTextures() {
        System.out.println("DemoGame: Loading textures...");
        try {
            textures.put("stone", new Texture("textures/stone.png"));
            textures.put("wood", new Texture("textures/wood.png"));
            textures.put("grass", new Texture("textures/grass.png"));
            System.out.println("DemoGame: Custom textures loaded.");
        } catch (Exception e) {
            System.err.println("DemoGame: Failed to load custom textures: " + e.getMessage());
            e.printStackTrace();
        }
        textures.put("default", createDefaultTexture());
        System.out.println("DemoGame: Default texture created.");
    }

    private void loadMeshes() {
        System.out.println("DemoGame: Loading meshes...");
        meshes.put("cube", MeshLoader.createCube());
        meshes.put("plane", MeshLoader.createPlane(10.0f));
        System.out.println("DemoGame: Basic meshes created.");
        try {
            Mesh bunnyMesh = ModelLoader.loadMesh("models/bunny.obj");
            if (bunnyMesh != null) {
                meshes.put("bunny", bunnyMesh);
                System.out.println("DemoGame: Bunny mesh loaded.");
            } else {
                System.err.println("DemoGame: Bunny mesh could not be loaded.");
            }
        } catch (Exception e) {
            System.err.println("DemoGame: Error loading bunny model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSounds() {
        System.out.println("DemoGame: Loading sounds...");
        try {
            // *** UPEWNIJ SIĘ, ŻE PLIKI .wav ISTNIEJĄ I ŚCIEŻKI SĄ POPRAWNE ***
            int musicBuffer = audioManager.loadSound("audio/music.wav");
            stepSoundBuffer = audioManager.loadSound("audio/step.wav");

            backgroundMusicSource = audioManager.createSource(true, true);
            // Sprawdź czy bufor się załadował przed użyciem
            if (musicBuffer != -1) { // Zakładając, że loadSound zwraca -1 przy błędzie
                backgroundMusicSource.setBuffer(musicBuffer);
                backgroundMusicSource.setGain(0.3f);
            } else {
                System.err.println("DemoGame: Cannot set buffer for backgroundMusicSource, buffer not loaded.");
                backgroundMusicSource = null; // Ustaw na null, żeby nie próbować odtwarzać
            }


            stepSoundSource = audioManager.createSource(false, false);
            if(stepSoundBuffer != -1) {
                stepSoundSource.setBuffer(stepSoundBuffer);
                stepSoundSource.setPosition(new Vector3f(0, 0, 0));
                stepSoundSource.setGain(0.8f);
            } else {
                System.err.println("DemoGame: Cannot set buffer for stepSoundSource, buffer not loaded.");
                stepSoundSource = null;
            }


        } catch (Exception e) {
            System.err.println("Failed to load sounds: " + e.getMessage());
            e.printStackTrace();
            stepSoundBuffer = -1;
            backgroundMusicSource = null; // Ustaw na null w razie wyjątku
            stepSoundSource = null;
        }
    }

    // Metody createGameObjects, createLights bez zmian

    private void createGameObjects() {
        System.out.println("DemoGame: Creating game objects...");
        Texture defaultTex = textures.get("default");
        if (defaultTex == null) { System.err.println("DemoGame: FATAL - Default texture is null."); }

        Mesh planeMesh = meshes.get("plane");
        if (planeMesh != null && defaultTex != null) { // Dodatkowe sprawdzenie defaultTex
            GameObject floor = new GameObject(planeMesh, textures.getOrDefault("grass", defaultTex));
            floor.setPosition(0.0f, -0.5f, 0.0f);
            gameObjects.add(floor);
        } else { System.err.println("DemoGame: Cannot create floor (mesh or default texture missing)."); }

        Mesh cubeMesh = meshes.get("cube");
        if (cubeMesh != null && defaultTex != null) { // Dodatkowe sprawdzenie defaultTex
            GameObject cube1 = new GameObject(cubeMesh, textures.getOrDefault("stone", defaultTex));
            cube1.setPosition(-1.5f, 0.0f, -1.5f);
            gameObjects.add(cube1);

            GameObject cube2 = new GameObject(cubeMesh, textures.getOrDefault("wood", defaultTex));
            cube2.setPosition(1.5f, 0.5f, 0.0f);
            cube2.setScale(0.75f);
            gameObjects.add(cube2);

            GameObject cube3 = new GameObject(cubeMesh, textures.getOrDefault("stone", defaultTex));
            cube3.setPosition(0.0f, 0.2f, 1.8f);
            cube3.setScale(0.5f);
            gameObjects.add(cube3);
        } else { System.err.println("DemoGame: Cannot create cubes (mesh or default texture missing)."); }


        Mesh bunnyMesh = meshes.get("bunny");
        if (bunnyMesh != null && defaultTex != null) { // Dodatkowe sprawdzenie defaultTex
            GameObject bunny = new GameObject(bunnyMesh, textures.getOrDefault("wood", defaultTex));
            bunny.setPosition(0.0f, -0.45f, 0.5f);
            bunny.setScale(0.7f);
            gameObjects.add(bunny);
            System.out.println("DemoGame: Bunny GameObject created.");
        } else { System.err.println("DemoGame: Cannot create bunny (mesh or default texture missing)."); }

        System.out.println("DemoGame: Game objects creation attempt finished.");
    }

    private void createLights() {
        System.out.println("DemoGame: Creating lights...");
        directionalLight = new DirectionalLight(
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(-0.5f, -1.0f, -0.3f),
                1.0f
        );
        if (this.directionalLight != null) {
            System.out.println("DemoGame: Directional light created.");
        } else {
            System.err.println("FATAL ERROR in createLights(): directionalLight is still null after creation!");
        }
    }

    @Override
    public void input(Window window, Input input, Camera camera, float deltaTime) {
        camera.processKeyboard(deltaTime);
        Vector2f mouseDelta = Input.getMouseDelta();
        if (Math.abs(mouseDelta.x) < 1000 && Math.abs(mouseDelta.y) < 1000) {
            camera.processMouseMovement(mouseDelta.x, -mouseDelta.y, true);
        }

        boolean isMoving = Input.isKeyDown(GLFW.GLFW_KEY_W) || Input.isKeyDown(GLFW.GLFW_KEY_A) ||
                Input.isKeyDown(GLFW.GLFW_KEY_S) || Input.isKeyDown(GLFW.GLFW_KEY_D);

        timeSinceLastStep += deltaTime;
        if (isMoving && stepSoundSource != null && !stepSoundSource.isPlaying() && timeSinceLastStep >= stepSoundCooldown) {
            stepSoundSource.setPosition(camera.getPosition());
            stepSoundSource.play();
            timeSinceLastStep = 0f;
        }
    }

    @Override
    public void update(float deltaTime) {
        Mesh cubeMesh = meshes.get("cube");
        if (cubeMesh != null) {
            // Znajdowanie obiektów po indeksie jest BARDZO ZŁE, jeśli kolejność się zmieni.
            // Lepsze byłoby przechowywanie referencji do obiektów, które mają się obracać.
            // Na razie zostawiamy, ale to jest kandydat do refaktoryzacji.
            try {
                if (gameObjects.size() > 1 && gameObjects.get(1).getMesh() == cubeMesh) {
                    gameObjects.get(1).rotate(deltaTime * 0.5f, 0.0f, 1.0f, 0.0f);
                }
                if (gameObjects.size() > 3 && gameObjects.get(3).getMesh() == cubeMesh) {
                    gameObjects.get(3).rotate(deltaTime * 0.8f, 1.0f, 0.3f, 0.5f);
                }
            } catch (IndexOutOfBoundsException e) {
                // Zabezpieczenie, jeśli obiekty zostały usunięte lub ich kolejność zmieniona
                // System.err.println("Error updating object rotation due to index issues.");
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
        // AudioManager.cleanup() zajmie się dźwiękami

        System.out.println("DemoGame: Cleaning up meshes...");
        for (Map.Entry<String, Mesh> entry : meshes.entrySet()) {
            if (entry.getValue() != null) entry.getValue().cleanup();
        }
        meshes.clear();

        System.out.println("DemoGame: Cleaning up textures...");
        for (Map.Entry<String, Texture> entry : textures.entrySet()) {
            if (entry.getValue() != null) entry.getValue().cleanup();
        }
        textures.clear();
        System.out.println("DemoGame: Resource cleanup finished.");
    }

    private Texture createDefaultTexture() {
        ByteBuffer defaultPixel = null;
        try {
            defaultPixel = MemoryUtil.memAlloc(4);
            defaultPixel.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
            return new Texture(1, 1, defaultPixel);
        } catch(Exception e) {
            System.err.println("Failed to create default texture: " + e.getMessage());
            return null;
        } finally {
            if (defaultPixel != null) {
                MemoryUtil.memFree(defaultPixel);
            }
        }
    }
}