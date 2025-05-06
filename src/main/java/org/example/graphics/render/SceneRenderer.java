package org.example.graphics.render;

import org.example.core.Window;
import org.example.graphics.Camera;
import org.example.graphics.Material;
import org.example.graphics.ShaderProgram;
import org.example.graphics.Texture;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.light.PointLight;
import org.example.graphics.light.SpotLight;
import org.example.scene.GameObject;
import org.joml.Vector3f; // Dodano import

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.example.graphics.render.Renderer.MAX_POINT_LIGHTS; // Import stałych
import static org.example.graphics.render.Renderer.MAX_SPOT_LIGHTS; // Import stałych

/**
 * Odpowiada za główny przebieg renderowania sceny (Forward Pass).
 * Renderuje obiekty do domyślnego bufora ramki, uwzględniając oświetlenie,
 * materiały i cienie (przy użyciu mapy cieni z ShadowRenderer).
 */
public class SceneRenderer {

    private final Window window;
    private ShaderProgram sceneShader; // Zależność wstrzykiwana
    private Texture defaultTexture;    // Zależność wstrzykiwana
    private Material defaultMaterial;   // Zależność wstrzykiwana
    private int shadowMapTextureId = -1; // Zależność wstrzykiwana (ID tekstury)

    private static final float DEFAULT_SHADOW_BIAS = 0.005f;

