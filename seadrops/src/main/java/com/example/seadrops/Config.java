package com.example.seadrops;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/** Settings, stored in config/seadrops.properties. The in-game menu edits these. */
public final class Config {

    public static final String DEFAULT_TRIGGERS =
            "GOOD CATCH!|GREAT CATCH!|OUTSTANDING CATCH!|PET DROP!|RARE DROP!";

    public static final double[] THRESHOLDS = {500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000};

    public static boolean enabled = true;
    public static boolean playSound = true;
    public static boolean screenFlash = true;
    public static boolean mobAnimations = true;
    public static boolean genericAnimations = true;
    /** Boss drop sequences (Titanoboa, Jawbus, ...) and item sequences (Radioactive Vial, 1M+ items). */
    public static boolean dropAnimations = true;
    /** Look up prices (public API, every 10 min) so drops worth over the threshold get an animation. */
    public static boolean valueAlerts = true;
    /** Also play the normal catch animation when one of the drop-only bosses spawns. Off = drop-only. */
    public static boolean bossCatchAnimation = false;
    /** Boss drops only animate when worth at least the threshold (unknown price still animates). */
    public static boolean bossOnlyValuable = true;
    public static double valueThreshold = 1_000_000;
    /** How long after a boss spawns its drops still count as "from that boss". */
    public static int bossWindowSec = 180;
    public static Rarity minRarity = Rarity.RARE;

    private static final Map<Theme, Boolean> THEMES = new EnumMap<>(Theme.class);

    public static String[] triggers = DEFAULT_TRIGGERS.split("\\|");
    public static String customMobs = "";
    public static String customItems = "";
    public static String lowestBinUrl = "https://moulberry.codes/lowestbin.json";

    private Config() {}

    public static boolean isThemeOn(Theme t) {
        return THEMES.getOrDefault(t, true);
    }

    public static void setThemeOn(Theme t, boolean on) {
        THEMES.put(t, on);
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("seadrops.properties");
    }

    public static void load() {
        Properties p = new Properties();
        Path file = file();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) {
            }
        }

        enabled = bool(p, "enabled", true);
        playSound = bool(p, "playSound", true);
        screenFlash = bool(p, "screenFlash", true);
        mobAnimations = bool(p, "mobAnimations", true);
        genericAnimations = bool(p, "genericAnimations", true);
        dropAnimations = bool(p, "dropAnimations", true);
        valueAlerts = bool(p, "valueAlerts", true);
        bossCatchAnimation = bool(p, "bossCatchAnimation", false);
        bossOnlyValuable = bool(p, "bossOnlyValuable", true);

        try {
            valueThreshold = Double.parseDouble(p.getProperty("valueThreshold", "1000000"));
        } catch (NumberFormatException e) {
            valueThreshold = 1_000_000;
        }
        try {
            bossWindowSec = Integer.parseInt(p.getProperty("bossWindowSec", "180"));
        } catch (NumberFormatException e) {
            bossWindowSec = 180;
        }

        Rarity r = Rarity.fromName(p.getProperty("minRarity", "rare"));
        minRarity = r != null ? r : Rarity.RARE;

        for (Theme t : Theme.values()) {
            THEMES.put(t, bool(p, "theme." + t.name().toLowerCase(), true));
        }

        triggers = p.getProperty("triggers", DEFAULT_TRIGGERS).split("\\|");
        customMobs = p.getProperty("customMobs", "");
        customItems = p.getProperty("customItems", "");
        lowestBinUrl = p.getProperty("lowestBinUrl", lowestBinUrl);
        MobCatalog.loadCustom(customMobs);
        ItemCatalog.loadCustom(customItems);

        save(); // writes any missing keys so the file is easy to edit
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("playSound", String.valueOf(playSound));
        p.setProperty("screenFlash", String.valueOf(screenFlash));
        p.setProperty("mobAnimations", String.valueOf(mobAnimations));
        p.setProperty("genericAnimations", String.valueOf(genericAnimations));
        p.setProperty("dropAnimations", String.valueOf(dropAnimations));
        p.setProperty("valueAlerts", String.valueOf(valueAlerts));
        p.setProperty("bossCatchAnimation", String.valueOf(bossCatchAnimation));
        p.setProperty("bossOnlyValuable", String.valueOf(bossOnlyValuable));
        p.setProperty("valueThreshold", String.valueOf((long) valueThreshold));
        p.setProperty("bossWindowSec", String.valueOf(bossWindowSec));
        p.setProperty("minRarity", minRarity.label.toLowerCase());
        for (Theme t : Theme.values()) {
            p.setProperty("theme." + t.name().toLowerCase(), String.valueOf(isThemeOn(t)));
        }
        p.setProperty("triggers", String.join("|", triggers));
        p.setProperty("customMobs", customMobs);
        p.setProperty("customItems", customItems);
        p.setProperty("lowestBinUrl", lowestBinUrl);

        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "SeaDrops. triggers = generic catch phrases separated by |. "
                    + "customMobs = phrase~Name~rarity~theme entries separated by |. "
                    + "customItems = phrase~scene~hexcolor entries separated by |");
        } catch (IOException ignored) {
        }
    }

    /** Cycles the value threshold through THRESHOLDS. */
    public static void nextThreshold() {
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (THRESHOLDS[i] > valueThreshold) {
                valueThreshold = THRESHOLDS[i];
                return;
            }
        }
        valueThreshold = THRESHOLDS[0];
    }

    private static boolean bool(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)));
    }
}
