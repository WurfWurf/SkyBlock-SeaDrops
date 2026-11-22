package com.example.seadrops;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Looks up item values so drops worth over your threshold (default 1M) get an animation.
 * Two public, key-less GET requests, refreshed at most every 10 minutes, nothing about you is sent:
 *   - Hypixel bazaar API  (bazaar items)
 *   - lowest-BIN JSON     (auction items; URL is configurable as lowestBinUrl)
 * Turn "Value alerts" off in the menu and no requests are made.
 * Names are turned into item ids by upper-casing and replacing spaces with underscores, which works for
 * most items but not for pets, enchanted books and a few others - use customItems for those.
 */
public final class PriceService {

    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final long STALE_MS = 10 * 60 * 1000;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private static volatile Map<String, Double> prices = Map.of();
    private static volatile long lastFetch = 0;
    private static volatile boolean fetching = false;

    private PriceService() {}

    public static void refreshIfStale() {
        if (!Config.valueAlerts || fetching) return;
        if (System.currentTimeMillis() - lastFetch < STALE_MS) return;
        fetching = true;
        lastFetch = System.currentTimeMillis();

        Map<String, Double> fresh = new HashMap<>();
        fetch(Config.lowestBinUrl, body -> {
            JsonObject o = JsonParser.parseString(body).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                if (e.getValue().isJsonPrimitive()) fresh.put(e.getKey(), e.getValue().getAsDouble());
            }
        }, () -> fetch(BAZAAR_URL, body -> {
            JsonObject products = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("products");
            for (Map.Entry<String, JsonElement> e : products.entrySet()) {
                JsonObject q = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
                // the higher of buy/sell, so borderline items (e.g. 1.07M buy / 700K sell) still count
                double best = Math.max(q.get("buyPrice").getAsDouble(), q.get("sellPrice").getAsDouble());
                if (best > 0) fresh.put(e.getKey(), best); // bazaar wins over BIN
            }
        }, () -> {
            if (!fresh.isEmpty()) prices = fresh;
            fetching = false;
        }));
    }

    private interface Parser {
        void parse(String body) throws Exception;
    }

    /** Fetches a URL, parses it, then always runs `then` (even when the request fails). */
    private static void fetch(String url, Parser parser, Runnable then) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "SeaDrops")
                    .GET()
                    .build();
            HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                try {
                    if (err == null && resp.statusCode() == 200) parser.parse(resp.body());
                } catch (Exception ignored) {
                }
                then.run();
            });
        } catch (Exception e) {
            then.run();
        }
    }

    /** Value in coins, or -1 if unknown. */
    public static double valueOf(String itemName) {
        Double v = prices.get(toId(itemName));
        if (v == null) {
            String ench = enchantId(itemName);
            if (ench != null) v = prices.get(ench);
        }
        return v != null ? v : -1;
    }

    /** "Flash I" -> ENCHANTMENT_FLASH_1, "Luck of the Sea VI" -> ENCHANTMENT_LUCK_OF_THE_SEA_6; null if no roman numeral. */
    static String enchantId(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length < 2) return null;
        int level = roman(parts[parts.length - 1]);
        if (level <= 0) return null;
        String base = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
        return "ENCHANTMENT_" + toId(base) + "_" + level;
    }

    private static int roman(String s) {
        int total = 0, prev = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int v = switch (s.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                default -> -1;
            };
            if (v < 0) return -1;
            total += v < prev ? -v : v;
            prev = Math.max(prev, v);
        }
        return total;
    }

    static String toId(String name) {
        String id = name.toUpperCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        // dyes are stored as DYE_<NAME> (Aquamarine Dye -> DYE_AQUAMARINE)
        if (id.endsWith("_DYE")) id = "DYE_" + id.substring(0, id.length() - 4);
        return id;
    }

    public static String fmtCoins(double v) {
        if (v >= 1_000_000_000) return String.format(Locale.ROOT, "%.1fB", v / 1_000_000_000);
        if (v >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", v / 1_000_000);
        if (v >= 1_000) return String.format(Locale.ROOT, "%.0fK", v / 1_000);
        return String.format(Locale.ROOT, "%.0f", v);
    }
}
