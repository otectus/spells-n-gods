package com.otectus.spells_n_gods.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Venatas's Crossbow of the Wild — Nature-themed divine crossbow.
 * - 25% faster charge speed
 * - Base bolt damage: 11
 * - Passive: Killing bolts have 25% chance for bonus loot drop
 * - Special (Shift+Right-click when loaded): Ensnaring Shot — root + poison, 16s cooldown
 */
public class DivineCrossbowItem extends CrossbowItem {

    private static final int ABILITY_COOLDOWN_TICKS = 320; // 16 seconds

    public DivineCrossbowItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .durability(3000));
    }

    // ─── Faster charge: override getChargeDuration for 25% faster ───

    /**
     * Returns the charge duration for this crossbow.
     * Vanilla is 25 ticks (1.25s), we do ~19 ticks (~0.95s).
     */
    public static int getDivineChargeDuration(ItemStack stack) {
        // Vanilla base: 25 ticks. Apply Quick Charge enchantment as usual, then reduce by 25%
        int vanillaDuration = CrossbowItem.getChargeDuration(stack);
        return Math.max(5, (int) (vanillaDuration * 0.75));
    }

    // ─── Shift+Right-click: Ensnaring Shot ───

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift+right-click when loaded = Ensnaring Shot
        if (player.isShiftKeyDown() && CrossbowItem.isCharged(stack)) {
            if (level.isClientSide()) {
                return InteractionResultHolder.success(stack);
            }
            if (isAbilityOnCooldown(stack)) {
                // Ability still cooling down — swallow the input rather than firing the loaded bolt.
                return InteractionResultHolder.fail(stack);
            }
            return abilityEnsnaringShot((ServerLevel) level, (ServerPlayer) player, stack, hand);
        }

        // Default crossbow behavior (never blocked by ability cooldown)
        return super.use(level, player, hand);
    }

    private static boolean isAbilityOnCooldown(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt("AbilityCooldownTicks") > 0;
    }

    private InteractionResultHolder<ItemStack> abilityEnsnaringShot(ServerLevel level, ServerPlayer player,
                                                                     ItemStack stack, InteractionHand hand) {
        // Fire a special ensnaring bolt
        Arrow bolt = new Arrow(level, player);
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 3.15f, 0.5f);
        bolt.setBaseDamage(11.0);
        bolt.setCritArrow(true);
        // Tag for ensnare effect on hit
        bolt.addTag("ensnaring_bolt");
        bolt.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(bolt);

        // Unload the crossbow
        CrossbowItem.setCharged(stack, false);

        // Nature particles
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 1.2, player.getZ(),
                15, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.COMPOSTER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                10, 0.2, 0.2, 0.2, 0.02);

        level.playSound(null, player.blockPosition(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BIG_DRIPLEAF_TILT_UP, SoundSource.PLAYERS, 0.8F, 1.2F);

        stack.getOrCreateTag().putInt("AbilityCooldownTicks", ABILITY_COOLDOWN_TICKS);
        stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Called from a Forge event handler when an ensnaring bolt hits.
     * Applies root (Slowness 127) + Poison II.
     */
    public static void onEnsnaringBoltHit(AbstractArrow arrow, LivingEntity hitEntity) {
        if (arrow.getTags().contains("ensnaring_bolt") && arrow.level() instanceof ServerLevel serverLevel) {
            // Root: Slowness 127 for 4 seconds (immobilize)
            hitEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 127, false, true));
            // Poison II for 6 seconds
            hitEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1, false, true));
            // Jump suppression via high-level jump boost (negative) — use slowness as root
            // (Jump isn't easily suppressed without mixins, so the extreme Slowness handles it)

            // Leaf particle ring
            double radius = 2.0;
            for (int i = 0; i < 24; i++) {
                double angle = (i / 24.0) * Math.PI * 2;
                serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                        hitEntity.getX() + Math.cos(angle) * radius,
                        hitEntity.getY() + 0.2,
                        hitEntity.getZ() + Math.sin(angle) * radius,
                        2, 0.1, 0.1, 0.1, 0.01);
            }
            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    hitEntity.getX(), hitEntity.getY() + 1.0, hitEntity.getZ(),
                    20, 0.5, 0.5, 0.5, 0.03);

            serverLevel.playSound(null, hitEntity.blockPosition(),
                    SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.NEUTRAL, 1.0F, 0.7F);
        }
    }

    // ─── Cooldown tick-down ───

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide()) return;

        // Decrement ability cooldown (ticks down even if not held)
        if (stack.hasTag() && stack.getTag().getInt("AbilityCooldownTicks") > 0) {
            int remaining = stack.getTag().getInt("AbilityCooldownTicks") - 1;
            if (remaining <= 0) {
                stack.getTag().remove("AbilityCooldownTicks");
            } else {
                stack.getTag().putInt("AbilityCooldownTicks", remaining);
            }
        }
    }

    // ─── Tooltip ───

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Ensnaring Shot")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Shift+Right-click (loaded): Root + Poison bolt")
                .withStyle(ChatFormatting.GRAY));
        if (isAbilityOnCooldown(stack)) {
            int remainingSeconds = (stack.getTag().getInt("AbilityCooldownTicks") + 19) / 20;
            tooltip.add(Component.literal("Cooldown: " + remainingSeconds + "s remaining")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Cooldown: 16s")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Passive: 25% chance for bonus loot on killing blow.")
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 22;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.getItem() == com.otectus.spells_n_gods.registry.ModItems.RUNIC_FRAGMENT.get();
    }
}
