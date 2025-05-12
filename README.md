# Silnik 3D ebil Engine

## Wprowadzenie

**ebil Engine** to prosty silnik graficzny 3D napisany w języku Java przy użyciu bibliotek LWJGL 3 (Lightweight Java Game Library) oraz JOML (Java OpenGL Math Library). Projekt ten stanowi podstawę do nauki i tworzenia gier oraz aplikacji 3D, koncentrując się na kluczowych aspektach renderowania grafiki czasu rzeczywistego.

Architektura silnika została zaprojektowana z myślą o modularności, oddzielając rdzeń silnika od logiki konkretnej gry poprzez interfejs `IEngineLogic`.

**Aktualna Wersja: 1.2.7** 

## Główne Funkcje

*   **Zarządzanie Oknem i Kontekstem OpenGL:** Inicjalizacja i obsługa okna aplikacji (GLFW) oraz kontekstu graficznego OpenGL (Core Profile 3.3+).
*   **Pętla Gry:** Standardowa implementacja pętli gry (input, update, render).
*   **Obsługa Wejścia:** Śledzenie stanu klawiatury i myszy (`Input.java`), dynamiczne przełączanie callbacków GLFW.
*   **Kamera 3D:** Kamera typu FPS (`Camera`) z możliwością generowania promienia patrzenia.
*   **System Audio:** Podstawowe zarządzanie dźwiękiem przestrzennym przy użyciu OpenAL.
*   **Renderowanie Grafiki:** (Wersja z monolitycznym `org.example.graphics.render.Renderer`)
    *   Ładowanie i renderowanie siatek (Mesh).
    *   Obsługa tekstur 2D (ładowanie z plików obrazów STB).
    *   System materiałów (`Material`).
    *   Ładowanie modeli 3D (Assimp).
*   **Oświetlenie:**
    *   Obsługa światła kierunkowego, punktowych i reflektorowych.
    *   Dynamiczne cienie rzucane przez światło kierunkowe.
*   **Scena i Obiekty Gry:**
    *   `GameObject` i `GameObjectProperties`.
    *   Interakcja z obiektami oparta na raycastingu (niszczenie obiektów).
*   **Interfejs Użytkownika (GUI):**
    *   Integracja z biblioteką **Nuklear** (`NuklearGui.java`).
    *   **Menu Pauzy:** Aktywowane klawiszem `ESC`, pozwala na wznowienie gry lub jej zamknięcie. Kursor myszy jest zarządzany odpowiednio do stanu pauzy.
*   **Zarządzanie Zasobami:** Ładowanie zasobów z classpath (`ResourceLoader`).
*   **Obsługa Błędów:** Dedykowane wyjątki.

*(Szczegółowy dziennik zmian znajduje się w pliku CHANGELOG.md)*

## Struktura Projektu

Projekt jest zorganizowany w następujące główne pakiety:

*   `org.example.core`: Rdzeń silnika (Engine, Window, Input, Timer).
*   `org.example.audio`: System dźwięku.
*   `org.example.graphics`: Podstawowe komponenty graficzne (Camera, Mesh, Material, Texture, ShaderProgram, Renderer, light/, shadow/).
    *   **`org.example.graphics.render`**: (To była pomyłka, używamy `org.example.graphics.Renderer` na tym etapie)
*   `org.example.scene`: Elementy sceny.
*   `org.example.util`: Klasy narzędziowe.
*   `org.example.ui`: Komponenty interfejsu użytkownika (**`NuklearGui.java`**).
*   `org.example.game`: Interfejs logiki gry (`IEngineLogic`) oraz `DemoGame`.
*   `org.example.exception`: Niestandardowe wyjątki.

Zasoby (czcionki, shadery, tekstury, modele, dźwięki) znajdują się w katalogu `src/main/resources/`.
Wersje archiwalne silnika będą dostępne w folderze `/legacy/`.

## Wymagania Systemowe i Konfiguracja
* Java Development Kit (JDK): Wersja 11 lub nowsza (zalecana 17+).
* Maven: Projekt wykorzystuje Maven do zarządzania zależnościami i budowania.
* System Operacyjny: Windows, Linux lub macOS. Należy pamiętać o odpowiedniej konfiguracji natywnych bibliotek LWJGL w pliku pom.xml dla docelowego systemu.

### Zależności (Maven `pom.xml`)
*   LWJGL (Core, GLFW, OpenGL, OpenAL, STB, Assimp, **Nuklear**)
*   JOML

## Sterowanie w Demo

*   **W, A, S, D:** Poruszanie się kamerą.
*   **Spacja:** Kamera w górę.
*   **Lewy Shift:** Kamera w dół.
*   **Mysz:** Rozglądanie się (gdy gra nie jest zapauzowana).
*   **ESC:** Pauza / Wznowienie gry (otwiera/zamyka menu pauzy).
    *   **Mysz w menu:** Wybór opcji.
*   **F:** Interakcja / Atak (na obiekt, na który patrzysz, gdy gra nie jest zapauzowana).
*   **T:** Włącz/Wyłącz latarkę (gdy gra nie jest zapauzowana).
*   **B:** (Testowo) Przełącz widoczność obiektu "BunnyStatue" (gdy gra nie jest zapauzowana).

## Potencjalne Rozszerzenia i Przyszłe Prace

*   **Refaktoryzacja systemu renderowania**: Podział klasy `org.example.graphics.Renderer` na mniejsze komponenty.
*   **Ulepszenie zarządzania callbackami GLFW** między `Input` a `NuklearGui` (np. poprzez system zdarzeń lub centralny menedżer inputu).
*   Rozbudowa menu pauzy o dodatkowe opcje (np. ustawienia grafiki, dźwięku).
*   Implementacja graficznego celownika 2D.
*   Cienie dla świateł punktowych i reflektorowych.

## Autor
*Pjongi*

## Licencja

---