# Silnik 3D ebil Engine

## Wprowadzenie

**ebil Engine** to prosty silnik graficzny 3D napisany w języku Java przy użyciu bibliotek LWJGL 3 (Lightweight Java Game Library) oraz JOML (Java OpenGL Math Library). Projekt ten stanowi podstawę do nauki i tworzenia gier oraz aplikacji 3D, koncentrując się na kluczowych aspektach renderowania grafiki czasu rzeczywistego.

Architektura silnika została zaprojektowana z myślą o modularności, oddzielając rdzeń silnika od logiki konkretnej gry poprzez interfejs `IEngineLogic`.

**Aktualna Wersja: 1.2.6** (z dnia: RRRR-MM-DD - Zastąp datą)

## Główne Funkcje

*   **Zarządzanie Oknem i Kontekstem OpenGL:** Inicjalizacja i obsługa okna aplikacji (GLFW) oraz kontekstu graficznego OpenGL (Core Profile 3.3+).
*   **Pętla Gry:** Standardowa implementacja pętli gry (input, update, render).
*   **Obsługa Wejścia:** Śledzenie stanu klawiatury i myszy.
*   **Kamera 3D:** Kamera typu FPS z możliwością poruszania się i rozglądania.
    *   Generowanie promienia patrzenia (`getRayOrigin()`, `getRayDirection()`) na potrzeby raycastingu.
*   **System Audio:** Podstawowe zarządzanie dźwiękiem przestrzennym przy użyciu OpenAL (ładowanie plików WAV, źródła dźwięku, słuchacz).
*   **Renderowanie Grafiki:** (Stan przed refaktoryzacją na podkomponenty renderujące)
    *   Ładowanie i renderowanie siatek (Mesh) z obsługą VAO, VBO, EBO.
    *   Obsługa tekstur 2D (ładowanie z plików obrazów przy użyciu STB).
    *   System materiałów definiujący właściwości powierzchni obiektów.
    *   Ładowanie modeli 3D z popularnych formatów (np. `.obj`) przy użyciu biblioteki Assimp.
*   **Oświetlenie:**
    *   Obsługa wielu źródeł światła:
        *   Światło kierunkowe (Directional Light).
        *   Światła punktowe (Point Light) z tłumieniem (attenuation).
        *   Światła reflektorowe (Spot Light) z tłumieniem i stożkiem.
    *   Dynamiczne cienie rzucane przez światło kierunkowe (Shadow Mapping z teksturą 2D, zarządzane przez klasę `org.example.graphics.Renderer`).
*   **Scena i Obiekty Gry:**
    *   Struktura `GameObject` agregująca siatkę, materiał i transformację.
        *   Metoda `intersectsRay()` do sprawdzania przecięcia z promieniem (bounding sphere).
    *   `GameObjectProperties` do przechowywania dodatkowych atrybutów obiektów (np. typ, możliwość zniszczenia, punkty życia, `canBeTargeted`) z wykorzystaniem wzorca Budowniczego.
    *   **Interakcja z obiektami oparta na raycastingu:** Możliwość uszkadzania obiektów, na które bezpośrednio patrzy gracz.
*   **Zarządzanie Zasobami:**
    *   Ładowanie zasobów (tekstury, modele, dźwięki) z classpath.
    *   Narzędzia pomocnicze do tworzenia prostych siatek i parsowania plików WAV.
*   **Obsługa Błędów:** Dedykowane wyjątki dla problemów z ładowaniem zasobów.

*(Szczegółowy dziennik zmian znajduje się w pliku CHANGELOG.md)*

## Struktura Projektu

Projekt jest zorganizowany w następujące główne pakiety:

*   `org.example.core`: Rdzeń silnika (Engine, Window, Input, Timer).
*   `org.example.audio`: System dźwięku (AudioManager, SoundSource, Listener).
*   `org.example.graphics`: Podstawowe komponenty graficzne (Camera, Mesh, Material, Texture, ShaderProgram, Renderer, light/, shadow/).
*   `org.example.scene`: Elementy sceny (GameObject, GameObjectProperties).
*   `org.example.util`: Klasy narzędziowe (ResourceLoader, ModelLoader, MeshLoader, WavLoader).
*   `org.example.game`: Interfejs logiki gry (`IEngineLogic`) oraz przykładowa implementacja (`DemoGame`).
*   `org.example.exception`: Niestandardowe wyjątki.

Zasoby (shadery, tekstury, modele, dźwięki) znajdują się w katalogu `src/main/resources/`.
Wersje archiwalne silnika będą dostępne w folderze `/legacy/` (jeśli zdecydujesz się go tak utrzymywać).

## Wymagania Systemowe i Konfiguracja

*   **Java Development Kit (JDK):** Wersja 11 lub nowsza (zalecana 17+).
*   **Maven:** Projekt wykorzystuje Maven do zarządzania zależnościami i budowania.
*   **System Operacyjny:** Windows, Linux lub macOS. Należy pamiętać o odpowiedniej konfiguracji natywnych bibliotek LWJGL w pliku `pom.xml` dla docelowego systemu.

### Zależności (Maven `pom.xml`)

Główne zależności to:
*   LWJGL (Core, GLFW, OpenGL, OpenAL, STB, Assimp)
*   JOML (Java OpenGL Math Library)


## Sterowanie w Demo

*   **W, A, S, D:** Poruszanie się kamerą.
*   **Spacja:** Kamera w górę.
*   **Lewy Shift:** Kamera w dół.
*   **Mysz:** Rozglądanie się.
*   **ESC:** Zamyka aplikację.
*   **F:** Interakcja / Atak (na obiekt, na który patrzysz).
*   **T:** Włącz/Wyłącz latarkę.
*   **B:** (Testowo) Przełącz widoczność obiektu "BunnyStatue".

## Potencjalne Rozszerzenia i Przyszłe Prace

*   **Implementacja menu pauzy:** Umożliwienie pauzowania gry i wyboru opcji.
*   **Refaktoryzacja systemu renderowania:** Podział klasy `Renderer` na mniejsze, wyspecjalizowane komponenty.
*   Implementacja bardziej zaawansowanego systemu materiałów (np. PBR).
*   Dodanie graficznego interfejsu użytkownika (GUI) dla menu i HUD.
*   Cienie dla świateł punktowych i reflektorowych.
*   Optymalizacja oświetlenia.
*   Wprowadzenie systemu fizyki.
*   Zaawansowane techniki renderowania (Normal Mapping, Post-processing, Skybox).
*   Rozbudowa systemu zarządzania sceną.

## Autor

*   Pjongi

## Licencja


---