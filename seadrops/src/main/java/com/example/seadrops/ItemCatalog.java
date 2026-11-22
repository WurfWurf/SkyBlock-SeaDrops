package com.example.seadrops;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Items that get their own animation. The drop line looks like
 *   RARE DROP! Radioactive Vial (+X% ★ Magic Find)
 * and the item name is matched anywhere in the name part.
 *
 * "always" entries ignore the value threshold (use for items you want animated whatever they sell for).
 *
 * Add more in config/seadrops.properties under customItems:
 *   phrase~scene~hexcolor  or  phrase~scene~hexcolor~always   (entries separated by |)
 * e.g.  Jawbus Follower~jawbus~FF4A1A~always
 */
public final class ItemCatalog {

    public record Entry(String phrase, ItemAnimation.Scene scene, int rgb, boolean always) {}

    private static Entry e(String phrase, ItemAnimation.Scene scene, int rgb) {
        return new Entry(phrase, scene, rgb, false);
    }

    private static final List<Entry> BUILT_IN = List.of(
            // Titanoboa
            e("Titanoboa Shed", ItemAnimation.Scene.BEHEAD, 0x5BD35B),
            e("Snake Eyes", ItemAnimation.Scene.BEHEAD, 0x5BD35B),
            // Lord Jawbus
            e("Radioactive Vial", ItemAnimation.Scene.VIAL, 0x7CFC00),
            new Entry("Magma Lord Fragment", ItemAnimation.Scene.JAWBUS, 0xFF4A1A, true),
            // Wiki Tiki
            e("Tiki Mask", ItemAnimation.Scene.TIKI, 0xFF9A2E),
            e("Troubled Bubble", ItemAnimation.Scene.TIKI, 0xFF9A2E),
            // Ragnarok
            e("Chain of the End Times", ItemAnimation.Scene.RAGNAROK, 0xE0245E),
            e("Brimstone Handle", ItemAnimation.Scene.RAGNAROK, 0xE0245E),
            e("Burnt Texts", ItemAnimation.Scene.RAGNAROK, 0xE0245E),
            // Blue Ringed Octopus
            e("Octopus Tendril", ItemAnimation.Scene.OCTOPUS, 0x2FA8FF),
            // Thunder (the Flash I enchanted book)
            e("Flash I", ItemAnimation.Scene.THUNDER, 0xFFE94D));
    // Shared items - Aquamarine Dye, Carmine Dye, Bobbin' Scriptures - are not listed on purpose: several bosses
    // drop them, so they use whichever boss spawned last.

    private static final List<Entry> CUSTOM = new ArrayList<>();
    private static List<Entry> sorted = new ArrayList<>(BUILT_IN);

    private ItemCatalog() {}

    public static void loadCustom(String raw) {
        CUSTOM.clear();
        if (raw != null) {
            for (String entry : raw.split("\\|")) {
                String[] p = entry.split("~");
                if ((p.length != 3 && p.length != 4) || p[0].isBlank()) continue;
                ItemAnimation.Scene scene = ItemAnimation.Scene.fromName(p[1].trim());
                if (scene == null) continue;
                try {
                    int rgb = Integer.parseInt(p[2].trim().replace("#", ""), 16);
                    boolean always = p.length == 4 && p[3].trim().equalsIgnoreCase("always");
                    CUSTOM.add(new Entry(p[0].trim(), scene, rgb & 0xFFFFFF, always));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        List<Entry> all = new ArrayList<>(CUSTOM); // custom first so it wins ties
        all.addAll(BUILT_IN);
        all.sort(Comparator.comparingInt((Entry x) -> x.phrase().length()).reversed());
        sorted = all;
    }

    public static Entry find(String itemName) {
        String lower = itemName.toLowerCase();
        for (Entry x : sorted) {
            if (lower.contains(x.phrase().toLowerCase())) return x;
        }
        return null;
    }
}
