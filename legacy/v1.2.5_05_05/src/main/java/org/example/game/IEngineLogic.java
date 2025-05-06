package org.example.game;

import org.example.audio.AudioManager;
import org.example.core.Input;
import org.example.core.Window;
import org.example.graphics.Camera;
// Usuń lub zakomentuj stary import, jeśli istnieje:
// import org.example.graphics.Renderer;
// Dodaj nowy import:
import org.example.graphics.render.Renderer;

public interface IEngineLogic {

    // Zaktualizuj typ parametru 'renderer'
    void init(Window window, Renderer renderer, AudioManager audioManager) throws Exception;

    void input(Window window, Input input, Camera camera, float deltaTime);
    void update(float deltaTime);

    // Zaktualizuj typ parametru 'renderer'
    void render(Window window, Camera camera, Renderer renderer);

    void cleanup();
}