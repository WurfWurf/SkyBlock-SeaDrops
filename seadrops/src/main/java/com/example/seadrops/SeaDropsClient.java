package com.example.seadrops;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SeaDropsClient implements ClientModInitializer {

    public static final String MOD_ID = "seadrops";

    // Commands run while the chat screen is still open; open the menu on the next tick instead.
    // The chat screen closes itself right after a command runs and would close the menu with it, so wait a
    // couple of ticks before opening. 0 = nothing to open.
    private static int openMenuIn = 0;

    @Override
    public void onInitializeClient() {
        Config.load();
        DropDetector.register();

        // addLast = drawn on top of the vanilla HUD and other mods' layers
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "animation"),
                AnimationOverlay.INSTANCE::render);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "drop_animation"),
                ItemAnimation.INSTANCE::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PriceService.refreshIfStale(); // cheap check; at most one fetch per 10 minutes
            if (openMenuIn > 0 && --openMenuIn == 0) {
                client.setScreen(new MenuScreen());
            }
        });

        // /seadrops                  - open the menu
        // /seadrops <name>           - preview a rarity (common..mythic) or location (water, lava, ...)
        // /seadrops drop <scene>     - preview a drop sequence (titanoboa, wikitiki, jawbus, ragnarok,
        //                              octopus, thunder, vial, coins)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("seadrops")
                        .executes(ctx -> {
                            openMenuIn = 2;
                            return 1;
                        })
                        .then(ClientCommandManager.literal("drop")
                                .then(ClientCommandManager.argument("scene", StringArgumentType.word())
                                        .executes(ctx -> previewDrop(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "scene")))))
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> preview(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private static int previewDrop(FabricClientCommandSource source, String name) {
        ItemAnimation.Scene sc = ItemAnimation.Scene.fromName(name);
        if (sc == null) {
            source.sendFeedback(Component.literal(
                    "Unknown scene. Try titanoboa, wikitiki, jawbus, ragnarok, octopus, thunder, vial or coins."));
            return 0;
        }
        ItemAnimation.INSTANCE.playPreview(sc);
        source.sendFeedback(Component.literal("Playing the " + sc.label + " drop animation."));
        return 1;
    }

    private static int preview(FabricClientCommandSource source, String name) {
        Rarity r = Rarity.fromName(name);
        if (r != null) {
            AnimationOverlay.INSTANCE.playRarity(r);
            source.sendFeedback(Component.literal("Playing " + r.label + " animation."));
            return 1;
        }
        Theme t = Theme.fromName(name);
        if (t != null && t != Theme.GENERIC) {
            AnimationOverlay.INSTANCE.playTheme(t);
            source.sendFeedback(Component.literal("Playing " + t.label + " animation."));
            return 1;
        }
        source.sendFeedback(Component.literal(
                "Unknown name. Try a rarity (common..mythic), a location (water, lava, thunder, ice, bayou, spooky, deep), "
                        + "'drop <scene>' for the boss drop sequences, or just /seadrops for the menu."));
        return 0;
    }
}
