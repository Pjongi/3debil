package org.example.graphics.light;

import org.joml.Vector3f;

public class PointLight {
    public Vector3f position;
    public Vector3f color;
    public float intensity;
    public Attenuation attenuation; // Używamy klasy Attenuation

    public PointLight(Vector3f position, Vector3f color, float intensity, Attenuation attenuation) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
        this.intensity = Math.max(0f, intensity);
        this.attenuation = attenuation != null ? attenuation : new Attenuation(1.0f, 0.0f, 0.09f); // Domyślne tłumienie
    }

    // Konstruktor z predefiniowanym zasięgiem
    public PointLight(Vector3f position, Vector3f color, float intensity, float range) {
        this(position, color, intensity, Attenuation.forRange(range));
    }

    // Prosty konstruktor
    public PointLight(Vector3f position, Vector3f color, float intensity) {
        this(position, color, intensity, new Attenuation(1.0f, 0.09f, 0.032f)); // Domyślne tłumienie dla ~50 jednostek
    }
}