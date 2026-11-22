package com.example.seadrops;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * HUD layer. Every theme has its own particles + backdrop:
 * water = fountain splash and rising tide, lava = embers and heat glow, thunder = lightning bolts,
 * ice = snowfall and frost, bayou = bubbles and swamp fog, spooky = wisps and vignette,
 * deep = crystal sparkle and light beam. Higher rarity = more particles and more rings.
 */
public final class AnimationOverlay {

    public static final AnimationOverlay INSTANCE = new AnimationOverlay();

    private static final long DURATION_MS = 3800;
    private static final float CENTER_Y = 1f / 3f;
    private static final int[] RAINBOW = {0xFF55FF, 0x55FFFF, 0xFFFF55, 0x55FF55};

    private static final class Particle {
        float fx, fy, ox, oy, vx, vy, ax, ay, wobble, wobbleSpeed, life, delay;
        int size, color;
        boolean twinkle;
    }

    private static final class Bolt {
        int[] dx;
        float start;
    }

    private final Random rng = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();

    private long startTime = -1;
    private String title = "";
    private Component subtitle = Component.empty();
    private Theme theme = Theme.GENERIC;
    private int tier = Rarity.LEGENDARY.ordinal();
    private int accent = Rarity.LEGENDARY.color;

    private AnimationOverlay() {}

    // ------------------------------------------------------------------ entry points

    public void playMob(MobCatalog.Mob mob) {
        begin(mob.name(), rarityLine(mob.rarity()), mob.theme(), mob.rarity().ordinal(), mob.theme().primary);
    }

    public void playGeneric(String title, String subtitle, int rgb) {
        Rarity r = Rarity.fromColor(rgb);
        int t = r != null ? r.ordinal() : Rarity.LEGENDARY.ordinal();
        begin(title, Component.literal(subtitle).withColor(0xFFFFFF), Theme.GENERIC, t, rgb & 0xFFFFFF);
    }

    /** Menu preview of a location theme, using a sample creature. */
    public void playTheme(Theme t) {
        begin(t.sample, rarityLine(t.sampleRarity), t, t.sampleRarity.ordinal(), t.primary);
    }

    /** Menu preview of a rarity tier with the generic style. */
    public void playRarity(Rarity r) {
        playGeneric("GREAT CATCH!", "Test " + r.label + " drop", r.color);
    }

    private static Component rarityLine(Rarity r) {
        return Component.literal(r.label.toUpperCase()).withColor(r.color)
                .append(Component.literal("  Sea Creature").withColor(0xFFFFFF));
    }

    private void begin(String title, Component subtitle, Theme theme, int tier, int accent) {
        this.title = title;
        this.subtitle = subtitle;
        this.theme = theme;
        this.tier = tier;
        this.accent = accent & 0xFFFFFF;
        this.startTime = System.currentTimeMillis();

        particles.clear();
        bolts.clear();
        spawn();
        playSounds();
    }

