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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Velox's Bow of Agility — Lightning-themed divine bow.
 * - 30% faster draw speed
 * - Arrows have no gravity (fly straight)
 * - Base arrow damage: 9
 * - Passive: Speed I while held in mainhand
 * - Special (Shift+Right-click): Stormstrike — instant lightning arrow, 12s cooldown
 */
public class DivineBowItem extends BowItem {

    private static final int ABILITY_COOLDOWN_TICKS = 240; // 12 seconds
    private static final float BASE_ARROW_DAMAGE = 9.0f;

    public DivineBowItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .durability(3000));
    }

    // ─── Faster draw: arrows reach full power 30% sooner ───

    /**
     * Override to get power level — we scale so full power is reached at ~14 ticks
     * instead of vanilla's ~20 ticks (30% faster).
     */
    public static float getDivinePowerForTime(int useTicks) {
        // Vanilla: power = ticks / 20, capped at 1.0
        // Divine: power = ticks / 14, capped at 1.0
        float power = (float) useTicks / 14.0f;
        power = (power * power + power * 2.0f) / 3.0f;
        return Math.min(power, 1.0f);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeLeft) {
        if (!(shooter instanceof Player player)) return;

        int useDuration = this.getUseDuration(stack) - timeLeft;
        float power = getDivinePowerForTime(useDuration);
        if (power < 0.1f) return;

        boolean hasInfinity = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
        ItemStack arrowStack = player.getProjectile(stack);

        if (!arrowStack.isEmpty() || hasInfinity) {
            if (arrowStack.isEmpty()) {
                arrowStack = new ItemStack(Items.ARROW);
            }

            if (!level.isClientSide()) {
                ArrowItem arrowItem = (ArrowItem) (arrowStack.getItem() instanceof ArrowItem
                        ? arrowStack.getItem() : Items.ARROW);
                AbstractArrow arrow = arrowItem.createArrow(level, arrowStack, player);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f,
                        power * 3.0f, 0.5f);

                // Divine bow bonuses
                arrow.setBaseDamage(BASE_ARROW_DAMAGE);
                arrow.setNoGravity(true); // Arrows fly straight

                if (power >= 1.0f) {
                    arrow.setCritArrow(true);
                }

                // Apply enchantments
                int powerEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                if (powerEnch > 0) {
                    arrow.setBaseDamage(arrow.getBaseDamage() + powerEnch * 0.5 + 0.5);
                }
                int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
                if (punch > 0) {
                    arrow.setKnockback(punch);
                }
                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) {
                    arrow.setSecondsOnFire(100);
                }

                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));

                if (hasInfinity) {
                    arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                } else {
                    arrowStack.shrink(1);
                }

                level.addFreshEntity(arrow);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                    1.0f, 1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + power * 0.5f);
        }
    }

    // ─── Shift+Right-click: Stormstrike ───

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift+right-click = ability
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                return InteractionResultHolder.success(stack);
            }
            if (isAbilityOnCooldown(stack)) {
                return InteractionResultHolder.pass(stack);
            }
            return abilityStormstrike((ServerLevel) level, (ServerPlayer) player, stack, hand);
        }

        // Normal right-click = draw bow (never blocked by ability cooldown)
        boolean hasAmmo = !player.getProjectile(stack).isEmpty();
        if (hasAmmo) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    private static boolean isAbilityOnCooldown(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt("AbilityCooldownTicks") > 0;
    }

    private InteractionResultHolder<ItemStack> abilityStormstrike(ServerLevel level, ServerPlayer player,
                                                                   ItemStack stack, InteractionHand hand) {
        // Fire an instant lightning arrow (no draw needed)
        Arrow arrow = new Arrow(level, player);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 3.0f, 0.0f);
        arrow.setBaseDamage(BASE_ARROW_DAMAGE + 6.0); // +6 lightning bonus
        arrow.setNoGravity(true);
        arrow.setCritArrow(true);
        // Tag the arrow so we can spawn lightning on impact (via event listener)
        arrow.addTag("stormstrike_arrow");
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);

        // Grant Speed II for 4s
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));

        // Electric particles at bow
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.5, player.getZ(),
                20, 0.3, 0.3, 0.3, 0.2);

        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.5F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);

        stack.getOrCreateTag().putInt("AbilityCooldownTicks", ABILITY_COOLDOWN_TICKS);
        stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Called from a Forge ProjectileImpactEvent or entity tick to handle
     * Stormstrike lightning on impact. Should be called from an event handler.
     */
    public static void onStormstrikeArrowHit(AbstractArrow arrow, LivingEntity hitEntity) {
        if (arrow.getTags().contains("stormstrike_arrow") && arrow.level() instanceof ServerLevel serverLevel) {
            // Summon visual-only lightning
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.setVisualOnly(true);
                bolt.moveTo(hitEntity.getX(), hitEntity.getY(), hitEntity.getZ());
                serverLevel.addFreshEntity(bolt);
            }

            // Extra lightning damage
            hitEntity.hurt(arrow.damageSources().lightningBolt(), 6.0f);

            // Electric particles
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    hitEntity.getX(), hitEntity.getY() + 1.0, hitEntity.getZ(),
                    30, 0.4, 0.6, 0.4, 0.2);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    hitEntity.getX(), hitEntity.getY() + 1.0, hitEntity.getZ(),
                    1, 0, 0, 0, 0);
        }
    }

    // ─── Passive: Speed I while held ───

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        // Decrement ability cooldown (ticks down even if not held)
        if (stack.hasTag() && stack.getTag().getInt("AbilityCooldownTicks") > 0) {
            int remaining = stack.getTag().getInt("AbilityCooldownTicks") - 1;
            if (remaining <= 0) {
                stack.getTag().remove("AbilityCooldownTicks");
            } else {
                stack.getTag().putInt("AbilityCooldownTicks", remaining);
            }
        }

        if (player.getMainHandItem() != stack) return;

        // Speed I (refresh every 3 seconds to keep it active)
        if (level.getGameTime() % 60 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false));
        }
    }

    // ─── Tooltip ───

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Stormstrike")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Shift+Right-click: Instant lightning arrow")
                .withStyle(ChatFormatting.GRAY));
        if (isAbilityOnCooldown(stack)) {
            int remainingSeconds = (stack.getTag().getInt("AbilityCooldownTicks") + 19) / 20;
            tooltip.add(Component.literal("Cooldown: " + remainingSeconds + "s remaining")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Cooldown: 12s")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Passive: Arrows fly straight. Speed I while held.")
                .withStyle(ChatFormatting.DARK_PURPLE));
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
