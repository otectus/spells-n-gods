package com.otectus.spells_n_gods.client;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.animation.PlayerAnimationType;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.network.PlayerAnimationPacket;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PlayerAnimationHandler {

    private static final Map<PlayerAnimationType, KeyframeAnimation> ANIMATIONS = new EnumMap<>(PlayerAnimationType.class);
    private static boolean initialized = false;

    public static void init() {
        if (!ModList.get().isLoaded("playeranimator")) {
            SpellsNGodsMod.LOGGER.info("[SpellsNGods] playerAnimator not present, skipping animation init");
            return;
        }

        PlayerAnimationSetup.registerAnimationLayer();
        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Player animation handler initialized (animations load on first use)");
    }

    private static void ensureAnimationsLoaded() {
        if (initialized) return;
        initialized = true;

        var resourceManager = Minecraft.getInstance().getResourceManager();

        for (PlayerAnimationType type : PlayerAnimationType.values()) {
            ResourceLocation location = type.getResourceLocation();
            try {
                Resource resource = resourceManager.getResourceOrThrow(location);
                try (InputStream stream = resource.open()) {
                    List<KeyframeAnimation> animations = AnimationSerializing.deserializeAnimation(stream);
                    if (!animations.isEmpty()) {
                        ANIMATIONS.put(type, animations.get(0));
                        SpellsNGodsMod.LOGGER.debug("[SpellsNGods] Loaded animation: {}", type.getAnimationId());
                    } else {
                        SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Animation file empty: {}", location);
                    }
                }
            } catch (Exception e) {
                SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Could not load animation {}: {}", location, e.getMessage());
            }
        }

        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Loaded {}/{} player animations",
                ANIMATIONS.size(), PlayerAnimationType.values().length);
    }

    public static void handleAnimation(UUID targetPlayerId, PlayerAnimationType animationType,
                                        PlayerAnimationPacket.Action action) {
        if (!SpellsNGodsConfig.COMMON.playerAnimationsEnabled.get()) return;
        if (!ModList.get().isLoaded("playeranimator")) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        AbstractClientPlayer targetPlayer = null;
        for (AbstractClientPlayer player : level.players()) {
            if (player.getUUID().equals(targetPlayerId)) {
                targetPlayer = player;
                break;
            }
        }

        if (targetPlayer == null) return;

        IAnimation rawLayer = PlayerAnimationAccess.getPlayerAssociatedData(targetPlayer)
                .get(PlayerAnimationSetup.ANIMATION_LAYER_ID);
        if (!(rawLayer instanceof ModifierLayer<?> modLayer)) return;

        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) modLayer;

        switch (action) {
            case PLAY -> {
                ensureAnimationsLoaded();
                KeyframeAnimation animation = ANIMATIONS.get(animationType);
                if (animation == null) return;

                // Looping is controlled by "loop": true in the animation JSON
                KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
                layer.replaceAnimationWithFade(
                        AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE),
                        animPlayer);
            }
            case STOP -> {
                layer.replaceAnimationWithFade(
                        AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE),
                        null);
            }
        }
    }
}
