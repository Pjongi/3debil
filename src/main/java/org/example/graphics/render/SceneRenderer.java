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
import org.example.scene.GameObjectProperties; // Dodano, jeśli używasz do efektów
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.example.graphics.render.Renderer.MAX_POINT_LIGHTS;
import static org.example.graphics.render.Renderer.MAX_SPOT_LIGHTS;

public class SceneRenderer {

    private final Window window;
    private ShaderProgram sceneShader;
    private Texture defaultTexture;
    private Material defaultMaterial;
    private int shadowMapTextureId = -1; // Dla cieni kierunkowych

    // Opcjonalnie, jeśli integrujesz cienie reflektorowe bezpośrednio tutaj
    // private int spotLightShadowCubeMapTextureId = -1;
    // private float spotLightShadowFarPlane = 30.0f; // Domyślna wartość

    private static final float DEFAULT_SHADOW_BIAS = 0.005f;

    public SceneRenderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for SceneRenderer");
        this.window = window;
    }

    // Główna metoda konfiguracji zależności
    public void setupDependencies(ShaderProgram sceneShader, Texture defaultTexture, Material defaultMaterial, int shadowMapTextureId) {
        this.sceneShader = sceneShader;
        this.defaultTexture = defaultTexture;
        this.defaultMaterial = defaultMaterial;
        this.shadowMapTextureId = shadowMapTextureId;

        if (this.sceneShader == null || this.defaultTexture == null || this.defaultMaterial == null /* shadowMapTextureId może być -1, jeśli nie ma cieni */) {
            // Złagodzono warunek dla shadowMapTextureId, ponieważ cienie mogą być opcjonalne
            if (this.sceneShader == null || this.defaultTexture == null || this.defaultMaterial == null) {
                throw new IllegalStateException("SceneRenderer core dependencies (shader, defaultTexture, defaultMaterial) not fully set.");
            }
        }
        System.out.println("  SceneRenderer: Dependencies set.");
    }

    // Opcjonalna metoda do ustawienia tekstury cieni reflektorowych, jeśli shader sceny ich używa
    /*
    public void setSpotLightShadowMapTexture(int cubeMapTextureId, float farPlane) {
        this.spotLightShadowCubeMapTextureId = cubeMapTextureId;
        this.spotLightShadowFarPlane = farPlane;
    }
    */

    public void render(Camera camera, List<GameObject> gameObjects,
                       DirectionalLight dirLight, List<PointLight> pointLights, List<SpotLight> spotLights) {

        if (sceneShader == null || defaultTexture == null || defaultMaterial == null) {
            System.err.println("SceneRenderer.render(): Core dependencies not set. Skipping scene pass.");
            return;
        }
        if (camera == null || gameObjects == null) {
            System.err.println("SceneRenderer.render(): Camera or gameObjects list is null. Skipping scene pass.");
            return;
        }

        // 1. Ustaw viewport na rozmiar okna i wyczyść bufor ramki
        // Kolor czyszczenia jest ustawiany globalnie w Renderer.setupOpenGLState()
        // lub może być ustawiony tutaj, jeśli chcesz inny kolor tła dla tej konkretnej sceny.
        // glClearColor(0.1f, 0.1f, 0.15f, 1.0f); // Przykład - jeśli chcesz nadpisać globalny
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // <--- KLUCZOWE CZYSZCZENIE EKRANU

        sceneShader.bind();

        // 2. Ustawienie uniformów globalnych (per-frame)
        setCameraUniforms(camera);
        setDirectionalLightUniforms(dirLight); // Uwzględnia shadowMapTextureId
        setPointLightsUniforms(pointLights);
        setSpotLightsUniforms(spotLights);     // Obecnie bez obsługi cieni reflektorowych w tym shaderze
        setSamplerUniforms();

        // 3. Renderowanie obiektów sceny
        renderSceneObjects(gameObjects);

        sceneShader.unbind();
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

            if (shadowMapTextureId != -1) {
                glActiveTexture(GL_TEXTURE2); // Jednostka tekstury dla mapy cieni (np. 2)
                glBindTexture(GL_TEXTURE_2D, shadowMapTextureId);
            }
        } else {
            sceneShader.setUniform("dirLight.intensity", 0.0f);
            // Opcjonalnie odwiąż teksturę, jeśli była wcześniej związana
            // glActiveTexture(GL_TEXTURE2);
            // glBindTexture(GL_TEXTURE_2D, 0);
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
                String plBase = base + "pl."; // Struktura zagnieżdżona w shaderze
                sceneShader.setUniform(plBase + "position", sl.pointLight.position);
                sceneShader.setUniform(plBase + "color", sl.pointLight.color);
                sceneShader.setUniform(plBase + "intensity", sl.pointLight.intensity);
                sceneShader.setUniform(plBase + "att.constant", sl.pointLight.attenuation.constant);
                sceneShader.setUniform(plBase + "att.linear", sl.pointLight.attenuation.linear);
                sceneShader.setUniform(plBase + "att.quadratic", sl.pointLight.attenuation.quadratic);
                sceneShader.setUniform(base + "direction", sl.direction);
                sceneShader.setUniform(base + "cutOffCos", sl.getCutOffCos());
                sceneShader.setUniform(base + "outerCutOffCos", sl.getOuterCutOffCos());

                // Jeśli shader obsługuje cienie reflektorowe:
                /*
                if (spotLightShadowCubeMapTextureId != -1 && i == 0) { // Załóżmy, że tylko pierwszy reflektor rzuca cień dla uproszczenia
                    glActiveTexture(GL_TEXTURE3); // Inna jednostka tekstury
                    glBindTexture(GL_TEXTURE_CUBE_MAP, spotLightShadowCubeMapTextureId);
                    sceneShader.setUniform("spotLightShadowMap", 3); // Sampler cube mapy
                    sceneShader.setUniform("spotLightFarPlane", spotLightShadowFarPlane);
                    sceneShader.setUniform("spotLightPos", sl.pointLight.position); // Pozycja światła potrzebna do obliczeń w shaderze
                }
                */
            }
        }
        sceneShader.setUniform("numSpotLights", numActiveLights);
    }

    private void setSamplerUniforms() {
        sceneShader.setUniform("diffuseSampler", Material.DIFFUSE_MAP_TEXTURE_UNIT);   // 0
        sceneShader.setUniform("specularSampler", Material.SPECULAR_MAP_TEXTURE_UNIT); // 1
        if (shadowMapTextureId != -1) { // Ustawiaj sampler tylko jeśli mapa cieni jest używana
            sceneShader.setUniform("shadowMapSampler", 2); // Jednostka tekstury 2 dla cieni kierunkowych
        }
        // Jeśli używasz cieni reflektorowych, ustaw odpowiedni sampler:
        // if (spotLightShadowCubeMapTextureId != -1) {
        //     sceneShader.setUniform("spotLightShadowMap", 3); // Np. jednostka 3
        // }
    }

    private void renderSceneObjects(List<GameObject> gameObjects) {
        for (GameObject go : gameObjects) {
            if (go == null || !go.isVisible() || go.getMesh() == null) {
                continue;
            }

            sceneShader.setUniform("model", go.getModelMatrix());

            Material materialToUse = go.getMaterial() != null ? go.getMaterial() : defaultMaterial;
            GameObjectProperties props = go.getProperties();

            // Logika dla efektu wizualnego trafienia (zmiana koloru)
            // To jest prosta implementacja; może wymagać dostosowania, jeśli materiały są współdzielone.
            Vector3f originalColor = null;
            if (props.isHitEffectActive() && materialToUse != null) {
                if (props.getOriginalMaterialDiffuseColor() == null) {
                    props.setOriginalMaterialDiffuseColor(materialToUse.getDiffuseColor());
                }
                originalColor = new Vector3f(materialToUse.getDiffuseColor()); // Zapisz aktualny
                // Ustaw kolor "błysku" - można go parametryzować
                materialToUse.setDiffuseColor(new Vector3f(1.0f, 0.6f, 0.6f)); // Jasnoczerwony błysk
            }

            materialToUse.bind(sceneShader, defaultTexture);

            // Przywróć oryginalny kolor materiału PO związaniu i wysłaniu uniformów,
            // aby nie wpłynąć na inne obiekty współdzielące ten sam materiał.
            if (originalColor != null && materialToUse != null && props.getOriginalMaterialDiffuseColor() != null) {
                // Jeśli efekt się skończył w tej klatce (po updateHitEffect, a przed renderowaniem tego obiektu)
                if (!props.isHitEffectActive()) {
                    materialToUse.setDiffuseColor(props.getOriginalMaterialDiffuseColor());
                    props.setOriginalMaterialDiffuseColor(null); // Wyczyść
                } else {
                    // Efekt jest nadal aktywny, ale musimy przywrócić kolor na wypadek współdzielenia materiału
                    materialToUse.setDiffuseColor(originalColor);
                }
            } else if (!props.isHitEffectActive() && props.getOriginalMaterialDiffuseColor() != null && materialToUse != null) {
                // Ten przypadek obsługuje sytuację, gdy efekt skończył się w poprzedniej klatce,
                // a `originalColor` nie został ustawiony w tej iteracji pętli.
                materialToUse.setDiffuseColor(props.getOriginalMaterialDiffuseColor());
                props.setOriginalMaterialDiffuseColor(null);
            }


            go.getMesh().render();
        }
    }

    public void cleanup() {
        System.out.println("  SceneRenderer: Cleanup (no direct GPU resources to clean, owned by other managers).");
        sceneShader = null;
        defaultTexture = null;
        defaultMaterial = null;
        shadowMapTextureId = -1;
        // spotLightShadowCubeMapTextureId = -1;
    }
}