    // Konstruktor przyjmuje niezbędne zależności
    public SceneRenderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for SceneRenderer");
        this.window = window;
    }

    // Metoda do ustawienia zależności po ich zainicjalizowaniu w głównym Rendererze
    public void setupDependencies(ShaderProgram sceneShader, Texture defaultTexture, Material defaultMaterial, int shadowMapTextureId) {
        this.sceneShader = sceneShader;
        this.defaultTexture = defaultTexture;
        this.defaultMaterial = defaultMaterial;
        this.shadowMapTextureId = shadowMapTextureId;

        if (this.sceneShader == null || this.defaultTexture == null || this.defaultMaterial == null || this.shadowMapTextureId == -1) {
            throw new IllegalStateException("SceneRenderer dependencies not fully set.");
        }
        System.out.println("  SceneRenderer: Dependencies set.");
    }


    /**
     * Wykonuje główny przebieg renderowania sceny.
     *
     * @param camera Kamera sceny.
     * @param gameObjects Lista obiektów do renderowania.
     * @param dirLight Światło kierunkowe (może być null).
     * @param pointLights Lista świateł punktowych.
     * @param spotLights Lista świateł reflektorowych.
     */
    public void render(Camera camera, List<GameObject> gameObjects,
                       DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {

        if (sceneShader == null || defaultTexture == null || defaultMaterial == null || shadowMapTextureId == -1) {
            System.err.println("SceneRenderer.render(): Dependencies not set. Skipping scene pass.");
            return;
        }
        if (camera == null || gameObjects == null) {
            System.err.println("SceneRenderer.render(): Camera or gameObjects list is null. Skipping scene pass.");
            return;
        }

        // Ustaw viewport na rozmiar okna
        glViewport(0, 0, window.getWidth(), window.getHeight());
        // Wyczyść domyślny bufor ramki
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        sceneShader.bind();

        // --- Ustawienie uniformów globalnych (per-frame) ---
        setGlobalUniforms(camera, dirLight, pointLights, spotLights);

        // --- Renderowanie obiektów sceny ---
        renderSceneObjects(gameObjects);

        sceneShader.unbind();
    }

    private void setGlobalUniforms(Camera camera, DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {
        setCameraUniforms(camera);
        setDirectionalLightUniforms(dirLight);
        setPointLightsUniforms(pointLights);
        setSpotLightsUniforms(spotLights);
        setSamplerUniforms();
    }

    private void setCameraUniforms(Camera camera) {
        float aspectRatio = (float) window.getWidth() / Math.max(1, window.getHeight());
        sceneShader.setUniform("projection", camera.getProjectionMatrix(aspectRatio));
        sceneShader.setUniform("view", camera.getViewMatrix());
        sceneShader.setUniform("viewPos", camera.getPosition());
    }

    private void setDirectionalLightUniforms(DirectionalLight dirLight) {
        if (dirLight != null) {
            sceneShader.setUniform("dirLight.direction", dirLight.getDirection());
            sceneShader.setUniform("dirLight.color", dirLight.getColor());
            sceneShader.setUniform("dirLight.intensity", dirLight.getIntensity());
            sceneShader.setUniform("lightSpaceMatrix", dirLight.getLightSpaceMatrix());
            sceneShader.setUniform("shadowBias", DEFAULT_SHADOW_BIAS);

            // Zwiąż teksturę mapy cieni z jednostką 2 (zgodnie z ustawieniem samplera)
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, shadowMapTextureId);
        } else {
            sceneShader.setUniform("dirLight.intensity", 0.0f);
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, 0); // Odwiąż teksturę
        }
    }

    private void setPointLightsUniforms(List<PointLight> pointLights) {
        int numActiveLights = 0;
        if (pointLights != null) {
            numActiveLights = Math.min(pointLights.size(), MAX_POINT_LIGHTS);
            for (int i = 0; i < numActiveLights; i++) {
                PointLight pl = pointLights.get(i);
                if (pl == null) continue;
                String base = "pointLights[" + i + "].";
                sceneShader.setUniform(base + "position", pl.position);
                sceneShader.setUniform(base + "color", pl.color);
                sceneShader.setUniform(base + "intensity", pl.intensity);
                sceneShader.setUniform(base + "att.constant", pl.attenuation.constant);
                sceneShader.setUniform(base + "att.linear", pl.attenuation.linear);
                sceneShader.setUniform(base + "att.quadratic", pl.attenuation.quadratic);
            }
        }
        sceneShader.setUniform("numPointLights", numActiveLights);
    }

    private void setSpotLightsUniforms(List<SpotLight> spotLights) {
        int numActiveLights = 0;
        if (spotLights != null) {
            numActiveLights = Math.min(spotLights.size(), MAX_SPOT_LIGHTS);
            for (int i = 0; i < numActiveLights; i++) {
                SpotLight sl = spotLights.get(i);
                if (sl == null || sl.pointLight == null) continue;
                String base = "spotLights[" + i + "].";
                String plBase = base + "pl.";
                sceneShader.setUniform(plBase + "position", sl.pointLight.position);
                sceneShader.setUniform(plBase + "color", sl.pointLight.color);
                sceneShader.setUniform(plBase + "intensity", sl.pointLight.intensity);
                sceneShader.setUniform(plBase + "att.constant", sl.pointLight.attenuation.constant);
                sceneShader.setUniform(plBase + "att.linear", sl.pointLight.attenuation.linear);
                sceneShader.setUniform(plBase + "att.quadratic", sl.pointLight.attenuation.quadratic);
                sceneShader.setUniform(base + "direction", sl.direction);
                sceneShader.setUniform(base + "cutOffCos", sl.getCutOffCos());
                sceneShader.setUniform(base + "outerCutOffCos", sl.getOuterCutOffCos());
            }
        }
        sceneShader.setUniform("numSpotLights", numActiveLights);
    }

    private void setSamplerUniforms() {
        sceneShader.setUniform("diffuseSampler", Material.DIFFUSE_MAP_TEXTURE_UNIT);   // 0
        sceneShader.setUniform("specularSampler", Material.SPECULAR_MAP_TEXTURE_UNIT); // 1
        sceneShader.setUniform("shadowMapSampler", 2);                                // 2
    }

    private void renderSceneObjects(List<GameObject> gameObjects) {
        for (GameObject go : gameObjects) {
            if (go == null || !go.isVisible() || go.getMesh() == null) {
                continue;
            }

            sceneShader.setUniform("model", go.getModelMatrix());

            Material materialToBind = go.getMaterial() != null ? go.getMaterial() : defaultMaterial;
            materialToBind.bind(sceneShader, defaultTexture); // bind używa defaultTexture jako fallbacku

            go.getMesh().render();
        }
    }

    // SceneRenderer nie zarządza bezpośrednio zasobami GPU (shadery, tekstury),
    // więc jego metoda cleanup jest pusta lub niepotrzebna.
    // Zasoby są zarządzane przez ShaderManager, DefaultResourceManager, ShadowRenderer.
    public void cleanup() {
        System.out.println("  SceneRenderer: Cleanup (no direct GPU resources to clean).");
        // Ewentualnie zerowanie referencji dla pewności
        sceneShader = null;
        defaultTexture = null;
        defaultMaterial = null;
        shadowMapTextureId = -1;
    }
}