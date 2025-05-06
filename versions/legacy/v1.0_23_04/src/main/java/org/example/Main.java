package org.example;

import org.example.core.Input;
import org.example.core.Timer;
import org.example.core.Window;
import org.example.graphics.Camera;
import org.example.graphics.Mesh;
import org.example.graphics.Renderer;
import org.example.graphics.light.DirectionalLight;
import org.example.scene.GameObject;
import org.example.util.MeshLoader;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private Window window;
    private Input input;
    private Timer timer;
    private Camera camera;
    private Renderer renderer;
    private List<GameObject> gameObjects;
    private DirectionalLight directionalLight;

    public void run() {
        System.out.println("Starting 3D ebil Engine...");
        System.out.println("LWJGL " + org.lwjgl.Version.getVersion() + "!");

        try {
            init();
            loop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void init() throws Exception {
        input = new Input();
        timer = new Timer();
        window = new Window("3D ebil Engine", 1280, 720);
        window.init(input);
        timer.init();

        renderer = new Renderer(window);
        renderer.init();

        camera = new Camera(new Vector3f(0.0f, 1.0f, 5.0f), new Vector3f(0.0f, 1.0f, 0.0f));

        gameObjects = new ArrayList<>();

        Mesh cubeMesh = MeshLoader.createCube();
        Mesh planeMesh = MeshLoader.createPlane(10.0f);

        GameObject floor = new GameObject(planeMesh);
        floor.setPosition(0.0f, -0.5f, 0.0f);
        floor.setColor(0.5f, 0.5f, 0.5f);
        gameObjects.add(floor);

        GameObject cube1 = new GameObject(cubeMesh);
        cube1.setPosition(-1.5f, 0.0f, -1.5f);
        cube1.setColor(1.0f, 0.0f, 0.0f);
        gameObjects.add(cube1);

        GameObject cube2 = new GameObject(cubeMesh);
        cube2.setPosition(1.5f, 0.5f, 0.0f);
        cube2.setScale(0.75f);
        cube2.setColor(0.0f, 1.0f, 0.0f);
        gameObjects.add(cube2);

        GameObject cube3 = new GameObject(cubeMesh);
        cube3.setPosition(0.0f, 0.2f, 1.8f);
        cube3.setScale(0.5f);
        cube3.setColor(0.0f, 0.0f, 1.0f);
        gameObjects.add(cube3);

        directionalLight = new DirectionalLight(
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(-0.5f, -1.0f, -0.3f),
                1.0f
        );
    }

    private void loop() {
        while (!window.windowShouldClose()) {
            timer.update();
            float deltaTime = timer.getDeltaTime();

            processInput(deltaTime);
            updateGameLogic(deltaTime);
            renderer.render(camera, gameObjects, directionalLight);

            window.update();
            input.update();
        }
    }

    private void processInput(float deltaTime) {
        camera.processKeyboard(deltaTime);
        Vector2f mouseDelta = Input.getMouseDelta();
        float xoffset = mouseDelta.x;
        float yoffset = -mouseDelta.y;
        camera.processMouseMovement(xoffset, yoffset, true);
    }

    private void updateGameLogic(float deltaTime) {
        if (gameObjects.size() > 1 && gameObjects.get(1).getMesh() != gameObjects.get(0).getMesh()) {
            GameObject cube1 = gameObjects.get(1);
            cube1.rotate(deltaTime * 0.5f, 0.0f, 1.0f, 0.0f);
        }
        if (gameObjects.size() > 3 && gameObjects.get(3).getMesh() != gameObjects.get(0).getMesh()) {
            GameObject cube3 = gameObjects.get(3);
            cube3.rotate(deltaTime * 0.8f, 1.0f, 0.3f, 0.5f);
        }
    }

    private void cleanup() {
        System.out.println("Cleaning up 3D ebil Engine...");
        if (renderer != null) renderer.cleanup();
        if (gameObjects != null) {
            java.util.Set<Mesh> meshesToClean = new java.util.HashSet<>();
            for (GameObject go : gameObjects) {
                if (go.getMesh() != null) {
                    meshesToClean.add(go.getMesh());
                }
            }
            for (Mesh mesh : meshesToClean) {
                mesh.cleanup();
            }
        }
        if (window != null) window.cleanup();
        System.out.println("Cleanup complete.");
    }

    public static void main(String[] args) {
        new Main().run();
    }
}