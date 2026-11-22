package com.example.seadrops;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fishing mobs and the chat phrase that announces each one.
 * Phrases are matched case-insensitively anywhere in the message, longest phrase first.
 * Wrong or missing one? Add it in config/seadrops.properties under customMobs
 * (format: phrase~Name~rarity~theme, entries separated by |).
 */
public final class MobCatalog {

    public record Mob(String name, String phrase, Rarity rarity, Theme theme) {}

    private static final List<Mob> BUILT_IN = new ArrayList<>();
    private static final List<Mob> CUSTOM = new ArrayList<>();
    private static List<Mob> sorted = new ArrayList<>();

    private static void add(String name, String phrase, Rarity r, Theme t) {
        BUILT_IN.add(new Mob(name, phrase, r, t));
    }

    static {
        // ---- Ocean / lakes ----
        add("Squid", "A Squid appeared", Rarity.COMMON, Theme.WATER);
        add("Sea Walker", "caught a Sea Walker", Rarity.COMMON, Theme.WATER);
        add("Night Squid", "caught a Night Squid", Rarity.COMMON, Theme.WATER);
        add("Sea Guardian", "stumbled upon a Sea Guardian", Rarity.COMMON, Theme.WATER);
        add("Agarimoo", "it's an Agarimoo", Rarity.COMMON, Theme.WATER);
        add("Sea Witch", "disrupted the Sea Witch", Rarity.UNCOMMON, Theme.WATER);
        add("Sea Archer", "reeled in a Sea Archer", Rarity.UNCOMMON, Theme.WATER);
        add("Rider of the Deep", "Monster of The Deep", Rarity.UNCOMMON, Theme.WATER);
        add("Oasis Rabbit", "Oasis Rabbit appears", Rarity.UNCOMMON, Theme.WATER);
        add("Oasis Sheep", "Oasis Sheep appears", Rarity.UNCOMMON, Theme.WATER);
        add("Catfish", "A Catfish!", Rarity.RARE, Theme.WATER);
        add("Sea Leech", "A Sea Leech!", Rarity.RARE, Theme.WATER);
        add("Carrot King", "Carrot King", Rarity.RARE, Theme.WATER);
        add("Guardian Defender", "Guardian Defender of the sea", Rarity.EPIC, Theme.WATER);
        add("Deep Sea Protector", "Deep Sea Protector", Rarity.EPIC, Theme.WATER);
        add("Water Hydra", "Water Hydra has come", Rarity.LEGENDARY, Theme.WATER);
        add("Sea Emperor", "Sea Emperor arises", Rarity.LEGENDARY, Theme.WATER);

        // ---- Fishing Festival sharks ----
        add("Nurse Shark", "caught a Nurse Shark", Rarity.UNCOMMON, Theme.WATER);
        add("Blue Shark", "it's a Blue Shark", Rarity.RARE, Theme.WATER);
        add("Tiger Shark", "the wild Tiger Shark", Rarity.EPIC, Theme.WATER);
        add("Great White Shark", "Great White Shark has tracked", Rarity.LEGENDARY, Theme.WATER);

        // ---- Crimson Isle (lava) ----
        add("Magma Slug", "appears a Magma Slug", Rarity.RARE, Theme.LAVA);
        add("Moogma", "A Moogma appears", Rarity.RARE, Theme.LAVA);
        add("Lava Leech", "Lava Leech emerges", Rarity.RARE, Theme.LAVA);
        add("Pyroclastic Worm", "Pyroclastic Worm surfaces", Rarity.RARE, Theme.LAVA);
        add("Lava Flame", "A Lava Flame flies out", Rarity.RARE, Theme.LAVA);
        add("Fire Eel", "Fire Eel slithers", Rarity.RARE, Theme.LAVA);
        add("Taurus", "Taurus and his steed", Rarity.RARE, Theme.LAVA);
        add("Plhlegblast", "Plhlegblast appeared", Rarity.LEGENDARY, Theme.LAVA);
        add("Thunder", "Thunder emerges", Rarity.MYTHIC, Theme.THUNDER);
        add("Lord Jawbus", "Lord Jawbus has arrived", Rarity.MYTHIC, Theme.LAVA);

        // ---- Crystal Hollows ----
        add("Water Worm", "A Water Worm surfaces", Rarity.RARE, Theme.DEEP);
        add("Poisoned Water Worm", "Poisoned Water Worm surfaces", Rarity.RARE, Theme.DEEP);
        add("Flaming Worm", "Flaming Worm surfaces", Rarity.RARE, Theme.LAVA);
        add("Lava Blaze", "Lava Blaze has surfaced", Rarity.EPIC, Theme.LAVA);
        add("Lava Pigman", "Lava Pigman arose", Rarity.EPIC, Theme.LAVA);
        add("Abyssal Miner", "Abyssal Miner surfaces", Rarity.LEGENDARY, Theme.DEEP);

        // ---- Jerry's Workshop ----
        add("Frozen Steve", "Frozen Steve fell into the pond", Rarity.COMMON, Theme.ICE);
        add("Frosty", "Its a Snowman", Rarity.COMMON, Theme.ICE);
        add("Grinch", "The Grinch stole", Rarity.UNCOMMON, Theme.ICE);
        add("Nutcracker", "forgotten Nutcracker", Rarity.RARE, Theme.ICE);
        add("Yeti", "What is this creature", Rarity.LEGENDARY, Theme.ICE);
        add("Reindrake", "Reindrake forms", Rarity.LEGENDARY, Theme.ICE);

        // ---- Spooky Festival ----
        add("Scarecrow", "only a scarecrow", Rarity.COMMON, Theme.SPOOKY);
        add("Nightmare", "caught a Nightmare", Rarity.RARE, Theme.SPOOKY);
        add("Werewolf", "it's a Werewolf", Rarity.EPIC, Theme.SPOOKY);
        add("Phantom Fisher", "Phantom Fisher has come", Rarity.LEGENDARY, Theme.SPOOKY);
        add("Grim Reaper", "manifestation of death", Rarity.LEGENDARY, Theme.SPOOKY);

        // ---- Backwater Bayou ----
        // NOTE: exact catch messages and rarities for these were not verified - they match on the
        // creature name. Fix or add entries through customMobs in the config if one is off.
        add("Trash Gobbler", "Trash Gobbler", Rarity.UNCOMMON, Theme.BAYOU);
        add("Dumpster Diver", "Dumpster Diver", Rarity.UNCOMMON, Theme.BAYOU);
        add("Snapping Turtle", "Snapping Turtle", Rarity.RARE, Theme.BAYOU);
        add("Alligator", "Alligator", Rarity.RARE, Theme.BAYOU);
        add("Banshee", "Banshee", Rarity.EPIC, Theme.BAYOU);
        // catch message from the wiki: "A garish set of tentacles arise. It's a Blue Ringed Octopus!"
        add("Blue Ringed Octopus", "It's a Blue Ringed Octopus", Rarity.EPIC, Theme.BAYOU);
        add("Wiki Tiki", "Wiki Tiki", Rarity.LEGENDARY, Theme.BAYOU);
        add("Titanoboa", "Titanoboa", Rarity.LEGENDARY, Theme.BAYOU);

        // Ragnarok: catch message and rarity unverified - matches on the name only.
        add("Ragnarok", "Ragnarok", Rarity.MYTHIC, Theme.LAVA);

        rebuild();
    }

