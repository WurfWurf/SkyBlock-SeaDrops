package com.example.seadrops;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Modern, custom-drawn menu: dark card, pill switches, two tabs.
 *  Settings  - toggles and per-location switches
 *  Previews  - play every boss drop sequence, every rarity burst, and set the value alert threshold
 * Open with /seadrops. Animations keep playing behind it.
 */
public class MenuScreen extends Screen {

    private static final int PANEL = 0xF0141821;
    private static final int HEADER = 0xFF1B2130;
    private static final int HOVER = 0x26FFFFFF;
    private static final int TEXT = 0xFFE8ECF4;
    private static final int MUTED = 0xFF8A93A6;
    private static final int ON = 0xFF3DDC84;
    private static final int OFF = 0xFF3A4152;
    private static final int ACCENT = 0xFF4FC3F7;

    private static final int SWITCH_W = 26;
    private static final int SWITCH_H = 12;
    private static final int PAD = 6;
    private static final int COL_W = 164;

    private static int page = 0; // remembered between openings

    private static final class Row {
        int x, y, w, h;
        String label;
        int dot = -1;
        BooleanSupplier state;
        Runnable toggle;
        Runnable play;
        boolean cycle;
        Supplier<String> value;
        IntSupplier valueColor;
        float knob = -1;
    }

    private final List<Row> rows = new ArrayList<>();
    private final int[][] tabs = new int[2][4]; // x, y, w, h
    private static final String[] TAB_NAMES = {"Settings", "Previews"};
    private int px, py, pw, ph;
    private String leftTitle = "", rightTitle = "";

    public MenuScreen() {
        super(Component.literal("SeaDrops"));
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        rows.clear();

        pw = 372;
        ph = Math.min(232, this.height - 8);
        px = (this.width - pw) / 2;
        py = Math.max(4, this.height - ph - 6);

        int lx = px + 12;
        int rx = lx + COL_W + 20;
        int rowH = 18;
        int top = py + 46;

        // tabs (header, right side)
        int tx = px + pw - 12;
        for (int i = TAB_NAMES.length - 1; i >= 0; i--) {
            int w = this.font.width(TAB_NAMES[i]) + 14;
            tx -= w;
            tabs[i] = new int[]{tx, py + 6, w, 18};
            tx -= 4;
        }

        if (page == 0) buildSettings(lx, rx, top, rowH);
        else buildPreviews(lx, rx, top, rowH);
    }

    private void buildSettings(int lx, int rx, int top, int rowH) {
        leftTitle = "SETTINGS";
        rightTitle = "LOCATIONS";
        int y = top;
        y = toggle(lx, y, rowH, "Mod enabled", () -> Config.enabled, () -> Config.enabled = !Config.enabled);
        y = toggle(lx, y, rowH, "Sound", () -> Config.playSound, () -> Config.playSound = !Config.playSound);
        y = toggle(lx, y, rowH, "Screen flash", () -> Config.screenFlash, () -> Config.screenFlash = !Config.screenFlash);
        y = toggle(lx, y, rowH, "Mob animations", () -> Config.mobAnimations, () -> Config.mobAnimations = !Config.mobAnimations);
        y = toggle(lx, y, rowH, "Generic catches", () -> Config.genericAnimations,
                () -> Config.genericAnimations = !Config.genericAnimations);
        y = toggle(lx, y, rowH, "Drop animations", () -> Config.dropAnimations,
                () -> Config.dropAnimations = !Config.dropAnimations);
        y = toggle(lx, y, rowH, "Value alerts", () -> Config.valueAlerts, () -> Config.valueAlerts = !Config.valueAlerts);
        y = toggle(lx, y, rowH, "Boss spawn alerts", () -> Config.bossCatchAnimation,
                () -> Config.bossCatchAnimation = !Config.bossCatchAnimation);

        cycle(lx, y, rowH, "Minimum rarity", () -> Config.minRarity.label + "+", () -> Config.minRarity.color,
                () -> Config.minRarity = Rarity.values()[(Config.minRarity.ordinal() + 1) % Rarity.values().length]);

        y = top;
        for (Theme t : Theme.values()) {
            if (t == Theme.GENERIC) continue;
            Row r = new Row();
            r.x = rx; r.y = y; r.w = COL_W; r.h = rowH;
            r.label = t.label;
            r.dot = t.primary;
            r.state = () -> Config.isThemeOn(t);
            r.toggle = () -> Config.setThemeOn(t, !Config.isThemeOn(t));
            r.play = () -> AnimationOverlay.INSTANCE.playTheme(t);
            rows.add(r);
            y += rowH;
        }
    }

    private void buildPreviews(int lx, int rx, int top, int rowH) {
        leftTitle = "DROP ANIMATIONS";
        rightTitle = "RARITY BURSTS";

        int y = top;
        for (ItemAnimation.Scene sc : ItemAnimation.Scene.values()) {
            if (sc == ItemAnimation.Scene.SLASH) continue; // only used through customItems
            playRow(lx, y, rowH, sc.label, sc.primary, () -> ItemAnimation.INSTANCE.playPreview(sc));
            y += rowH;
        }

        y = top;
        for (Rarity ra : Rarity.values()) {
            playRow(rx, y, rowH, ra.label, ra.color, () -> AnimationOverlay.INSTANCE.playRarity(ra));
            y += rowH;
        }
        y += 6;
        cycle(rx, y, rowH, "Value alert at", () -> PriceService.fmtCoins(Config.valueThreshold), () -> 0xFFD966,
                Config::nextThreshold);
        toggle(rx, y + rowH, rowH, "Boss: valuable only", () -> Config.bossOnlyValuable,
                () -> Config.bossOnlyValuable = !Config.bossOnlyValuable);
    }

