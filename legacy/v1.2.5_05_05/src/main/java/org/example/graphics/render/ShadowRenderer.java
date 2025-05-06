package org.example.graphics.render;

import org.example.core.Window;
import org.example.exception.ResourceLoadException;
import org.example.graphics.ShaderProgram;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.shadow.ShadowMap;
import org.example.scene.GameObject;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

/**
 * Odpowiada za przebieg renderowania mapy cieni (Depth Pass).
 * Używa dedykowanego shadera głębi i renderuje scenę
 * z perspektywy światła do obiektu ShadowMap (FBO).
 */
public class ShadowRenderer {

    private ShadowMap shadowMap;
    private final Window window; // Potrzebne do przywrócenia viewportu

    public ShadowRenderer(Window window) {
        if (window == null) throw new IllegalArgumentException("Window cannot be null for ShadowRenderer");
        this.window = window;
    }

    public void init() throws ResourceLoadException {
        System.out.println("  ShadowRenderer: Initializing shadow map...");
        try {
            shadowMap = new ShadowMap();
            System.out.println("    Shadow Map initialized (Texture ID: " + shadowMap.getDepthMapTexture() + ").");
        } catch (Exception e) {
            throw new ResourceLoadException("Failed to create Shadow Map", e);
        }
        System.out.println("  ShadowRenderer: Initialized successfully.");
    }

    /**
     * Wykonuje przebieg generowania mapy cieni dla światła kierunkowego.
     *
     * @param gameObjects Lista obiektów w scenie.
     * @param dirLight Światło kierunkowe rzucające cień (jeśli null, mapa cieni jest tylko czyszczona).
     * @param depthShader Shader programu używany do renderowania głębokości.
     */
    public void render(List<GameObject> gameObjects, DirectionalLight dirLight, ShaderProgram depthShader) {
        if (shadowMap == null || depthShader == null) {
            System.err.println("ShadowRenderer.render(): ShadowMap or DepthShader not initialized. Skipping depth pass.");
            return;
        }

        // Jeśli nie ma światła kierunkowego, tylko wyczyść mapę cieni.
        if (dirLight == null) {
            shadowMap.bindForWriting();
            // glClear(GL_DEPTH_BUFFER_BIT); // Jest już w bindForWriting()
            shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight());
            return;
        }

        shadowMap.bindForWriting(); // Zwiąż FBO mapy cieni i wyczyść

        depthShader.bind();
        depthShader.setUniform("lightSpaceMatrix", dirLight.getLightSpaceMatrix());

        // Renderuj geometrię widocznych obiektów
        for (GameObject go : gameObjects) {
            if (go != null && go.getMesh() != null && go.isVisible()) {
                depthShader.setUniform("model", go.getModelMatrix());
                go.getMesh().render();
            }
        }

        depthShader.unbind();
        shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight()); // Odwiąż FBO i przywróć viewport
    }

    public ShadowMap getShadowMap() {
        if (shadowMap == null) {
            throw new IllegalStateException("ShadowMap accessed before initialization.");
        }
        return shadowMap;
    }

    public int getShadowMapTextureId() {
        return getShadowMap().getDepthMapTexture();
    }

    public void cleanup() {
        System.out.println("  ShadowRenderer: Cleaning up shadow map...");
        if (shadowMap != null) {
            shadowMap.cleanup();
            shadowMap = null;
            System.out.println("    Shadow map cleaned.");
        }
        System.out.println("  ShadowRenderer: Cleanup complete.");
    }
}