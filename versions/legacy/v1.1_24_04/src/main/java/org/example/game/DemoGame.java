// src/main/java/org/example/game/DemoGame.java
package org.example.game;

import org.example.core.Input;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Implementacja logiki dla prostego dema z sześcianami i podłogą.
public class DemoGame implements IEngineLogic {

    private List<GameObject> gameObjects;
    private Set<Mesh> meshes; // Przechowujemy meshe, aby je posprzątać
    private DirectionalLight directionalLight;

    @Override
    public void init(Window window, Renderer renderer) throws Exception {
        gameObjects = new ArrayList<>();
        meshes = new HashSet<>(); // Używamy Set, aby uniknąć podwójnego czyszczenia

        // Utwórz i zapamiętaj siatki
        Mesh cubeMesh = MeshLoader.createCube();
        Mesh planeMesh = MeshLoader.createPlane(10.0f);
        meshes.add(cubeMesh);
        meshes.add(planeMesh);

        // Utwórz podłogę (płaszczyznę)
        GameObject floor = new GameObject(planeMesh);
        floor.setPosition(0.0f, -0.5f, 0.0f);
        floor.setColor(0.5f, 0.5f, 0.5f);
        gameObjects.add(floor);

        // Utwórz kilka sześcianów
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

        // Utwórz światło kierunkowe
        directionalLight = new DirectionalLight(
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(-0.5f, -1.0f, -0.3f),
                1.0f
        );
    }

    @Override
    public void input(Window window, Input input, Camera camera, float deltaTime) {
        // Tutaj przenosimy logikę sterowania kamerą
        camera.processKeyboard(deltaTime);

        Vector2f mouseDelta = Input.getMouseDelta();
        float xoffset = mouseDelta.x;
        float yoffset = -mouseDelta.y;
        camera.processMouseMovement(xoffset, yoffset, true);

        // Tutaj można dodać obsługę wejścia specyficzną dla gry
        // np. if (Input.isKeyDown(GLFW.GLFW_KEY_E)) { /* zrób coś */ }
    }

    @Override
    public void update(float deltaTime) {
        // Tutaj przenosimy logikę aktualizacji stanu gry (obracanie sześcianów)
        if (gameObjects.size() > 1 && gameObjects.get(1).getMesh() != gameObjects.get(0).getMesh()) {
            GameObject cube1 = gameObjects.get(1);
            cube1.rotate(deltaTime * 0.5f, 0.0f, 1.0f, 0.0f);
        }
        if (gameObjects.size() > 3 && gameObjects.get(3).getMesh() != gameObjects.get(0).getMesh()) {
            GameObject cube3 = gameObjects.get(3);
            cube3.rotate(deltaTime * 0.8f, 1.0f, 0.3f, 0.5f);
        }
    }

    @Override
    public void render(Window window, Camera camera, Renderer renderer) {
        // Po prostu wywołujemy metodę renderowania silnika, przekazując obiekty i światło z tej klasy
        renderer.render(camera, gameObjects, directionalLight);
    }

    @Override
    public void cleanup() {
        // Sprzątamy zasoby specyficzne dla gry - w tym przypadku meshe
        for (Mesh mesh : meshes) {
            if (mesh != null) {
                mesh.cleanup();
            }
        }
        meshes.clear(); // Wyczyść set po sprzątnięciu
        // Nie musimy sprzątać gameObjects indywidualnie, bo nie alokują zasobów OpenGL
    }
}