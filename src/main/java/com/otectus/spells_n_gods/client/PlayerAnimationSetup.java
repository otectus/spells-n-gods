package com.otectus.spells_n_gods.client;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerAnimationSetup {

    public static final ResourceLocation ANIMATION_LAYER_ID =
            new ResourceLocation(SpellsNGodsMod.MODID, "divine_animations");

    public static void registerAnimationLayer() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ANIMATION_LAYER_ID,
                500,
                (AbstractClientPlayer player) -> new ModifierLayer<IAnimation>()
        );
        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Player animation layer registered");
    }
}