    private void playSounds() {
        if (!Config.playSound) return;
        Minecraft mc = Minecraft.getInstance();
        float pitch = 0.8f + 0.1f * tier;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, pitch));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f));
        if (theme == Theme.THUNDER) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f));
        }
    }

    // ------------------------------------------------------------------ particle setup

    private Particle add(float fx, float fy, float ox, float oy, float vx, float vy,
                         float ax, float ay, float life, float delay, int size, int color) {
        Particle p = new Particle();
        p.fx = fx; p.fy = fy; p.ox = ox; p.oy = oy;
        p.vx = vx; p.vy = vy; p.ax = ax; p.ay = ay;
        p.life = life; p.delay = delay; p.size = size; p.color = color;
        particles.add(p);
        return p;
    }

    private int pick(int... colors) {
        return colors[rng.nextInt(colors.length)];
    }

    private void radial(int n, float minSpeed, float maxSpeed, float gravity, float life,
                        boolean twinkle, int... colors) {
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            float speed = minSpeed + rng.nextFloat() * (maxSpeed - minSpeed);
            Particle p = add(0.5f, CENTER_Y, 0, 0,
                    (float) Math.cos(a) * speed, (float) Math.sin(a) * speed,
                    0, gravity, life * (0.7f + rng.nextFloat() * 0.6f), 0, 2 + rng.nextInt(3), pick(colors));
            p.twinkle = twinkle;
        }
    }

    private void spawn() {
        float k = 0.6f + 0.2f * tier; // intensity
        int pc = theme.primary;
        int sc = theme.secondary;

        switch (theme) {
            case GENERIC -> {
                if (tier >= Rarity.MYTHIC.ordinal()) radial((int) (70 * k), 60, 210, 60, 1.4f, false, RAINBOW);
                else radial((int) (70 * k), 60, 210, 60, 1.4f, false, accent, accent, accent, 0xFFFFFF);
            }
            case WATER -> {
                // fountain from the bottom centre
                for (int i = 0; i < (int) (80 * k); i++) {
                    add(0.5f, 1.0f, (rng.nextFloat() - 0.5f) * 120, 0,
                            (rng.nextFloat() - 0.5f) * 260, -(260 + rng.nextFloat() * 220),
                            0, 520, 2.0f, rng.nextFloat() * 0.25f, 3 + rng.nextInt(2),
                            pick(pc, pc, sc, 0xFFFFFF));
                }
            }
            case LAVA -> {
                // embers drifting up across the whole screen
                for (int i = 0; i < (int) (70 * k); i++) {
                    Particle p = add(rng.nextFloat(), 1.02f, 0, 0,
                            (rng.nextFloat() - 0.5f) * 20, -(50 + rng.nextFloat() * 110),
                            0, -10, 2.6f, rng.nextFloat() * 0.8f, 2 + rng.nextInt(3),
                            pick(pc, sc, 0xFF2A00));
                    p.wobble = 6 + rng.nextFloat() * 8;
                    p.wobbleSpeed = 3 + rng.nextFloat() * 3;
                }
            }
            case THUNDER -> {
                radial((int) (40 * k), 40, 160, 0, 0.9f, false, pc, 0xFFFFFF);
                int count = Math.min(4, 2 + tier / 2);
                for (int i = 0; i < count; i++) {
                    Bolt b = new Bolt();
                    b.start = i * 0.13f;
                    b.dx = new int[9];
                    b.dx[0] = (int) ((rng.nextFloat() - 0.5f) * 120);
                    for (int j = 1; j < b.dx.length; j++) {
                        b.dx[j] = b.dx[j - 1] + (int) ((rng.nextFloat() - 0.5f) * 44);
                    }
                    bolts.add(b);
                }
            }
            case ICE -> {
                // snowfall from above
                for (int i = 0; i < (int) (90 * k); i++) {
                    Particle p = add(rng.nextFloat(), -0.05f, 0, 0,
                            (rng.nextFloat() - 0.5f) * 30, 30 + rng.nextFloat() * 70,
                            0, 0, 3.2f, rng.nextFloat() * 1.0f, 2 + rng.nextInt(3), pick(0xFFFFFF, pc));
                    p.wobble = 8 + rng.nextFloat() * 8;
                    p.wobbleSpeed = 2 + rng.nextFloat() * 2;
                }
                radial((int) (30 * k), 40, 120, 0, 1.0f, false, 0xFFFFFF, pc);
            }
            case BAYOU -> {
                // swamp bubbles rising
                for (int i = 0; i < (int) (60 * k); i++) {
                    Particle p = add(0.15f + rng.nextFloat() * 0.7f, 1.02f, 0, 0,
                            0, -(40 + rng.nextFloat() * 60), 0, 0, 3.0f, rng.nextFloat() * 1.0f,
                            3 + rng.nextInt(4), pick(pc, sc, 0x2E8B2E));
                    p.wobble = 10 + rng.nextFloat() * 10;
                    p.wobbleSpeed = 2 + rng.nextFloat() * 2;
                }
            }
            case SPOOKY -> {
                // drifting wisps
                for (int i = 0; i < (int) (50 * k); i++) {
                    Particle p = add(0.3f + rng.nextFloat() * 0.4f, 0.55f + rng.nextFloat() * 0.4f, 0, 0,
                            0, -(20 + rng.nextFloat() * 40), 0, 0, 2.8f, rng.nextFloat() * 0.8f,
                            2 + rng.nextInt(3), pick(pc, sc));
                    p.wobble = 14;
                    p.wobbleSpeed = 2 + rng.nextFloat() * 2;
                    p.twinkle = true;
                }
            }
            case DEEP -> radial((int) (70 * k), 30, 130, 0, 1.8f, true, pc, sc, 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ rendering

    /** Registered as a HUD element in SeaDropsClient. */
    public void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (startTime < 0) return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > DURATION_MS) {
            startTime = -1;
            return;
        }
        float t = elapsed / (float) DURATION_MS;
        float s = elapsed / 1000f;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = (int) (h * CENTER_Y);

        drawBackdrop(g, w, h, cx, cy, t, s, elapsed);

        drawRing(g, cx, cy, t, 0.00f, 100, accent);
        if (tier >= Rarity.UNCOMMON.ordinal()) drawRing(g, cx, cy, t, 0.08f, 75, accent);
        if (tier >= Rarity.EPIC.ordinal()) drawRing(g, cx, cy, t, 0.16f, 135, theme.secondary);

        drawParticles(g, w, h, s);
        for (Bolt b : bolts) drawBolt(g, b, cx, cy, s);
        drawText(g, cx, cy, w, t);
    }

    private void drawBackdrop(GuiGraphicsExtractor g, int w, int h, int cx, int cy,
                              float t, float s, long elapsed) {
        float env = envelope(t);

        if (Config.screenFlash) {
            if (theme == Theme.THUNDER) {
                // two sharp white flashes
                if (elapsed < 90) g.fill(0, 0, w, h, (110 << 24) | 0xFFFFFF);
                else if (elapsed > 170 && elapsed < 260) g.fill(0, 0, w, h, (90 << 24) | 0xFFFFFF);
            } else if (elapsed < 300) {
                int a = (int) (70 * (1 - elapsed / 300f));
                if (a > 3) g.fill(0, 0, w, h, (a << 24) | accent);
            }
        }

        switch (theme) {
            case WATER -> {
                // rising tide at the bottom
                float rise = t < 0.3f ? easeOut(t / 0.3f) : 1f;
                int level = (int) (h * 0.16f * rise);
                int a = (int) (70 * env);
                if (a > 3 && level > 0) {
                    g.fill(0, h - level, w, h, (a << 24) | theme.primary);
                    g.fill(0, h - level, w, h - level + 2, ((a + 40 > 255 ? 255 : a + 40) << 24) | theme.secondary);
                }
            }
            case LAVA -> bands(g, w, h, 0xFF4010, 0.035f, 12, 90 * env);
            case BAYOU -> bands(g, w, h, 0x2E8B2E, 0.045f, 10, 70 * env);
            case ICE -> {
                // frost creeping in from the screen edges
                for (int i = 0; i < 3; i++) {
                    int thick = 16 - i * 5;
                    int a = (int) ((55 - i * 15) * env);
                    if (a <= 3) continue;
                    int c = (a << 24) | 0xCFF3FF;
                    g.fill(0, 0, w, thick, c);
                    g.fill(0, h - thick, w, h, c);
                    g.fill(0, thick, thick, h - thick, c);
                    g.fill(w - thick, thick, w, h - thick, c);
                }
            }
            case SPOOKY -> {
                for (int i = 0; i < 3; i++) {
                    int thick = 24 - i * 7;
                    int a = (int) ((90 - i * 25) * env);
                    if (a <= 3) continue;
                    int c = (a << 24) | 0x2A0A3A;
                    g.fill(0, 0, w, thick, c);
                    g.fill(0, h - thick, w, h, c);
                    g.fill(0, thick, thick, h - thick, c);
                    g.fill(w - thick, thick, w, h - thick, c);
                }
            }
            case DEEP -> {
                // light beam dropping onto the title
                int a = (int) (80 * env);
                if (a > 3) {
                    g.fill(cx - 7, 0, cx + 7, cy, (a << 24) | theme.primary);
                    g.fill(cx - 2, 0, cx + 2, cy, (Math.min(255, a + 60) << 24) | 0xFFFFFF);
                }
            }
            default -> {
            }
        }
    }

    /** Stacked translucent bands rising from the bottom edge (heat glow / swamp fog). */
    private void bands(GuiGraphicsExtractor g, int w, int h, int rgb, float bandFrac, int count, float maxAlpha) {
        int bh = Math.max(2, (int) (h * bandFrac));
        for (int i = 0; i < count; i++) {
            int a = (int) (maxAlpha * (1 - i / (float) count));
            if (a <= 3) continue;
            int y = h - (i + 1) * bh;
            g.fill(0, y, w, y + bh, (a << 24) | rgb);
        }
    }

    private void drawRing(GuiGraphicsExtractor g, int cx, int cy, float t, float delay, int maxRadius, int rgb) {
        float rt = (t - delay) / 0.45f;
        if (rt < 0 || rt > 1) return;
        float radius = easeOut(rt) * maxRadius;
        int alpha = (int) (200 * (1 - rt));
        if (alpha <= 3) return;
        int color = (alpha << 24) | (rgb & 0xFFFFFF);
        int steps = 72;
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) (Math.cos(a) * radius);
            int y = cy + (int) (Math.sin(a) * radius);
            g.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    private void drawParticles(GuiGraphicsExtractor g, int w, int h, float s) {
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            float age = s - p.delay;
            if (age < 0 || age > p.life) continue;
            float f = age / p.life;

            float fade = Math.min(1f, f / 0.1f) * (1 - f);
            int alpha = (int) (255 * fade);
            if (alpha <= 3) continue;
            if (p.twinkle && ((int) (age * 12) + i) % 2 == 0) continue;

            float x = p.fx * w + p.ox + p.vx * age + 0.5f * p.ax * age * age
                    + (float) Math.sin(age * p.wobbleSpeed) * p.wobble;
            float y = p.fy * h + p.oy + p.vy * age + 0.5f * p.ay * age * age;
            int ix = (int) x;
            int iy = (int) y;
            g.fill(ix, iy, ix + p.size, iy + p.size, (alpha << 24) | p.color);
        }
    }

    private void drawBolt(GuiGraphicsExtractor g, Bolt b, int cx, int cy, float s) {
        float age = s - b.start;
        if (age < 0 || age > 0.45f) return;
        int a = (int) (255 * (1 - age / 0.45f));
        if (((int) (age * 40)) % 3 == 2) a /= 2; // flicker
        if (a <= 3) return;

        int core = (a << 24) | 0xFFFFFF;
        int glow = ((a / 3) << 24) | theme.primary;
        int n = b.dx.length;
        for (int i = 0; i < n - 1; i++) {
            int x1 = cx + b.dx[i];
            int x2 = cx + b.dx[i + 1];
            int y1 = (int) (cy * (i / (float) (n - 1)));
            int y2 = (int) (cy * ((i + 1) / (float) (n - 1)));
            int steps = Math.max(1, (y2 - y1) / 2);
            for (int st = 0; st < steps; st++) {
                int px = x1 + (x2 - x1) * st / steps;
                int py = y1 + (y2 - y1) * st / steps;
                g.fill(px - 3, py, px + 4, py + 2, glow);
                g.fill(px - 1, py, px + 1, py + 2, core);
            }
        }
    }

    private void drawText(GuiGraphicsExtractor g, int cx, int cy, int w, float t) {
        Font font = Minecraft.getInstance().font;

        float fade = t > 0.8f ? (1 - t) / 0.2f : 1f;
        int alpha = Math.max(5, (int) (255 * fade));

        // pop-in with a little overshoot
        float pop;
        if (t < 0.10f) pop = t / 0.10f * 1.3f;
        else if (t < 0.18f) pop = 1.3f - (t - 0.10f) / 0.08f * 0.3f;
        else pop = 1f;

        Component big = Component.literal(title).withStyle(ChatFormatting.BOLD);
        int tw = Math.max(1, font.width(big));
        float scale = Math.min(3f, (w * 0.85f) / tw) * pop;

        Matrix3x2fStack pose = g.pose();

        pose.pushMatrix();
        pose.translate(cx, cy - 10);
        pose.scale(scale, scale);
        g.text(font, big, -tw / 2, -font.lineHeight / 2, (alpha << 24) | accent, true);
        pose.popMatrix();

        if (t > 0.15f) {
            float sub = Math.min(1f, (t - 0.15f) / 0.1f);
            int subAlpha = Math.max(5, (int) (alpha * sub));
            int sw = font.width(subtitle);
            pose.pushMatrix();
            pose.translate(cx, cy + 18);
            pose.scale(1.5f, 1.5f);
            g.text(font, subtitle, -sw / 2, 0, (subAlpha << 24) | 0xFFFFFF, true);
            pose.popMatrix();
        }
    }

    private static float envelope(float t) {
        float up = Math.min(1f, t / 0.1f);
        float down = Math.min(1f, (1 - t) / 0.3f);
        return Math.max(0f, Math.min(up, down));
    }

    private static float easeOut(float x) {
        float inv = 1 - x;
        return 1 - inv * inv * inv;
    }
}
