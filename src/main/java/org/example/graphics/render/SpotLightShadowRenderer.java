package org.example.graphics.render;

import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.graphics.ShaderProgram;
import org.example.graphics.light.SpotLight;
import org.example.graphics.shadow.SpotLightShadowMap; // Używamy nowej mapy cieni
import org.example.scene.GameObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Odpowiada za renderowanie mapy głębi (cube map) dla cieni rzucanych przez SpotLight.
 * Wykonuje 6 przebiegów renderowania (po jednym dla każdej ściany cube mapy).
 */
public class SpotLightShadowRenderer {

    private SpotLightShadowMap spotLightShadowMap;
    private final Window window; // Potrzebne do przywrócenia viewportu

    // Parametry projekcji dla mapy cieni
    private static final float SHADOW_NEAR_PLANE = 0.1f;
    // FAR_PLANE będzie pobierane z zasięgu światła

    public SpotLightShadowRenderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for SpotLightShadowRenderer");
        this.window = window;
    }

    /**
     * Inicjalizuje zasoby potrzebne do renderowania cieni reflektora,
     * w tym tworzy obiekt SpotLightShadowMap (FBO i cube mapę).
     * @throws ResourceLoadException Jeśli tworzenie SpotLightShadowMap się nie powiedzie.
     */
    public void init() throws ResourceLoadException {
        System.out.println("  SpotLightShadowRenderer: Initializing SpotLight shadow map...");
        try {
            spotLightShadowMap = new SpotLightShadowMap();
            System.out.println("    SpotLight Shadow Map (Cube Map) initialized (Texture ID: " + spotLightShadowMap.getDepthCubeMapTexture() + ").");
        } catch (Exception e) {
            throw new ResourceLoadException("Failed to create SpotLightShadowMap", e);
        }
        System.out.println("  SpotLightShadowRenderer: Initialized successfully.");
    }

    /**
     * Renderuje mapę głębi (cube mapę) dla podanego reflektora.
     *
     * @param gameObjects Lista obiektów w scenie do renderowania.
     * @param spotLight Reflektor, dla którego generowana jest mapa cieni.
     * @param depthShader Shader programu używany do renderowania głębokości (powinien to być spotLightDepthShader z ShaderManager).
     */
    public void render(List<GameObject> gameObjects, SpotLight spotLight, ShaderProgram depthShader) {
        if (spotLightShadowMap == null || depthShader == null || spotLight == null || gameObjects == null) {
            System.err.println("SpotLightShadowRenderer.render(): Dependencies not met (ShadowMap, Shader, SpotLight, or GameObjects are null). Skipping spot shadow pass.");
            return;
        }
        if (spotLight.pointLight == null) {
            System.err.println("SpotLightShadowRenderer.render(): SpotLight's internal PointLight is null. Skipping spot shadow pass.");
            return;
        }

        // Pobierz pozycję światła i zasięg (far plane)
        Vector3f lightPos = spotLight.pointLight.position;
        // Potrzebujemy sposobu na określenie far plane. Na razie przyjmijmy, że jest to zapisane
        // w PointLight lub obliczone z tłumienia. Dodajmy pole 'range' do PointLight jeśli go nie ma,
        // lub obliczmy je. Dla uproszczenia, dodajmy getter dla zasięgu do PointLight.
        // Załóżmy, że PointLight ma metodę getEffectiveRange().
        //float lightFarPlane = spotLight.pointLight.getEffectiveRange(); // TODO: Implement getEffectiveRange() in PointLight
        // Tymczasowo, użyjmy stałej lub wartości z Attenuation, jeśli jest dostępna.
        // Jeśli używamy Attenuation.forRange(), możemy przechować ten range.
        // Na razie użyjemy stałej wartości, np. zasięgu latarki.
        float lightFarPlane = 30.0f; // TODO: Pobierz to dynamicznie z właściwości światła

        // Wygeneruj macierze projekcji i widoku dla cube mapy
        Matrix4f shadowProj = SpotLightShadowMap.getCubeMapProjectionMatrix(SHADOW_NEAR_PLANE, lightFarPlane);
        Matrix4f[] shadowViews = SpotLightShadowMap.getCubeMapViewMatrices(lightPos);

        // --- Pętla renderowania 6 ścian cube mapy ---
        depthShader.bind();
        for (int i = 0; i < 6; ++i) {
            // 1. Oblicz macierz transformacji dla bieżącej ściany
            Matrix4f lightSpaceMatrix = new Matrix4f(shadowProj).mul(shadowViews[i]);

            // 2. Zwiąż FBO dla zapisu do odpowiedniej ściany cube mapy
            spotLightShadowMap.bindForWritingToFace(i); // To również ustawia viewport i czyści bufor

            // 3. Ustaw uniformy shadera głębi
            depthShader.setUniform("lightSpaceMatrix", lightSpaceMatrix);
            // Jeśli używalibyśmy zlinearyzowanej głębi:
            // depthShader.setUniform("lightPos", lightPos);
            // depthShader.setUniform("far_plane", lightFarPlane);

            // 4. Renderuj obiekty sceny
            for (GameObject go : gameObjects) {
                if (go != null && go.getMesh() != null && go.isVisible()) {
                    // TODO: Optymalizacja - Frustum Culling dla każdej ściany?
                    depthShader.setUniform("model", go.getModelMatrix());
                    go.getMesh().render();
                }
            }
        }
        depthShader.unbind();

        // 5. Odwiąż FBO i przywróć viewport okna
        spotLightShadowMap.unbindAfterWriting(window.getWidth(), window.getHeight());
    }

    /**
     * Zwraca ID tekstury cube mapy głębi.
     * @return ID tekstury OpenGL.
     * @throws IllegalStateException jeśli mapa cieni nie została zainicjalizowana.
     */
    public int getDepthCubeMapTextureId() {
        if (spotLightShadowMap == null) {
            throw new IllegalStateException("SpotLightShadowMap accessed before initialization in SpotLightShadowRenderer.");
        }
        return spotLightShadowMap.getDepthCubeMapTexture();
    }

    /** Zwraca zasięg (far plane) używany do generowania mapy cieni. */
    public float getShadowFarPlane(SpotLight spotLight) {
        // TODO: Implement dynamiczne pobieranie far plane
        if (spotLight == null || spotLight.pointLight == null) return 30.0f; // Wartość domyślna
        // float range = spotLight.pointLight.getEffectiveRange(); // Zakładając, że taka metoda istnieje
        // return range > 0 ? range : 30.0f;
        return 30.0f; // Na razie stała
    }

    public void cleanup() {
        System.out.println("  SpotLightShadowRenderer: Cleaning up SpotLight shadow map...");
        if (spotLightShadowMap != null) {
            spotLightShadowMap.cleanup();
            spotLightShadowMap = null;
            System.out.println("    SpotLight Shadow Map (Cube Map) cleaned.");
        }
        System.out.println("  SpotLightShadowRenderer: Cleanup complete.");
    }
}