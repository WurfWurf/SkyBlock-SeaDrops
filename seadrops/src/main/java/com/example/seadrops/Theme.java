package com.example.seadrops;

/** Visual style of an animation. Each fishing location gets its own look. */
public enum Theme {
    GENERIC("Generic", 0xFFAA00, 0xFFFFFF, "GREAT CATCH!", Rarity.LEGENDARY),
    WATER("Ocean & Lakes", 0x3FA7FF, 0xBFEFFF, "Sea Emperor", Rarity.LEGENDARY),
    LAVA("Crimson Isle", 0xFF5A1F, 0xFFD23F, "Lord Jawbus", Rarity.MYTHIC),
    THUNDER("Thunder", 0xFFE94D, 0xFFFFFF, "Thunder", Rarity.MYTHIC),
    ICE("Jerry's Workshop", 0x8FE3FF, 0xFFFFFF, "Reindrake", Rarity.LEGENDARY),
    BAYOU("Backwater Bayou", 0x5BD35B, 0xC8E86A, "Titanoboa", Rarity.LEGENDARY),
    SPOOKY("Spooky Festival", 0xFF8A1F, 0xB05CFF, "Phantom Fisher", Rarity.LEGENDARY),
    DEEP("Crystal Hollows", 0xB86BFF, 0x59F3FF, "Abyssal Miner", Rarity.LEGENDARY);

    public final String label;
    public final int primary;
    public final int secondary;
    /** Creature shown when previewing this theme from the menu. */
    public final String sample;
    public final Rarity sampleRarity;

    Theme(String label, int primary, int secondary, String sample, Rarity sampleRarity) {
        this.label = label;
        this.primary = primary;
        this.secondary = secondary;
        this.sample = sample;
        this.sampleRarity = sampleRarity;
    }

    public static Theme fromName(String name) {
        for (Theme t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
