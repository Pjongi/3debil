package org.example.game;

import org.example.audio.AudioManager;
import org.example.audio.SoundSource;
import org.example.core.Input;
import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.Camera;
import org.example.graphics.Mesh;
import org.example.graphics.Renderer;
import org.example.graphics.Texture;
import org.example.graphics.light.DirectionalLight;
import org.example.scene.GameObject;
import org.example.scene.GameObjectProperties; // Import nowej klasy
import org.example.util.MeshLoader;
import org.example.util.ModelLoader;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW; // Import dla kodów klawiszy
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random; // Do losowego obracania

public class DemoGame implements IEngineLogic {

    private List<GameObject> gameObjects;
    private Map<String, Mesh> meshes;
    private Map<String, Texture> textures;
    private DirectionalLight directionalLight;
    private AudioManager audioManager; // Przechowaj referencję
    private SoundSource backgroundMusicSource;
    private SoundSource stepSoundSource;
    private int stepSoundBuffer = -1; // ID bufora dźwięku kroku
    private final float stepSoundCooldown = 0.35f; // Czas (w sekundach) między krokami
    private float timeSinceLastStep = 0f;
    private Random random = new Random(); // Generator liczb losowych

    // Domyślna tekstura tworzona raz
    private Texture defaultTexture = null;

    // Flagi do obsługi pojedynczego naciśnięcia klawisza (debounce)
    private boolean fKeyPressed = false;
    private boolean bKeyPressed = false;

