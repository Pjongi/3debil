package org.example.scene;

public class GameObjectProperties {

    private final String typeName;
    private final String material;
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
        // Ostrzeżenie o fizyce dla obiektów statycznych jest mniej krytyczne, można je zakomentować
        // if (this.isStatic && this.hasPhysics) {
        //     System.out.println("Warning: Static object ('" + typeName + "') has physics enabled. Consider disabling for static objects unless static collision detection is intended.");
        // }
    }

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

    public void setVisible(boolean visible) {
        // Jeśli obiekt jest zniszczony (nie żyje) i próbujemy go uczynić widocznym,
        // nie pozwalamy na to, chyba że logika gry zaimplementuje "respawn",
        // który resetuje HP i inne stany.
        if (!isAlive() && visible) {
            // System.out.println("Cannot make destroyed object '" + typeName + "' visible again without respawn logic.");
            // W tym przypadku, jeśli jest zniszczony, pozostaje niewidoczny.
            this.isVisible = false;
            return;
        }
        this.isVisible = visible;
    }

    /**
     * Zadaje obrażenia obiektowi.
     * @param amount Ilość obrażeń.
     * @return true, jeśli obiekt został zniszczony (HP spadło do 0 lub poniżej) w wyniku tego ataku.
     */
    public boolean takeDamage(int amount) {
        if (isDestructible && isAlive() && amount > 0) {
            this.currentHitPoints -= amount;
            if (this.currentHitPoints <= 0) {
                this.currentHitPoints = 0;
                this.isVisible = false; // Zniszczony obiekt staje się niewidoczny
                this.canBeTargeted = false; // Nie można już go targetować
                // System.out.println("Object '" + typeName + "' destroyed! Became invisible and untargetable.");
                return true; // Zwraca true, bo został zniszczony
            }
            return false; // Otrzymał obrażenia, ale jeszcze nie zniszczony
        }
        return false; // Niezniszczalny, już zniszczony, lub negatywne obrażenia
    }

    public void setCurrentHitPoints(int amount) {
        if (isDestructible) {
            this.currentHitPoints = Math.max(0, Math.min(maxHitPoints, amount));
            if (!isAlive()) {
                this.isVisible = false;
                this.canBeTargeted = false;
            } else {
                // Jeśli obiekt był niewidoczny z powodu bycia zniszczonym, a teraz HP > 0,
                // potencjalnie powinien stać się znowu widoczny i targetowalny.
                // To zależy od logiki "respawnu" gry. Na razie setVisible() obsłuży to.
                // Jeśli this.isVisible było false, trzeba by je ręcznie ustawić na true, jeśli chcemy
                // aby "wyleczony" obiekt od razu się pojawił.
                // if (this.currentHitPoints > 0 && !this.isVisible) {
                //    this.isVisible = true;
                //    this.canBeTargeted = true;
                // }
            }
        }
    }

    public boolean isAlive() {
        return !isDestructible || currentHitPoints > 0;
    }

    public static class Builder {
        private String typeName = "GenericObject";
        private String material = "Default";
        private boolean hasPhysics = false;
        private boolean isStatic = false;
        private boolean isCollectable = false;
        private boolean isVisible = true;
        private boolean canBeTargeted = true; // Domyślnie można targetować
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
            if (isStatic && this.hasPhysics) { // Jeśli ustawiamy na statyczny i fizyka jest włączona,
                System.out.println("Warning: Setting object '" + typeName + "' as static. Disabling physics as well for consistency, unless explicitly re-enabled.");
                this.hasPhysics = false; // często statyczne obiekty nie potrzebują dynamicznej fizyki.
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
                System.err.println("Warning: Attempted to make object '" + typeName + "' destructible with non-positive maxHp (" + maxHp + "). Setting as non-destructible.");
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