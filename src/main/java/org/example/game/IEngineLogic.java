package org.example.game;

import org.example.audio.AudioManager;
import org.example.core.Input;
import org.example.core.Window;
import org.example.graphics.Camera;
// Zmień ten import, aby wskazywał na nowy Renderer
import org.example.graphics.render.Renderer; // <--- TUTAJ ZMIANA

public interface IEngineLogic {

    void init(Window window, Renderer renderer, AudioManager audioManager) throws Exception; // Typ Renderer musi być spójny

    void input(Window window, Input input, Camera camera, float deltaTime);
    void update(float deltaTime);

    // Upewnij się, że typ Renderer jest poprawny
    void render(Window window, Camera camera, Renderer renderer); // <--- TUTAJ ZMIANA (jeśli była inna)

    void cleanup();
}