package com.otectus.spells_n_gods.item;

import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.item.ability.DivineAbility;
import com.otectus.spells_n_gods.item.ability.DivineAbilityDefinition;
import com.otectus.spells_n_gods.item.ability.DivineAbilityRegistry;
import com.otectus.spells_n_gods.registry.ModItems;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Base class for the 7 melee divine weapons. Each god's weapon has unique stats,
 * a right-click special ability, and passive on-hit effects.
 * <p>
 * Weapons: Warhammer of Creation (Deus), Philosopher's Dagger (Celia),
 * Warmonger's Sword (Bella), Peacemaking Staff (Bricoleur), Void Orb (Ingenium),
 * Book of Magery (Magnus), Glacial Battleaxe (Glacia).
 */
public class DivineWeaponItem extends SwordItem {

    private final String godId;
    private final int abilityCooldownTicks;
    private final String abilityName;
    private final String abilityDesc;
    private final String passiveDesc;

    public DivineWeaponItem(String godId, int attackDamage, float attackSpeed,
                            int abilityCooldownTicks, String abilityName,
                            String abilityDesc, String passiveDesc) {
        super(DivineTier.INSTANCE, attackDamage - (int) DivineTier.INSTANCE.getAttackDamageBonus(),
                attackSpeed - 4.0f, // SwordItem constructor offsets by 4.0
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.godId = godId;
        this.abilityCooldownTicks = abilityCooldownTicks;
        this.abilityName = abilityName;
        this.abilityDesc = abilityDesc;
        this.passiveDesc = passiveDesc;
    }

    public String getGodId() {
        return godId;
    }

    // ─── Right-click Ability ───

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        boolean success = switch (godId) {
            case "deus" -> abilityDeus(serverLevel, serverPlayer, stack);
            case "celia" -> abilityCelia(serverLevel, serverPlayer, stack);
            case "bella" -> abilityBella(serverLevel, serverPlayer, stack);
            case "bricoleur" -> abilityBricoleur(serverLevel, serverPlayer, stack);
            case "ingenium" -> abilityIngenium(serverLevel, serverPlayer, stack);
            case "magnus" -> abilityMagnus(serverLevel, serverPlayer, stack);
            case "glacia" -> abilityGlacia(serverLevel, serverPlayer, stack);
            default -> tryDataDrivenAbility(serverLevel, serverPlayer, stack);
        };

        if (success) {
            player.getCooldowns().addCooldown(this, abilityCooldownTicks);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    // ─── Passive On-Hit ───

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        switch (godId) {
            case "deus" -> {
                // +3 bonus damage to undead
                if (target.isInvertedHealAndHarm()) {
                    target.hurt(attacker.damageSources().magic(), 3.0f);
                }
            }
            case "celia" -> {
                // Lifesteal during Sanguine Pact: check for effect tag
                if (attacker instanceof Player player && stack.hasTag()
                        && stack.getTag().getBoolean("SanguinePact")) {
                    // Approximate 30% lifesteal from weapon damage
                    player.heal(getDamage() * 0.3f);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                }
            }
            case "bella" -> {
                // Set target on fire for 3 seconds
                target.setSecondsOnFire(3);
            }
            case "glacia" -> {
                // Slowness I for 2 seconds
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
        }
        return result;
    }

    // ─── Held-in-hand Passives (tick-based) ───

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        // Only apply mainhand passives when actually held
        if (player.getMainHandItem() != stack) return;

        // Subtle school-colored idle glow (1-2 particles every 2 seconds)
        if (level.getGameTime() % 40 == 0 && level instanceof ServerLevel sl) {
            GodDefinition god = SpellsNGodsDataManager.getGods().get(
                    new ResourceLocation("spells_n_gods", godId));
            String school = god != null ? god.magicSchool() : "";
            Vector3f color = SchoolColors.getSchoolColor(school);
            DustParticleOptions dust = new DustParticleOptions(color, 0.6f);
            sl.sendParticles(dust,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.3,
                    player.getY() + 0.8 + player.getRandom().nextDouble() * 0.4,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.3,
                    1, 0, 0.02, 0, 0.0);
        }

        switch (godId) {
            case "bricoleur" -> {
                // Hostile mobs within 8 blocks get Glowing
                if (level.getGameTime() % 20 == 0) { // Check once per second
                    AABB area = player.getBoundingBox().inflate(8.0);
                    level.getEntitiesOfClass(Mob.class, area,
                            m -> m.isAlive() && m instanceof net.minecraft.world.entity.monster.Monster
                    ).forEach(mob ->
                            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false))
                    );
                }
            }
            case "celia" -> {
                // Sanguine Pact timer — decrement and remove when expired
                if (stack.hasTag() && stack.getTag().getBoolean("SanguinePact")) {
                    int remaining = stack.getTag().getInt("SanguinePactTicks");
                    if (remaining <= 0) {
                        stack.getTag().remove("SanguinePact");
                        stack.getTag().remove("SanguinePactTicks");
                    } else {
                        stack.getTag().putInt("SanguinePactTicks", remaining - 1);
                        // Crimson particle trail
                        if (level instanceof ServerLevel sl && level.getGameTime() % 3 == 0) {
                            DustParticleOptions red = new DustParticleOptions(new Vector3f(0.8f, 0.0f, 0.1f), 1.2f);
                            sl.sendParticles(red, player.getX(), player.getY() + 1.0, player.getZ(),
                                    3, 0.3, 0.4, 0.3, 0.02);
                        }
                    }
                }
            }
        }
    }

    // ─── Kill Tracking (Celia passive: kill = +1 heart) ───

    /**
     * Called from a Forge LivingDeathEvent listener to grant Celia's kill-heal passive.
     */
    public static void onKillWithDivineWeapon(Player player, LivingEntity killed) {
        ItemStack mainhand = player.getMainHandItem();
        if (mainhand.getItem() instanceof DivineWeaponItem divine && divine.godId.equals("celia")) {
            player.heal(2.0f); // 1 heart
        }
        if (mainhand.getItem() instanceof DivineWeaponItem divine && divine.godId.equals("magnus")) {
            player.giveExperiencePoints(player.getXpNeededForNextLevel() / 7); // ~3 levels scaled
        }
    }

    // ─── Tooltip ───

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(abilityName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal(abilityDesc)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Cooldown: " + (abilityCooldownTicks / 20) + "s")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Passive: " + passiveDesc)
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /**
     * Try to execute a data-driven ability from the god's JSON definition.
     * Used as a fallback when no hardcoded ability exists for this god.
     */
    private boolean tryDataDrivenAbility(ServerLevel level, ServerPlayer player, ItemStack stack) {
        ResourceLocation godResLoc = new ResourceLocation("spells_n_gods", godId);
        GodDefinition god = SpellsNGodsDataManager.getGods().get(godResLoc);
        if (god == null || god.weaponAbility() == null) {
            return false;
        }

        DivineAbilityDefinition def = god.weaponAbility();
        DivineAbility ability = DivineAbilityRegistry.get(def.type());
        if (ability == null) {
            return false;
        }

        return ability.execute(level, player, stack, def.parameters());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ABILITY IMPLEMENTATIONS
    // ═════════════════════════════════════════════════════════════════════

    /** Deus — Divine Mandate: AoE shockwave + Glowing + Resistance */
    private boolean abilityDeus(ServerLevel level, ServerPlayer player, ItemStack stack) {
        double radius = 5.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().magic(), 8.0f);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0)); // 10s
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(0.8);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
            target.hurtMarked = true;
        }

        // Resistance I for 6s
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));

        // Golden particle burst
        for (int i = 0; i < 36; i++) {
            double angle = (i / 36.0) * Math.PI * 2;
            for (int r = 1; r <= 3; r++) {
                double dist = r * (radius / 3.0);
                level.sendParticles(ParticleTypes.END_ROD,
                        player.getX() + Math.cos(angle) * dist,
                        player.getY() + 0.3,
                        player.getZ() + Math.sin(angle) * dist,
                        1, 0, 0.2, 0, 0.02);
            }
        }
        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                2, 0, 0, 0, 0);

        level.playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.5F);
        return true;
    }

    /** Celia — Sanguine Pact: sacrifice health for lifesteal + Wither on-hit */
    private boolean abilityCelia(ServerLevel level, ServerPlayer player, ItemStack stack) {
        // Must have at least 5 hearts to sacrifice 4
        if (player.getHealth() <= 10.0f) {
            player.sendSystemMessage(Component.literal("Not enough health for the sacrifice!")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Sacrifice 4 hearts
        player.hurt(player.damageSources().magic(), 8.0f);

        // Activate lifesteal mode for 8 seconds (160 ticks)
        stack.getOrCreateTag().putBoolean("SanguinePact", true);
        stack.getOrCreateTag().putInt("SanguinePactTicks", 160);

        // Visual feedback
        DustParticleOptions bloodRed = new DustParticleOptions(new Vector3f(0.7f, 0.0f, 0.0f), 2.0f);
        level.sendParticles(bloodRed,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                player.getX(), player.getY() + 1.5, player.getZ(),
                20, 0.4, 0.4, 0.4, 0.02);

        level.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    /** Bella — Inferno Charge: dash forward, ignite everything in path */
    private boolean abilityBella(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.position();
        double dashDist = 6.0;

        // Raycast for wall check
        BlockHitResult blockHit = level.clip(new ClipContext(
                player.getEyePosition(), player.getEyePosition().add(look.scale(dashDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double actualDist = blockHit.getType() == HitResult.Type.BLOCK
                ? Math.max(0, player.getEyePosition().distanceTo(blockHit.getLocation()) - 1.0)
                : dashDist;

        Vec3 end = start.add(look.scale(actualDist));
        player.teleportTo(end.x, end.y, end.z);

        // Damage and ignite everything along the dash path
        AABB dashBox = player.getBoundingBox().expandTowards(look.scale(actualDist)).inflate(1.5);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, dashBox,
                e -> e != player && e.isAlive());
        for (LivingEntity target : hit) {
            target.hurt(player.damageSources().playerAttack(player), 10.0f);
            target.setSecondsOnFire(6);
        }

        // Fire Resistance for wielder
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0));

        // Fire particle trail
        for (int i = 0; i < (int) (actualDist * 4); i++) {
            double t = i / (actualDist * 4);
            Vec3 pos = start.add(look.scale(t * actualDist));
            level.sendParticles(ParticleTypes.FLAME,
                    pos.x, pos.y + 0.5, pos.z, 3, 0.2, 0.2, 0.2, 0.03);
            level.sendParticles(ParticleTypes.LAVA,
                    pos.x, pos.y + 0.1, pos.z, 1, 0.3, 0.05, 0.3, 0.0);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    /** Bricoleur — Prophet's Judgment: summon 3 vex allies */
    private boolean abilityBricoleur(ServerLevel level, ServerPlayer player, ItemStack stack) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < 3; i++) {
            Vex vex = EntityType.VEX.create(level);
            if (vex == null) continue;

            double offsetX = (rng.nextDouble() - 0.5) * 4;
            double offsetZ = (rng.nextDouble() - 0.5) * 4;
            vex.moveTo(player.getX() + offsetX, player.getY() + 1.5, player.getZ() + offsetZ);

            // Vexes expire after 15 seconds
            vex.setLimitedLife(300); // 15 seconds = 300 ticks

            level.addFreshEntity(vex);
        }

        // Wielder buffs while vexes active
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 300, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));

        // Enchantment particles
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40, 1.0, 1.0, 1.0, 0.5);
        level.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY() + 2.0, player.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);

        level.playSound(null, player.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.VEX_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.2F);
        return true;
    }

    /** Ingenium — Rift Walk: teleport + void pulse on arrival */
    private boolean abilityIngenium(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.position();
        double maxDist = 16.0;

        // Raycast for block collision
        BlockHitResult blockHit = level.clip(new ClipContext(
                player.getEyePosition(), player.getEyePosition().add(look.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double actualDist = blockHit.getType() == HitResult.Type.BLOCK
                ? Math.max(0, player.getEyePosition().distanceTo(blockHit.getLocation()) - 1.5)
                : maxDist;

        Vec3 end = start.add(look.scale(actualDist));

        // Particles at origin
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                start.x, start.y + 1.0, start.z, 30, 0.3, 0.6, 0.3, 0.1);
        level.sendParticles(ParticleTypes.PORTAL,
                start.x, start.y + 1.0, start.z, 20, 0.4, 0.8, 0.4, 0.3);

        // Teleport
        player.teleportTo(end.x, end.y, end.z);

        // Void pulse at destination
        double pulseRadius = 4.0;
        AABB area = player.getBoundingBox().inflate(pulseRadius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());
        for (LivingEntity target : nearby) {
            target.hurt(player.damageSources().magic(), 6.0f);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)); // 3s
        }

        // Particles at destination
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                end.x, end.y + 1.0, end.z, 40, 0.5, 0.8, 0.5, 0.15);
        level.sendParticles(ParticleTypes.PORTAL,
                end.x, end.y + 1.0, end.z, 30, 0.4, 0.8, 0.4, 0.4);

        // Sound
        level.playSound(null, BlockPos.containing(start),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.4F, 1.5F);
        return true;
    }

    /** Magnus — Eldritch Barrage: channel then fire 5 homing magic projectiles */
    private boolean abilityMagnus(ServerLevel level, ServerPlayer player, ItemStack stack) {
        // For simplicity, this is an instant barrage (channeling would require
        // use-duration mechanics). Fire 5 homing magic damage bursts.
        LivingEntity primaryTarget = null;
        double closestDist = 144.0; // 12 blocks squared
        AABB searchArea = player.getBoundingBox().inflate(12.0);
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e != player && e.isAlive() && e instanceof net.minecraft.world.entity.monster.Monster);

        for (LivingEntity e : hostiles) {
            double dist = player.distanceToSqr(e);
            if (dist < closestDist) {
                closestDist = dist;
                primaryTarget = e;
            }
        }

        if (primaryTarget == null) {
            player.sendSystemMessage(Component.literal("No hostile targets nearby!")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return false;
        }

        // Apply slow during "channel"
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));

        // 5 projectile hits (simulated — instant damage with staggered particles)
        float damagePerHit = 5.0f;
        LivingEntity target = primaryTarget;
        for (int i = 0; i < 5; i++) {
            target.hurt(player.damageSources().magic(), damagePerHit);

            // Staggered particle trails from player to target
            Vec3 from = player.getEyePosition().add(0, -0.3, 0);
            Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 dir = to.subtract(from);
            double dist = dir.length();
            Vec3 normDir = dir.normalize();

            // Add slight random offset per projectile for visual variety
            double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5;
            double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5;

            int steps = (int) Math.min(dist * 2, 20);
            for (int s = 0; s < steps; s++) {
                double t = s / (double) steps;
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                        from.x + normDir.x * t * dist + offsetX * t,
                        from.y + normDir.y * t * dist,
                        from.z + normDir.z * t * dist + offsetZ * t,
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Impact particles
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                target.getX(), target.getY() + 1.0, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                target.getX(), target.getY() + 1.0, target.getZ(),
                1, 0, 0, 0, 0);

        // Caster particles
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1.5, player.getZ(),
                15, 0.3, 0.5, 0.3, 0.05);

        level.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8F, 1.5F);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.2F);
        return true;
    }

    /** Glacia — Absolute Zero: frost nova with freeze + ground ice */
    private boolean abilityGlacia(ServerLevel level, ServerPlayer player, ItemStack stack) {
        double radius = 6.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().freeze(), 8.0f);
            // Frozen in place: 3s (Slowness 127 + Mining Fatigue III)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 127, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 2, false, false));
            // After freeze: Slowness II for 8s
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 1, false, true));
            // Freeze visual
            target.setTicksFrozen(Math.min(target.getTicksFrozen() + 300, 300));

            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(0.4);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.15, knockback.z));
            target.hurtMarked = true;
        }

        // Ice/snow particle blanket
        for (int i = 0; i < 48; i++) {
            double angle = (i / 48.0) * Math.PI * 2;
            for (int r = 1; r <= 4; r++) {
                double dist = r * (radius / 4.0);
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        player.getX() + Math.cos(angle) * dist,
                        player.getY() + 0.3,
                        player.getZ() + Math.sin(angle) * dist,
                        2, 0.1, 0.2, 0.1, 0.02);
            }
        }
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40, radius * 0.5, 0.5, radius * 0.5, 0.1);

        // Frost Walker ice on ground (temporary — blocks revert in ~10s naturally)
        BlockPos center = player.blockPosition();
        int intRadius = (int) radius;
        for (int dx = -intRadius; dx <= intRadius; dx++) {
            for (int dz = -intRadius; dz <= intRadius; dz++) {
                if (dx * dx + dz * dz > intRadius * intRadius) continue;
                BlockPos surface = center.offset(dx, 0, dz);
                BlockPos above = surface.above();
                var surfaceState = level.getBlockState(surface);
                // Frost Walker: turn water source blocks to frosted ice...
                if (surfaceState.getBlock() == Blocks.WATER && surfaceState.getFluidState().isSource()) {
                    level.setBlockAndUpdate(surface, Blocks.FROSTED_ICE.defaultBlockState());
                }
                // ...and dust a thin snow layer over solid ground where it can survive.
                else if (surfaceState.isSolidRender(level, surface)
                        && level.isEmptyBlock(above)
                        && Blocks.SNOW.defaultBlockState().canSurvive(level, above)) {
                    level.setBlockAndUpdate(above, Blocks.SNOW.defaultBlockState());
                }
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.5F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  DIVINE TIER
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Custom tier for divine weapons. Above netherite: 3000 durability,
     * repairable with Runic Fragments.
     */
    public enum DivineTier implements Tier {
        INSTANCE;

        @Override public int getUses() { return 3000; }
        @Override public float getSpeed() { return 10.0f; }
        @Override public float getAttackDamageBonus() { return 0; } // Handled per-weapon in constructor
        @Override public int getLevel() { return 5; } // Above netherite (4)
        @Override public int getEnchantmentValue() { return 22; }
        @Override public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.RUNIC_FRAGMENT.get());
        }
    }
}
