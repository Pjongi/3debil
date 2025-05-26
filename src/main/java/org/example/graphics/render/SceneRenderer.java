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
import org.example.scene.GameObjectProperties;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11; // Import dla GL11 explicitnie

import java.util.List;

import static org.lwjgl.opengl.GL11.*; // Ogólne importy GL11
import static org.lwjgl.opengl.GL13.*; // Dla jednostek tekstur
import static org.example.graphics.render.Renderer.MAX_POINT_LIGHTS;
import static org.example.graphics.render.Renderer.MAX_SPOT_LIGHTS;

public class SceneRenderer {

    private final Window window;
    private ShaderProgram sceneShader;
    private Texture defaultTexture;
    private Material defaultMaterial;
    private int shadowMapTextureId = -1;

    private static final float DEFAULT_SHADOW_BIAS = 0.005f;

    public SceneRenderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for SceneRenderer");
        this.window = window;
    }

    public void setupDependencies(ShaderProgram sceneShader, Texture defaultTexture, Material defaultMaterial, int shadowMapTextureId) {
        this.sceneShader = sceneShader;
        this.defaultTexture = defaultTexture;
        this.defaultMaterial = defaultMaterial;
        this.shadowMapTextureId = shadowMapTextureId;

        if (this.sceneShader == null || this.defaultTexture == null || this.defaultMaterial == null) {
            throw new IllegalStateException("SceneRenderer core dependencies (shader, defaultTexture, defaultMaterial) not fully set.");
        }
        System.out.println("  SceneRenderer: Dependencies set.");
    }

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

        // 1. Ustaw viewport i wyczyść bufor ramki.
        // Ustawienie bardzo charakterystycznego koloru tła do testów.
        // Jeśli tło pod GUI jest inne niż ten kolor, to znaczy, że coś dzieje się PÓŹNIEJ.
        GL11.glClearColor(0.0f, 1.0f, 0.0f, 1.0f); // JASNOZIELONY, NIEPRZEZROCZYSTY
        // System.out.println("SCENERENDERER: Clearing screen with GREEN"); // Możesz odkomentować ten log

        // Użyj rozmiarów framebuffera dla viewportu, jeśli są dostępne, inaczej rozmiar okna
        int fbWidth = window.getWidth(); // Domyślnie szerokość logiczna
        int fbHeight = window.getHeight(); // Domyślnie wysokość logiczna
        // Można by dodać metody getFramebufferWidth/Height do klasy Window, jeśli przechowuje te wartości
        // lub pobierać je bezpośrednio z GLFW, ale to wymagałoby przekazania windowHandle lub instancji Window
        // Na razie używamy getWidth()/getHeight(), zakładając, że są to odpowiednie rozmiary dla viewportu.
        // W Window.init() viewport jest ustawiany na podstawie glfwGetFramebufferSize.
        glViewport(0, 0, fbWidth, fbHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        sceneShader.bind();

        // 2. Ustawienie uniformów globalnych
        setCameraUniforms(camera);
        setDirectionalLightUniforms(dirLight);
        setPointLightsUniforms(pointLights);
        setSpotLightsUniforms(spotLights);
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
                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, shadowMapTextureId);
            }
        } else {
            sceneShader.setUniform("dirLight.intensity", 0.0f);
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
        sceneShader.setUniform("diffuseSampler", Material.DIFFUSE_MAP_TEXTURE_UNIT);
        sceneShader.setUniform("specularSampler", Material.SPECULAR_MAP_TEXTURE_UNIT);
        if (shadowMapTextureId != -1) {
            sceneShader.setUniform("shadowMapSampler", 2);
        }
    }

    private void renderSceneObjects(List<GameObject> gameObjects) {
        for (GameObject go : gameObjects) {
            if (go == null || !go.isVisible() || go.getMesh() == null) {
                continue;
            }

            sceneShader.setUniform("model", go.getModelMatrix());

            Material materialToUse = go.getMaterial() != null ? go.getMaterial() : defaultMaterial;
            GameObjectProperties props = go.getProperties();
            Vector3f originalColorCache = null;

            if (props.isHitEffectActive() && materialToUse != null) {
                if (props.getOriginalMaterialDiffuseColor() == null) {
                    props.setOriginalMaterialDiffuseColor(materialToUse.getDiffuseColor());
                }
                originalColorCache = new Vector3f(materialToUse.getDiffuseColor());
                materialToUse.setDiffuseColor(new Vector3f(1.0f, 0.5f, 0.5f));
            } else if (!props.isHitEffectActive() && props.getOriginalMaterialDiffuseColor() != null && materialToUse != null) {
                materialToUse.setDiffuseColor(props.getOriginalMaterialDiffuseColor());
                props.setOriginalMaterialDiffuseColor(null);
            }

            materialToUse.bind(sceneShader, defaultTexture);

            if (originalColorCache != null && props.isHitEffectActive() && materialToUse != null) {
                materialToUse.setDiffuseColor(originalColorCache);
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
    }
}