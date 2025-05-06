package org.example.graphics; // Upewnij się, że pakiet jest poprawny

import org.joml.*;
import org.example.core.Input; // Załóżmy, że Input jest w org.example.core
import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private Vector3f position;
    private Vector3f front;
    private Vector3f up; // Ten wektor potrzebujemy udostępnić
    private Vector3f right;
    private final Vector3f worldUp; // Zazwyczaj (0, 1, 0)

    // Kąty Eulera
    private float yaw;   // Kąt obrotu w poziomie (wokół osi Y)
    private float pitch; // Kąt obrotu w pionie (wokół osi X)

    // Opcje kamery - mogą być final, jeśli nie zmieniasz ich po inicjalizacji
    private final float movementSpeed;
    private final float mouseSensitivity;
    private float fov; // Może być zmieniane, np. zoom

    /**
     * Konstruktor z wektorami.
     * @param position Pozycja początkowa kamery.
     * @param worldUp Wektor "góry" świata (zazwyczaj (0, 1, 0)).
     */
    public Camera(Vector3f position, Vector3f worldUp) {
        this.position = position;
        this.worldUp = new Vector3f(worldUp).normalize();
        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.yaw = -90.0f;
        this.pitch = 0.0f;
        // Domyślne wartości - można je też przekazać w konstruktorze
        this.movementSpeed = 2.5f;
        this.mouseSensitivity = 0.1f;
        this.fov = 45.0f;
        updateCameraVectors(); // Oblicz wektory right i up na podstawie front i worldUp
    }

    /**
     * Konstruktor domyślny.
     * Ustawia kamerę w pozycji (0, 0, 3), patrzącą na (0, 0, 0) z worldUp=(0, 1, 0).
     */
    public Camera() {
        this(new Vector3f(0.0f, 0.0f, 3.0f), new Vector3f(0.0f, 1.0f, 0.0f));
    }

    /**
     * Zwraca macierz widoku obliczoną przy użyciu kątów Eulera i macierzy LookAt.
     * @return Macierz widoku (View Matrix).
     */
    public Matrix4f getViewMatrix() {
        // Cel kamery to pozycja + wektor kierunku (front)
        Vector3f center = new Vector3f(position).add(front);
        // lookAt(eye, center, up)
        return new Matrix4f().lookAt(position, center, up);
    }

    /**
     * Zwraca macierz projekcji perspektywicznej.
     * @param aspectRatio Stosunek szerokości do wysokości okna.
     * @return Macierz projekcji (Projection Matrix).
     */
    public Matrix4f getProjectionMatrix(float aspectRatio) {
        // Rzutuj wynik toRadians() na float
        return new Matrix4f().perspective((float) java.lang.Math.toRadians(fov), aspectRatio, 0.1f, 100.0f);
        // fovy, aspect, zNear, zFar
    }

    /**
     * Przetwarza wejście z klawiatury (ruch kamery).
     * @param deltaTime Czas od ostatniej klatki.
     */
    public void processKeyboard(float deltaTime) {
        float velocity = movementSpeed * deltaTime;
        // Kopiujemy wektory, aby nie modyfikować oryginałów przed obliczeniem wszystkich ruchów
        Vector3f frontMovement = new Vector3f(front).mul(velocity);
        Vector3f rightMovement = new Vector3f(right).mul(velocity);
        Vector3f upMovement = new Vector3f(worldUp).mul(velocity); // Używamy worldUp do ruchu góra/dół

        if (Input.isKeyDown(GLFW_KEY_W))
            position.add(frontMovement);
        if (Input.isKeyDown(GLFW_KEY_S))
            position.sub(frontMovement);
        if (Input.isKeyDown(GLFW_KEY_A))
            position.sub(rightMovement);
        if (Input.isKeyDown(GLFW_KEY_D))
            position.add(rightMovement);
        if (Input.isKeyDown(GLFW_KEY_SPACE))
            position.add(upMovement); // W górę
        if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT))
            position.sub(upMovement); // W dół
    }

    /**
     * Przetwarza ruch myszy (obrót kamery).
     * @param xoffset Zmiana pozycji X myszy.
     * @param yoffset Zmiana pozycji Y myszy.
     * @param constrainPitch Czy ograniczać kąt pitch, aby uniknąć "przewrotki".
     */
    public void processMouseMovement(float xoffset, float yoffset, boolean constrainPitch) {
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        yaw += xoffset;
        pitch += yoffset; // Wiele implementacji odwraca yoffset (pitch -= yoffset)

        // Ograniczenie kąta pitch
        if (constrainPitch) {
            if (pitch > 89.0f)
                pitch = 89.0f;
            if (pitch < -89.0f)
                pitch = -89.0f;
        }

        // Aktualizacja wektorów front, right i up na podstawie zaktualizowanych kątów Eulera
        updateCameraVectors();
    }

    /**
     * Oblicza wektory front, right i up na podstawie aktualnych kątów yaw i pitch.
     */
    private void updateCameraVectors() {
        // Oblicz nowy wektor 'front' używając jawnie java.lang.Math
        Vector3f newFront = new Vector3f();
        newFront.x = (float) (java.lang.Math.cos(java.lang.Math.toRadians(yaw)) * java.lang.Math.cos(java.lang.Math.toRadians(pitch)));
        newFront.y = (float) java.lang.Math.sin(java.lang.Math.toRadians(pitch));
        newFront.z = (float) (java.lang.Math.sin(java.lang.Math.toRadians(yaw)) * java.lang.Math.cos(java.lang.Math.toRadians(pitch)));
        front = newFront.normalize(); // Znormalizuj nowy wektor kierunku

        // Przelicz wektor 'right' jako iloczyn wektorowy front i worldUp
        // Normalizacja jest ważna, bo wektory mogą nie być idealnie prostopadłe
        right = new Vector3f(front).cross(worldUp).normalize();

        // Przelicz wektor 'up' jako iloczyn wektorowy right i front
        // To zapewnia, że wszystkie trzy wektory (front, right, up) tworzą układ ortonormalny
        up = new Vector3f(right).cross(front).normalize();
    }

    // --- Gettery ---

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getFront() {
        return front;
    }

    /**
     * Zwraca aktualny wektor "góry" kamery.
     * Ten wektor jest prostopadły do wektora 'front' i 'right'.
     * @return Wektor 'up' kamery.
     */
    public Vector3f getUp() { // Dodano ten getter
        return up;
    }

    public float getFov() {
        return fov;
    }

    // --- Settery (jeśli potrzebne) ---
    public void setFov(float fov) {
        // Można dodać ograniczenia np. 1.0f do 120.0f
        this.fov = fov;
    }
}