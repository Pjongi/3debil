package org.example.game;

import org.example.audio.AudioManager; // Zmieniono typ z powrotem
import org.example.core.Input;
import org.example.core.Window;
import org.example.graphics.Camera;
import org.example.graphics.Renderer;

public interface IEngineLogic {

    // Zmieniono typ AudioManager z powrotem
    void init(Window window, Renderer renderer, AudioManager audioManager) throws Exception;

    void input(Window window, Input input, Camera camera, float deltaTime);
    void update(float deltaTime);
    void render(Window window, Camera camera, Renderer renderer);
    void cleanup();
}