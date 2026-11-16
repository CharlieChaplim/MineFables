package io.github.charliechaplim.minefables.resource;

import java.util.Arrays;
import java.util.Optional;

/** Um recurso. A ausencia de energia nao e um tipo de barra. */
public enum ResourceType {
    STAMINA("stamina", "Estamina", 100, 100, 0x50C878),
    FAITH("faith", "Fe", 50, 50, 0xF5D76E),
    MANA("mana", "Mana", 200, 200, 0x477CE3),
    BLOOD("blood", "Sangue", 50, 50, 0xBD3443),
    ELECTRIC("electric", "Energia (FE)", 800_000, 0, 0xE5B33E),
    CURSED("cursed", "Energia Amaldicoada", 1_000, 1_000, 0x9A55D9),
    STACK("stack", "Stack", 100, 0, 0xE67F42);

    private final String id;
    private final String displayName;
    private final long baseCapacity;
    private final long initialValue;
    private final int rgb;

    ResourceType(String id, String displayName, long baseCapacity, long initialValue, int rgb) {
        this.id = id;
        this.displayName = displayName;
        this.baseCapacity = baseCapacity;
        this.initialValue = initialValue;
        this.rgb = rgb;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public long baseCapacity() { return baseCapacity; }
    public long initialValue() { return initialValue; }
    public int rgb() { return rgb; }

    public static Optional<ResourceType> fromId(String id) {
        return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
    }
}
