package org.example.scene;

/**
 * Przechowuje dodatkowe, opisowe właściwości obiektu gry.
 * Używa wzorca Budowniczy (Builder) dla łatwego i czytelnego tworzenia.
 * Wiele pól jest finalnych, zakładając, że podstawowe cechy obiektu
 * (jak materiał czy możliwość zniszczenia) nie zmieniają się w trakcie gry.
 * Właściwości takie jak HP czy widoczność mogą być modyfikowane.
 */
public class GameObjectProperties {

    // --- Właściwości podstawowe (zwykle niezmienne) ---
    private final String typeName;       // Np. "Player", "Tree", "Rock" - dla identyfikacji/logiki
    private final String material;       // Np. "Wood", "Stone", "Metal" - do fizyki, efektów
    private final boolean hasPhysics;    // Czy podlega podstawowej symulacji fizyki (np. kolizje)
    private final boolean isStatic;      // Czy jest nieruchomy (optymalizacja dla fizyki/renderowania)
    private final boolean isCollectable; // Czy można go podnieść/zebrać

    // --- Właściwości wizualne/interakcji ---
    private boolean isVisible;     // Czy obiekt jest aktualnie renderowany
    private boolean canBeTargeted; // Czy może być celem (np. dla AI, strzałów)

    // --- Właściwości związane ze zniszczeniem ---
    private final boolean isDestructible; // Czy obiekt może zostać zniszczony
    private final int maxHitPoints;       // Maksymalne punkty wytrzymałości (jeśli isDestructible=true)
    private int currentHitPoints;         // Aktualne punkty wytrzymałości

    // --- Inne potencjalne "głupoty" ---
    private final float mass;           // Masa (dla bardziej zaawansowanej fizyki)
    private final float friction;       // Tarcie (dla fizyki)
    // private Sound interactSound;     // Dźwięk interakcji
    // private ParticleEffect destroyEffect; // Efekt cząsteczkowy zniszczenia

    // Prywatny konstruktor - używany tylko przez Builder
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

        // Walidacja: jeśli obiekt jest zniszczalny, musi mieć dodatnie max HP
        if (this.isDestructible && this.maxHitPoints <= 0) {
            throw new IllegalArgumentException("Destructible object ('" + typeName + "') must have positive maxHitPoints.");
        }
        // Jeśli statyczny, nie powinien mieć fizyki (chociaż kolizje statyczne są możliwe)
        if (this.isStatic && this.hasPhysics) {
            System.out.println("Warning: Static object ('" + typeName + "') has physics enabled. Consider disabling physics for static objects unless static collision detection is intended.");
        }
    }

    // --- Gettery dla właściwości ---

    public String getTypeName() { return typeName; }
    public String getMaterial() { return material; }
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

    // --- Settery dla właściwości modyfikowalnych ---

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    /**
     * Zadaje obrażenia obiektowi, jeśli jest zniszczalny.
     * Zwraca true, jeśli obiekt został zniszczony (HP <= 0).
     * @param amount Ilość obrażeń (powinna być dodatnia).
     * @return true, jeśli obiekt został zniszczony, false w przeciwnym razie.
     */
    public boolean takeDamage(int amount) {
        if (isDestructible && amount > 0) {
            this.currentHitPoints -= amount;
            if (this.currentHitPoints <= 0) {
                this.currentHitPoints = 0;
                System.out.println("Object '" + typeName + "' destroyed!");
                // Tutaj można by wywołać logikę zniszczenia (efekty, usunięcie obiektu itp.)
                return true;
            }
        }
        return false;
    }

    /**
     * Ustawia bezpośrednio aktualne HP (np. przy leczeniu).
     * Wartość jest ograniczana do zakresu [0, maxHitPoints].
     * @param amount Nowa wartość HP.
     */
    public void setCurrentHitPoints(int amount) {
        if (isDestructible) {
            this.currentHitPoints = Math.max(0, Math.min(maxHitPoints, amount));
        }
    }

    /** Sprawdza, czy obiekt ma jeszcze punkty życia. */
    public boolean isAlive() {
        return !isDestructible || currentHitPoints > 0;
    }


    // --- Klasa Budowniczego (Builder) ---
    public static class Builder {
        // Domyślne wartości dla właściwości
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

        /** Ustawia nazwę typu obiektu (np. "Player", "Tree"). */
        public Builder typeName(String typeName) {
            this.typeName = (typeName != null && !typeName.trim().isEmpty()) ? typeName : "UnnamedObject";
            return this;
        }

        /** Ustawia materiał obiektu (np. "Wood", "Stone"). */
        public Builder material(String material) {
            this.material = (material != null && !material.trim().isEmpty()) ? material : "Unknown";
            return this;
        }

        /** Włącza/wyłącza podstawową fizykę (kolizje). Domyślnie false. */
        public Builder physicsEnabled(boolean enabled) {
            this.hasPhysics = enabled;
            return this;
        }

        /** Oznacza obiekt jako statyczny (nieruchomy). Domyślnie false. */
        public Builder setStatic(boolean isStatic) {
            this.isStatic = isStatic;
            // Jeśli statyczny, domyślnie wyłącz fizykę (chyba że celowo włączona później)
            if (isStatic) this.hasPhysics = false;
            return this;
        }

        /** Oznacza obiekt jako możliwy do zebrania. Domyślnie false. */
        public Builder collectable(boolean isCollectable) {
            this.isCollectable = isCollectable;
            return this;
        }

        /** Ustawia początkową widoczność obiektu. Domyślnie true. */
        public Builder visible(boolean isVisible) {
            this.isVisible = isVisible;
            return this;
        }

        /** Ustawia, czy obiekt może być celem. Domyślnie true. */
        public Builder targetable(boolean canBeTargeted) {
            this.canBeTargeted = canBeTargeted;
            return this;
        }

        /**
         * Konfiguruje obiekt jako zniszczalny z podaną maksymalną liczbą HP.
         * @param maxHp Maksymalne punkty wytrzymałości (> 0).
         */
        public Builder makeDestructible(int maxHp) {
            if (maxHp <= 0) {
                // Można rzucić wyjątek lub zignorować/ostrzec
                System.err.println("Warning: Attempted to make object '" + typeName + "' destructible with non-positive maxHp (" + maxHp + "). Ignoring.");
                this.isDestructible = false;
                this.maxHitPoints = 0;
                this.currentHitPoints = 0;
            } else {
                this.isDestructible = true;
                this.maxHitPoints = maxHp;
                this.currentHitPoints = maxHp; // Na starcie ma pełne HP
            }
            return this;
        }

        /** Ustawia masę obiektu. Domyślnie 1.0. */
        public Builder mass(float mass) {
            this.mass = Math.max(0.1f, mass); // Masa powinna być dodatnia
            return this;
        }

        /** Ustawia tarcie obiektu. Domyślnie 0.5. */
        public Builder friction(float friction) {
            this.friction = Math.max(0.0f, Math.min(1.0f, friction)); // Tarcie w zakresie [0, 1]
            return this;
        }

        /** Buduje finalny obiekt GameObjectProperties. */
        public GameObjectProperties build() {
            return new GameObjectProperties(this);
        }
    }
}