// src/main/java/org/example/game/IEngineLogic.java
package org.example.game;

import org.example.core.Input;
import org.example.core.Window;
import org.example.graphics.Camera;
import org.example.graphics.Renderer;

// Interfejs definiujący metody, które musi zaimplementować
// logika konkretnej gry lub dema, aby mogła być uruchomiona przez silnik.
public interface IEngineLogic {

    // Wywoływana raz na początku, po inicjalizacji silnika, okna i renderera.
    // Służy do ładowania zasobów gry (modele, tekstury), tworzenia obiektów sceny.
    void init(Window window, Renderer renderer) throws Exception;

    // Wywoływana w każdej klatce pętli gry.
    // Służy do przetwarzania wejścia specyficznego dla gry (np. akcje gracza, sterowanie kamerą).
    void input(Window window, Input input, Camera camera, float deltaTime);

    // Wywoływana w każdej klatce pętli gry po przetworzeniu wejścia.
    // Służy do aktualizacji stanu gry (np. fizyka, AI, animacje).
    void update(float deltaTime);

    // Wywoływana w każdej klatce pętli gry po aktualizacji logiki.
    // Służy do przygotowania i wywołania renderowania sceny gry.
    // Implementacja powinna zazwyczaj wywołać renderer.render(...) przekazując
    // obiekty i światła zarządzane przez logikę gry.
    void render(Window window, Camera camera, Renderer renderer);

    // Wywoływana raz na końcu, przed zamknięciem silnika.
    // Służy do zwolnienia zasobów specyficznych dla gry (np. meshe, tekstury).
    void cleanup();
}