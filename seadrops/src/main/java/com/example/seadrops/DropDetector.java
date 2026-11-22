package com.example.seadrops;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.Map;

/**
 * Listens to incoming game messages. Purely passive: it never cancels or edits a message, so it
 * coexists with SkyHanni, Skyblocker, Enhanced Chat and the rest of the SkyBlock Enhanced pack.
 *
 * Boss drops: the drop message ("RARE DROP! Titanoboa Shed ...") does not name the mob, so the last
 * boss that spawned (Titanoboa, Wiki Tiki, Lord Jawbus, Ragnarok, Blue Ringed Octopus, Thunder) is
 * remembered for a few minutes and the next drop is attributed to it. The boss animation only plays
 * when something actually drops - not when the boss spawns.
 */
public final class DropDetector {

    private static final Map<String, ItemAnimation.Scene> BOSS_SCENES = Map.of(
            "Titanoboa", ItemAnimation.Scene.BEHEAD,
            "Wiki Tiki", ItemAnimation.Scene.TIKI,
            "Lord Jawbus", ItemAnimation.Scene.JAWBUS,
            "Ragnarok", ItemAnimation.Scene.RAGNAROK,
            "Blue Ringed Octopus", ItemAnimation.Scene.OCTOPUS,
            "Thunder", ItemAnimation.Scene.THUNDER);

    private static final long COOLDOWN_MS = 1000;
    private static long lastTrigger = 0;

    private static MobCatalog.Mob currentBoss = null;
    private static long bossUntil = 0;

    private DropDetector() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handle(message);
        });
    }

    private static void handle(Component message) {
        if (!Config.enabled) return;
        String text = message.getString(); // formatting codes already stripped

        // 1) An item drop ("RARE DROP! <item> (+x Magic Find)")
        if (Config.dropAnimations && text.contains("DROP!")) {
            if (handleDrop(text)) return;
        }

        // 2) A known fishing mob spawning
        MobCatalog.Mob mob = MobCatalog.find(text);
        if (mob != null) {
            boolean boss = BOSS_SCENES.containsKey(mob.name());
            if (boss) {
                currentBoss = mob;
                bossUntil = System.currentTimeMillis() + Config.bossWindowSec * 1000L;
            }
            boolean show = Config.mobAnimations && (!boss || Config.bossCatchAnimation);
            if (show && mob.rarity().ordinal() >= Config.minRarity.ordinal()
                    && Config.isThemeOn(mob.theme()) && cooldownOk()) {
                AnimationOverlay.INSTANCE.playMob(mob);
            }
            return;
        }

        // 3) Generic "GREAT CATCH!" style messages -> rarity-coloured burst
        if (Config.genericAnimations) {
            String upper = text.toUpperCase();
            for (String trigger : Config.triggers) {
                String t = trigger == null ? "" : trigger.trim();
                if (t.isEmpty()) continue;
                int idx = upper.indexOf(t.toUpperCase());
                if (idx < 0) continue;
                if (text.substring(0, idx).contains(":")) return; // spoofed player chat

                int color = colorOf(message);
                Rarity r = Rarity.fromColor(color);
                if (r != null && r.ordinal() < Config.minRarity.ordinal()) return;
                if (!Config.isThemeOn(Theme.GENERIC) || !cooldownOk()) return;

                String subtitle = text.substring(idx + t.length()).trim();
                AnimationOverlay.INSTANCE.playGeneric(t, subtitle, color);
                return;
            }
        }
    }

    /** Returns true when the message was a drop line (whether or not an animation played). */
    private static boolean handleDrop(String text) {
        int idx = text.indexOf("DROP!");
        if (text.substring(0, idx).contains(":")) return true; // someone typing in chat

        String item = text.substring(idx + "DROP!".length()).trim();
        int paren = item.indexOf(" (");
        if (paren > 0) item = item.substring(0, paren);
        item = item.trim();
        if (item.isEmpty()) return false;

        double value = Config.valueAlerts ? PriceService.valueOf(item) : -1;
        ItemAnimation.Scene scene = null;
        String bossLabel = null;
        int rgb = 0;

        ItemCatalog.Entry entry = ItemCatalog.find(item);
        boolean bossActive = currentBoss != null && System.currentTimeMillis() < bossUntil;

        // Boss drops only count when worth the threshold. A known lower price skips the animation;
        // an unknown price still animates (these messages only appear for rare drops).
        boolean cheap = Config.bossOnlyValuable && value >= 0 && value < Config.valueThreshold;

        if (entry != null) {
            if (!cheap || entry.always()) {
                scene = entry.scene();
                rgb = entry.rgb();
                bossLabel = bossNameFor(scene);
            }
        } else if (bossActive) {
            if (!cheap) {
                scene = BOSS_SCENES.get(currentBoss.name());
                rgb = scene.primary;
                bossLabel = currentBoss.name();
            }
        } else if (Config.valueAlerts && value >= Config.valueThreshold) {
            scene = ItemAnimation.Scene.COINS;
            rgb = scene.primary;
        }

        if (scene == null || !cooldownOk()) return true;
        ItemAnimation.INSTANCE.play(scene, item, bossLabel, value, rgb);
        return true;
    }

    /** Name of the boss a scene belongs to (null for scenes with no single boss, e.g. coins). */
    private static String bossNameFor(ItemAnimation.Scene scene) {
        if (scene == ItemAnimation.Scene.VIAL) return "Lord Jawbus"; // the vial drops from Jawbus
        for (Map.Entry<String, ItemAnimation.Scene> e : BOSS_SCENES.entrySet()) {
            if (e.getValue() == scene) return e.getKey();
        }
        return null;
    }

    private static boolean cooldownOk() {
        long now = System.currentTimeMillis();
        if (now - lastTrigger < COOLDOWN_MS) return false;
        lastTrigger = now;
        return true;
    }

    /** Last non-white/grey colour in the message, usually the item's rarity colour. */
    private static int colorOf(Component message) {
        int color = Rarity.LEGENDARY.color;
        for (Component part : message.toFlatList()) {
            if (part.getString().isBlank()) continue;
            TextColor tc = part.getStyle().getColor();
            if (tc == null) continue;
            int rgb = tc.getValue() & 0xFFFFFF;
            if (rgb == 0xFFFFFF || rgb == 0xAAAAAA) continue;
            color = rgb;
        }
        return color;
    }
}
