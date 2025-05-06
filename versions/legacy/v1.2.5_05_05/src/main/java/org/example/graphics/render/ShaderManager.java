package org.example.graphics.render;

import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.ShaderProgram;

import static org.example.graphics.render.Renderer.MAX_POINT_LIGHTS; // Import stałych
import static org.example.graphics.render.Renderer.MAX_SPOT_LIGHTS; // Import stałych

/**
 * Zarządza cyklem życia programów shaderowych używanych w rendererze.
 * Odpowiada za ich ładowanie, kompilację, linkowanie i udostępnianie.
 */
public class ShaderManager {

    private ShaderProgram sceneShaderProgram;
    private ShaderProgram depthShaderProgram;

    public void init() throws ResourceNotFoundException, ResourceLoadException {
        System.out.println("  ShaderManager: Initializing shaders...");
        try {
            initDepthShaderProgram();
            initSceneShaderProgram();
            System.out.println("  ShaderManager: Shaders initialized successfully.");
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            System.err.println("  ShaderManager: Shader initialization failed!");
            cleanup(); // Posprzątaj, co się da
            throw e; // Rzuć dalej
        }
    }

    private void initDepthShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        depthShaderProgram = new ShaderProgram();
        try {
            depthShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_vertex.glsl"));
            depthShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_fragment.glsl"));
            depthShaderProgram.link();
            if (!depthShaderProgram.isLinked()) {
                throw new ResourceLoadException("Depth shader program failed to link.");
            }
            // Utwórz tylko niezbędne uniformy dla tego shadera
            depthShaderProgram.createUniform("model");
            depthShaderProgram.createUniform("lightSpaceMatrix");
            System.out.println("    Depth shader program created and linked.");
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            if (depthShaderProgram != null) depthShaderProgram.cleanup();
            depthShaderProgram = null;
            throw e;
        }
    }

    private void initSceneShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        sceneShaderProgram = new ShaderProgram();
        try {
            sceneShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_vertex.glsl"));
            sceneShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_fragment.glsl"));
            sceneShaderProgram.link();
            if (!sceneShaderProgram.isLinked()) {
                throw new ResourceLoadException("Scene shader program failed to link.");
            }
            createSceneShaderUniforms(); // Tworzenie uniformów po udanym linkowaniu
            System.out.println("    Scene shader program created and linked.");
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
            sceneShaderProgram = null;
            throw e;
        }
    }

    // Metoda pomocnicza do tworzenia uniformów shadera sceny
    private void createSceneShaderUniforms() throws ResourceLoadException {
        // Uniformy transformacji i kamery
        sceneShaderProgram.createUniform("projection");
        sceneShaderProgram.createUniform("view");
        sceneShaderProgram.createUniform("model");
        sceneShaderProgram.createUniform("viewPos");

        // Uniformy światła kierunkowego i cienia
        sceneShaderProgram.createUniform("dirLight.direction");
        sceneShaderProgram.createUniform("dirLight.color");
        sceneShaderProgram.createUniform("dirLight.intensity");
        sceneShaderProgram.createUniform("lightSpaceMatrix");
        sceneShaderProgram.createUniform("shadowMapSampler");
        sceneShaderProgram.createUniform("shadowBias");

        // Uniformy materiału
        sceneShaderProgram.createUniform("material.ambient");
        sceneShaderProgram.createUniform("material.diffuse");
        sceneShaderProgram.createUniform("material.specular");
        sceneShaderProgram.createUniform("material.reflectance");
        sceneShaderProgram.createUniform("material.hasDiffuseMap");
        sceneShaderProgram.createUniform("material.hasSpecularMap");

        // Uniformy samplerów tekstur
        sceneShaderProgram.createUniform("diffuseSampler");
        sceneShaderProgram.createUniform("specularSampler");

        // Uniformy liczby aktywnych świateł
        sceneShaderProgram.createUniform("numPointLights");
        sceneShaderProgram.createUniform("numSpotLights");

        // Uniformy dla tablic świateł punktowych
        for (int i = 0; i < MAX_POINT_LIGHTS; i++) {
            String base = "pointLights[" + i + "].";
            sceneShaderProgram.createUniform(base + "position"); sceneShaderProgram.createUniform(base + "color");
            sceneShaderProgram.createUniform(base + "intensity"); sceneShaderProgram.createUniform(base + "att.constant");
            sceneShaderProgram.createUniform(base + "att.linear"); sceneShaderProgram.createUniform(base + "att.quadratic");
        }
        // Uniformy dla tablic świateł reflektorowych
        for (int i = 0; i < MAX_SPOT_LIGHTS; i++) {
            String base = "spotLights[" + i + "].";
            sceneShaderProgram.createUniform(base + "pl.position"); sceneShaderProgram.createUniform(base + "pl.color");
            sceneShaderProgram.createUniform(base + "pl.intensity"); sceneShaderProgram.createUniform(base + "pl.att.constant");
            sceneShaderProgram.createUniform(base + "pl.att.linear"); sceneShaderProgram.createUniform(base + "pl.att.quadratic");
            sceneShaderProgram.createUniform(base + "direction"); sceneShaderProgram.createUniform(base + "cutOffCos");
            sceneShaderProgram.createUniform(base + "outerCutOffCos");
        }
    }

    public ShaderProgram getSceneShaderProgram() {
        if (sceneShaderProgram == null || !sceneShaderProgram.isLinked()) {
            throw new IllegalStateException("Scene shader program accessed before successful initialization or is not linked.");
        }
        return sceneShaderProgram;
    }

    public ShaderProgram getDepthShaderProgram() {
        if (depthShaderProgram == null || !depthShaderProgram.isLinked()) {
            throw new IllegalStateException("Depth shader program accessed before successful initialization or is not linked.");
        }
        return depthShaderProgram;
    }

    public void cleanup() {
        System.out.println("  ShaderManager: Cleaning up shaders...");
        if (sceneShaderProgram != null) {
            sceneShaderProgram.cleanup();
            sceneShaderProgram = null;
            System.out.println("    Scene shader program cleaned.");
        }
        if (depthShaderProgram != null) {
            depthShaderProgram.cleanup();
            depthShaderProgram = null;
            System.out.println("    Depth shader program cleaned.");
        }
        System.out.println("  ShaderManager: Shader cleanup complete.");
    }

    public boolean areShadersReady() {
        return sceneShaderProgram != null && sceneShaderProgram.isLinked() &&
                depthShaderProgram != null && depthShaderProgram.isLinked();
    }
}