    @Override
    public void init(Window window, Renderer renderer, AudioManager audioManager) {
        this.audioManager = audioManager; // Zapisz referencję
        gameObjects = new ArrayList<>();
        meshes = new HashMap<>();
        textures = new HashMap<>();

        System.out.println("DemoGame: Initializing resources...");
        long startTime = System.nanoTime();

        try {
            // 1. Utwórz domyślną teksturę (krytyczne, zrób na początku)
            defaultTexture = createDefaultTexture();
            if (defaultTexture == null) {
                throw new ResourceLoadException("Failed to create essential default texture.");
            }
            System.out.println("  DemoGame: Default texture created.");

            // 2. Ładuj inne zasoby z obsługą błędów
            loadTexturesSafe();
            loadMeshesSafe();
            loadSoundsSafe();

            // 3. Twórz obiekty tylko jeśli podstawowe zasoby się załadowały
            // (Plane i Cube są ważne dla dema)
            if (meshes.containsKey("plane") && meshes.containsKey("cube")) {
                createGameObjects();
            } else {
                System.err.println("  DemoGame: Skipping GameObject creation due to missing essential meshes (plane or cube).");
            }

            // 4. Twórz światła
            createLights();

            // 5. Odtwórz muzykę w tle, jeśli załadowana
            if (backgroundMusicSource != null) {
                backgroundMusicSource.play();
                System.out.println("  DemoGame: Playing background music.");
            }

        } catch (Exception e) { // Złap wszystkie błędy inicjalizacji DemoGame
            System.err.println("###################################################");
            System.err.println("FATAL ERROR during DemoGame initialization:");
            e.printStackTrace();
            System.err.println("###################################################");
            // Można by tu ustawić flagę błędu, aby zapobiec dalszemu działaniu gry,
            // ale Engine.run() już łapie wyjątki z init i przerywa.
            // Wyczyść to, co mogło zostać utworzone w DemoGame przed błędem
            cleanupPartialInit();
            // Rzuć wyjątek dalej, aby Engine mógł go obsłużyć
            // Użyj RuntimeException, jeśli init nie deklaruje throws Exception
            throw new RuntimeException("DemoGame initialization failed", e);
        }

        long endTime = System.nanoTime();
        System.out.println("DemoGame: Initialization attempt finished (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    // --- Prywatne metody ładowania zasobów z obsługą błędów ---

    private void loadTexturesSafe() {
        System.out.println("  DemoGame: Loading textures...");
        // Używaj ścieżek względnych do classpath (bez src/main/resources/)
        loadTexture("stone", "textures/stone.png");
        loadTexture("wood", "textures/wood.png");
        loadTexture("grass", "textures/grass.png");
    }

    private void loadTexture(String name, String path) {
        try {
            textures.put(name, new Texture(path));
            System.out.println("    Texture loaded: " + name + " from " + path);
        } catch (ResourceNotFoundException e) {
            System.err.println("    ERROR - Texture file not found: " + path);
        } catch (ResourceLoadException e) {
            System.err.println("    ERROR - Failed to load texture '" + name + "' from " + path + ": " + e.getMessage());
            // Można dodać e.printStackTrace() dla szczegółów błędu
        } catch (Exception e) {
            System.err.println("    UNEXPECTED ERROR loading texture '" + name + "' from " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMeshesSafe() {
        System.out.println("  DemoGame: Loading meshes...");
        try {
            meshes.put("cube", MeshLoader.createCube());
            meshes.put("plane", MeshLoader.createPlane(20.0f, 10.0f)); // Większa płaszczyzna, powtarzanie UV x10
            System.out.println("    Basic meshes (cube, plane) created.");
        } catch (Exception e) {
            System.err.println("    ERROR - Failed to create basic meshes (cube or plane): " + e.getMessage());
            // Te siatki są często kluczowe, można rozważyć przerwanie
        }
        // Używaj ścieżek względnych do classpath
        loadModel("bunny", "models/bunny.obj");
        // Dodaj więcej modeli w razie potrzeby
    }

    private void loadModel(String name, String path) {
        try {
            meshes.put(name, ModelLoader.loadMesh(path));
            System.out.println("    Model loaded: " + name + " from " + path);
        } catch (ResourceNotFoundException e) {
            System.err.println("    ERROR - Model file not found: " + path);
        } catch (ResourceLoadException e) {
            System.err.println("    ERROR - Failed to load model '" + name + "' from " + path + ": " + e.getMessage());
            // e.printStackTrace();
        } catch (Exception e) {
            System.err.println("    UNEXPECTED ERROR loading model '" + name + "' from " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void loadSoundsSafe() {
        System.out.println("  DemoGame: Loading sounds...");
        int musicBufferId = -1;
        try {
            // Używaj ścieżek względnych do classpath
            musicBufferId = audioManager.loadSound("audio/music.wav");
            backgroundMusicSource = audioManager.createSource(true, true); // loop=true, relative=true
            backgroundMusicSource.setBuffer(musicBufferId);
            backgroundMusicSource.setGain(0.3f); // Nieco ciszej
            System.out.println("    Background music loaded and source created.");
        } catch (ResourceNotFoundException e) {
            System.err.println("    ERROR - Music file not found: audio/music.wav"); // Poprawiona ścieżka w logu
            backgroundMusicSource = null;
        } catch (ResourceLoadException | RuntimeException e) { // Złap też błędy OpenAL
            System.err.println("    ERROR - Failed to load/process music: " + e.getMessage());
            if (backgroundMusicSource != null) { // Jeśli źródło zostało stworzone, ale bufor zawiódł
                backgroundMusicSource = null;
            }
        }

        stepSoundBuffer = -1; // Zresetuj ID
        try {
            // Używaj ścieżek względnych do classpath
            stepSoundBuffer = audioManager.loadSound("audio/step.wav");
            stepSoundSource = audioManager.createSource(false, false); // loop=false, relative=false (pozycyjny)
            stepSoundSource.setBuffer(stepSoundBuffer);
            stepSoundSource.setPosition(new Vector3f(0, 0, 0)); // Pozycja będzie aktualizowana
            stepSoundSource.setGain(0.8f);
            System.out.println("    Step sound loaded and source created.");
        } catch (ResourceNotFoundException e) {
            System.err.println("    ERROR - Step sound file not found: audio/step.wav"); // Poprawiona ścieżka w logu
            stepSoundSource = null;
        } catch (ResourceLoadException | RuntimeException e) {
            System.err.println("    ERROR - Failed to load/process step sound: " + e.getMessage());
            if (stepSoundSource != null) {
                stepSoundSource = null;
            }
            stepSoundBuffer = -1; // Upewnij się, że ID bufora jest nieprawidłowe
        }
    }

    // --- Tworzenie obiektów i świateł ---

    private void createGameObjects() {
        System.out.println("  DemoGame: Creating game objects...");
        Mesh planeMesh = meshes.get("plane");
        Mesh cubeMesh = meshes.get("cube");
        Mesh bunnyMesh = meshes.get("bunny");

        // Właściwości dla Podłogi
        GameObjectProperties floorProps = new GameObjectProperties.Builder()
                .typeName("Floor")
                .material("Grass")
                .setStatic(true)
                .physicsEnabled(true) // Kolizje statyczne
                .targetable(false)
                .build();

        if (planeMesh != null) {
            GameObject floor = new GameObject(planeMesh, textures.getOrDefault("grass", defaultTexture), floorProps);
            floor.setPosition(0.0f, -0.5f, 0.0f);
            gameObjects.add(floor);
        } else { System.err.println("    Cannot create floor, plane mesh is missing."); }

        // Właściwości dla Kamiennej Kostki
        GameObjectProperties stoneCubeProps = new GameObjectProperties.Builder()
                .typeName("StoneCube")
                .material("Stone")
                .physicsEnabled(true)
                .mass(5.0f)
                .friction(0.8f)
                .makeDestructible(150)
                .build();

        // Właściwości dla Drewnianej Kostki
        GameObjectProperties woodCubeProps = new GameObjectProperties.Builder()
                .typeName("WoodCube")
                .material("Wood")
                .physicsEnabled(true)
                .mass(1.5f)
                .friction(0.6f)
                .makeDestructible(50)
                .build();

        if (cubeMesh != null) {
            GameObject cube1 = new GameObject(cubeMesh, textures.getOrDefault("stone", defaultTexture), stoneCubeProps);
            cube1.setPosition(-1.5f, 0.0f, -1.5f);
            gameObjects.add(cube1);

            GameObject cube2 = new GameObject(cubeMesh, textures.getOrDefault("wood", defaultTexture), woodCubeProps);
            cube2.setPosition(1.5f, 0.5f, 0.0f);
            cube2.setScale(0.75f);
            gameObjects.add(cube2);

            GameObjectProperties stoneCubeProps2 = new GameObjectProperties.Builder()
                    .typeName("SmallStoneCube")
                    .material("Stone")
                    .physicsEnabled(true)
                    .mass(2.0f)
                    .friction(0.8f)
                    .makeDestructible(80)
                    .build();
            GameObject cube3 = new GameObject(cubeMesh, textures.getOrDefault("stone", defaultTexture), stoneCubeProps2);
            cube3.setPosition(0.0f, 0.2f, 1.8f);
            cube3.setScale(0.5f);
            gameObjects.add(cube3);
        } else { System.err.println("    Cannot create cubes, cube mesh is missing."); }

        if (bunnyMesh != null) {
            GameObjectProperties bunnyProps = new GameObjectProperties.Builder()
                    .typeName("BunnyStatue")
                    .material("Stone")
                    .setStatic(true)
                    .physicsEnabled(true) // Kolizje statyczne
                    .makeDestructible(300)
                    .targetable(true)
                    .build();

            GameObject bunny = new GameObject(bunnyMesh, textures.getOrDefault("stone", defaultTexture), bunnyProps);
            bunny.setPosition(0.0f, -0.45f, 0.5f);
            bunny.setScale(0.7f);
            bunny.setRotation((float)Math.toRadians(180), 0, 1, 0);
            gameObjects.add(bunny);
            System.out.println("    Bunny GameObject created.");
        } else { System.out.println("    Bunny mesh not loaded, skipping bunny object creation."); }

        System.out.println("  DemoGame: Game objects creation finished (" + gameObjects.size() + " objects).");
    }

    private void createLights() {
        System.out.println("  DemoGame: Creating lights...");
        directionalLight = new DirectionalLight(
                new Vector3f(1.0f, 1.0f, 0.9f),   // Lekko żółtawy kolor
                new Vector3f(0.6f, -0.8f, -0.4f), // Kierunek
                0.9f                            // Intensywność
        );
        System.out.println("  DemoGame: Directional light created.");
    }

    // --- Pętla gry: Input, Update, Render ---

    @Override
    public void input(Window window, Input input, Camera camera, float deltaTime) {
        // Ruch kamery i rozglądanie
        camera.processKeyboard(deltaTime);
        Vector2f mouseDelta = Input.getMouseDelta();
        if (Math.abs(mouseDelta.x) < 100 && Math.abs(mouseDelta.y) < 100) {
            camera.processMouseMovement(mouseDelta.x, -mouseDelta.y, true);
        }

        // Dźwięk kroków
        boolean isMoving = Input.isKeyDown(GLFW.GLFW_KEY_W) || Input.isKeyDown(GLFW.GLFW_KEY_A) ||
                Input.isKeyDown(GLFW.GLFW_KEY_S) || Input.isKeyDown(GLFW.GLFW_KEY_D);
        timeSinceLastStep += deltaTime;
        if (isMoving && stepSoundSource != null && stepSoundBuffer != -1) {
            if (!stepSoundSource.isPlaying() && timeSinceLastStep >= stepSoundCooldown) {
                stepSoundSource.setPosition(camera.getPosition());
                stepSoundSource.play();
                timeSinceLastStep = 0f;
            }
        }

        // Interakcja z obiektami (z prostym debouncem)
        handleInteractionInput();
    }

    private void handleInteractionInput() {
        // Atakowanie kostek klawiszem F (pojedyncze naciśnięcie)
        boolean fCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_F);
        if (fCurrentlyPressed && !fKeyPressed) {
            fKeyPressed = true; // Zarejestruj naciśnięcie
            boolean attacked = false;
            for (GameObject go : gameObjects) {
                if (go.getProperties().getTypeName().contains("Cube") && go.isVisible()) {
                    if (go.getProperties().isDestructible() && go.getProperties().isAlive()) {
                        System.out.println("Attacking " + go.getProperties().getTypeName() + "!");
                        boolean destroyed = go.takeDamage(25); // Zadaj 25 obrażeń
                        System.out.println("  " + go.getProperties().getTypeName() + " HP: " + go.getProperties().getCurrentHitPoints() + "/" + go.getProperties().getMaxHitPoints());
                        if (destroyed) {
                            go.setVisible(false); // Ukryj zniszczony obiekt
                            // W prawdziwej grze: oznacz do usunięcia, odtwórz efekt, upuść loot itp.
                        }
                        attacked = true;
                        break; // Zaatakuj tylko pierwszy znaleziony
                    }
                }
            }
            if (!attacked) System.out.println("No destructible cube found to attack!");
        } else if (!fCurrentlyPressed) {
            fKeyPressed = false; // Zresetuj flagę po zwolnieniu klawisza
        }

        // Przełączanie widoczności królika klawiszem B (pojedyncze naciśnięcie)
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
            if (!toggled) System.out.println("Bunny statue not found!");
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
            if (go.getMesh() == cubeMesh && go.isVisible()) { // Obracaj tylko widoczne kostki
                Vector3f pos = go.getPosition();
                if (Math.abs(pos.x - (-1.5f)) < 0.1f) { // Kostka 1
                    go.rotate(deltaTime * 0.3f, 0.0f, 1.0f, 0.0f);
                } else if (Math.abs(pos.x - 1.5f) < 0.1f) { // Kostka 2
                    go.rotate(deltaTime * 0.5f, 1.0f, 0.0f, 0.0f);
                } else if (Math.abs(pos.z - 1.8f) < 0.1f) { // Kostka 3
                    go.rotate(deltaTime * 0.8f, random.nextFloat(), random.nextFloat(), random.nextFloat());
                }
            }
        }

        // Można dodać tu logikę usuwania zniszczonych obiektów, jeśli są oznaczone
        // gameObjects.removeIf(go -> !go.getProperties().isAlive() && !go.isVisible());
    }

    @Override
    public void render(Window window, Camera camera, Renderer renderer) {
        if (renderer != null && renderer.isReady() && directionalLight != null) {
            // Filtruj listę obiektów PRZED przekazaniem do renderera
            // Chociaż renderer i tak powinien sprawdzać flagę isVisible
            List<GameObject> visibleObjects = new ArrayList<>();
            for(GameObject go : gameObjects) {
                // Sprawdzaj właściwość isVisible z GameObjectProperties
                if (go.getProperties().isVisible()) {
                    visibleObjects.add(go);
                }
            }
            // Przekaż tylko widoczne obiekty do renderowania
            renderer.render(camera, visibleObjects, directionalLight);
        } else {
            // Logowanie błędów renderowania
            if (renderer == null || !renderer.isReady()) System.err.println("DemoGame.render(): Skipping render - Renderer is not ready.");
            if (directionalLight == null) System.err.println("DemoGame.render(): Skipping render - DirectionalLight is null.");
        }
    }

    // --- Sprzątanie ---

    @Override
    public void cleanup() {
        System.out.println("DemoGame: Cleaning up resources...");
        long startTime = System.nanoTime();

        // Dźwięki sprzątane przez AudioManager

        System.out.println("  DemoGame: Cleaning up meshes...");
        for (Mesh mesh : meshes.values()) {
            if (mesh != null) { try { mesh.cleanup(); } catch (Exception e) { System.err.println("    Error cleaning up mesh: " + e.getMessage()); } }
        }
        meshes.clear();

        System.out.println("  DemoGame: Cleaning up textures...");
        for (Texture texture : textures.values()) {
            if (texture != null) { try { texture.cleanup(); } catch (Exception e) { System.err.println("    Error cleaning up texture: " + e.getMessage()); } }
        }
        textures.clear();

        if (defaultTexture != null) {
            try { defaultTexture.cleanup(); System.out.println("  DemoGame: Default texture cleaned up."); } catch (Exception e) { System.err.println("    Error cleaning up default texture: " + e.getMessage()); }
            defaultTexture = null;
        }

        gameObjects.clear();
        System.out.println("  DemoGame: GameObject list cleared.");

        long endTime = System.nanoTime();
        System.out.println("DemoGame: Resource cleanup finished (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    /** Metoda pomocnicza do sprzątania po częściowej inicjalizacji DemoGame. */
    private void cleanupPartialInit() {
        System.out.println("DemoGame: Cleaning up partially initialized resources due to init error...");
        for (Mesh mesh : meshes.values()) { if (mesh != null) mesh.cleanup(); }
        meshes.clear();
        for (Texture texture : textures.values()) { if (texture != null) texture.cleanup(); }
        textures.clear();
        if (defaultTexture != null) defaultTexture.cleanup();
        defaultTexture = null;
        gameObjects.clear();
        System.out.println("DemoGame: Partial cleanup finished.");
    }


    /** Tworzy prostą, jednokolorową teksturę 1x1. */
    private Texture createDefaultTexture() throws ResourceLoadException {
        ByteBuffer defaultPixel = null;
        Texture tex = null;
        try {
            defaultPixel = MemoryUtil.memAlloc(4); // RGBA
            defaultPixel.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip(); // Biały
            tex = new Texture(1, 1, defaultPixel, false); // Bez mipmap
            return tex;
        } catch(Exception e) {
            System.err.println("FATAL: Failed to create default 1x1 texture: " + e.getMessage());
            if (tex != null) tex.cleanup();
            throw new ResourceLoadException("Failed to create default texture", e);
        } finally {
            if (defaultPixel != null) MemoryUtil.memFree(defaultPixel);
        }
    }
}