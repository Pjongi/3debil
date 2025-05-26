package org.example.game;

import org.example.audio.AudioManager;
import org.example.audio.SoundSource;
import org.example.core.Input;
import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.Camera;
import org.example.graphics.Material;
import org.example.graphics.Mesh;
import org.example.graphics.Texture;
import org.example.graphics.light.Attenuation;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.light.PointLight;
import org.example.graphics.light.SpotLight;
import org.example.graphics.render.Renderer;
import org.example.scene.GameObject;
import org.example.scene.GameObjectProperties;
import org.example.ui.NuklearGui; // Upewnij się, że ten import jest
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
    private Map<String, Texture> textures;
    private Map<String, Material> materials;
    private DirectionalLight directionalLight;
    private List<PointLight> pointLights;
    private List<SpotLight> spotLights;

    private AudioManager audioManager;
    private NuklearGui nuklearGui; // Referencja do NuklearGui

    private SoundSource backgroundMusicSource;
    private SoundSource stepSoundSource;
    private int stepSoundBuffer = -1;
    private final float stepSoundCooldown = 0.35f;
    private float timeSinceLastStep = 0f;
    private Random random = new Random();
    private Texture defaultTexture = null;

    private boolean fKeyPressed = false;
    private boolean bKeyPressed = false;
    private boolean tKeyPressed = false;

    private boolean isFlashlightOn = true;
    private float originalFlashlightIntensity = 1.5f;

    private final float interactionMaxDistance = 10.0f;

    @Override
    public void init(Window window, Renderer renderer, AudioManager audioManager, NuklearGui nuklearGui) throws Exception {
        this.audioManager = audioManager;
        this.nuklearGui = nuklearGui; // Zapisz referencję
        this.gameObjects = new ArrayList<>();
        this.meshes = new HashMap<>();
        this.textures = new HashMap<>();
        this.materials = new HashMap<>();
        this.pointLights = new ArrayList<>();
        this.spotLights = new ArrayList<>();

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

            if (meshes.containsKey("plane")) {
                createGameObjects();
            } else {
                System.err.println("  DemoGame: Skipping GameObject creation due to missing essential meshes (e.g., plane).");
            }

            createLights();

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
                new Vector3f(0.2f, 0.2f, 0.2f),
                new Vector3f(0.8f, 0.8f, 0.8f),
                new Vector3f(0.9f, 0.9f, 0.9f),
                128f,
                diffuseTexForBunny,
                null
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
        if (audioManager == null) {
            System.err.println("    ERROR: AudioManager is null in loadSoundsSafe.");
            return;
        }
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
        Mesh planeMesh = meshes.get("plane");
        Mesh cubeMesh = meshes.get("cube");
        Mesh bunnyMesh = meshes.get("bunny");
        Material defaultMat = materials.getOrDefault("stone", new Material(defaultTexture));

        GameObjectProperties floorProps = new GameObjectProperties.Builder()
                .typeName("Floor").material("Grass").setStatic(true).physicsEnabled(false).targetable(false).build();
        if (planeMesh != null) {
            GameObject floor = new GameObject(planeMesh, materials.getOrDefault("grass", defaultMat), floorProps);
            floor.setPosition(0.0f, -0.5f, 0.0f);
            gameObjects.add(floor);
        }

        GameObjectProperties stoneCubeProps = new GameObjectProperties.Builder()
                .typeName("StoneCube").material("Stone").physicsEnabled(false).mass(5.0f).friction(0.8f)
                .makeDestructible(150).targetable(true).build();
        GameObjectProperties woodCubeProps = new GameObjectProperties.Builder()
                .typeName("WoodCube").material("Wood").physicsEnabled(false).mass(1.5f).friction(0.6f)
                .makeDestructible(50).targetable(true).build();
        GameObjectProperties smallStoneCubeProps = new GameObjectProperties.Builder()
                .typeName("SmallStoneCube").material("Stone").physicsEnabled(false).mass(2.0f).friction(0.8f)
                .makeDestructible(80).targetable(true).build();

        if (cubeMesh != null) {
            GameObject cube1 = new GameObject(cubeMesh, materials.getOrDefault("stone", defaultMat), stoneCubeProps);
            cube1.setPosition(-2.5f, 0.0f, -2.5f);
            gameObjects.add(cube1);

            GameObject cube2 = new GameObject(cubeMesh, materials.getOrDefault("wood", defaultMat), woodCubeProps);
            cube2.setPosition(2.5f, 0.5f, -1.0f);
            cube2.setScale(0.75f);
            gameObjects.add(cube2);

            GameObject cube3 = new GameObject(cubeMesh, materials.getOrDefault("stone", defaultMat), smallStoneCubeProps);
            cube3.setPosition(0.0f, 0.2f, 2.8f);
            cube3.setScale(0.5f);
            gameObjects.add(cube3);
        }

        GameObjectProperties bunnyProps = new GameObjectProperties.Builder()
                .typeName("BunnyStatue").material("ShinyStone").setStatic(true).physicsEnabled(false)
                .makeDestructible(300).targetable(true).build();
        if (bunnyMesh != null) {
            GameObject bunny = new GameObject(bunnyMesh, materials.getOrDefault("shiny_stone", defaultMat), bunnyProps);
            bunny.setPosition(0.0f, -0.45f, -4.5f);
            bunny.setScale(0.7f);
            bunny.setRotation((float)Math.toRadians(180), 0, 1, 0);
            gameObjects.add(bunny);
            System.out.println("    Bunny GameObject created.");
        }
        System.out.println("  DemoGame: Game objects creation finished (" + gameObjects.size() + " objects).");
    }


    private void createLights() {
        System.out.println("  DemoGame: Creating lights...");
        directionalLight = new DirectionalLight(new Vector3f(0.7f, 0.7f, 0.6f), new Vector3f(0.6f, -0.8f, -0.4f), 0.6f);
        System.out.println("    Directional light created.");

        pointLights.add(new PointLight(new Vector3f(-2.5f, 1.5f, -2.5f), new Vector3f(1.0f, 0.1f, 0.1f), 1.0f, 15.0f));
        pointLights.add(new PointLight(new Vector3f(2.0f, 0.0f, 0.0f), new Vector3f(0.1f, 0.1f, 1.0f), 0.9f, Attenuation.forRange(12f)));
        pointLights.add(new PointLight(new Vector3f(0.0f, 1.5f, 2.8f), new Vector3f(0.1f, 1.0f, 0.1f), 0.8f, Attenuation.forRange(8f)));
        System.out.println("    Created " + pointLights.size() + " point lights.");

        Vector3f flashlightColor = new Vector3f(0.95f, 0.95f, 0.8f);
        originalFlashlightIntensity = 1.5f;
        SpotLight flashlight = new SpotLight(
                new PointLight(new Vector3f(0f, 0f, 0f), flashlightColor, originalFlashlightIntensity, Attenuation.forRange(30f)),
                new Vector3f(0, -1, 0), 12.5f, 17.5f
        );
        spotLights.add(flashlight);
        isFlashlightOn = true;
        System.out.println("    Created " + spotLights.size() + " spot lights.");
    }


    @Override
    public void input(Window window, Input input, Camera camera, float deltaTime) {
        if (camera == null) return;

        camera.processKeyboard(deltaTime);
        Vector2f mouseDelta = Input.getMouseDelta();
        if (Math.abs(mouseDelta.x) < 100 && Math.abs(mouseDelta.y) < 100) {
            camera.processMouseMovement(mouseDelta.x, -mouseDelta.y, true);
        }

        boolean isMoving = Input.isKeyDown(GLFW.GLFW_KEY_W) || Input.isKeyDown(GLFW.GLFW_KEY_A) ||
                Input.isKeyDown(GLFW.GLFW_KEY_S) || Input.isKeyDown(GLFW.GLFW_KEY_D);
        timeSinceLastStep += deltaTime;
        if (isMoving && stepSoundSource != null && stepSoundBuffer != -1 && !stepSoundSource.isPlaying() && timeSinceLastStep >= stepSoundCooldown) {
            stepSoundSource.setPosition(camera.getPosition());
            stepSoundSource.play();
            timeSinceLastStep = 0f;
        }

        handleInteractionInput(camera);
        updateFlashlight(camera);
    }

    private void handleInteractionInput(Camera camera) {
        boolean fCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_F);
        if (fCurrentlyPressed && !fKeyPressed) {
            fKeyPressed = true;

            Vector3f rayOrigin = camera.getRayOrigin();
            Vector3f rayDirection = camera.getRayDirection();
            GameObject targetedObject = null;
            float closestDistance = interactionMaxDistance + 1.0f;

            for (GameObject go : gameObjects) {
                if (go.isVisible() && go.getProperties().canBeTargeted()) {
                    float distance = go.intersectsRay(rayOrigin, rayDirection, interactionMaxDistance);
                    if (distance >= 0 && distance < closestDistance) {
                        closestDistance = distance;
                        targetedObject = go;
                    }
                }
            }

            if (targetedObject != null) {
                System.out.println("Player is looking at: " + targetedObject.getProperties().getTypeName() + " at distance: " + String.format("%.2f", closestDistance));

                if (targetedObject.getProperties().isDestructible() && targetedObject.getProperties().isAlive()) {
                    System.out.println("Attacking " + targetedObject.getProperties().getTypeName() + "!");
                    if (targetedObject.takeDamage(25)) { // takeDamage wywołuje triggerHitEffect w GameObjectProperties
                        System.out.println("  " + targetedObject.getProperties().getTypeName() + " DESTROYED!");
                    }
                    System.out.println("  " + targetedObject.getProperties().getTypeName() + " HP: " + targetedObject.getProperties().getCurrentHitPoints() + "/" + targetedObject.getProperties().getMaxHitPoints());
                } else if (!targetedObject.getProperties().isDestructible()){
                    System.out.println("  " + targetedObject.getProperties().getTypeName() + " is not destructible.");
                    // Nawet jeśli niezniszczalny, ale targetowalny, pokażmy Hit Marker
                    targetedObject.getProperties().triggerHitEffect();
                } else if (!targetedObject.getProperties().isAlive()){
                    System.out.println("  " + targetedObject.getProperties().getTypeName() + " is already destroyed.");
                }

                // Trigger Hit Marker GUI
                if (this.nuklearGui != null) {
                    System.out.println("DEMOGAME: Triggering Hit Marker via NuklearGUI!"); // LOG
                    this.nuklearGui.triggerHitMarker();
                }

            } else {
                System.out.println("Player is looking at nothing interactable in range (" + interactionMaxDistance + " units).");
            }

        } else if (!fCurrentlyPressed) {
            fKeyPressed = false;
        }

        boolean bCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_B);
        if (bCurrentlyPressed && !bKeyPressed) {
            bKeyPressed = true;
            boolean toggled = false;
            for (GameObject go : gameObjects) {
                if ("BunnyStatue".equals(go.getProperties().getTypeName())) {
                    if (go.getProperties().isAlive()) {
                        go.setVisible(!go.isVisible());
                        System.out.println("Bunny visibility toggled to: " + go.isVisible());
                    } else {
                        System.out.println("Bunny is destroyed, cannot toggle visibility.");
                    }
                    toggled = true;
                    break;
                }
            }
            if (!toggled) System.out.println("Bunny not found!");
        } else if (!bCurrentlyPressed) {
            bKeyPressed = false;
        }

        boolean tCurrentlyPressed = Input.isKeyDown(GLFW.GLFW_KEY_T);
        if (tCurrentlyPressed && !tKeyPressed) {
            tKeyPressed = true;
            if (!spotLights.isEmpty()) {
                SpotLight playerFlashlight = spotLights.get(0);
                isFlashlightOn = !isFlashlightOn;
                playerFlashlight.pointLight.intensity = isFlashlightOn ? originalFlashlightIntensity : 0.0f;
                System.out.println("Flashlight toggled: " + (isFlashlightOn ? "ON" : "OFF"));
            }
        } else if (!tCurrentlyPressed) {
            tKeyPressed = false;
        }
    }

    private void updateFlashlight(Camera camera) {
        if (!spotLights.isEmpty() && camera != null) {
            SpotLight playerFlashlight = spotLights.get(0);
            playerFlashlight.pointLight.position.set(camera.getPosition());
            playerFlashlight.direction.set(camera.getFront());
        }
    }

    @Override
    public void update(float deltaTime) {
        for (GameObject go : gameObjects) {
            // Aktualizacja efektu trafienia (jeśli zaimplementowano wizualny efekt na obiekcie)
            go.getProperties().updateHitEffect(deltaTime);

            // Logika rotacji sześcianów
            if (go.getMesh() == meshes.get("cube") && go.isVisible() && go.getProperties().isAlive()) {
                Vector3f pos = go.getPosition();
                if (Math.abs(pos.x - (-2.5f)) < 0.1f && Math.abs(pos.z - (-2.5f)) < 0.1f) {
                    go.rotate(deltaTime * 0.3f, 0, 1, 0);
                } else if (Math.abs(pos.x - 2.5f) < 0.1f && Math.abs(pos.z - (-1.0f)) < 0.1f) {
                    go.rotate(deltaTime * 0.5f, 1, 0, 0);
                } else if (Math.abs(pos.z - 2.8f) < 0.1f) {
                    go.rotate(deltaTime * 0.8f, random.nextFloat(), random.nextFloat(), random.nextFloat());
                }
            }
        }
    }

    @Override
    public void render(Window window, Camera camera, Renderer renderer) {
        if (renderer != null && renderer.isReady() && camera != null) {
            List<GameObject> visibleObjects = new ArrayList<>();
            for (GameObject go : gameObjects) {
                if (go.isVisible()) {
                    // Tutaj można by dodać logikę zmiany materiału/uniformów dla efektu trafienia
                    // jeśli GameObjectProperties.isHitEffectActive() jest true
                    visibleObjects.add(go);
                }
            }
            renderer.render(camera, visibleObjects, directionalLight, pointLights, spotLights);
        } else {
            if (renderer == null || !renderer.isReady()) System.err.println("DemoGame.render(): Renderer not ready or null.");
            if (camera == null) System.err.println("DemoGame.render(): Camera is null.");
        }
    }


    @Override
    public void cleanup() {
        System.out.println("DemoGame: Cleaning up resources...");
        long startTime = System.nanoTime();

        if (meshes != null) {
            meshes.values().stream().filter(m -> m != null).forEach(Mesh::cleanup);
            meshes.clear();
            System.out.println("  Meshes cleaned.");
        }
        if (materials != null) {
            materials.clear();
            System.out.println("  Materials map cleared.");
        }
        if (textures != null) {
            textures.values().stream().filter(t -> t != null).forEach(Texture::cleanup);
            textures.clear();
            System.out.println("  Textures cleaned.");
        }
        if (defaultTexture != null) {
            defaultTexture.cleanup();
            defaultTexture = null;
            System.out.println("  Default texture cleaned.");
        }
        if (gameObjects != null) {
            gameObjects.clear();
            System.out.println("  GameObject list cleared.");
        }
        if (pointLights != null) pointLights.clear();
        if (spotLights != null) spotLights.clear();
        System.out.println("  Light lists cleared.");

        long endTime = System.nanoTime();
        System.out.println("DemoGame: Cleanup finished (" + (endTime - startTime) / 1_000_000 + " ms).");
    }

    private void cleanupPartialInit() {
        System.out.println("DemoGame: Cleaning up partially initialized resources...");
        cleanup();
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