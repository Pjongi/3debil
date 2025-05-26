package org.example.scene;

import org.joml.Vector3f; // Potrzebne dla originalMaterialColor, jeśli zdecydujesz się go używać

public class GameObjectProperties {

    private final String typeName;
    private final String material; // Nazwa materiału, nie sam obiekt Material
    private final boolean hasPhysics;
    private final boolean isStatic;
    private final boolean isCollectable;
    private boolean isVisible;
    private boolean canBeTargeted;
    private final boolean isDestructible;
    private final int maxHitPoints;
    private int currentHitPoints;
    private final float mass;
    private final float friction;

    // Pola dla efektu trafienia (np. błysk koloru)
    private transient boolean hitEffectActive = false; // Użyj 'transient', jeśli nie chcesz tego serializować
    private transient float hitEffectTimer = 0.0f;
    public static final float HIT_EFFECT_DURATION = 0.15f; // Czas trwania efektu w sekundach
    // Opcjonalnie, do przechowywania oryginalnego koloru materiału, jeśli go zmieniasz
    private transient Vector3f originalMaterialDiffuseColor = null;


    private GameObjectProperties(Builder builder) {
        this.typeName = builder.typeName;
        this.material = builder.material;
        this.hasPhysics = builder.hasPhysics;
        this.isStatic = builder.isStatic;
        this.isCollectable = builder.isCollectable;
        this.isVisible = builder.isVisible;
        this.canBeTargeted = builder.canBeTargeted;
        this.isDestructible = builder.isDestructible;
        this.maxHitPoints = builder.maxHitPoints;
        this.currentHitPoints = builder.currentHitPoints;
        this.mass = builder.mass;
        this.friction = builder.friction;

        if (this.isDestructible && this.maxHitPoints <= 0) {
            throw new IllegalArgumentException("Destructible object ('" + typeName + "') must have positive maxHitPoints.");
        }
    }

    // --- Gettery dla podstawowych właściwości ---
    public String getTypeName() { return typeName; }
    public String getMaterial() { return material; } // Zwraca nazwę materiału
    public boolean hasPhysics() { return hasPhysics; }
    public boolean isStatic() { return isStatic; }
    public boolean isCollectable() { return isCollectable; }
    public boolean isVisible() { return isVisible; }
    public boolean canBeTargeted() { return canBeTargeted; }
    public boolean isDestructible() { return isDestructible; }
    public int getMaxHitPoints() { return maxHitPoints; }
    public int getCurrentHitPoints() { return currentHitPoints; }
    public float getMass() { return mass; }
    public float getFriction() { return friction; }

    public void setVisible(boolean visible) {
        if (!isAlive() && visible) {
            this.isVisible = false;
            return;
        }
        this.isVisible = visible;
    }

    public boolean takeDamage(int amount) {
        if (isDestructible && isAlive() && amount > 0) {
            this.currentHitPoints -= amount;
            // Aktywuj efekt trafienia, nawet jeśli obiekt nie został zniszczony
            triggerHitEffect(); // <<--- DODANO AKTYWACJĘ EFEKTU

            if (this.currentHitPoints <= 0) {
                this.currentHitPoints = 0;
                this.isVisible = false;
                this.canBeTargeted = false;
                return true;
            }
            return false;
        }
        return false;
    }

    public void setCurrentHitPoints(int amount) {
        if (isDestructible) {
            this.currentHitPoints = Math.max(0, Math.min(maxHitPoints, amount));
            if (!isAlive()) {
                this.isVisible = false;
                this.canBeTargeted = false;
            }
        }
    }

    public boolean isAlive() {
        return !isDestructible || currentHitPoints > 0;
    }

    // --- Metody dla efektu trafienia ---

    /**
     * Aktywuje wizualny efekt trafienia na obiekcie.
     */
    public void triggerHitEffect() {
        // Aktywuj tylko jeśli obiekt jest nadal widoczny i targetowalny
        // (lub dostosuj logikę, jeśli chcesz efekt nawet na zniszczonym obiekcie przez moment)
        if (this.isVisible && this.canBeTargeted) { // Lub po prostu if (isAlive())
            this.hitEffectActive = true;
            this.hitEffectTimer = HIT_EFFECT_DURATION;
            // Zapis oryginalnego koloru materiału powinien być obsłużony przez logikę renderowania/aktualizacji,
            // która ma dostęp do faktycznego obiektu Material.
            // Tutaj tylko ustawiamy flagi.
            this.originalMaterialDiffuseColor = null; // Zresetuj, aby renderer mógł zapisać nowy
        }
    }

