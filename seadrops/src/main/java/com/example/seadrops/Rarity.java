package com.example.seadrops;

/** SkyBlock rarity tiers. Higher tiers get a bigger animation. */
public enum Rarity {
    COMMON("Common", 0xFFFFFF),
    UNCOMMON("Uncommon", 0x55FF55),
    RARE("Rare", 0x5555FF),
    EPIC("Epic", 0xAA00AA),
    LEGENDARY("Legendary", 0xFFAA00),
    MYTHIC("Mythic", 0xFF55FF);

    public final String label;
    public final int color;

    Rarity(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public static Rarity fromName(String name) {
        for (Rarity r : values()) {
            if (r.label.equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    public static Rarity fromColor(int rgb) {
        for (Rarity r : values()) {
            if (r.color == (rgb & 0xFFFFFF)) return r;
        }
        return null;
    }
}