    private MobCatalog() {}

    private static void rebuild() {
        List<Mob> all = new ArrayList<>(CUSTOM); // custom first so it wins ties
        all.addAll(BUILT_IN);
        all.sort(Comparator.comparingInt((Mob m) -> m.phrase().length()).reversed());
        sorted = all;
    }

    /** Parses "phrase~Name~rarity~theme|phrase~Name~rarity~theme". Bad entries are skipped. */
    public static void loadCustom(String raw) {
        CUSTOM.clear();
        if (raw != null) {
            for (String entry : raw.split("\\|")) {
                String[] p = entry.split("~");
                if (p.length != 4) continue;
                Rarity r = Rarity.fromName(p[2].trim());
                Theme t = Theme.fromName(p[3].trim());
                if (r == null || t == null || p[0].isBlank()) continue;
                CUSTOM.add(new Mob(p[1].trim(), p[0].trim(), r, t));
            }
        }
        rebuild();
    }

    public static Mob find(String text) {
        String lower = text.toLowerCase();
        for (Mob m : sorted) {
            int idx = lower.indexOf(m.phrase().toLowerCase());
            if (idx < 0) continue;
            // player chat looks like "Name: message" - don't let people spoof it
            if (text.substring(0, idx).contains(":")) continue;
            return m;
        }
        return null;
    }
}
