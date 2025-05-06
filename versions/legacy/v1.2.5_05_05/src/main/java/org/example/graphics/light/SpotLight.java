package org.example.graphics.light;

import org.joml.Vector3f;

public class SpotLight {
    public PointLight pointLight; // Zawiera pozycję, kolor, intensywność, tłumienie
    public Vector3f direction;
    public float cutOffAngle;   // Kąt wewnętrznego stożka w stopniach
    public float outerCutOffAngle; // Kąt zewnętrznego stożka w stopniach

    public SpotLight(PointLight pointLight, Vector3f direction, float cutOffAngle, float outerCutOffAngle) {
        if (pointLight == null) throw new IllegalArgumentException("PointLight cannot be null for SpotLight");
        this.pointLight = pointLight;
        this.direction = new Vector3f(direction).normalize();
        // Upewnij się, że outerCutOff jest większy lub równy cutOff
        this.outerCutOffAngle = Math.max(cutOffAngle, outerCutOffAngle);
        this.cutOffAngle = cutOffAngle;
    }

    // Konstruktor uproszczony
    public SpotLight(Vector3f position, Vector3f color, float intensity, float range, Vector3f direction, float cutOffAngle, float outerCutOffAngle) {
        this(new PointLight(position, color, intensity, range), direction, cutOffAngle, outerCutOffAngle);
    }

    // Gettery cosinusów kątów (często używane w shaderach)
    public float getCutOffCos() {
        return (float) Math.cos(Math.toRadians(cutOffAngle));
    }

    public float getOuterCutOffCos() {
        return (float) Math.cos(Math.toRadians(outerCutOffAngle));
    }
}