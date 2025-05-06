package org.example.scene;

import org.example.graphics.Material;
import org.example.graphics.Mesh;
import org.joml.*;
import org.joml.Math; // Dla Math.sqrt i Math.max

public class GameObject {

    private Mesh mesh;
    private Material material;
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;
    private final GameObjectProperties properties;
    private float baseBoundingSphereRadius; // Promień dla obiektu o skali (1,1,1)

    public GameObject(Mesh mesh, Material material, GameObjectProperties properties) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (properties == null) throw new IllegalArgumentException("Properties cannot be null");

        this.mesh = mesh;
        this.material = material;
        this.properties = properties;
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf().identity();
        this.scale = new Vector3f(1, 1, 1);

        // Inicjalizacja baseBoundingSphereRadius na podstawie typu obiektu
        // Te wartości są przybliżone i powinny być dostosowane do rzeczywistych wymiarów modeli
        String typeNameLower = properties.getTypeName() != null ? properties.getTypeName().toLowerCase() : "";
        if (typeNameLower.contains("cube")) {
            this.baseBoundingSphereRadius = 0.866f; // Dla sześcianu 1x1x1
        } else if (typeNameLower.contains("bunny")) {
            this.baseBoundingSphereRadius = 0.5f; // Przybliżony promień dla modelu królika (dostosuj!)
        } else if (typeNameLower.contains("plane")) {
            this.baseBoundingSphereRadius = 0.1f; // Płaszczyzna jest płaska, mały promień dla detekcji "nad"
        }
        else {
            this.baseBoundingSphereRadius = 0.75f; // Domyślna wartość dla innych obiektów
        }
    }

    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f();
        model.translate(position);
        model.rotate(rotation);
        model.scale(scale);
        return model;
    }

    // --- Gettery ---
    public Mesh getMesh() { return mesh; }
    public Material getMaterial() { return material; }
    public Vector3f getPosition() { return position; }
    public Quaternionf getRotation() { return rotation; }
    public Vector3f getScaleVector() { return scale; }
    public GameObjectProperties getProperties() { return properties; }

    /**
     * Zwraca promień kuli otaczającej, uwzględniając aktualną skalę obiektu.
     * Bierze pod uwagę największy komponent skali.
     */
    public float getBoundingSphereRadius() {
        // Używamy Math.max do znalezienia największego współczynnika skali
        float maxScaleComponent = Math.max(this.scale.x, Math.max(this.scale.y, this.scale.z));
        return baseBoundingSphereRadius * maxScaleComponent;
    }

    // --- Settery ---
    public void setMesh(Mesh mesh) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be set to null");
        this.mesh = mesh;
    }
    public void setMaterial(Material material) { this.material = material; }
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); }
    public void setPosition(Vector3f position) { this.position.set(position); }
    public void setRotation(float angleRad, float x, float y, float z) { this.rotation.fromAxisAngleRad(x, y, z, angleRad); }
    public void setRotation(Quaternionf rotation) { this.rotation.set(rotation); }
    public void rotate(float angleRad, float x, float y, float z) { this.rotation.rotateAxis(angleRad, x, y, z); }
    public void rotate(float angleRad, Vector3f axis) { this.rotation.rotateAxis(angleRad, axis); }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
    }
    public void setScale(float s) {
        this.scale.set(s, s, s);
    }
    public void setScale(Vector3f scale) {
        this.scale.set(scale);
    }

    // --- Metody delegujące do Properties ---
    public boolean isVisible() { return properties.isVisible(); }
    public void setVisible(boolean visible) { properties.setVisible(visible); }
    public boolean takeDamage(int amount) { return properties.takeDamage(amount); }

    /**
     * Sprawdza, czy promień (ray) przecina kulę otaczającą (bounding sphere) ten obiekt.
     *
     * @param rayOrigin Początek promienia (np. pozycja kamery).
     * @param rayDirection Znormalizowany wektor kierunku promienia.
     * @param maxDistance Maksymalna odległość, na jaką promień jest sprawdzany.
     * @return Odległość do punktu przecięcia, jeśli występuje i jest w zasięgu, w przeciwnym razie -1.0f.
     */
    public float intersectsRay(Vector3f rayOrigin, Vector3f rayDirection, float maxDistance) {
        if (!this.isVisible() || !this.getProperties().canBeTargeted()) {
            return -1.0f; // Nie można trafić niewidocznego lub nietargetowalnego obiektu
        }

        Vector3f L = new Vector3f(this.position).sub(rayOrigin);
        float tca = L.dot(rayDirection);

        // Jeśli tca < 0, kula jest za promieniem. Dodatkowo, jeśli długość L jest większa niż promień,
        // oznacza to, że początek promienia jest na zewnątrz kuli i za nią, więc nie ma przecięcia z przodu.
        // Jeśli początek promienia jest wewnątrz kuli (L.lengthSquared() <= radius^2),
        // to tca może być < 0, ale nadal chcemy sprawdzić przecięcie.
        float scaledRadius = this.getBoundingSphereRadius();
        if (tca < 0 && L.lengthSquared() > (scaledRadius * scaledRadius)) {
            return -1.0f;
        }

        float d2 = L.dot(L) - tca * tca;
        if (d2 > (scaledRadius * scaledRadius)) {
            return -1.0f;
        }

        float thc = (float)Math.sqrt((scaledRadius * scaledRadius) - d2);
        float t0 = tca - thc;
        float t1 = tca + thc;

        if (t0 > t1) { // Upewnij się, że t0 jest mniejsze
            float temp = t0; t0 = t1; t1 = temp;
        }

        if (t1 < 0) { // Oba punkty przecięcia są za początkiem promienia
            return -1.0f;
        }

        // Wybierz bliższy, nieujemny punkt przecięcia
        float intersectionDistance = (t0 < 0) ? t1 : t0;

        if (intersectionDistance <= maxDistance) {
            return intersectionDistance;
        } else {
            return -1.0f; // Przecięcie jest zbyt daleko
        }
    }
}