package org.example.scene;

import org.example.graphics.Material; // Zmieniono import
import org.example.graphics.Mesh;
// Usunięto import org.example.graphics.Texture;
import org.joml.*;

public class GameObject {

    private Mesh mesh;
    private Material material; // Zmieniono z Texture
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;
    private final GameObjectProperties properties;

    /**
     * Konstruktor obiektu gry.
     * @param mesh Siatka obiektu (nie może być null).
     * @param material Materiał obiektu (może być null, renderer użyje domyślnego).
     * @param properties Właściwości opisowe obiektu (nie może być null).
     */
    public GameObject(Mesh mesh, Material material, GameObjectProperties properties) { // Zmieniono Texture na Material
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (properties == null) throw new IllegalArgumentException("Properties cannot be null");
        // Materiał MOŻE być null, renderer sobie poradzi

        this.mesh = mesh;
        this.material = material; // Przypisz materiał
        this.properties = properties;
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf().identity();
        this.scale = new Vector3f(1, 1, 1);
    }

    /** Oblicza i zwraca macierz modelu dla tego obiektu. */
    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f();
        model.translate(position);
        model.rotate(rotation);
        model.scale(scale);
        return model;
    }

    // --- Gettery ---
    public Mesh getMesh() { return mesh; }
    public Material getMaterial() { return material; } // Zmieniono z getTexture()
    public Vector3f getPosition() { return position; }
    public Quaternionf getRotation() { return rotation; }
    public Vector3f getScale() { return scale; }
    public GameObjectProperties getProperties() { return properties; }

    // --- Settery ---
    public void setMesh(Mesh mesh) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be set to null");
        this.mesh = mesh;
    }
    public void setMaterial(Material material) { this.material = material; } // Zmieniono z setTexture()
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); }
    public void setPosition(Vector3f position) { this.position.set(position); }
    public void setRotation(float angleRad, float x, float y, float z) { this.rotation.fromAxisAngleRad(x, y, z, angleRad); }
    public void setRotation(Quaternionf rotation) { this.rotation.set(rotation); }
    public void rotate(float angleRad, float x, float y, float z) { this.rotation.rotateAxis(angleRad, x, y, z); }
    public void rotate(float angleRad, Vector3f axis) { this.rotation.rotateAxis(angleRad, axis); }
    public void setScale(float x, float y, float z) { this.scale.set(x, y, z); }
    public void setScale(float s) { this.scale.set(s, s, s); }
    public void setScale(Vector3f scale) { this.scale.set(scale); }

    // --- Metody delegujące do Properties ---
    public boolean isVisible() { return properties.isVisible(); }
    public void setVisible(boolean visible) { properties.setVisible(visible); }
    public boolean takeDamage(int amount) { return properties.takeDamage(amount); }
}