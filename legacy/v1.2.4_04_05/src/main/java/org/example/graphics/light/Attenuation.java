package org.example.graphics.light;

/**
 * Przechowuje współczynniki tłumienia światła (dla PointLight i SpotLight).
 * Wzór: attenuation = 1.0 / (constant + linear * distance + quadratic * distance^2)
 */
public class Attenuation {
    public float constant;
    public float linear;
    public float quadratic;

    /**
     * Domyślne tłumienie (światło nie gaśnie).
     */
    public Attenuation() {
        this.constant = 1.0f;
        this.linear = 0.0f;
        this.quadratic = 0.0f;
    }

    public Attenuation(float constant, float linear, float quadratic) {
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
    }

    // Można dodać predefiniowane tłumienia dla różnych zasięgów
    public static Attenuation forRange(float range) {
        // Przykładowe wartości (wymagają dostrojenia!)
        if (range <= 7) return new Attenuation(1.0f, 0.7f, 1.8f);
        if (range <= 13) return new Attenuation(1.0f, 0.35f, 0.44f);
        if (range <= 20) return new Attenuation(1.0f, 0.22f, 0.20f);
        if (range <= 32) return new Attenuation(1.0f, 0.14f, 0.07f);
        if (range <= 50) return new Attenuation(1.0f, 0.09f, 0.032f);
        if (range <= 65) return new Attenuation(1.0f, 0.07f, 0.017f);
        if (range <= 100) return new Attenuation(1.0f, 0.045f, 0.0075f);
        // Domyślne dla większych zasięgów
        return new Attenuation(1.0f, 0.022f, 0.0019f);
    }
}