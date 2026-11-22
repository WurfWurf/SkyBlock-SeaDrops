package com.example.seadrops;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The big drop sequences. One hand-built scene per boss, all drawn from rectangles/discs/lines so
 * they run on any GUI scale. A scene plays its action, then reveals the item (gem, name, boss, value).
 *
 *  BEHEAD    Titanoboa: the serpent slithers in, a slash cuts its head off, the item rises from the stump
 *  TIKI      Wiki Tiki: a glowing tiki mask shakes, cracks and shatters
 *  JAWBUS    Lord Jawbus: lava jaws rise, snap shut, then spit the item out
 *  RAGNAROK  rune circle, meteor strikes, a pillar of light
 *  OCTOPUS   Blue Ringed Octopus: ink cloud, tentacles and pulsing blue rings
 *  THUNDER   three lightning strikes, the last one huge
 *  VIAL      Radioactive Vial: a bubbling vial cracks and bursts into gas
 *  COINS     any drop worth over your threshold: coin shower
 *  SLASH     generic crossing slash (for customItems)
 */
public final class ItemAnimation {

    public static final ItemAnimation INSTANCE = new ItemAnimation();

    public enum Scene {
        BEHEAD("Titanoboa", 0x5BD35B, 0xC8E86A, 2.0f),
        TIKI("Wiki Tiki", 0xFF9A2E, 0x2FBF5B, 2.2f),
        JAWBUS("Lord Jawbus", 0xFF4A1A, 0xFFD23F, 2.4f),
        RAGNAROK("Ragnarok", 0xE0245E, 0xFFC247, 2.4f),
        OCTOPUS("Blue Ringed Octopus", 0x2FA8FF, 0x9AF0FF, 2.3f),
        THUNDER("Thunder", 0xFFE94D, 0xFFFFFF, 1.9f),
        VIAL("Radioactive Vial", 0x7CFC00, 0xE6FF8A, 2.2f),
        COINS("Big drop", 0xFFC832, 0xFFF1A8, 1.0f),
        SLASH("Slash", 0xFF5A5A, 0xFFFFFF, 1.2f);

        public final String label;
        public final int primary;
        public final int secondary;
        public final float revealAt;

        Scene(String label, int primary, int secondary, float revealAt) {
            this.label = label;
            this.primary = primary;
            this.secondary = secondary;
            this.revealAt = revealAt;
        }

        public static Scene fromName(String n) {
            if (n == null) return null;
            String k = n.trim().toLowerCase(Locale.ROOT).replace(" ", "");
            for (Scene s : values()) {
                if (s.name().toLowerCase(Locale.ROOT).equals(k)
                        || s.label.toLowerCase(Locale.ROOT).replace(" ", "").equals(k)) return s;
            }
            return switch (k) {
                case "titanboa", "snake" -> BEHEAD;
                case "wikitiki" -> TIKI;
                case "lordjawbus" -> JAWBUS;
                case "octopus" -> OCTOPUS;
                case "coin", "value" -> COINS;
                default -> null;
            };
        }
    }

    private static final class P {
        float ox, oy, vx, vy, ay, delay, life;
        int size, color;
    }

    private static final float TOTAL = 5.6f;

    private final Random rng = new Random();
    private final List<P> ps = new ArrayList<>();
    private final List<float[]> chunks = new ArrayList<>(); // tiki: lx, ly, vx, vy
    private final List<float[]> bolts = new ArrayList<>();  // thunder: time, offsetX, dx[0..9]

    private float[] impacts = new float[0];
    private float shakeMag = 4;
    private int nextImpact = 0;
    private boolean revealSounded = false;

    private long start = -1;
    private Scene scene = Scene.COINS;
    private String itemName = "";
    private String bossLine = "";
    private double value = -1;
    private int accent = 0xFFFFFF;

    private float gA = 1f; // global alpha (end fade)
    private int W, H;

    private ItemAnimation() {}

    // ------------------------------------------------------------------ entry points

    public void play(Scene sc, String item, String boss, double val, int rgb) {
        scene = sc;
        itemName = item;
        bossLine = boss == null ? sc.label : boss;
        value = val;
        accent = rgb & 0xFFFFFF;
        start = System.currentTimeMillis();
        prepare();
    }

    public void playPreview(Scene sc) {
        String item = switch (sc) {
            case BEHEAD -> "Titanoboa Shed";
            case VIAL -> "Radioactive Vial";
            default -> "Preview Item";
        };
        play(sc, item, sc.label, 2_400_000, sc.primary);
    }

    // ------------------------------------------------------------------ setup

