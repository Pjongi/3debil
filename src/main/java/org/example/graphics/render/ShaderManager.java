package org.example.graphics.render;

import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.graphics.ShaderProgram;

import static org.example.graphics.render.Renderer.MAX_POINT_LIGHTS;
import static org.example.graphics.render.Renderer.MAX_SPOT_LIGHTS;

public class ShaderManager {

    private ShaderProgram sceneShaderProgram;
    private ShaderProgram depthShaderProgram; // Dla cieni kierunkowych (2D texture)
    private ShaderProgram spotLightDepthShaderProgram; // Dla cieni reflektorowych/punktowych (cube map)

    public void init() throws ResourceNotFoundException, ResourceLoadException {
        System.out.println("  ShaderManager: Initializing shaders...");
        try {
            initDepthShaderProgram();
            initSpotLightDepthShaderProgram(); // Dodano inicjalizację nowego shadera
            initSceneShaderProgram();
            System.out.println("  ShaderManager: Shaders initialized successfully.");
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            System.err.println("  ShaderManager: Shader initialization failed!");
            cleanup();
            throw e;
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
            depthShaderProgram.createUniform("model");
            depthShaderProgram.createUniform("lightSpaceMatrix");
            System.out.println("    Directional Depth shader program created and linked.");
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            if (depthShaderProgram != null) depthShaderProgram.cleanup();
            depthShaderProgram = null;
            throw e;
        }
    }

    private void initSpotLightDepthShaderProgram() throws ResourceNotFoundException, ResourceLoadException {
        spotLightDepthShaderProgram = new ShaderProgram();
        try {
            // Upewnij się, że te pliki istnieją i zawierają odpowiedni kod
            spotLightDepthShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/spotlight_depth_vertex.glsl"));
            spotLightDepthShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/spotlight_depth_fragment.glsl"));spotLightDepthShaderProgram.link();
            if (!spotLightDepthShaderProgram.isLinked()) {
                throw new ResourceLoadException("SpotLight Depth shader program failed to link.");
            }
            // Uniformy potrzebne dla shadera głębi światła punktowego/reflektorowego
            spotLightDepthShaderProgram.createUniform("model");
            spotLightDepthShaderProgram.createUniform("lightSpaceMatrix"); // Dla każdej z 6 macierzy widoku cube mapy

            // Opcjonalne uniformy, jeśli używasz linearyzacji głębi w shaderze
            // spotLightDepthShaderProgram.createUniform("lightPos");
            // spotLightDepthShaderProgram.createUniform("far_plane");

            System.out.println("    SpotLight Depth shader program created and linked.");
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            if (spotLightDepthShaderProgram != null) spotLightDepthShaderProgram.cleanup();
            spotLightDepthShaderProgram = null;
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
            createSceneShaderUniforms();
            System.out.println("    Scene shader program created and linked.");
        } catch (ResourceNotFoundException | ResourceLoadException e) {
            if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
            sceneShaderProgram = null;
            throw e;
        }
    }

    private void createSceneShaderUniforms() throws ResourceLoadException {
        // Transformacje i kamera
        sceneShaderProgram.createUniform("projection");
        sceneShaderProgram.createUniform("view");
        sceneShaderProgram.createUniform("model");
        sceneShaderProgram.createUniform("viewPos");

        // Materiał
        sceneShaderProgram.createUniform("material.ambient");
        sceneShaderProgram.createUniform("material.diffuse");
        sceneShaderProgram.createUniform("material.specular");
        sceneShaderProgram.createUniform("material.reflectance");
        sceneShaderProgram.createUniform("material.hasDiffuseMap");
        sceneShaderProgram.createUniform("material.hasSpecularMap");
        sceneShaderProgram.createUniform("diffuseSampler");    // Jednostka 0
        sceneShaderProgram.createUniform("specularSampler");  // Jednostka 1

        // Światło kierunkowe i jego cień (2D Texture)
        sceneShaderProgram.createUniform("dirLight.direction");
        sceneShaderProgram.createUniform("dirLight.color");
        sceneShaderProgram.createUniform("dirLight.intensity");
        sceneShaderProgram.createUniform("lightSpaceMatrix"); // Dla cienia kierunkowego
        sceneShaderProgram.createUniform("shadowMapSampler"); // sampler2D dla cienia kierunkowego (jednostka 2)
        sceneShaderProgram.createUniform("shadowBias");

        // Światła punktowe
        sceneShaderProgram.createUniform("numPointLights");
        for (int i = 0; i < MAX_POINT_LIGHTS; i++) {
            String base = "pointLights[" + i + "].";
            sceneShaderProgram.createUniform(base + "position");
            sceneShaderProgram.createUniform(base + "color");
            sceneShaderProgram.createUniform(base + "intensity");
            sceneShaderProgram.createUniform(base + "att.constant");
            sceneShaderProgram.createUniform(base + "att.linear");
            sceneShaderProgram.createUniform(base + "att.quadratic");
        }

        // Światła reflektorowe
        sceneShaderProgram.createUniform("numSpotLights");
        for (int i = 0; i < MAX_SPOT_LIGHTS; i++) {
            String base = "spotLights[" + i + "].";
            // Właściwości PointLight wewnętrznego reflektora
            sceneShaderProgram.createUniform(base + "pl.position");
            sceneShaderProgram.createUniform(base + "pl.color");
            sceneShaderProgram.createUniform(base + "pl.intensity");
            sceneShaderProgram.createUniform(base + "pl.att.constant");
            sceneShaderProgram.createUniform(base + "pl.att.linear");
            sceneShaderProgram.createUniform(base + "pl.att.quadratic");
            // Właściwości specyficzne dla SpotLight
            sceneShaderProgram.createUniform(base + "direction");
            sceneShaderProgram.createUniform(base + "cutOffCos");
            sceneShaderProgram.createUniform(base + "outerCutOffCos");

            // Uniformy dla cieni reflektorowych (Cube Map) - jeśli Twój shader sceny je obsługuje
            // Nazwy mogą się różnić w zależności od implementacji shadera
            // sceneShaderProgram.createUniform(base + "shadowCubeMap"); // samplerCube (np. jednostka 3 + i)
            // sceneShaderProgram.createUniform(base + "farPlane");
            // sceneShaderProgram.createUniform(base + "shadowOn"); // bool/int do włączania/wyłączania cienia dla danego światła
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
            throw new IllegalStateException("Directional Depth shader program accessed before successful initialization or is not linked.");
        }
        return depthShaderProgram;
    }

    public ShaderProgram getSpotLightDepthShaderProgram() {
        if (spotLightDepthShaderProgram == null || !spotLightDepthShaderProgram.isLinked()) {
            throw new IllegalStateException("SpotLight Depth shader program accessed before successful initialization or is not linked.");
        }
        return spotLightDepthShaderProgram;
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
            System.out.println("    Directional Depth shader program cleaned.");
        }
        if (spotLightDepthShaderProgram != null) {
            spotLightDepthShaderProgram.cleanup();
            spotLightDepthShaderProgram = null;
            System.out.println("    SpotLight Depth shader program cleaned.");
        }
        System.out.println("  ShaderManager: Shader cleanup complete.");
    }

    public boolean areShadersReady() {
        return sceneShaderProgram != null && sceneShaderProgram.isLinked() &&
                depthShaderProgram != null && depthShaderProgram.isLinked() &&
                spotLightDepthShaderProgram != null && spotLightDepthShaderProgram.isLinked();
    }
}