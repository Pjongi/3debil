// src/main/java/org/example/Main.java
package org.example;

import org.example.core.Engine;
import org.example.game.DemoGame;
import org.example.game.IEngineLogic;

// Główny punkt wejścia aplikacji. Tworzy logikę gry i uruchamia silnik.
public class Main {

    public static void main(String[] args) {
        try {
            // Utwórz instancję konkretnej logiki gry (naszego dema)
            IEngineLogic gameLogic = new DemoGame();

            // Utwórz instancję silnika, przekazując tytuł okna, rozmiar i logikę gry
            Engine gameEng = new Engine("3D ebil Engine - Demo", 1280, 720, gameLogic);

            // Uruchom silnik (rozpocznie pętlę gry)
            gameEng.run();

        } catch (Exception e) {
            System.err.println("Failed to start the engine:");
            e.printStackTrace();
            System.exit(-1); // Zakończ z błędem
        }
    }
}