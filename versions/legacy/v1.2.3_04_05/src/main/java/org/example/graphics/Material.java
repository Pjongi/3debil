package org.example.graphics;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL13; // Dla jednostek tekstur

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;

/**
 * Reprezentuje właściwości materiałowe obiektu, definiujące jak oddziałuje ze światłem.
 */
public class Material {

    // Konwencja jednostek tekstur - można przenieść do stałych w Rendererze
    public static final int DIFFUSE_MAP_TEXTURE_UNIT = 0;
    public static final int SPECULAR_MAP_TEXTURE_UNIT = 1;
    // public static final int NORMAL_MAP_TEXTURE_UNIT = 2; // Na przyszłość

    // Domyślne wartości
    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.8f, 0.8f, 0.8f); // Szary zamiast białego
    private static final float DEFAULT_REFLECTANCE = 32.0f;

    private Vector3f ambientColor;
    private Vector3f diffuseColor;
    private Vector3f specularColor;
    private float reflectance; // Odpowiednik "shininess"
    private Texture diffuseMap;  // Albedo/Base Color Map (może być null)
    private Texture specularMap; // Mapa kontrolująca odbicia (może być null)

    /**
     * Konstruktor domyślny - tworzy podstawowy, szary, matowy materiał.
     */
    public Material() {
        this.ambientColor = new Vector3f(DEFAULT_COLOR).mul(0.2f); // Ambient ciemniejszy
        this.diffuseColor = new Vector3f(DEFAULT_COLOR);
        this.specularColor = new Vector3f(0.1f); // Słabe odbicia domyślnie
        this.reflectance = DEFAULT_REFLECTANCE;
        this.diffuseMap = null;
        this.specularMap = null;
    }

    /**
     * Konstruktor materiału z teksturą diffuse i domyślnymi innymi wartościami.
     * @param diffuseMap Tekstura diffuse (albedo).
     */
    public Material(Texture diffuseMap) {
        this(); // Wywołaj konstruktor domyślny
        this.diffuseMap = diffuseMap;
        // Jeśli mamy teksturę diffuse, możemy założyć, że kolory bazowe to biały
        this.ambientColor = new Vector3f(1.0f).mul(0.2f);
        this.diffuseColor = new Vector3f(1.0f);
    }

    /**
     * Pełny konstruktor materiału.
     * @param ambientColor Kolor ambient.
     * @param diffuseColor Kolor diffuse.
     * @param specularColor Kolor specular.
     * @param reflectance Współczynnik odbicia (shininess).
     * @param diffuseMap Tekstura diffuse (może być null).
     * @param specularMap Tekstura specular (może być null).
     */
    public Material(Vector3f ambientColor, Vector3f diffuseColor, Vector3f specularColor, float reflectance, Texture diffuseMap, Texture specularMap) {
        this.ambientColor = ambientColor != null ? new Vector3f(ambientColor) : new Vector3f(DEFAULT_COLOR).mul(0.2f);
        this.diffuseColor = diffuseColor != null ? new Vector3f(diffuseColor) : new Vector3f(DEFAULT_COLOR);
        this.specularColor = specularColor != null ? new Vector3f(specularColor) : new Vector3f(0.1f);
        this.reflectance = reflectance > 0 ? reflectance : DEFAULT_REFLECTANCE;
        this.diffuseMap = diffuseMap;
        this.specularMap = specularMap;
    }


    /**
     * Wiąże właściwości materiału i tekstury do shadera.
     * UWAGA: Zakłada, że uniformy samplerów (`diffuseSampler`, `specularSampler`)
     * zostały już ustawione przez Renderer wskazując odpowiednie jednostki teksturujące.
     * @param shader Program shaderowy, do którego bindowane są uniformy.
     * @param defaultTexture Tekstura używana, gdy brakuje mapy (np. biały piksel). Może być null.
     */
    public void bind(ShaderProgram shader, Texture defaultTexture) {
        // Ustaw uniformy kolorów i odbicia (używamy bardziej opisowych nazw)
        shader.setUniform("material.ambient", ambientColor);
        shader.setUniform("material.diffuse", diffuseColor);
        shader.setUniform("material.specular", specularColor);
        shader.setUniform("material.reflectance", reflectance);

        boolean hasDiffuse = (this.diffuseMap != null);
        // Użyj setUniform(String, int) zamiast setUniform(String, boolean)
        shader.setUniform("material.hasDiffuseMap", hasDiffuse ? 1 : 0); // <--- ZMIANA
        Texture texToBindDiffuse = hasDiffuse ? this.diffuseMap : defaultTexture;
        if (texToBindDiffuse != null) {
            texToBindDiffuse.bind(DIFFUSE_MAP_TEXTURE_UNIT);
        } else {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + DIFFUSE_MAP_TEXTURE_UNIT);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        boolean hasSpecular = (this.specularMap != null);
        // Użyj setUniform(String, int) zamiast setUniform(String, boolean)
        shader.setUniform("material.hasSpecularMap", hasSpecular ? 1 : 0); // <--- ZMIANA
        Texture texToBindSpecular = hasSpecular ? this.specularMap : defaultTexture;
        if (texToBindSpecular != null) {
            texToBindSpecular.bind(SPECULAR_MAP_TEXTURE_UNIT);
        } else {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + SPECULAR_MAP_TEXTURE_UNIT);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    // --- Gettery i Settery ---
    public Vector3f getAmbientColor() { return ambientColor; }
    public void setAmbientColor(Vector3f ambientColor) { this.ambientColor = ambientColor; }
    public Vector3f getDiffuseColor() { return diffuseColor; }
    public void setDiffuseColor(Vector3f diffuseColor) { this.diffuseColor = diffuseColor; }
    public Vector3f getSpecularColor() { return specularColor; }
    public void setSpecularColor(Vector3f specularColor) { this.specularColor = specularColor; }
    public float getReflectance() { return reflectance; }
    public void setReflectance(float reflectance) { this.reflectance = reflectance > 0 ? reflectance : DEFAULT_REFLECTANCE; }
    public Texture getDiffuseMap() { return diffuseMap; }
    public void setDiffuseMap(Texture diffuseMap) { this.diffuseMap = diffuseMap; }
    public Texture getSpecularMap() { return specularMap; }
    public void setSpecularMap(Texture specularMap) { this.specularMap = specularMap; }

    // Nie ma metody cleanup() - Material nie jest właścicielem obiektów Texture
}