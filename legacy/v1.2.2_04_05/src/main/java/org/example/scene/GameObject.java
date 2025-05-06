package org.example.scene;

import org.example.graphics.Mesh;
import org.example.graphics.Texture;
import org.joml.*;

public class GameObject {

    private Mesh mesh;
    private Texture texture; // Może być null
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;

    private final GameObjectProperties properties; // Dodane pole właściwości

    /**
     * Konstruktor obiektu gry.
     * @param mesh Siatka obiektu (nie może być null).
     * @param texture Tekstura obiektu (może być null).
     * @param properties Właściwości opisowe obiektu (nie może być null).
     */
    public GameObject(Mesh mesh, Texture texture, GameObjectProperties properties) {
        if (mesh == null) {
            throw new IllegalArgumentException("Mesh cannot be null for a GameObject");
        }
        if (properties == null) {
            throw new IllegalArgumentException("GameObjectProperties cannot be null for a GameObject");
        }
        this.mesh = mesh;
        this.texture = texture;
        this.properties = properties; // Przypisz właściwości

        // Inicjalizacja transformacji
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf().identity();
        this.scale = new Vector3f(1, 1, 1);
    }

    /**
     * Oblicza i zwraca macierz modelu dla tego obiektu.
     * @return Macierz modelu (transformacji).
     */
    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f();
        model.translate(position);
        model.rotate(rotation);
        model.scale(scale);
        return model;
    }

    // --- Gettery ---
    public Mesh getMesh() { return mesh; }
    public Texture getTexture() { return texture; }
    public Vector3f getPosition() { return position; }
    public Quaternionf getRotation() { return rotation; }
    public Vector3f getScale() { return scale; }
    public GameObjectProperties getProperties() { return properties; } // Getter dla właściwości

    // --- Settery (dla transformacji) ---
    public void setMesh(Mesh mesh) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be set to null");
        this.mesh = mesh;
    }
    public void setTexture(Texture texture) { this.texture = texture; }
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); }
    public void setPosition(Vector3f position) { this.position.set(position); }
    public void setRotation(float angleRad, float x, float y, float z) { this.rotation.fromAxisAngleRad(x, y, z, angleRad); }
    public void setRotation(Quaternionf rotation) { this.rotation.set(rotation); }
    public void rotate(float angleRad, float x, float y, float z) { this.rotation.rotateAxis(angleRad, x, y, z); }
    public void rotate(float angleRad, Vector3f axis) { this.rotation.rotateAxis(angleRad, axis); }
    public void setScale(float x, float y, float z) { this.scale.set(x, y, z); }
    public void setScale(float s) { this.scale.set(s, s, s); }
    public void setScale(Vector3f scale) { this.scale.set(scale); }

    // --- Metody delegujące do Properties (dla wygody) ---

    /** Sprawdza, czy obiekt jest aktualnie widoczny (deleguje do Properties). */
    public boolean isVisible() {
        return properties.isVisible();
    }

    /** Ustawia widoczność obiektu (deleguje do Properties). */
    public void setVisible(boolean visible) {
        properties.setVisible(visible);
    }

    /** Zadaje obrażenia obiektowi (deleguje do Properties). */
    public boolean takeDamage(int amount) {
        return properties.takeDamage(amount);
    }

    // Można dodać więcej metod delegujących w razie potrzeby
}