    private int toggle(int x, int y, int h, String label, BooleanSupplier state, Runnable toggle) {
        Row r = new Row();
        r.x = x; r.y = y; r.w = COL_W; r.h = h;
        r.label = label;
        r.state = state;
        r.toggle = toggle;
        rows.add(r);
        return y + h;
    }

    private void cycle(int x, int y, int h, String label, Supplier<String> value, IntSupplier color, Runnable next) {
        Row r = new Row();
        r.x = x; r.y = y; r.w = COL_W; r.h = h;
        r.label = label;
        r.cycle = true;
        r.value = value;
        r.valueColor = color;
        r.toggle = next;
        rows.add(r);
    }

    private void playRow(int x, int y, int h, String label, int dot, Runnable play) {
        Row r = new Row();
        r.x = x; r.y = y; r.w = COL_W; r.h = h;
        r.label = label;
        r.dot = dot;
        r.play = play;
        rows.add(r);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // light dim only - no blur - so the animation stays visible behind the menu
        g.fill(0, 0, this.width, this.height, 0x50000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        rounded(g, px - 1, py - 1, pw + 2, ph + 2, 0x30FFFFFF);
        rounded(g, px, py, pw, ph, PANEL);
        rounded(g, px, py, pw, 30, HEADER);
        g.fill(px + 12, py + 25, px + 12 + 44, py + 27, ACCENT);
        g.text(this.font, Component.literal("SeaDrops").withStyle(ChatFormatting.BOLD), px + 12, py + 9, TEXT, false);

        for (int i = 0; i < tabs.length; i++) {
            int[] t = tabs[i];
            boolean active = i == page;
            if (active) rounded(g, t[0], t[1], t[2], t[3], 0x334FC3F7);
            else if (inside(mouseX, mouseY, t[0], t[1], t[2], t[3])) rounded(g, t[0], t[1], t[2], t[3], HOVER);
            int tw = this.font.width(TAB_NAMES[i]);
            text(g, TAB_NAMES[i], t[0] + (t[2] - tw) / 2, t[1] + 5, active ? ACCENT : MUTED);
        }

        int lx = px + 12;
        int rx = lx + COL_W + 20;
        text(g, leftTitle, lx + PAD, py + 34, MUTED);
        text(g, rightTitle, rx + PAD, py + 34, MUTED);

        for (Row r : rows) drawRow(g, r, mouseX, mouseY);

        String hint = page == 0
                ? "Click a row to toggle  -  play icon previews  -  ESC to close"
                : "Click a row to play it  -  ESC to close";
        text(g, hint, lx + PAD, py + ph - 14, MUTED);
    }

    private void drawRow(GuiGraphicsExtractor g, Row r, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, r.x, r.y, r.w, r.h)) rounded(g, r.x, r.y, r.w, r.h, HOVER);

        int tx = r.x + PAD;
        if (r.dot >= 0) {
            rounded(g, r.x + PAD, r.y + 6, 6, 6, 0xFF000000 | r.dot);
            tx = r.x + PAD + 11;
        }
        text(g, r.label, tx, r.y + 5, TEXT);

        if (r.cycle) {
            String v = r.value.get();
            text(g, v, r.x + r.w - PAD - this.font.width(v), r.y + 5, 0xFF000000 | r.valueColor.getAsInt());
            return;
        }
        if (r.state == null) { // play-only row
            drawPlay(g, r.x + r.w - PAD - 6, r.y + 4, inside(mouseX, mouseY, r.x, r.y, r.w, r.h) ? 0xFFFFFFFF : MUTED);
            return;
        }

        boolean on = r.state.getAsBoolean();
        if (r.knob < 0) r.knob = on ? 1f : 0f;
        r.knob += ((on ? 1f : 0f) - r.knob) * 0.35f; // smooth slide

        int sx = r.x + r.w - PAD - SWITCH_W;
        int sy = r.y + 3;
        rounded(g, sx, sy, SWITCH_W, SWITCH_H, lerp(OFF, ON, r.knob));
        rounded(g, sx + 2 + Math.round(r.knob * 14), sy + 2, 8, 8, 0xFFFFFFFF);

        if (r.play != null) {
            boolean hoverPlay = inside(mouseX, mouseY, sx - 18, r.y, 16, r.h);
            drawPlay(g, sx - 14, r.y + 4, hoverPlay ? 0xFFFFFFFF : MUTED);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();

            for (int i = 0; i < tabs.length; i++) {
                int[] t = tabs[i];
                if (inside(mx, my, t[0], t[1], t[2], t[3])) {
                    if (page != i) {
                        page = i;
                        this.rebuildWidgets(); // re-runs init()
                    }
                    return true;
                }
            }

            for (Row r : rows) {
                if (!inside(mx, my, r.x, r.y, r.w, r.h)) continue;
                int sx = r.x + r.w - PAD - SWITCH_W;
                if (r.state == null && !r.cycle && r.play != null) {
                    r.play.run();
                } else if (r.play != null && inside(mx, my, sx - 18, r.y, 16, r.h)) {
                    r.play.run();
                } else if (r.toggle != null) {
                    r.toggle.run();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(this.font, s, x, y, color, false);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Rectangle with softly rounded corners (stepped, radius ~2). */
    private static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + h, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
    }

    private static void drawPlay(GuiGraphicsExtractor g, int x, int y, int color) {
        for (int i = 0; i < 5; i++) {
            g.fill(x + i, y + i, x + i + 1, y + 10 - i, color);
        }
    }

    private static int lerp(int a, int b, float f) {
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        int br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * f);
        int gr = (int) (ag + (bg - ag) * f);
        int bl = (int) (ab + (bb - ab) * f);
        return 0xFF000000 | r << 16 | gr << 8 | bl;
    }
}
