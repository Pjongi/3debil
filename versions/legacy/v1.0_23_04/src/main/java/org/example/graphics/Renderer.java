package org.example.graphics;

import org.example.core.Window;
import org.example.graphics.light.DirectionalLight;
import org.example.graphics.shadow.ShadowMap;
import org.example.scene.GameObject;
import org.joml.Matrix4f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class Renderer {

    private ShaderProgram sceneShaderProgram;
    private ShaderProgram depthShaderProgram;
    private ShadowMap shadowMap;
    private final Window window;

    public Renderer(Window window) {
        this.window = window;
    }

    public void init() throws Exception {
        shadowMap = new ShadowMap();

        sceneShaderProgram = new ShaderProgram();
        sceneShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_vertex.glsl"));
        sceneShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/scene_fragment.glsl"));
        sceneShaderProgram.link();

        sceneShaderProgram.createUniform("projection");
        sceneShaderProgram.createUniform("view");
        sceneShaderProgram.createUniform("model");
        sceneShaderProgram.createUniform("objectColor");
        sceneShaderProgram.createUniform("viewPos");
        sceneShaderProgram.createUniform("dirLight.direction");
        sceneShaderProgram.createUniform("dirLight.color");
        sceneShaderProgram.createUniform("dirLight.intensity");
        sceneShaderProgram.createUniform("lightSpaceMatrix");
        sceneShaderProgram.createUniform("shadowMap");
        sceneShaderProgram.createUniform("shadowBias");

        depthShaderProgram = new ShaderProgram();
        depthShaderProgram.createVertexShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_vertex.glsl"));
        depthShaderProgram.createFragmentShader(ShaderProgram.loadShaderSource("src/main/resources/shaders/depth_fragment.glsl"));
        depthShaderProgram.link();

        depthShaderProgram.createUniform("lightSpaceMatrix");
        depthShaderProgram.createUniform("model");

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    public void render(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        renderDepthMap(gameObjects, light);
        renderScene(camera, gameObjects, light);
    }

    private void renderDepthMap(List<GameObject> gameObjects, DirectionalLight light) {
        shadowMap.bindForWriting();
        depthShaderProgram.bind();

        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        depthShaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix);

        for (GameObject go : gameObjects) {
            depthShaderProgram.setUniform("model", go.getModelMatrix());
            go.getMesh().render();
        }

        depthShaderProgram.unbind();
        shadowMap.unbindAfterWriting(window.getWidth(), window.getHeight());
    }

    private void renderScene(Camera camera, List<GameObject> gameObjects, DirectionalLight light) {
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        sceneShaderProgram.bind();

        sceneShaderProgram.setUniform("projection", camera.getProjectionMatrix((float)window.getWidth() / window.getHeight()));
        sceneShaderProgram.setUniform("view", camera.getViewMatrix());
        sceneShaderProgram.setUniform("viewPos", camera.getPosition());

        sceneShaderProgram.setUniform("dirLight.direction", light.getDirection());
        sceneShaderProgram.setUniform("dirLight.color", light.getColor());
        sceneShaderProgram.setUniform("dirLight.intensity", light.getIntensity());

        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        sceneShaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix);
        sceneShaderProgram.setUniform("shadowBias", 0.005f);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthMapTexture());
        sceneShaderProgram.setUniform("shadowMap", 0);

        for (GameObject go : gameObjects) {
            sceneShaderProgram.setUniform("model", go.getModelMatrix());
            sceneShaderProgram.setUniform("objectColor", go.getColor());
            go.getMesh().render();
        }

        sceneShaderProgram.unbind();
    }

    public void cleanup() {
        if (sceneShaderProgram != null) sceneShaderProgram.cleanup();
        if (depthShaderProgram != null) depthShaderProgram.cleanup();
        if (shadowMap != null) shadowMap.cleanup();
    }
}