    private void burst(float delay, int n, float ox, float oy, float vmin, float vmax,
                       float vyBias, float ay, float life, int... colors) {
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            float sp = vmin + rng.nextFloat() * (vmax - vmin);
            P p = new P();
            p.ox = ox;
            p.oy = oy;
            p.vx = (float) Math.cos(a) * sp;
            p.vy = (float) Math.sin(a) * sp + vyBias;
            p.ay = ay;
            p.delay = delay;
            p.life = life * (0.7f + rng.nextFloat() * 0.6f);
            p.size = 2 + rng.nextInt(3);
            p.color = colors[rng.nextInt(colors.length)];
            ps.add(p);
        }
    }

    private void drift(int n, float spread, float oy, float vmin, float vmax, float life,
                       float maxDelay, int... colors) {
        for (int i = 0; i < n; i++) {
            P p = new P();
            p.ox = (rng.nextFloat() - 0.5f) * 2 * spread;
            p.oy = oy;
            p.vx = (rng.nextFloat() - 0.5f) * 14;
            p.vy = -(vmin + rng.nextFloat() * (vmax - vmin));
            p.life = life;
            p.delay = rng.nextFloat() * maxDelay;
            p.size = 2 + rng.nextInt(2);
            p.color = colors[rng.nextInt(colors.length)];
            ps.add(p);
        }
    }

    private void prepare() {
        ps.clear();
        chunks.clear();
        bolts.clear();
        impacts = new float[0];
        shakeMag = 4;
        nextImpact = 0;
        revealSounded = false;

        switch (scene) {
            case BEHEAD -> {
                impacts = new float[]{1.25f};
                burst(1.25f, 34, 6, 0, 40, 160, -120, 420, 0.9f, 0x5BD35B, 0xC8E86A, 0x2E8B2E);
            }
            case TIKI -> {
                impacts = new float[]{1.7f};
                shakeMag = 5;
                for (int gx = 0; gx < 4; gx++) {
                    for (int gy = 0; gy < 5; gy++) {
                        float dx = gx - 1.5f, dy = gy - 2f;
                        chunks.add(new float[]{gx * 10 - 20, gy * 10 - 25,
                                dx * 38 + (rng.nextFloat() - 0.5f) * 30,
                                dy * 30 - 90 + (rng.nextFloat() - 0.5f) * 30});
                    }
                }
                burst(1.7f, 44, 0, 0, 50, 190, -60, 380, 1.0f, 0xFF9A2E, 0xFFD23F, 0x2FBF5B, 0xD62F2F);
            }
            case JAWBUS -> {
                impacts = new float[]{1.35f};
                shakeMag = 6;
                burst(1.35f, 50, 0, 30, 60, 200, -160, 300, 1.2f, 0xFF4A1A, 0xFFD23F, 0xFF2A00);
                drift(40, 180, 130, 40, 120, 2.0f, 2.0f, 0xFF4A1A, 0xFFD23F, 0xFF2A00);
            }
            case RAGNAROK -> {
                impacts = new float[]{1.0f, 1.3f, 1.6f};
                shakeMag = 5;
                float[] ox = {-35, 30, 0};
                for (int i = 0; i < 3; i++) {
                    burst(impacts[i], 22, ox[i], 30, 40, 150, -90, 320, 0.9f, 0xFFC247, 0xE0245E, 0xFFFFFF);
                }
                drift(40, 16, 40, 60, 140, 1.6f, 1.0f, 0xFFC247, 0xFFFFFF);
                for (P p : ps) {
                    if (p.vy < 0 && p.vx != 0 && p.delay < 1.0f && p.life > 1.5f) p.delay += 2.0f;
                }
            }
            case OCTOPUS -> {
                drift(40, 180, 130, 30, 80, 3.0f, 3.0f, 0x9AF0FF, 0x2FA8FF);
                burst(2.2f, 30, 0, 0, 40, 150, 0, 60, 1.2f, 0x2FA8FF, 0x9AF0FF, 0xFFFFFF);
            }
            case THUNDER -> {
                impacts = new float[]{0.5f, 0.85f, 1.2f};
                shakeMag = 5;
                float[] times = {0.5f, 0.85f, 1.2f};
                float[] offs = {-50, 45, 0};
                for (int b = 0; b < 3; b++) {
                    float[] arr = new float[12];
                    arr[0] = times[b];
                    arr[1] = offs[b];
                    float[] dx = new float[10];
                    for (int j = 1; j < 10; j++) dx[j] = dx[j - 1] + (rng.nextFloat() - 0.5f) * 30;
                    for (int j = 0; j < 10; j++) arr[2 + j] = dx[j] - dx[9] * j / 9f; // ends on target
                    bolts.add(arr);
                }
                burst(1.2f, 50, 0, 40, 60, 220, -80, 300, 0.8f, 0xFFE94D, 0xFFFFFF);
            }
            case VIAL -> {
                impacts = new float[]{1.5f};
                shakeMag = 3;
                burst(1.5f, 36, 0, 0, 50, 170, -100, 360, 1.1f, 0xBFEFF7, 0xBFEFF7, 0x7CFC00, 0xE6FF8A);
            }
            case COINS -> {
                int[] cols = {0xFFC832, 0xFFE066, 0xFFA81A};
                for (int i = 0; i < 70; i++) {
                    P p = new P();
                    p.ox = (rng.nextFloat() - 0.5f) * 400;
                    p.oy = -(140 + rng.nextFloat() * 120);
                    p.vx = (rng.nextFloat() - 0.5f) * 20;
                    p.vy = 110 + rng.nextFloat() * 90;
                    p.ay = 150;
                    p.delay = rng.nextFloat() * 0.7f;
                    p.life = 2.4f;
                    p.size = 5;
                    p.color = cols[rng.nextInt(cols.length)];
                    ps.add(p);
                }
            }
            case SLASH -> {
                impacts = new float[]{0.35f};
                burst(0.35f, 40, 0, 0, 60, 200, -60, 300, 1.0f, accent, 0xFFFFFF);
            }
        }
    }

    // ------------------------------------------------------------------ drawing helpers

    private int col(int rgb, float a) {
        int ai = Math.max(0, Math.min(255, Math.round(a * gA * 255f)));
        return (ai << 24) | (rgb & 0xFFFFFF);
    }

    private void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int rgb, float a) {
        if (w <= 0 || h <= 0 || a * gA < 0.01f) return;
        g.fill(x, y, x + w, y + h, col(rgb, a));
    }

    private void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int rgb, float a) {
        if (r <= 0 || a * gA < 0.01f) return;
        int c = col(rgb, a);
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, c);
        }
    }

    private void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int th, int rgb, float a) {
        if (r <= 0 || a * gA < 0.01f) return;
        int c = col(rgb, a);
        int ri = Math.max(0, r - th);
        for (int dy = -r; dy <= r; dy++) {
            int ho = (int) Math.sqrt(r * r - dy * dy);
            if (Math.abs(dy) < ri) {
                int hi = (int) Math.sqrt(ri * ri - dy * dy);
                g.fill(cx - ho, cy + dy, cx - hi, cy + dy + 1, c);
                g.fill(cx + hi + 1, cy + dy, cx + ho + 1, cy + dy + 1, c);
            } else {
                g.fill(cx - ho, cy + dy, cx + ho + 1, cy + dy + 1, c);
            }
        }
    }

    private void line(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int th, int rgb, float a) {
        if (a * gA < 0.01f) return;
        int c = col(rgb, a);
        int n = (int) Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        for (int i = 0; i <= n; i++) {
            int x = (int) (x1 + (x2 - x1) * i / n);
            int y = (int) (y1 + (y2 - y1) * i / n);
            g.fill(x, y, x + th, y + th, c);
        }
    }

    private void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int rgb, float a) {
        if (a * gA < 0.01f) return;
        int c = col(rgb, a);
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, c);
        }
    }

    private static float ease(float x) {
        float inv = 1 - x;
        return 1 - inv * inv * inv;
    }

    private static float ramp(float s, float over) {
        return Math.max(0f, Math.min(1f, s / over));
    }

    // ------------------------------------------------------------------ render loop

    /** Registered as a HUD element in SeaDropsClient. */
    public void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (start < 0) return;

        float s = (System.currentTimeMillis() - start) / 1000f;
        if (s > TOTAL) {
            start = -1;
            return;
        }
        W = g.guiWidth();
        H = g.guiHeight();
        int cx = W / 2;
        int sy = (int) (H * 0.36f);
        gA = s > TOTAL - 0.8f ? Math.max(0f, (TOTAL - s) / 0.8f) : 1f;

        soundCues(s);

        float sh = shake(s);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        if (sh > 0.2f) {
            pose.translate((float) ((Math.random() - 0.5) * 2 * sh), (float) ((Math.random() - 0.5) * 2 * sh));
        }

        switch (scene) {
            case BEHEAD -> behead(g, s, cx, sy);
            case TIKI -> tiki(g, s, cx, sy);
            case JAWBUS -> jawbus(g, s, cx, sy);
            case RAGNAROK -> ragnarok(g, s, cx, sy);
            case OCTOPUS -> octopus(g, s, cx, sy);
            case THUNDER -> thunder(g, s, cx, sy);
            case VIAL -> vial(g, s, cx, sy);
            case COINS -> coins(g, s, cx, sy);
            case SLASH -> slash(g, s, cx, sy);
        }

        drawParticles(g, s, cx, sy);
        reveal(g, s, cx, sy);
        pose.popMatrix();
    }

    private float shake(float s) {
        float sum = 0;
        for (float t : impacts) {
            float dt = s - t;
            if (dt >= 0 && dt < 0.4f) sum += shakeMag * (1 - dt / 0.4f);
        }
        if (scene == Scene.JAWBUS && s > 1.45f && s < 1.9f) sum += 1.5f;
        return sum;
    }

    private void soundCues(float s) {
        while (nextImpact < impacts.length && s >= impacts[nextImpact]) {
            nextImpact++;
            if (scene == Scene.THUNDER) sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f);
            else sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f);
        }
        if (!revealSounded && s >= scene.revealAt) {
            revealSounded = true;
            sound(SoundEvents.PLAYER_LEVELUP, 0.8f + 0.05f * scene.ordinal());
        }
    }

    private void sound(SoundEvent ev, float pitch) {
        if (!Config.playSound) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch));
    }

    private void drawParticles(GuiGraphicsExtractor g, float s, int cx, int sy) {
        for (P p : ps) {
            float age = s - p.delay;
            if (age < 0 || age > p.life) continue;
            float f = age / p.life;
            float a = Math.min(1f, f / 0.08f) * (1 - f * f);
            int x = (int) (cx + p.ox + p.vx * age);
            int y = (int) (sy + p.oy + p.vy * age + 0.5f * p.ay * age * age);
            rect(g, x, y, p.size, p.size, p.color, a);
        }
    }

    // ------------------------------------------------------------------ scenes

    private void behead(GuiGraphicsExtractor g, float s, int cx, int sy) {
        final int g1 = 0x5BD35B, g2 = 0x2E8B2E, belly = 0xC8E86A;
        final float cut = 1.25f;
        rect(g, 0, 0, W, H, 0x061A0C, 0.30f * ramp(s, 0.4f));

        float ap = ease(Math.min(1f, s / 0.9f));
        float hx = cx + 130 - 130 * ap;

        for (int i = 14; i >= 1; i--) {
            float wave = (float) Math.sin(s * 5 + i * 0.7) * 8;
            float x = hx + 6 + i * 9, y = sy + wave, fall = 0, a = 1;
            if (s > cut) {
                float dt = s - cut;
                fall = dt * dt * 60 * (0.3f + i / 14f);
                x += dt * 14 * i / 14f;
                a = Math.max(0f, 1 - dt / 1.0f);
            }
            rect(g, (int) x - 5, (int) (y + fall) - 5, 10, 10, i % 2 == 0 ? g1 : g2, a);
            rect(g, (int) x - 3, (int) (y + fall) + 1, 6, 3, belly, a * 0.9f);
        }

        float hy = sy + (float) Math.sin(s * 5) * 8, hxx = hx, ha = 1;
        if (s > cut) {
            float dt = s - cut;
            hxx = cx - 70 * dt;
            hy = sy - 150 * dt + 190 * dt * dt;
            ha = Math.max(0f, 1 - Math.max(0f, dt - 0.8f) / 0.5f);
        }
        serpentHead(g, (int) hxx, (int) hy, ha, s);

        if (s >= 1.1f && s < 1.6f) {
            float p = Math.min(1f, (s - 1.1f) / 0.15f);
            float fa = 1 - (s - 1.1f) / 0.5f;
            float x1 = cx - 35, y1 = sy - 52, x2 = cx + 50, y2 = sy + 52;
            line(g, x1, y1, x1 + (x2 - x1) * p, y1 + (y2 - y1) * p, 7, 0x9AFFB0, fa * 0.4f);
            line(g, x1, y1, x1 + (x2 - x1) * p, y1 + (y2 - y1) * p, 3, 0xFFFFFF, fa);
        }
        if (s >= 1.25f && s < 1.42f) rect(g, 0, 0, W, H, 0xFFFFFF, 0.35f * (1 - (s - 1.25f) / 0.17f));
    }

    private void serpentHead(GuiGraphicsExtractor g, int x, int y, float a, float s) {
        rect(g, x - 9, y - 7, 18, 14, 0x5BD35B, a);
        rect(g, x - 9, y + 3, 18, 4, 0xC8E86A, a * 0.9f);
        rect(g, x - 14, y - 3, 6, 8, 0x4CB84C, a);
        rect(g, x - 4, y - 5, 3, 3, 0xFFE14D, a);
        rect(g, x + 2, y - 5, 3, 3, 0xFFE14D, a);
        rect(g, x - 3, y - 5, 1, 3, 0x000000, a);
        rect(g, x + 3, y - 5, 1, 3, 0x000000, a);
        rect(g, x - 2, y - 7, 2, 2, 0x2E8B2E, a);
        rect(g, x + 4, y - 7, 2, 2, 0x2E8B2E, a);
        if (((int) (s * 8)) % 2 == 0) {
            rect(g, x - 22, y + 1, 8, 1, 0xE03030, a);
            rect(g, x - 24, y - 1, 2, 2, 0xE03030, a);
            rect(g, x - 24, y + 2, 2, 2, 0xE03030, a);
        }
    }

    private void tiki(GuiGraphicsExtractor g, float s, int cx, int sy) {
        rect(g, 0, 0, W, H, 0x1A0E05, 0.35f * ramp(s, 0.5f));
        float rise = 1 - ease(Math.min(1f, s / 0.5f));
        float amp = (s > 0.5f && s < 1.7f) ? (s - 0.5f) * 2.5f : 0;
        int dx = (int) (Math.sin(s * 70) * amp);
        int mx = cx - 20 + dx, my = sy - 25 + (int) (rise * 40);
        float a = Math.min(1f, s / 0.35f);
        float glow = 0.5f + 0.5f * (float) Math.sin(s * 9);

        if (s < 1.7f) {
            disc(g, cx, sy, 34 + (int) (4 * glow), 0xFF9A2E, 0.10f * a);
            tikiMask(g, mx, my, a, glow);
            // cracks creep across the mask
            float p = Math.min(1f, Math.max(0f, (s - 1.35f) / 0.35f));
            if (p > 0) {
                int[][] pts = {{-2, -25}, {4, -12}, {-3, 0}, {5, 10}, {0, 25}};
                int segs = (int) Math.ceil(p * 4);
                for (int i = 0; i < segs && i < 4; i++) {
                    line(g, cx + pts[i][0], sy + pts[i][1], cx + pts[i + 1][0], sy + pts[i + 1][1], 4, 0xFF9A2E, 0.3f);
                    line(g, cx + pts[i][0], sy + pts[i][1], cx + pts[i + 1][0], sy + pts[i + 1][1], 2, 0xFFE066, 1f);
                }
            }
        } else {
            float dt = s - 1.7f;
            int[] shades = {0x7A4A21, 0x6B3F1B, 0x8A5628};
            for (int i = 0; i < chunks.size(); i++) {
                float[] c = chunks.get(i);
                int x = (int) (cx + c[0] + c[2] * dt);
                int y = (int) (sy + c[1] + c[3] * dt + 210 * dt * dt);
                rect(g, x, y, 10, 10, shades[i % 3], Math.max(0f, 1 - dt / 1.4f));
            }
            if (s < 1.9f) rect(g, 0, 0, W, H, 0xFF9A2E, 0.35f * (1 - (s - 1.7f) / 0.2f));
        }
    }

    private void tikiMask(GuiGraphicsExtractor g, int x, int y, float a, float glow) {
        rect(g, x - 2, y - 2, 44, 54, 0x3A200C, a);
        rect(g, x, y, 40, 50, 0x7A4A21, a);
        rect(g, x + 2, y + 2, 36, 46, 0x8A5628, a);
        rect(g, x + 2, y + 24, 36, 2, 0x6B3F1B, a);
        int[] fc = {0x2FBF5B, 0xFF9A2E, 0xD62F2F, 0xFF9A2E, 0x2FBF5B};
        for (int i = 0; i < 5; i++) {
            int extra = i == 2 ? 8 : (i % 2 == 0 ? 2 : 5);
            rect(g, x + i * 8 + 1, y - 8 - extra, 6, 10 + extra, fc[i], a);
        }
        rect(g, x + 4, y + 10, 32, 4, 0x3A200C, a);
        rect(g, x + 6, y + 16, 11, 10, 0x120802, a);
        rect(g, x + 23, y + 16, 11, 10, 0x120802, a);
        float pa = a * (0.6f + 0.4f * glow);
        rect(g, x + 9, y + 19, 5, 5, 0xFFB020, pa);
        rect(g, x + 26, y + 19, 5, 5, 0xFFB020, pa);
        rect(g, x + 17, y + 26, 6, 12, 0x5A3417, a);
        rect(g, x + 8, y + 40, 24, 7, 0x120802, a);
        for (int t = 0; t < 4; t++) {
            rect(g, x + 10 + t * 6, y + 40, 3, 3, 0xF2E8D0, a);
            rect(g, x + 10 + t * 6, y + 44, 3, 3, 0xF2E8D0, a);
        }
    }

    private float jawGap(float s) {
        if (s < 0.7f) return 34;
        if (s < 1.3f) return 34 + 3 * (float) Math.sin(s * 14);
        if (s < 1.42f) return 34 + (-5 - 34) * ((s - 1.3f) / 0.12f);
        if (s < 1.9f) return -5;
        return -5 + 35 * ease(Math.min(1f, (s - 1.9f) / 0.5f));
    }

    private void jawbus(GuiGraphicsExtractor g, float s, int cx, int sy) {
        rect(g, 0, 0, W, H, 0x2A0500, 0.35f * ramp(s, 0.5f));
        for (int i = 0; i < 10; i++) {
            rect(g, 0, H - (i + 1) * 5, W, 5, 0xFF4010, 0.35f * (1 - i / 10f) * ramp(s, 0.5f));
        }

        float gap = jawGap(s);
        int off = (int) ((1 - ease(Math.min(1f, s / 0.7f))) * 110);
        int yU = (int) (sy - gap) - off;
        int yL = (int) (sy + gap) + off;
        int[] tw = {10, 8, 6, 3};
        int bone = 0xF2E8D0;

        rect(g, cx - 56, yU - 26, 112, 14, 0x5A0E06, 1f);
        rect(g, cx - 56, yU - 26, 112, 3, 0xA3200C, 1f);
        rect(g, cx - 50, yU - 34, 4, 8, 0x3A0804, 1f);
        rect(g, cx + 46, yU - 34, 4, 8, 0x3A0804, 1f);
        float eye = 0.6f + 0.4f * (float) Math.sin(s * 10);
        rect(g, cx - 34, yU - 22, 7, 3, 0xFFD23F, eye);
        rect(g, cx + 27, yU - 22, 7, 3, 0xFFD23F, eye);
        rect(g, cx - 56, yL + 12, 112, 14, 0x5A0E06, 1f);
        rect(g, cx - 56, yL + 23, 112, 3, 0xA3200C, 1f);

        for (int i = 0; i < 8; i++) {
            int tx = cx - 56 + 7 + i * 14;
            for (int r = 0; r < 4; r++) rect(g, tx - tw[r] / 2, yU - 12 + r * 3, tw[r], 3, bone, 1f);
        }
        for (int i = 0; i < 7; i++) {
            int tx = cx - 56 + 14 + i * 14;
            for (int r = 0; r < 4; r++) rect(g, tx - tw[r] / 2, yL + 9 - r * 3, tw[r], 3, bone, 1f);
        }

        if (s >= 1.3f && s < 1.45f) rect(g, 0, 0, W, H, 0xFF7A2E, 0.35f * (1 - (s - 1.3f) / 0.15f));
        if (s >= 1.42f && s < 1.92f) {
            float dt = s - 1.42f;
            ring(g, cx, sy, (int) (dt * 220), 3, 0xFFD23F, 1 - dt / 0.5f);
        }
    }

    private void ragnarok(GuiGraphicsExtractor g, float s, int cx, int sy) {
        rect(g, 0, 0, W, H, 0x2A0010, 0.50f * ramp(s, 1.0f));

        float R = ease(Math.min(1f, s / 0.8f)) * 50;
        if (R > 2) {
            ring(g, cx, sy, (int) R, 2, 0xE0245E, 0.9f);
            ring(g, cx, sy, (int) (R * 0.62f), 1, 0x7B2CBF, 0.8f);
            for (int k = 0; k < 12; k++) {
                double a1 = s * 1.1 + k * Math.PI / 6, a2 = s * 1.1 + (k + 4) * Math.PI / 6;
                line(g, (float) (cx + Math.cos(a1) * R), (float) (sy + Math.sin(a1) * R),
                        (float) (cx + Math.cos(a2) * R), (float) (sy + Math.sin(a2) * R), 1, 0xE0245E, 0.35f);
                rect(g, (int) (cx + Math.cos(a1) * R) - 1, (int) (sy + Math.sin(a1) * R) - 1, 3, 3, 0xFFC247, 1f);
            }
            for (int k = 0; k < 8; k++) {
                double a = -s * 1.6 + k * Math.PI / 4;
                rect(g, (int) (cx + Math.cos(a) * R * 0.62f), (int) (sy + Math.sin(a) * R * 0.62f), 2, 2, 0xB06CFF, 1f);
            }
            disc(g, cx, sy, 4 + (int) (2 * Math.sin(s * 8)), 0xFFC247, 0.8f);
        }

        float[] times = {1.0f, 1.3f, 1.6f};
        int[] ox = {-35, 30, 0};
        for (int i = 0; i < 3; i++) {
            int ix = cx + ox[i], iy = sy + 30;
            float lead = 0.35f;
            if (s >= times[i] - lead && s < times[i]) {
                float p = (s - (times[i] - lead)) / lead;
                float hx = ix + (1 - p) * 90, hy = iy - (1 - p) * 140;
                line(g, hx, hy, hx + 30, hy - 46, 5, 0xE0245E, 0.5f);
                line(g, hx, hy, hx + 30, hy - 46, 3, 0xFFC247, 1f);
                disc(g, (int) hx, (int) hy, 4, 0xFFFFFF, 1f);
            }
            float dt = s - times[i];
            if (dt >= 0 && dt < 0.5f) {
                ring(g, ix, iy, (int) (dt * 140), 3, 0xFFC247, 1 - dt / 0.5f);
                if (dt < 0.25f) disc(g, ix, iy, (int) (14 * (1 - dt / 0.25f)), 0xFFFFFF, 0.9f);
            }
        }

        if (s >= 1.9f) {
            float k = ease(Math.min(1f, (s - 1.9f) / 0.4f));
            int w = (int) (8 + 10 * k);
            rect(g, cx - w / 2, 0, w, sy, 0xFFC247, 0.35f * k);
            rect(g, cx - 2, 0, 4, sy, 0xFFFFFF, 0.7f * k);
            ring(g, cx, sy, (int) (55 * k), 2, 0xFFC247, 0.6f * k);
        }
    }

    private void octopus(GuiGraphicsExtractor g, float s, int cx, int sy) {
        rect(g, 0, 0, W, H, 0x041A38, 0.40f * ramp(s, 0.5f));

        float inkIn = ease(Math.min(1f, Math.max(0f, (s - 0.1f) / 0.9f)));
        float inkOut = s > 1.6f ? Math.max(0f, 1 - (s - 1.6f) / 0.7f) : 1f;
        for (int j = 0; j < 7; j++) {
            double a = j * 0.9;
            int ox = (int) (Math.cos(a) * (12 + j * 4)), oy = (int) (Math.sin(a) * (12 + j * 4));
            disc(g, cx + ox, sy + oy, (int) (inkIn * (26 + j * 3)), 0x070B1E, 0.5f * inkOut);
        }

        float len = ease(Math.min(1f, Math.max(0f, (s - 0.2f) / 0.9f))) * 150;
        if (s > 2f) len *= 1 - ease(Math.min(1f, (s - 2f) / 0.4f));
        if (len > 4) {
            for (int k = 0; k < 6; k++) {
                double ang = Math.toRadians(k * 60 + 15);
                float dxn = (float) Math.cos(ang), dyn = (float) Math.sin(ang);
                for (int j = 0; j < 16; j++) {
                    float f = j / 15f;
                    float dist = 200 - len * f;
                    float sway = (float) Math.sin(s * 3 + f * 6 + k) * 10 * f;
                    int x = (int) (cx + dxn * dist - dyn * sway);
                    int y = (int) (sy + dyn * dist + dxn * sway);
                    int r = Math.max(2, (int) (7 - 5 * f));
                    disc(g, x, y, r, 0x16365E, 0.95f);
                    disc(g, x - 1, y - 1, Math.max(1, r - 2), 0x1E4D8A, 0.9f);
                    if (j % 3 == 1) ring(g, x, y, 3, 1, 0x2FA8FF, 0.9f);
                }
            }
        }

        if (s > 0.9f) {
            float e = Math.min(1f, (s - 0.9f) / 0.3f);
            int pulse = (int) (3 * Math.sin(s * 8));
            ring(g, cx, sy, 20 + pulse, 3, 0x2FA8FF, 0.9f * e);
            ring(g, cx, sy, 12, 2, 0x9AF0FF, 0.8f * e);
            disc(g, cx, sy, 5, 0xFFD23F, 0.6f * e);
            for (int n = 0; n < 3; n++) {
                float t = (s * 1.4f + n / 3f) % 1f;
                ring(g, cx, sy, (int) (t * 70), 1, 0x2FA8FF, (1 - t) * 0.6f * e);
            }
        }
        if (s >= 2.2f && s < 2.4f) rect(g, 0, 0, W, H, 0x9AF0FF, 0.3f * (1 - (s - 2.2f) / 0.2f));
    }

    private void thunder(GuiGraphicsExtractor g, float s, int cx, int sy) {
        rect(g, 0, 0, W, H, 0x0A0F1A, 0.45f * ramp(s, 0.4f));
        int ground = sy + 40;

        for (int b = 0; b < bolts.size(); b++) {
            float[] bolt = bolts.get(b);
            float age = s - bolt[0];
            if (age < 0 || age > 0.4f) continue;
            boolean big = b == 2;
            float a = 1 - age / 0.4f;
            if (((int) (age * 40)) % 3 == 2) a *= 0.5f;
            for (int i = 0; i < 9; i++) {
                float x1 = cx + bolt[1] + bolt[2 + i], x2 = cx + bolt[1] + bolt[3 + i];
                float y1 = ground * i / 9f, y2 = ground * (i + 1) / 9f;
                line(g, x1, y1, x2, y2, big ? 11 : 7, 0xFFE94D, a * 0.4f);
                line(g, x1, y1, x2, y2, big ? 4 : 2, 0xFFFFFF, a);
            }
            if (age < 0.12f) rect(g, 0, 0, W, H, 0xFFFFFF, (big ? 0.7f : 0.5f) * (1 - age / 0.12f));
        }

        float dt = s - 1.2f;
        if (dt >= 0 && dt < 0.6f) ring(g, cx, ground, (int) (dt * 200), 3, 0xFFE94D, 1 - dt / 0.6f);
        if (s > 1.2f && s < 2.0f) {
            for (int i = 0; i < 5; i++) {
                double ang = Math.random() * Math.PI * 2;
                double r = 10 + Math.random() * 30;
                line(g, cx, ground, (float) (cx + Math.cos(ang) * r), (float) (ground + Math.sin(ang) * r * 0.5), 1, 0xFFE94D, 0.8f);
            }
        }
    }

    private void vial(GuiGraphicsExtractor g, float s, int cx, int sy) {
        rect(g, 0, 0, W, H, 0x0A1A05, 0.35f * ramp(s, 0.5f));
        float pulse = 0.5f + 0.5f * (float) Math.sin(s * 7);

        if (s < 1.5f) {
            int dx = s > 1.0f ? (int) (Math.sin(s * 70) * 2) : 0;
            int x = cx + dx;
            disc(g, cx, sy, 30 + (int) (4 * pulse), 0x7CFC00, 0.16f);
            if (s > 0.3f) {
                for (int n = 0; n < 3; n++) {
                    float t = ((s - 0.3f) * 0.9f + n / 3f) % 1f;
                    ring(g, cx, sy, (int) (t * 60), 1, 0x7CFC00, (1 - t) * 0.5f);
                }
            }
            rect(g, x - 12, sy - 18, 24, 40, 0xBFE8F2, 0.35f);
            rect(g, x - 5, sy - 28, 10, 10, 0xBFE8F2, 0.35f);
            rect(g, x - 10, sy - 6, 20, 26, 0x7CFC00, 0.9f);
            rect(g, x - 10, sy - 6, 20, 2, 0xE6FF8A, 0.9f);
            rect(g, x - 6, sy - 33, 12, 6, 0x8B5A2B, 1f);
            for (int i = 0; i < 6; i++) {
                float by = sy + 18 - ((s * 40 + i * 13) % 30);
                rect(g, x - 7 + i * 3 + (int) (Math.sin(s * 6 + i) * 1.5), (int) by, 2, 2, 0xE6FF8A, 0.9f);
            }
            if (s > 1.2f) {
                float p = (s - 1.2f) / 0.3f;
                line(g, x - 2, sy - 18, x + 4, sy - 4, 1, 0xFFFFFF, p);
                line(g, x + 4, sy - 4, x - 3, sy + 8, 1, 0xFFFFFF, p);
                line(g, x - 3, sy + 8, x + 5, sy + 20, 1, 0xFFFFFF, p);
            }
        } else {
            float dt = s - 1.5f;
            for (int i = 0; i < 5; i++) {
                disc(g, cx + (i - 2) * 14, sy + (i % 2) * 6, (int) (ease(Math.min(1f, dt / 0.8f)) * (30 + i * 4)),
                        0x7CFC00, 0.30f * Math.max(0f, 1 - dt / 1.1f));
            }
            if (dt < 0.18f) rect(g, 0, 0, W, H, 0x7CFC00, 0.3f * (1 - dt / 0.18f));
            if (dt < 0.7f) ring(g, cx, sy, (int) (dt * 160), 2, 0xE6FF8A, 1 - dt / 0.7f);
        }
    }

    private void coins(GuiGraphicsExtractor g, float s, int cx, int sy) {
        rect(g, 0, 0, W, H, 0x2A1E00, 0.25f * ramp(s, 0.3f));
        float k = Math.min(1f, s / 0.6f);
        ring(g, cx, sy, (int) (ease(k) * 80), 3, 0xFFC832, 1 - k);
    }

    private void slash(GuiGraphicsExtractor g, float s, int cx, int sy) {
        if (s >= 0.2f && s < 0.8f) {
            float p = Math.min(1f, (s - 0.2f) / 0.12f);
            float fa = 1 - (s - 0.2f) / 0.6f;
            float x1 = cx - 60, y1 = sy - 50, x2 = cx + 60, y2 = sy + 50;
            line(g, x1, y1, x1 + (x2 - x1) * p, y1 + (y2 - y1) * p, 6, scene.primary, fa * 0.5f);
            line(g, x1, y1, x1 + (x2 - x1) * p, y1 + (y2 - y1) * p, 2, 0xFFFFFF, fa);
            line(g, x2, y1, x2 + (x1 - x2) * p, y1 + (y2 - y1) * p, 6, scene.primary, fa * 0.5f);
            line(g, x2, y1, x2 + (x1 - x2) * p, y1 + (y2 - y1) * p, 2, 0xFFFFFF, fa);
        }
        if (s >= 0.35f && s < 0.5f) rect(g, 0, 0, W, H, 0xFFFFFF, 0.3f * (1 - (s - 0.35f) / 0.15f));
    }

    // ------------------------------------------------------------------ item reveal

    private void reveal(GuiGraphicsExtractor g, float s, int cx, int sy) {
        float r = scene.revealAt;
        if (s < r) return;
        float k = ease(Math.min(1f, (s - r) / 0.5f));
        int gy = sy - 4 - (int) (10 * k);
        float pulse = 0.5f + 0.5f * (float) Math.sin(s * 6);

        disc(g, cx, gy, 22 + (int) (3 * pulse), scene.secondary, 0.10f * k);
        disc(g, cx, gy, 14, scene.primary, 0.18f * k);
        diamond(g, cx, gy, 11, accent, k);
        diamond(g, cx, gy, 7, scene.secondary, k * 0.9f);
        rect(g, cx - 1, gy - 18, 2, 36, 0xFFFFFF, 0.5f * k * pulse);
        rect(g, cx - 18, gy - 1, 36, 2, 0xFFFFFF, 0.5f * k * pulse);
        for (int i = 0; i < 6; i++) {
            double a = s * 2 + i * 1.047;
            rect(g, (int) (cx + Math.cos(a) * 30), (int) (gy + Math.sin(a) * 18), 3, 3, scene.secondary, k);
        }

        Font font = Minecraft.getInstance().font;
        int alpha = Math.max(5, Math.round(255 * k * gA));
        float ts = Math.min(1f, (s - r) / 0.3f);
        float pop = 0.6f + 0.4f * ease(ts) + (ts < 1f ? 0.12f * (float) Math.sin(ts * Math.PI) : 0f);

        Component name = Component.literal(itemName).withStyle(ChatFormatting.BOLD);
        int tw = Math.max(1, font.width(name));
        float sc = Math.min(2f, (W * 0.8f) / tw) * pop;
        drawName(g, font, name, tw, cx, sy + 66, sc, (alpha << 24) | accent);

        drawLine(g, font, bossLine.toUpperCase(Locale.ROOT) + " DROP", cx, sy + 84, 1f,
                (alpha << 24) | scene.primary);
        if (value > 0) {
            drawLine(g, font, "~" + PriceService.fmtCoins(value) + " coins", cx, sy + 96, 1f,
                    (alpha << 24) | 0xFFD966);
        }
    }

    private void drawName(GuiGraphicsExtractor g, Font font, Component name, int tw, int cx, int cy,
                          float sc, int color) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(sc, sc);
        g.text(font, name, -tw / 2, -font.lineHeight / 2, color, true);
        pose.popMatrix();
    }

    private void drawLine(GuiGraphicsExtractor g, Font font, String text, int cx, int cy, float sc, int color) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(sc, sc);
        g.text(font, text, -font.width(text) / 2, -font.lineHeight / 2, color, true);
        pose.popMatrix();
    }
}
