# Dziennik Zmian (Changelog) - Silnik 3D ebil Engine

Wszystkie znaczące zmiany w tym projekcie będą dokumentowane w tym pliku.
Format opiera się na [Keep a Changelog](https://keepachangelog.com/pl/1.0.0/).

## [Nieopublikowane] - Wersja Rozwojowa

### Planowane / W trakcie

*   Implementacja menu pauzy.
*   Refaktoryzacja systemu renderowania na komponenty (`ShaderManager`, `SceneRenderer` itp.).
*   Implementacja cieni dla świateł reflektorowych (SpotLight) przy użyciu cube map.
*   Implementacja celownika 2D.

## [1.2.6] - 2025-05-06 ##

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

*Wcześniejsze wersje (przed 1.2.6) są traktowane jako "legacy" i nie są szczegółowo dokumentowane w tym changelogu.*