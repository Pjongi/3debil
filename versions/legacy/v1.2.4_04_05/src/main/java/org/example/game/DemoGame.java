package org.example.game;

import org.example.audio.AudioManager;
import org.example.audio.SoundSource;
import org.example.core.Input;
import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.*; // Importuj cały pakiet graphics
import org.example.graphics.light.Attenuation; // Dodano import
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.light.PointLight;   // Dodano import
import org.example.graphics.light.SpotLight;    // Dodano import
import org.example.scene.GameObject;
import org.example.scene.GameObjectProperties;
import org.example.util.MeshLoader;
import org.example.util.ModelLoader;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW; // Potrzebne dla GLFW.GLFW_KEY_T
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
    private Map<String, Texture> textures;
    private Map<String, Material> materials;
    private DirectionalLight directionalLight; // Główne światło kierunkowe
    private List<PointLight> pointLights;      // Lista świateł punktowych
    private List<SpotLight> spotLights;        // Lista świateł reflektorowych
    private AudioManager audioManager;
    private SoundSource backgroundMusicSource;
    private SoundSource stepSoundSource;
    private int stepSoundBuffer = -1;
    private final float stepSoundCooldown = 0.35f;
    private float timeSinceLastStep = 0f;
    private Random random = new Random();
    private Texture defaultTexture = null;

    // Flagi debounce dla interakcji
    private boolean fKeyPressed = false;
    private boolean bKeyPressed = false;
    private boolean tKeyPressed = false; // Flaga debounce dla klawisza T

    // Stan latarki
    private boolean isFlashlightOn = true; // Stan latarki
    private float originalFlashlightIntensity = 1.5f; // Przechowaj oryginalną intensywność

    @Override
    public void init(Window window, Renderer renderer, AudioManager audioManager) {
        this.audioManager = audioManager;
        gameObjects = new ArrayList<>();
        meshes = new HashMap<>();
        textures = new HashMap<>();
        materials = new HashMap<>();
        pointLights = new ArrayList<>(); // Inicjalizuj listy świateł
        spotLights = new ArrayList<>();

        System.out.println("DemoGame: Initializing resources...");
        long startTime = System.nanoTime();

        try {
            defaultTexture = createDefaultTexture();
            if (defaultTexture == null) throw new ResourceLoadException("Failed default texture creation.");
            System.out.println("  DemoGame: Default texture created.");

            loadTexturesSafe();
            createMaterials();
            loadMeshesSafe();
            loadSoundsSafe();

            if (meshes.containsKey("plane") && meshes.containsKey("cube")) {
                createGameObjects();
            } else {
                System.err.println("  DemoGame: Skipping GameObject creation due to missing essential meshes.");
            }

            createLights(); // Tworzy wszystkie typy świateł

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

    // --- Metody ładowania zasobów i tworzenia ---

    private void loadTexturesSafe() {
        System.out.println("  DemoGame: Loading textures...");
        loadTexture("stone", "textures/stone.png");
        loadTexture("wood", "textures/wood.png");
        loadTexture("grass", "textures/grass.png");
    }

    private void loadTexture(String name, String path) {
        try {
            textures.put(name, new Texture(path));
            System.out.println("    Texture loaded: " + name + " from " + path);
        } catch (ResourceNotFoundException e) { System.err.println("    ERROR - Texture not found: " + path);
        } catch (Exception e) { System.err.println("    ERROR loading texture '" + name + "': " + e.getMessage()); }
    }

    private void createMaterials() {
        System.out.println("  DemoGame: Creating materials...");
        Material defaultMatInstance = new Material();

        Texture stoneTex = textures.get("stone");
        Material stoneMat = new Material(stoneTex != null ? stoneTex : defaultTexture);
        stoneMat.setSpecularColor(new Vector3f(0.1f)); stoneMat.setReflectance(8f);
        materials.put("stone", stoneMat);
        System.out.println("    Material created: stone (uses default texture: " + (stoneTex == null) + ")");

        Texture woodTex = textures.get("wood");
        Material woodMat = new Material(
                new Vector3f(0.2f), new Vector3f(1.0f), new Vector3f(0.3f), 32f,
                woodTex != null ? woodTex : defaultTexture, null
        );
        materials.put("wood", woodMat);
        System.out.println("    Material created: wood (uses default texture: " + (woodTex == null) + ")");

        Texture grassTex = textures.get("grass");
        Material grassMat = new Material(grassTex != null ? grassTex : defaultTexture);
        grassMat.setSpecularColor(new Vector3f(0.05f)); grassMat.setReflectance(4f);
        materials.put("grass", grassMat);
        System.out.println("    Material created: grass (uses default texture: " + (grassTex == null) + ")");

        Texture diffuseTexForBunny = (stoneTex != null) ? stoneTex : defaultTexture;
        Material shinyStoneMat = new Material(
                new Vector3f(0.2f), new Vector3f(1.0f), new Vector3f(0.5f), 64f,
                diffuseTexForBunny, null
        );
        materials.put("shiny_stone", shinyStoneMat);
        System.out.println("    Material created: shiny_stone");
    }

    private void loadMeshesSafe() {
        System.out.println("  DemoGame: Loading meshes...");
        try {
            meshes.put("cube", MeshLoader.createCube());
            meshes.put("plane", MeshLoader.createPlane(20.0f, 10.0f));
            System.out.println("    Basic meshes created.");
        } catch (Exception e) { System.err.println("    ERROR creating basic meshes: " + e.getMessage()); }
        loadModel("bunny", "models/bunny.obj");
    }

    private void loadModel(String name, String path) {
        try {
            meshes.put(name, ModelLoader.loadMesh(path));
            System.out.println("    Model loaded: " + name + " from " + path);
        } catch (ResourceNotFoundException e) { System.err.println("    ERROR - Model not found: " + path);
        } catch (Exception e) { System.err.println("    ERROR loading model '" + name + "': " + e.getMessage());}
    }

    private void loadSoundsSafe() {
        System.out.println("  DemoGame: Loading sounds...");
        try {
            int musicBuf = audioManager.loadSound("audio/music.wav");
            backgroundMusicSource = audioManager.createSource(true, true);
            backgroundMusicSource.setBuffer(musicBuf); backgroundMusicSource.setGain(0.3f);
            System.out.println("    Background music loaded.");
        } catch (Exception e) { System.err.println("    ERROR loading music: " + e.getMessage()); backgroundMusicSource = null;}
        try {
            stepSoundBuffer = audioManager.loadSound("audio/step.wav");
            stepSoundSource = audioManager.createSource(false, false);
            stepSoundSource.setBuffer(stepSoundBuffer); stepSoundSource.setGain(0.8f);
            System.out.println("    Step sound loaded.");
        } catch (Exception e) { System.err.println("    ERROR loading step sound: " + e.getMessage()); stepSoundSource = null; stepSoundBuffer = -1;}
    }

    private void createGameObjects() {
        System.out.println("  DemoGame: Creating game objects...");
        Mesh planeMesh = meshes.get("plane"); Mesh cubeMesh = meshes.get("cube"); Mesh bunnyMesh = meshes.get("bunny");
        Material defaultMatInstance = new Material();

        GameObjectProperties floorProps = new GameObjectProperties.Builder().typeName("Floor").material("Grass").setStatic(true).physicsEnabled(true).targetable(false).build();
        if (planeMesh != null) { gameObjects.add(new GameObject(planeMesh, materials.getOrDefault("grass", defaultMatInstance), floorProps)); gameObjects.get(gameObjects.size()-1).setPosition(0.0f, -0.5f, 0.0f);}

        GameObjectProperties stoneCubeProps = new GameObjectProperties.Builder().typeName("StoneCube").material("Stone").physicsEnabled(true).mass(5.0f).friction(0.8f).makeDestructible(150).build();
        GameObjectProperties woodCubeProps = new GameObjectProperties.Builder().typeName("WoodCube").material("Wood").physicsEnabled(true).mass(1.5f).friction(0.6f).makeDestructible(50).build();
        GameObjectProperties stoneCubeProps2 = new GameObjectProperties.Builder().typeName("SmallStoneCube").material("Stone").physicsEnabled(true).mass(2.0f).friction(0.8f).makeDestructible(80).build();
        if (cubeMesh != null) {
            gameObjects.add(new GameObject(cubeMesh, materials.getOrDefault("stone", defaultMatInstance), stoneCubeProps)); gameObjects.get(gameObjects.size()-1).setPosition(-1.5f, 0.0f, -1.5f);
            gameObjects.add(new GameObject(cubeMesh, materials.getOrDefault("wood", defaultMatInstance), woodCubeProps)); gameObjects.get(gameObjects.size()-1).setPosition(1.5f, 0.5f, 0.0f); gameObjects.get(gameObjects.size()-1).setScale(0.75f);
            gameObjects.add(new GameObject(cubeMesh, materials.getOrDefault("stone", defaultMatInstance), stoneCubeProps2)); gameObjects.get(gameObjects.size()-1).setPosition(0.0f, 0.2f, 1.8f); gameObjects.get(gameObjects.size()-1).setScale(0.5f);
        }

        GameObjectProperties bunnyProps = new GameObjectProperties.Builder().typeName("BunnyStatue").material("Stone").setStatic(true).physicsEnabled(true).makeDestructible(300).targetable(true).build();
        if (bunnyMesh != null) {
            gameObjects.add(new GameObject(bunnyMesh, materials.getOrDefault("shiny_stone", defaultMatInstance), bunnyProps));
            GameObject bunny = gameObjects.get(gameObjects.size()-1); bunny.setPosition(0.0f, -0.45f, 0.5f); bunny.setScale(0.7f); bunny.setRotation((float)Math.toRadians(180), 0, 1, 0);
            System.out.println("    Bunny GameObject created.");
        }
        System.out.println("  DemoGame: Game objects creation finished (" + gameObjects.size() + " objects).");
    }

    private void createLights() {
        System.out.println("  DemoGame: Creating lights...");
        directionalLight = new DirectionalLight(new Vector3f(0.7f, 0.7f, 0.6f), new Vector3f(0.6f, -0.8f, -0.4f), 0.6f);
        System.out.println("    Directional light created.");

        pointLights.add(new PointLight(new Vector3f(-1.5f, 1.5f, -1.5f), new Vector3f(1.0f, 0.1f, 0.1f), 1.0f, 15.0f));
        pointLights.add(new PointLight(new Vector3f(1.0f, -0.2f, 1.0f), new Vector3f(0.1f, 0.1f, 1.0f), 0.9f, Attenuation.forRange(12f)));
        pointLights.add(new PointLight(new Vector3f(0.0f, 0.5f, 0.0f), new Vector3f(0.1f, 1.0f, 0.1f), 0.8f, Attenuation.forRange(8f)));
        System.out.println("    Created " + pointLights.size() + " point lights.");

        Vector3f flashlightColor = new Vector3f(0.95f, 0.95f, 0.8f);
        originalFlashlightIntensity = 1.5f; // Zapisz oryginalną intensywność
        SpotLight flashlight = new SpotLight(
                new PointLight(new Vector3f(0f, 3f, 3f), flashlightColor, originalFlashlightIntensity, 30f),
                new Vector3f(0, -1, 0), 12.5f, 17.5f
        );
        spotLights.add(flashlight);
        isFlashlightOn = true; // Domyślnie włączona
        System.out.println("    Created " + spotLights.size() + " spot lights.");
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
        boolean isMoving = Input.isKeyDown(GLFW.GLFW_KEY_W) || Input.isKeyDown(GLFW.GLFW_KEY_A) || Input.isKeyDown(GLFW.GLFW_KEY_S) || Input.isKeyDown(GLFW.GLFW_KEY_D);
        timeSinceLastStep += deltaTime;
        if (isMoving && stepSoundSource != null && stepSoundBuffer != -1 && !stepSoundSource.isPlaying() && timeSinceLastStep >= stepSoundCooldown) {
            stepSoundSource.setPosition(camera.getPosition()); stepSoundSource.play(); timeSinceLastStep = 0f;
        }

        // Interakcja i latarka
        handleInteractionInput(camera); // Przekaż kamerę

        // Aktualizacja latarki musi być po obsłudze inputu kamery
        updateFlashlight(camera);
    }

    private void handleInteractionInput(Camera camera) {
        // Atak klawiszem F
        boolean fCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_F);
        if (fCurrentlyPressed && !fKeyPressed) {
            fKeyPressed = true; boolean attacked = false;
            for (GameObject go : gameObjects) {
                if (go.getProperties().getTypeName().contains("Cube") && go.isVisible() && go.getProperties().isDestructible() && go.getProperties().isAlive()) {
                    System.out.println("Attacking " + go.getProperties().getTypeName() + "!");
                    if (go.takeDamage(25)) go.setVisible(false);
                    System.out.println("  " + go.getProperties().getTypeName() + " HP: " + go.getProperties().getCurrentHitPoints() + "/" + go.getProperties().getMaxHitPoints());
                    attacked = true; break;
                }
            }
            if (!attacked) System.out.println("No destructible cube found!");
        } else if (!fCurrentlyPressed) fKeyPressed = false;

        // Przełączanie widoczności królika klawiszem B
        boolean bCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_B);
        if (bCurrentlyPressed && !bKeyPressed) {
            bKeyPressed = true; boolean toggled = false;
            for (GameObject go : gameObjects) {
                if ("BunnyStatue".equals(go.getProperties().getTypeName())) {
                    go.setVisible(!go.isVisible()); System.out.println("Bunny visibility toggled to: " + go.isVisible());
                    toggled = true; break;
                }
            }
            if (!toggled) System.out.println("Bunny not found!");
        } else if (!bCurrentlyPressed) bKeyPressed = false;

        // Przełączanie latarki klawiszem T
        boolean tCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_T);
        if (tCurrentlyPressed && !tKeyPressed) {
            tKeyPressed = true;
            if (!spotLights.isEmpty()) {
                SpotLight playerFlashlight = spotLights.get(0);
                isFlashlightOn = !isFlashlightOn;
                playerFlashlight.pointLight.intensity = isFlashlightOn ? originalFlashlightIntensity : 0.0f;
                System.out.println("Flashlight toggled: " + (isFlashlightOn ? "ON" : "OFF"));
            }
        } else if (!tCurrentlyPressed) tKeyPressed = false;
    }

    /** Aktualizuje pozycję i kierunek latarki gracza */
    private void updateFlashlight(Camera camera) {
        if (!spotLights.isEmpty() && camera != null) { // Sprawdź też kamerę
            SpotLight playerFlashlight = spotLights.get(0);
            playerFlashlight.pointLight.position.set(camera.getPosition());
            playerFlashlight.direction.set(camera.getFront());
        }
    }


    @Override
    public void update(float deltaTime) {
        // Obracanie kostek
        Mesh cubeMesh = meshes.get("cube"); if (cubeMesh == null) return;
        for (GameObject go : gameObjects) {
            if (go.getMesh() == cubeMesh && go.isVisible()) {
                Vector3f pos = go.getPosition();
                if (Math.abs(pos.x - (-1.5f)) < 0.1f) go.rotate(deltaTime * 0.3f, 0, 1, 0);
                else if (Math.abs(pos.x - 1.5f) < 0.1f) go.rotate(deltaTime * 0.5f, 1, 0, 0);
                else if (Math.abs(pos.z - 1.8f) < 0.1f) go.rotate(deltaTime * 0.8f, random.nextFloat(), random.nextFloat(), random.nextFloat());
            }
        }
    }

    @Override
    public void render(Window window, Camera camera, Renderer renderer) {
        if (renderer != null && renderer.isReady()) {
            List<GameObject> visibleObjects = new ArrayList<>();
            for(GameObject go : gameObjects) { if (go.getProperties().isVisible()) visibleObjects.add(go); }
            // Przekaż listy świateł do renderera
            renderer.render(camera, visibleObjects, directionalLight, pointLights, spotLights);
        } else {
            if (renderer == null || !renderer.isReady()) System.err.println("DemoGame.render(): Renderer not ready.");
        }
    }

    // --- Sprzątanie ---
    @Override
    public void cleanup() {
        System.out.println("DemoGame: Cleaning up resources...");
        long startTime = System.nanoTime();
        meshes.values().stream().filter(m -> m != null).forEach(Mesh::cleanup); meshes.clear(); System.out.println("  Meshes cleaned.");
        materials.clear(); System.out.println("  Materials map cleared.");
        textures.values().stream().filter(t -> t != null).forEach(Texture::cleanup); textures.clear(); System.out.println("  Textures cleaned.");
        if (defaultTexture != null) { defaultTexture.cleanup(); defaultTexture = null; System.out.println("  Default texture cleaned."); }
        gameObjects.clear(); System.out.println("  GameObject list cleared.");
        pointLights.clear(); spotLights.clear(); System.out.println("  Light lists cleared."); // Dodano
        long endTime = System.nanoTime();
        System.out.println("DemoGame: Cleanup finished (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void cleanupPartialInit() {
        System.out.println("DemoGame: Cleaning up partially initialized resources...");
        meshes.values().stream().filter(m -> m != null).forEach(Mesh::cleanup); meshes.clear();
        materials.clear();
        textures.values().stream().filter(t -> t != null).forEach(Texture::cleanup); textures.clear();
        if (defaultTexture != null) defaultTexture.cleanup(); defaultTexture = null;
        gameObjects.clear();
        pointLights.clear(); spotLights.clear(); // Dodano
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