    /**
     * Sprawdza, czy efekt trafienia jest aktualnie aktywny.
     * @return true, jeśli efekt jest aktywny.
     */
    public boolean isHitEffectActive() {
        return hitEffectActive;
    }

    /**
     * Aktualizuje timer efektu trafienia. Powinno być wywoływane co klatkę.
     * @param deltaTime Czas od ostatniej klatki.
     */
    public void updateHitEffect(float deltaTime) {
        if (hitEffectActive) {
            hitEffectTimer -= deltaTime;
            if (hitEffectTimer <= 0) {
                hitEffectActive = false;
                // Logika przywrócenia oryginalnego wyglądu (np. koloru)
                // powinna być obsłużona przez Renderer lub GameObject,
                // gdy wykryje, że hitEffectActive stało się false.
                // Tutaj tylko resetujemy flagę.
            }
        }
    }

    /**
     * Zwraca zapisany oryginalny kolor diffuse materiału. Używane do przywrócenia koloru po efekcie.
     * @return Wektor koloru lub null, jeśli nie zapisano.
     */
    public Vector3f getOriginalMaterialDiffuseColor() {
        return originalMaterialDiffuseColor;
    }

    /**
     * Ustawia (zapisuje) oryginalny kolor diffuse materiału.
     * @param color Kolor do zapisania.
     */
    public void setOriginalMaterialDiffuseColor(Vector3f color) {
        if (color != null) {
            this.originalMaterialDiffuseColor = new Vector3f(color);
        } else {
            this.originalMaterialDiffuseColor = null;
        }
    }


    // --- Klasa Builder (bez zmian w stosunku do twojego kodu) ---
    public static class Builder {
        private String typeName = "GenericObject";
        private String material = "Default";
        private boolean hasPhysics = false;
        private boolean isStatic = false;
        private boolean isCollectable = false;
        private boolean isVisible = true;
        private boolean canBeTargeted = true;
        private boolean isDestructible = false;
        private int maxHitPoints = 0;
        private int currentHitPoints = 0;
        private float mass = 1.0f;
        private float friction = 0.5f;

        public Builder typeName(String typeName) {
            this.typeName = (typeName != null && !typeName.trim().isEmpty()) ? typeName : "UnnamedObject";
            return this;
        }
        public Builder material(String material) {
            this.material = (material != null && !material.trim().isEmpty()) ? material : "Unknown";
            return this;
        }
        public Builder physicsEnabled(boolean enabled) {
            this.hasPhysics = enabled;
            return this;
        }
        public Builder setStatic(boolean isStatic) {
            this.isStatic = isStatic;
            if (isStatic && this.hasPhysics) {
                // System.out.println("Warning: Setting object '" + typeName + "' as static. Disabling physics as well for consistency, unless explicitly re-enabled.");
                this.hasPhysics = false;
            }
            return this;
        }
        public Builder collectable(boolean isCollectable) {
            this.isCollectable = isCollectable;
            return this;
        }
        public Builder visible(boolean isVisible) {
            this.isVisible = isVisible;
            return this;
        }
        public Builder targetable(boolean canBeTargeted) {
            this.canBeTargeted = canBeTargeted;
            return this;
        }
        public Builder makeDestructible(int maxHp) {
            if (maxHp <= 0) {
                // System.err.println("Warning: Attempted to make object '" + typeName + "' destructible with non-positive maxHp (" + maxHp + "). Setting as non-destructible.");
                this.isDestructible = false; this.maxHitPoints = 0; this.currentHitPoints = 0;
            } else {
                this.isDestructible = true; this.maxHitPoints = maxHp; this.currentHitPoints = maxHp;
            }
            return this;
        }
        public Builder mass(float mass) { this.mass = Math.max(0.1f, mass); return this; }
        public Builder friction(float friction) { this.friction = Math.max(0.0f, Math.min(1.0f, friction)); return this; }

        public GameObjectProperties build() {
            return new GameObjectProperties(this);
        }
    }
}