# Dziennik Zmian (Changelog) - Silnik 3D ebil Engine

Wszystkie znaczące zmiany w tym projekcie będą dokumentowane w tym pliku.
Format opiera się na [Keep a Changelog](https://keepachangelog.com/pl/1.0.0/).

## [Nieopublikowane] - Wersja Rozwojowa

### Planowane / W trakcie

*   Refaktoryzacja systemu renderowania na komponenty (`ShaderManager`, `SceneRenderer` itp.).
*   Implementacja cieni dla świateł reflektorowych (SpotLight) przy użyciu cube map.
*   Implementacja graficznego celownika 2D.
*   Ulepszenie zarządzania callbackami GLFW między `Input` a `NuklearGui`.

## [1.2.7] - 2025-05-09

### Dodano

*   **Integracja biblioteki GUI Nuklear:**
    *   Do projektu dodano zależność Maven dla `lwjgl-nuklear`.
    *   Stworzono klasę `NuklearGui.java` (`org.example.ui`) odpowiedzialną za inicjalizację kontekstu Nuklear, ładowanie czcionek, konfigurację shaderów i buforów OpenGL dla Nukleara oraz renderowanie interfejsu.
    *   `NuklearGui` zarządza teraz własnym zestawem callbacków GLFW, które są dynamicznie włączane/wyłączane.
*   **Podstawowe Menu Pauzy GUI:**
    *   Po naciśnięciu klawisza `ESC`, gra pauzuje, a na ekranie pojawia się okno menu stworzone przy użyciu Nukleara.
    *   Menu zawiera opcje "Resume Game" (wznawia grę) oraz "Exit to Desktop" (zamyka aplikację).
    *   Okno menu można zamknąć również standardowym przyciskiem 'X', co także wznawia grę.
*   **Dynamiczne zarządzanie stanem pauzy i kursem w `Engine.java`:**
    *   Metoda `Engine.setPaused()` kontroluje stan pauzy, przełącza aktywne callbacki GLFW między systemem inputu gry (`Input.java`) a systemem GUI (`NuklearGui.java`).
    *   Podczas pauzy kursor myszy staje się widoczny i odblokowany; po wznowieniu jest ukrywany i blokowany.
    *   Zaimplementowano `input.resetMouseDelta()` aby zapobiec "skokom" kamery po wznowieniu gry.
*   **Modyfikacja pętli głównej `Engine.loop()`:**
    *   Dostosowano kolejność wywołań `glfwPollEvents()`, `nuklearGui.beginInput()`, `nuklearGui.endInput()` dla poprawnej obsługi zdarzeń przez Nuklear.
    *   Logika gry (`input`, `update`) jest wstrzymywana podczas pauzy.
    *   Renderowanie sceny 3D odbywa się zawsze, a menu GUI jest renderowane na wierzchu tylko w stanie pauzy.

### Zmieniono

*   **Zarządzanie callbackami GLFW:** Klasy `Input.java` i `NuklearGui.java` posiadają teraz metody `setActiveCallbacks()` i `clearCallbacks()`. `Engine.java` jest odpowiedzialny za wywoływanie tych metod w odpowiednich momentach (pauza/wznowienie gry), aby dynamicznie przełączać, który system (gra czy GUI) obsługuje input.
*   Metoda `Window.update()` została uproszczona i wykonuje teraz tylko `glfwSwapBuffers()`. Wywołanie `glfwPollEvents()` zostało przeniesione do `Engine.loop()`.

### Poprawiono

*   Potencjalne konflikty w obsłudze inputu między logiką gry a interfejsem GUI poprzez dynamiczne zarządzanie aktywnymi callbackami GLFW.

---

## [1.2.6] 

### Dodano

*   **Interakcja z obiektami oparta na raycastingu:**
    *   Kamera (`Camera.java`) posiada metody `getRayOrigin()` i `getRayDirection()` do uzyskania promienia patrzenia.
    *   Obiekty gry (`GameObject.java`) posiadają metodę `intersectsRay()` do sprawdzania przecięcia z promieniem (używając uproszczonej kuli otaczającej - bounding sphere).
    *   W `DemoGame.java` zaimplementowano logikę, która pozwala na "atakowanie" (uszkadzanie) obiektów, na które bezpośrednio patrzy gracz (w określonym zasięgu `interactionMaxDistance`) po naciśnięciu klawisza 'F'.
*   **Właściwość `canBeTargeted` dla `GameObjectProperties`:**
    *   Do `GameObjectProperties.java` dodano flagę `canBeTargeted` (domyślnie `true`), która określa, czy dany obiekt może być celem dla raycastingu/interakcji.
    *   Obiekty zniszczone automatycznie stają się nietargetowalne.
*   **Ulepszone zarządzanie widocznością obiektów zniszczonych:**
    *   W `GameObjectProperties.java` metoda `takeDamage()` automatycznie ustawia `isVisible = false` i `canBeTargeted = false` po zniszczeniu obiektu.
    *   Metoda `setVisible()` zapobiega ponownemu uczynieniu widocznym obiektu, który został zniszczony.

### Zmieniono

*   **Logika ataku w `DemoGame.java`:** Zamiast atakować pierwszy napotkany obiekt, teraz atakowany jest tylko obiekt, na który patrzy kamera, wykryty przez raycasting.
*   W `GameObject.java` promień kuli otaczającej (`boundingSphereRadius`) jest teraz inicjalizowany bardziej świadomie na podstawie typu obiektu i uwzględnia skalę obiektu przy sprawdzaniu przecięcia.
*   W `DemoGame.java` zaktualizowano sposób renderowania, aby renderować tylko obiekty, które są `isVisible()`. Stan `isAlive()` jest teraz pośrednio zarządzany przez `isVisible` po zniszczeniu.

---
*Wcześniejsze wersje (przed 1.2.6) są traktowane jako "legacy" i nie są szczegółowo dokumentowane w tym changelogu. Kod dla tych wersji może znajdować się w folderze `/legacy` repozytorium, jeśli został tam umieszczony.*