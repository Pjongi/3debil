package org.example.game;

import org.example.audio.AudioManager;
import org.example.core.Window;
import org.example.graphics.Camera;
import org.example.graphics.render.Renderer;
import org.example.ui.NuklearGui; // Dodaj ten import
import org.example.core.Input;   // Upewnij się, że ten import jest, jeśli go używasz w sygnaturach

public interface IEngineLogic {

    // Zaktualizowana sygnatura metody init
    void init(Window window, Renderer renderer, AudioManager audioManager, NuklearGui nuklearGui) throws Exception;

    void input(Window window, Input input, Camera camera, float deltaTime);
    void update(float deltaTime);
    void render(Window window, Camera camera, Renderer renderer);
    void cleanup();
}