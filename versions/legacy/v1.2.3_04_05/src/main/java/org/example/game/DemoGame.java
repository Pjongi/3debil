package org.example.game;

import org.example.audio.AudioManager;
import org.example.audio.SoundSource;
import org.example.core.Input;
import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.*; // Importuj cały pakiet graphics
import org.example.graphics.light.DirectionalLight;
import org.example.scene.GameObject;
import org.example.scene.GameObjectProperties;
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
import java.util.Random;

public class DemoGame implements IEngineLogic {

    private List<GameObject> gameObjects;
    private Map<String, Mesh> meshes;
    private Map<String, Texture> textures; // Nadal przechowujemy tekstury osobno
    private Map<String, Material> materials; // Mapa do przechowywania materiałów
    private DirectionalLight directionalLight;
    private AudioManager audioManager;
    private SoundSource backgroundMusicSource;
    private SoundSource stepSoundSource;
    private int stepSoundBuffer = -1;
    private final float stepSoundCooldown = 0.35f;
    private float timeSinceLastStep = 0f;
    private Random random = new Random();
    private Texture defaultTexture = null; // Domyślna tekstura (biała)
    private boolean fKeyPressed = false;
    private boolean bKeyPressed = false;

    @Override
    public void init(Window window, Renderer renderer, AudioManager audioManager) {
        this.audioManager = audioManager;
        gameObjects = new ArrayList<>();
        meshes = new HashMap<>();
        textures = new HashMap<>();
        materials = new HashMap<>(); // Inicjalizuj mapę materiałów

        System.out.println("DemoGame: Initializing resources...");
        long startTime = System.nanoTime();

        try {
            // Musimy mieć domyślną teksturę jako fallback dla materiałów
            defaultTexture = createDefaultTexture();
            if (defaultTexture == null) throw new ResourceLoadException("Failed to create essential default texture.");
            System.out.println("  DemoGame: Default texture created.");

            loadTexturesSafe();   // 1. Ładuj tekstury
            createMaterials();    // 2. Twórz materiały używając tekstur
            loadMeshesSafe();     // 3. Ładuj siatki
            loadSoundsSafe();     // 4. Ładuj dźwięki

            // 5. Twórz obiekty, jeśli siatki są dostępne
            if (meshes.containsKey("plane") && meshes.containsKey("cube")) {
                createGameObjects(); // Użyje materiałów i siatek
            } else {
                System.err.println("  DemoGame: Skipping GameObject creation due to missing essential meshes.");
            }

            // 6. Twórz światła
            createLights();

            // 7. Odtwórz muzykę
            if (backgroundMusicSource != null) backgroundMusicSource.play();

        } catch (Exception e) {
            System.err.println("###################################################");
            System.err.println("FATAL ERROR during DemoGame initialization:");
            e.printStackTrace();
            System.err.println("###################################################");
            cleanupPartialInit();
            throw new RuntimeException("DemoGame initialization failed", e);
        }

        long endTime = System.nanoTime();
        System.out.println("DemoGame: Initialization attempt finished (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    // --- Metody ładowania zasobów ---

    private void loadTexturesSafe() {
        System.out.println("  DemoGame: Loading textures...");
        loadTexture("stone", "textures/stone.png");
        loadTexture("wood", "textures/wood.png");
        loadTexture("grass", "textures/grass.png");
        // Opcjonalnie: loadTexture("stone_specular", "textures/stone_specular.png");
    }

    private void loadTexture(String name, String path) {
        try {
            textures.put(name, new Texture(path));
            System.out.println("    Texture loaded: " + name + " from " + path);
        } catch (ResourceNotFoundException e) {
            System.err.println("    ERROR - Texture file not found: " + path);
        } catch (ResourceLoadException e) {
            System.err.println("    ERROR - Failed to load texture '" + name + "' from " + path + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("    UNEXPECTED ERROR loading texture '" + name + "' from " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMaterials() {
        System.out.println("  DemoGame: Creating materials...");
        Material defaultMatInstance = new Material(); // Referencja do domyślnego materiału

        // Kamień
        Texture stoneTex = textures.get("stone");
        // Texture stoneSpecTex = textures.get("stone_specular"); // Jeśli masz mapę specular
        if (stoneTex != null) {
            Material stoneMat = new Material(
                    new Vector3f(0.2f), // ambient
                    new Vector3f(1.0f), // diffuse (biały, kolor z tekstury)
                    new Vector3f(0.1f), // specular (słabe odbicia)
                    8f,                // reflectance (dość matowy)
                    stoneTex,          // diffuse map
                    null               // specular map (null lub stoneSpecTex)
            );
            materials.put("stone", stoneMat);
            System.out.println("    Material created: stone");
        } else {
            System.err.println("    Cannot create stone material, texture 'stone' not found. Using default.");
            materials.put("stone", defaultMatInstance);
        }

        // Drewno
        Texture woodTex = textures.get("wood");
        if (woodTex != null) {
            Material woodMat = new Material(
                    new Vector3f(0.2f), new Vector3f(1.0f), new Vector3f(0.3f),
                    32f, woodTex, null // Trochę bardziej błyszczące
            );
            materials.put("wood", woodMat);
            System.out.println("    Material created: wood");
        } else {
            System.err.println("    Cannot create wood material, texture 'wood' not found. Using default.");
            materials.put("wood", defaultMatInstance);
        }

        // Trawa
        Texture grassTex = textures.get("grass");
        if (grassTex != null) {
            Material grassMat = new Material(
                    new Vector3f(0.2f), new Vector3f(1.0f), new Vector3f(0.05f),
                    4f, grassTex, null // Bardzo matowe
            );
            materials.put("grass", grassMat);
            System.out.println("    Material created: grass");
        } else {
            System.err.println("    Cannot create grass material, texture 'grass' not found. Using default.");
            materials.put("grass", defaultMatInstance);
        }

        // Materiał dla królika (błyszczący kamień)
        Texture diffuseTexForBunny = (stoneTex != null) ? stoneTex : defaultTexture; // Wybierz teksturę diffuse

        Material shinyStoneMat = new Material(
                new Vector3f(0.2f),              // ambient (można dostosować)
                new Vector3f(1.0f),              // diffuse (biały, kolor z tekstury)
                new Vector3f(0.5f),              // specular (błyszczący)
                64f,                             // reflectance (skupione odbicia)
                diffuseTexForBunny,              // diffuse map
                null                             // specular map - JAWNIE null
        );
        materials.put("shiny_stone", shinyStoneMat);
        System.out.println("    Material created: shiny_stone");
    }


    private void loadMeshesSafe() {
        System.out.println("  DemoGame: Loading meshes...");
        try {
            meshes.put("cube", MeshLoader.createCube());
            meshes.put("plane", MeshLoader.createPlane(20.0f, 10.0f));
            System.out.println("    Basic meshes (cube, plane) created.");
        } catch (Exception e) {
            System.err.println("    ERROR - Failed to create basic meshes: " + e.getMessage());
        }
        loadModel("bunny", "models/bunny.obj");
    }

    private void loadModel(String name, String path) {
        try {
            meshes.put(name, ModelLoader.loadMesh(path));
            System.out.println("    Model loaded: " + name + " from " + path);
        } catch (ResourceNotFoundException e) {
            System.err.println("    ERROR - Model file not found: " + path);
        } catch (ResourceLoadException e) {
            System.err.println("    ERROR - Failed to load model '" + name + "' from " + path + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("    UNEXPECTED ERROR loading model '" + name + "' from " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSoundsSafe() {
        System.out.println("  DemoGame: Loading sounds...");
        // Używaj ścieżek względnych do classpath
        try {
            int musicBufferId = audioManager.loadSound("audio/music.wav");
            backgroundMusicSource = audioManager.createSource(true, true);
            backgroundMusicSource.setBuffer(musicBufferId);
            backgroundMusicSource.setGain(0.3f);
            System.out.println("    Background music loaded.");
        } catch (Exception e) { System.err.println("    ERROR loading music: " + e.getMessage()); backgroundMusicSource = null;}

        try {
            stepSoundBuffer = audioManager.loadSound("audio/step.wav");
            stepSoundSource = audioManager.createSource(false, false);
            stepSoundSource.setBuffer(stepSoundBuffer);
            stepSoundSource.setGain(0.8f);
            System.out.println("    Step sound loaded.");
        } catch (Exception e) { System.err.println("    ERROR loading step sound: " + e.getMessage()); stepSoundSource = null; stepSoundBuffer = -1;}
    }

    private void createGameObjects() {
        System.out.println("  DemoGame: Creating game objects...");
        Mesh planeMesh = meshes.get("plane");
        Mesh cubeMesh = meshes.get("cube");
        Mesh bunnyMesh = meshes.get("bunny");
        Material defaultMatInstance = new Material(); // Domyślny materiał

        // Podłoga
        GameObjectProperties floorProps = new GameObjectProperties.Builder().typeName("Floor").material("Grass").setStatic(true).physicsEnabled(true).targetable(false).build();
        if (planeMesh != null) {
            GameObject floor = new GameObject(planeMesh, materials.getOrDefault("grass", defaultMatInstance), floorProps);
            floor.setPosition(0.0f, -0.5f, 0.0f);
            gameObjects.add(floor);
        }

        // Kostki
        GameObjectProperties stoneCubeProps = new GameObjectProperties.Builder().typeName("StoneCube").material("Stone").physicsEnabled(true).mass(5.0f).friction(0.8f).makeDestructible(150).build();
        GameObjectProperties woodCubeProps = new GameObjectProperties.Builder().typeName("WoodCube").material("Wood").physicsEnabled(true).mass(1.5f).friction(0.6f).makeDestructible(50).build();
        GameObjectProperties stoneCubeProps2 = new GameObjectProperties.Builder().typeName("SmallStoneCube").material("Stone").physicsEnabled(true).mass(2.0f).friction(0.8f).makeDestructible(80).build();

        if (cubeMesh != null) {
            GameObject cube1 = new GameObject(cubeMesh, materials.getOrDefault("stone", defaultMatInstance), stoneCubeProps);
            cube1.setPosition(-1.5f, 0.0f, -1.5f); gameObjects.add(cube1);

            GameObject cube2 = new GameObject(cubeMesh, materials.getOrDefault("wood", defaultMatInstance), woodCubeProps);
            cube2.setPosition(1.5f, 0.5f, 0.0f); cube2.setScale(0.75f); gameObjects.add(cube2);

            GameObject cube3 = new GameObject(cubeMesh, materials.getOrDefault("stone", defaultMatInstance), stoneCubeProps2);
            cube3.setPosition(0.0f, 0.2f, 1.8f); cube3.setScale(0.5f); gameObjects.add(cube3);
        }

        // Królik
        GameObjectProperties bunnyProps = new GameObjectProperties.Builder().typeName("BunnyStatue").material("Stone").setStatic(true).physicsEnabled(true).makeDestructible(300).targetable(true).build();
        if (bunnyMesh != null) {
            GameObject bunny = new GameObject(bunnyMesh, materials.getOrDefault("shiny_stone", defaultMatInstance), bunnyProps);
            bunny.setPosition(0.0f, -0.45f, 0.5f); bunny.setScale(0.7f); bunny.setRotation((float)Math.toRadians(180), 0, 1, 0);
            gameObjects.add(bunny);
            System.out.println("    Bunny GameObject created.");
        }

        System.out.println("  DemoGame: Game objects creation finished (" + gameObjects.size() + " objects).");
    }

    private void createLights() {
        System.out.println("  DemoGame: Creating lights...");
        directionalLight = new DirectionalLight(
                new Vector3f(1.0f, 1.0f, 0.9f), new Vector3f(0.6f, -0.8f, -0.4f), 0.9f
        );
        System.out.println("  DemoGame: Directional light created.");
    }

    // --- Pętla gry ---

    @Override
    public void input(Window window, Input input, Camera camera, float deltaTime) {
        // Ruch kamery
        camera.processKeyboard(deltaTime);
        Vector2f mouseDelta = Input.getMouseDelta();
        if (Math.abs(mouseDelta.x) < 100 && Math.abs(mouseDelta.y) < 100) {
            camera.processMouseMovement(mouseDelta.x, -mouseDelta.y, true);
        }

        // Dźwięk kroków
        boolean isMoving = Input.isKeyDown(GLFW.GLFW_KEY_W) || Input.isKeyDown(GLFW.GLFW_KEY_A) ||
                Input.isKeyDown(GLFW.GLFW_KEY_S) || Input.isKeyDown(GLFW.GLFW_KEY_D);
        timeSinceLastStep += deltaTime;
        if (isMoving && stepSoundSource != null && stepSoundBuffer != -1 && !stepSoundSource.isPlaying() && timeSinceLastStep >= stepSoundCooldown) {
            stepSoundSource.setPosition(camera.getPosition());
            stepSoundSource.play();
            timeSinceLastStep = 0f;
        }

        // Interakcja
        handleInteractionInput();
    }

    private void handleInteractionInput() {
        // Atak klawiszem F
        boolean fCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_F);
        if (fCurrentlyPressed && !fKeyPressed) {
            fKeyPressed = true;
            boolean attacked = false;
            for (GameObject go : gameObjects) {
                if (go.getProperties().getTypeName().contains("Cube") && go.isVisible() && go.getProperties().isDestructible() && go.getProperties().isAlive()) {
                    System.out.println("Attacking " + go.getProperties().getTypeName() + "!");
                    if (go.takeDamage(25)) { // takeDamage zwraca true jeśli zniszczony
                        go.setVisible(false);
                    }
                    System.out.println("  " + go.getProperties().getTypeName() + " HP: " + go.getProperties().getCurrentHitPoints() + "/" + go.getProperties().getMaxHitPoints());
                    attacked = true;
                    break;
                }
            }
            if (!attacked) System.out.println("No destructible cube found!");
        } else if (!fCurrentlyPressed) {
            fKeyPressed = false;
        }

        // Przełączanie widoczności królika klawiszem B
        boolean bCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_B);
        if (bCurrentlyPressed && !bKeyPressed) {
            bKeyPressed = true;
            boolean toggled = false;
            for (GameObject go : gameObjects) {
                if ("BunnyStatue".equals(go.getProperties().getTypeName())) {
                    go.setVisible(!go.isVisible());
                    System.out.println("Bunny visibility toggled to: " + go.isVisible());
                    toggled = true;
                    break;
                }
            }
            if (!toggled) System.out.println("Bunny not found!");
        } else if (!bCurrentlyPressed) {
            bKeyPressed = false;
        }
    }


    @Override
    public void update(float deltaTime) {
        // Obracanie kostek
        Mesh cubeMesh = meshes.get("cube");
        if (cubeMesh == null) return;
        for (GameObject go : gameObjects) {
            if (go.getMesh() == cubeMesh && go.isVisible()) {
                Vector3f pos = go.getPosition();
                if (Math.abs(pos.x - (-1.5f)) < 0.1f) go.rotate(deltaTime * 0.3f, 0, 1, 0);
                else if (Math.abs(pos.x - 1.5f) < 0.1f) go.rotate(deltaTime * 0.5f, 1, 0, 0);
                else if (Math.abs(pos.z - 1.8f) < 0.1f) go.rotate(deltaTime * 0.8f, random.nextFloat(), random.nextFloat(), random.nextFloat());
            }
        }
        // Można dodać logikę usuwania: gameObjects.removeIf(go -> !go.isVisible() && !go.getProperties().isAlive());
    }

    @Override
    public void render(Window window, Camera camera, Renderer renderer) {
        if (renderer != null && renderer.isReady() && directionalLight != null) {
            // Renderer oczekuje teraz listy obiektów, które są już potencjalnie widoczne
            // Filtrowanie można zrobić tutaj lub pozostawić rendererowi
            List<GameObject> visibleObjects = new ArrayList<>();
            for(GameObject go : gameObjects) {
                if (go.getProperties().isVisible()) {
                    visibleObjects.add(go);
                }
            }
            renderer.render(camera, visibleObjects, directionalLight);
        } else {
            if (renderer == null || !renderer.isReady()) System.err.println("DemoGame.render(): Skipping render - Renderer is not ready.");
            if (directionalLight == null) System.err.println("DemoGame.render(): Skipping render - DirectionalLight is null.");
        }
    }

    // --- Sprzątanie ---

    @Override
    public void cleanup() {
        System.out.println("DemoGame: Cleaning up resources...");
        long startTime = System.nanoTime();

        System.out.println("  DemoGame: Cleaning up meshes...");
        meshes.values().stream().filter(m -> m != null).forEach(Mesh::cleanup);
        meshes.clear();

        System.out.println("  DemoGame: Clearing materials map...");
        materials.clear(); // Materiały same nie wymagają cleanup

        System.out.println("  DemoGame: Cleaning up textures...");
        textures.values().stream().filter(t -> t != null).forEach(Texture::cleanup);
        textures.clear();

        if (defaultTexture != null) {
            defaultTexture.cleanup(); defaultTexture = null;
            System.out.println("  DemoGame: Default texture cleaned up.");
        }

        gameObjects.clear();
        System.out.println("  DemoGame: GameObject list cleared.");

        long endTime = System.nanoTime();
        System.out.println("DemoGame: Resource cleanup finished (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void cleanupPartialInit() {
        System.out.println("DemoGame: Cleaning up partially initialized resources...");
        meshes.values().stream().filter(m -> m != null).forEach(Mesh::cleanup); meshes.clear();
        materials.clear();
        textures.values().stream().filter(t -> t != null).forEach(Texture::cleanup); textures.clear();
        if (defaultTexture != null) defaultTexture.cleanup(); defaultTexture = null;
        gameObjects.clear();
        System.out.println("DemoGame: Partial cleanup finished.");
    }

    private Texture createDefaultTexture() throws ResourceLoadException {
        ByteBuffer pixel = MemoryUtil.memAlloc(4);
        Texture tex = null;
        try {
            pixel.put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).flip();
            tex = new Texture(1, 1, pixel, false);
            return tex;
        } catch(Exception e) {
            if(tex != null) tex.cleanup();
            throw new ResourceLoadException("Failed to create default texture", e);
        } finally {
            MemoryUtil.memFree(pixel);
        }
    }
}