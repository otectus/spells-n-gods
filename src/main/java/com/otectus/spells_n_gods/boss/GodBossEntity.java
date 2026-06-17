package com.otectus.spells_n_gods.boss;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.boss.ai.*;
import com.otectus.spells_n_gods.compat.ModIntegrationLayer;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.util.SchoolColors;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import net.minecraft.core.particles.ParticleTypes;
import com.otectus.spells_n_gods.network.BossVisualEffectPacket;
import com.otectus.spells_n_gods.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GodBossEntity extends Monster implements GeoEntity {

    /** Y offset from structure center to the safe standing position above the altar. */
    public static final double ALTAR_STANDING_Y_OFFSET = 3.0;

    private static final EntityDataAccessor<String> DATA_GOD_ID =
            SynchedEntityData.defineId(GodBossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_EMERGING =
            SynchedEntityData.defineId(GodBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CASTING =
            SynchedEntityData.defineId(GodBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED =
            SynchedEntityData.defineId(GodBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHIELDING =
            SynchedEntityData.defineId(GodBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LEAPING =
            SynchedEntityData.defineId(GodBossEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private ServerBossEvent bossEvent;
    private BossPhase currentPhase = BossPhase.IDLE;
    private int spellCooldown = 0;
    private boolean statsApplied = false;
    private boolean enrageAnnounced = false;
    private boolean enrageAnimPlayed = false;
    private boolean idleVariantPlaying = false;

    // Cross-goal references for reactive behavior
    @Nullable private BossRepositionGoal repositionGoal;
    @Nullable private BossStrafeGoal strafeGoal;
    @Nullable private BossMeleeGoal meleeGoal;
    private boolean combatGoalsInitialized = false;

    // Movement smoothing state
    private int moveTicks = 0;
    private boolean wasMoving = false;
    private static final int ACCEL_TICKS = 5;
    private static final UUID ENRAGE_SPEED_UUID = UUID.fromString("c5e3a7b2-1d4f-4e8a-9b6c-3f2d1e0a5b7c");
    private static final UUID ENRAGE_DAMAGE_UUID = UUID.fromString("d6f4b8c3-2e5a-4f9b-8c7d-4a3e2f1b6c8d");

    public GodBossEntity(EntityType<? extends GodBossEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 500;
    }

    // --- Entity Data ---

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GOD_ID, "");
        this.entityData.define(DATA_EMERGING, false);
        this.entityData.define(DATA_CASTING, false);
        this.entityData.define(DATA_ENRAGED, false);
        this.entityData.define(DATA_SHIELDING, false);
        this.entityData.define(DATA_LEAPING, false);
    }

    public String getGodId() {
        return this.entityData.get(DATA_GOD_ID);
    }

    public void setGodId(String godId) {
        this.entityData.set(DATA_GOD_ID, godId);
    }

    public boolean isEmerging() {
        return this.entityData.get(DATA_EMERGING);
    }

    public void setEmerging(boolean emerging) {
        this.entityData.set(DATA_EMERGING, emerging);
    }

    public boolean isCasting() {
        return this.entityData.get(DATA_CASTING);
    }

    public void setCasting(boolean casting) {
        this.entityData.set(DATA_CASTING, casting);
    }

    public boolean isShielding() {
        return this.entityData.get(DATA_SHIELDING);
    }

    public void setShielding(boolean shielding) {
        this.entityData.set(DATA_SHIELDING, shielding);
    }

    public boolean isLeaping() {
        return this.entityData.get(DATA_LEAPING);
    }

    public void setLeaping(boolean leaping) {
        this.entityData.set(DATA_LEAPING, leaping);
    }

    @Nullable
    public GodDefinition getGodDefinition() {
        String id = getGodId();
        if (id.isEmpty()) return null;
        return SpellsNGodsDataManager.getGods().get(new ResourceLocation(id));
    }

    /**
     * Returns a gendered title + display name, e.g. "Goddess Celia" or "God Deus".
     * Falls back to the raw display name if gender is unrecognized, or "Divine Boss" if
     * no god definition is available.
     */
    public String getTitledName() {
        GodDefinition god = getGodDefinition();
        if (god == null) return "Divine Boss";
        String prefix = switch (god.gender().toLowerCase()) {
            case "female" -> "Goddess";
            case "male" -> "God";
            default -> "Divine";
        };
        return prefix + " " + god.displayName();
    }

    @Override
    public Component getName() {
        GodDefinition god = getGodDefinition();
        if (god != null) {
            return Component.literal(getTitledName());
        }
        return super.getName();
    }

    // --- Goal Accessors ---

    @Nullable
    public BossRepositionGoal getRepositionGoal() {
        return repositionGoal;
    }

    @Nullable
    public BossStrafeGoal getStrafeGoal() {
        return strafeGoal;
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public void applyGodStats() {
        GodDefinition god = getGodDefinition();
        if (god == null) {
            SpellsNGodsMod.LOGGER.debug("applyGodStats: no god definition for '{}'", getGodId());
            return;
        }
        GodDefinition.BossDefinition bossDef = god.boss();
        double difficultyMul = SpellsNGodsConfig.COMMON.bossDifficultyMultiplier.get();

        var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double scaledHealth = bossDef.maxHealth() * difficultyMul;
            healthAttr.setBaseValue(scaledHealth);
            this.setHealth((float) scaledHealth);
        }
        var armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) armorAttr.setBaseValue(bossDef.armor());

        var attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) attackAttr.setBaseValue(bossDef.attackDamage() * difficultyMul);

        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(bossDef.movementSpeed());

        var kbAttr = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(bossDef.knockbackResistance());

        // Equip weapon
        ItemStack weapon = ModIntegrationLayer.getWeaponForGod(bossDef);
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        // Prevent weapon from dropping on death (boss drops are handled separately)
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        SpellsNGodsMod.LOGGER.debug("Applied stats for god '{}': HP={}, ATK={}, weapon={}",
                god.displayName(), bossDef.maxHealth(), bossDef.attackDamage(),
                weapon.getItem().getDescriptionId());

        // Late-register ranged combat goals for bow/crossbow gods
        if (!combatGoalsInitialized) {
            initCombatGoals(bossDef);
            combatGoalsInitialized = true;
        }

        this.statsApplied = true;
    }

    /**
     * Conditionally replaces the default melee goal with ranged goals
     * for gods that wield bows or crossbows.
     */
    private void initCombatGoals(GodDefinition.BossDefinition bossDef) {
        String weaponId = bossDef.weaponId();
        if (weaponId.contains("bow_of_agility")) {
            // Velox: ranged bow at priority 6, fallback melee at 7
            if (meleeGoal != null) {
                this.goalSelector.removeGoal(meleeGoal);
            }
            this.goalSelector.addGoal(6, new BossBowAttackGoal(this));
            this.goalSelector.addGoal(7, new BossMeleeGoal(this));
            // Move strafe to priority 8 (was 7)
            if (strafeGoal != null) {
                this.goalSelector.removeGoal(strafeGoal);
                this.goalSelector.addGoal(8, strafeGoal);
            }
            SpellsNGodsMod.LOGGER.debug("Registered BossBowAttackGoal for ranged god");
        } else if (weaponId.contains("crossbow_of_the_wild")) {
            // Venatas: ranged crossbow at priority 6, fallback melee at 7
            if (meleeGoal != null) {
                this.goalSelector.removeGoal(meleeGoal);
            }
            this.goalSelector.addGoal(6, new BossCrossbowAttackGoal(this));
            this.goalSelector.addGoal(7, new BossMeleeGoal(this));
            if (strafeGoal != null) {
                this.goalSelector.removeGoal(strafeGoal);
                this.goalSelector.addGoal(8, strafeGoal);
            }
            SpellsNGodsMod.LOGGER.debug("Registered BossCrossbowAttackGoal for ranged god");
        }
    }

    // --- AI Goals ---

    @Override
    protected void registerGoals() {
        // Priority 0: Don't drown
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Priority 1: Stay within arena bounds (hard leash, teleports if too far)
        this.goalSelector.addGoal(1, new ArenaLeashGoal(this));
        // Priority 2: Dramatic leap attack (cooldown-based)
        this.goalSelector.addGoal(2, new BossLeapGoal(this));
        // Priority 3: Raise shield / push enemies back (cooldown-based)
        this.goalSelector.addGoal(3, new BossShieldGoal(this));
        // Priority 4: Cast spells from spell pool
        this.goalSelector.addGoal(4, new SpellcastingGoal(this));
        // Priority 5: Post-attack tactical reposition
        repositionGoal = new BossRepositionGoal(this);
        this.goalSelector.addGoal(5, repositionGoal);
        // Priority 6: Custom melee attack (may be replaced by ranged goal in applyGodStats)
        meleeGoal = new BossMeleeGoal(this);
        this.goalSelector.addGoal(6, meleeGoal);
        // Priority 7: Circle-strafe around target
        strafeGoal = new BossStrafeGoal(this);
        this.goalSelector.addGoal(7, strafeGoal);
        // Priority 8: Idle wander within arena
        this.goalSelector.addGoal(8, new BossIdleWanderGoal(this));
        // Priority 9: Soft return to arena center
        this.goalSelector.addGoal(9, new ReturnToArenaGoal(this));
        // Priority 10-11: Ambient
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));

        // Target selectors
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // --- Boss Bar ---

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!isEmerging()) {
            ensureBossEvent();
            if (bossEvent != null) {
                bossEvent.addPlayer(player);
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossEvent != null) {
            bossEvent.removePlayer(player);
        }
    }

    private void ensureBossEvent() {
        if (bossEvent != null) return;
        GodDefinition god = getGodDefinition();
        String name = getTitledName();

        BossEvent.BossBarColor color = BossEvent.BossBarColor.PURPLE;
        BossEvent.BossBarOverlay overlay = BossEvent.BossBarOverlay.PROGRESS;

        if (god != null) {
            color = parseBossBarColor(god.boss().bossBarColor());
            overlay = parseBossBarOverlay(god.boss().bossBarOverlay());
        }

        bossEvent = new ServerBossEvent(Component.literal(name), color, overlay);
        bossEvent.setVisible(true);
    }

    private static BossEvent.BossBarColor parseBossBarColor(String name) {
        try {
            return BossEvent.BossBarColor.byName(name);
        } catch (Exception e) {
            return BossEvent.BossBarColor.PURPLE;
        }
    }

    private static BossEvent.BossBarOverlay parseBossBarOverlay(String name) {
        return switch (name) {
            case "notched_6" -> BossEvent.BossBarOverlay.NOTCHED_6;
            case "notched_10" -> BossEvent.BossBarOverlay.NOTCHED_10;
            case "notched_12" -> BossEvent.BossBarOverlay.NOTCHED_12;
            case "notched_20" -> BossEvent.BossBarOverlay.NOTCHED_20;
            default -> BossEvent.BossBarOverlay.PROGRESS;
        };
    }

    // --- Tick ---

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!statsApplied && !getGodId().isEmpty()) {
            applyGodStats();
        }

        // Safety: if boss was emerging when saved but animation handler lost it, finalize.
        // The tickCount > 5 guard prevents this from firing during the first few ticks,
        // giving BossSpawnAnimationHandler time to register the sequence.
        if (isEmerging() && this.tickCount > 5 && !BossSpawnAnimationHandler.hasActiveSequence(this)) {
            setEmerging(false);
            setNoAi(false);
            setInvulnerable(false);
            removeEffect(MobEffects.INVISIBILITY);
        }

        // Show boss bar once emergence completes
        if (!isEmerging() && bossEvent == null && !getGodId().isEmpty()) {
            ensureBossEvent();
            if (bossEvent != null && this.level() instanceof ServerLevel serverLevel) {
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(this) < 4096) { // 64 blocks (tracking range)
                        bossEvent.addPlayer(player);
                    }
                }
            }
        }

        if (bossEvent != null) {
            bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }

        if (spellCooldown > 0) spellCooldown--;

        updatePhase();

        // Movement smoothing
        applySpeedSmoothing();

        // Death sequence: escalating school particles
        if (this.isDeadOrDying() && this.level() instanceof ServerLevel serverLevel) {
            GodDefinition god = getGodDefinition();
            String school = god != null ? god.magicSchool() : "";
            if (this.deathTime % 3 == 0) {
                int count = 2 + (this.deathTime * 2);
                double spread = 0.5 + this.deathTime * 0.1;
                serverLevel.sendParticles(SchoolColors.getSchoolParticle(school),
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        count, spread, 0.8, spread, 0.05);
            }
            // Final burst at deathTime == 19 (just before removal at 20)
            if (this.deathTime == 19) {
                serverLevel.sendParticles(SchoolColors.getSchoolParticle(school),
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        60, 2.0, 1.5, 2.0, 0.1);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        3, 0, 0, 0, 0);
                this.playSound(SoundEvents.ENDER_DRAGON_DEATH, 1.5F, 1.4F);
            }
        }

        // School-themed ambient particles during combat phases
        if (currentPhase.isInCombat() && this.level() instanceof ServerLevel serverLevel) {
            GodDefinition god = getGodDefinition();
            String school = god != null ? god.magicSchool() : "";
            if (currentPhase == BossPhase.ENRAGED) {
                // Enraged: every 5 ticks, 3-5 particles at higher intensity
                if (this.tickCount % 5 == 0) {
                    serverLevel.sendParticles(SchoolColors.getSchoolParticle(school),
                            this.getX(), this.getY() + 1.0, this.getZ(),
                            3 + this.random.nextInt(3), 0.4, 0.5, 0.4, 0.03);
                }
            } else {
                // Combat: every 15 ticks, 1-2 particles
                if (this.tickCount % 15 == 0) {
                    serverLevel.sendParticles(SchoolColors.getSchoolParticle(school),
                            this.getX(), this.getY() + 1.0, this.getZ(),
                            1 + this.random.nextInt(2), 0.3, 0.4, 0.3, 0.01);
                }
            }
        }
    }

    private void applySpeedSmoothing() {
        Vec3 delta = this.getDeltaMovement();
        double horizSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        boolean movingNow = horizSpeed > 0.01;

        if (movingNow && !wasMoving) {
            moveTicks = 0;
        }
        if (movingNow) {
            moveTicks++;
        } else {
            moveTicks = 0;
        }
        wasMoving = movingNow;

        // Quadratic ease-in: smoothly ramp from 50% to 100% speed over ACCEL_TICKS
        if (movingNow && moveTicks < ACCEL_TICKS) {
            double fraction = (double) moveTicks / ACCEL_TICKS;
            double smoothFraction = fraction * fraction;
            double scale = 0.5 + 0.5 * smoothFraction;
            this.setDeltaMovement(delta.x * scale, delta.y, delta.z * scale);
        }
    }

    private void updatePhase() {
        if (this.getTarget() == null) {
            if (currentPhase != BossPhase.IDLE) {
                if (currentPhase == BossPhase.ENRAGED) {
                    removeEnrageBuffs();
                }
                currentPhase = BossPhase.IDLE;
                this.entityData.set(DATA_ENRAGED, false);
                enrageAnnounced = false;
                enrageAnimPlayed = false;
            }
            return;
        }

        float healthPercent = this.getHealth() / this.getMaxHealth();
        GodDefinition god = getGodDefinition();
        double enrageThreshold = god != null ? god.boss().enrageHealthPercent() : 0.25;

        if (healthPercent <= enrageThreshold && currentPhase != BossPhase.ENRAGED) {
            currentPhase = BossPhase.ENRAGED;
            this.entityData.set(DATA_ENRAGED, true);
            applyEnrageBuffs();

            // Announce enrage to nearby players
            if (!enrageAnnounced) {
                enrageAnnounced = true;
                String name = getTitledName();
                Component enrageMsg = Component.translatable("spells_n_gods.boss.enraged", name)
                        .withStyle(style -> style.withColor(0xFF4444).withBold(true));

                for (Player player : this.level().players()) {
                    if (player.distanceToSqr(this) < 2500) { // 50 blocks
                        player.sendSystemMessage(enrageMsg);
                    }
                }

                // Dramatic effects
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            this.getX(), this.getY() + 1.0, this.getZ(),
                            1, 0, 0, 0, 0);
                    this.playSound(SoundEvents.WITHER_SPAWN, 1.0F, 1.2F);

                    // Screen effects for nearby players (school-colored flash)
                    String enrageSchool = god != null ? god.magicSchool() : "";
                    BossVisualEffectPacket packet = new BossVisualEffectPacket(
                            BossVisualEffectPacket.EffectType.ENRAGE_FLASH,
                            this.getX(), this.getY(), this.getZ(), enrageSchool);
                    for (ServerPlayer sp : serverLevel.players()) {
                        if (sp.distanceToSqr(this) < 2500) {
                            ModNetwork.sendToPlayer(sp, packet);
                        }
                    }
                }
            }
        } else if (currentPhase == BossPhase.IDLE) {
            currentPhase = BossPhase.COMBAT;
        }
    }

    private void applyEnrageBuffs() {
        GodDefinition god = getGodDefinition();
        if (god == null) return;

        double speedMul = god.boss().enrageSpeedMultiplier() - 1.0; // Convert multiplier to bonus fraction
        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && speedAttr.getModifier(ENRAGE_SPEED_UUID) == null) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    ENRAGE_SPEED_UUID, "Enrage speed boost", speedMul,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        double dmgMul = god.boss().enrageDamageMultiplier() - 1.0;
        var atkAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null && atkAttr.getModifier(ENRAGE_DAMAGE_UUID) == null) {
            atkAttr.addTransientModifier(new AttributeModifier(
                    ENRAGE_DAMAGE_UUID, "Enrage damage boost", dmgMul,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private void removeEnrageBuffs() {
        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(ENRAGE_SPEED_UUID);
        }
        var atkAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.removeModifier(ENRAGE_DAMAGE_UUID);
        }
    }

    // --- Damage & Death ---

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Reduce damage from non-player sources (prevent cheese with lava, cacti etc.)
        if (source.getEntity() != null && !(source.getEntity() instanceof Player)) {
            amount *= 0.25f;
        }
        // Immune to falling (for leap attack)
        if (source.equals(this.damageSources().fall())) {
            return false;
        }

        boolean result = super.hurt(source, amount);
        if (result && currentPhase == BossPhase.IDLE && this.getTarget() != null) {
            currentPhase = BossPhase.COMBAT;
        }

        // Reactive dodge: 40% chance to trigger strafe dodge when hit by a player
        if (result && strafeGoal != null && source.getEntity() instanceof Player) {
            if (ThreadLocalRandom.current().nextFloat() < 0.4f) {
                strafeGoal.requestDodge();
            }
        }

        return result;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide()) {
            if (bossEvent != null) {
                bossEvent.removeAllPlayers();
                bossEvent = null;
            }

            if (this.getServer() != null && !getGodId().isEmpty()) {
                GodWorldState state = GodWorldState.get(this.getServer());
                GodDefinition god = getGodDefinition();
                int respawnDelay = god != null ? god.boss().respawnDelayTicks() : 72000;
                state.markBossKilled(getGodId(), this.level().getGameTime(), respawnDelay);

                // Drop compat mod items programmatically
                ModIntegrationLayer.dropBossLoot(this, source);

                // Announce death
                String name = getTitledName();
                Component deathMsg = Component.translatable("spells_n_gods.boss.defeated", name)
                        .withStyle(style -> style.withColor(0xFFD700).withBold(true));
                for (Player player : this.level().players()) {
                    if (player.distanceToSqr(this) < 10000) { // 100 blocks
                        player.sendSystemMessage(deathMsg);
                    }
                }

                // School-colored loot pillar VFX (death flash packet)
                String deathSchool = god != null ? god.magicSchool() : "";
                BossVisualEffectPacket deathPacket = new BossVisualEffectPacket(
                        BossVisualEffectPacket.EffectType.DEATH_FLASH,
                        this.getX(), this.getY(), this.getZ(), deathSchool);
                for (ServerPlayer sp : ((ServerLevel) this.level()).players()) {
                    if (sp.distanceToSqr(this) < 10000) {
                        ModNetwork.sendToPlayer(sp, deathPacket);
                    }
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        // Only mark boss as gone for permanent removal reasons, not chunk unloads
        if (!this.level().isClientSide() && reason != RemovalReason.KILLED
                && reason != RemovalReason.UNLOADED_TO_CHUNK
                && reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            if (this.getServer() != null && !getGodId().isEmpty()) {
                GodWorldState state = GodWorldState.get(this.getServer());
                state.markBossAlive(getGodId(), false);
            }
        }
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
            bossEvent = null;
        }
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // Bosses never despawn
    }

    @Override
    public boolean isOnFire() {
        // Gods don't burn -- prevents visual fire overlay
        return false;
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("GodId", getGodId());
        tag.putString("Phase", currentPhase.name());
        tag.putInt("SpellCooldown", spellCooldown);
        tag.putBoolean("Emerging", isEmerging());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGodId(tag.getString("GodId"));
        try {
            currentPhase = BossPhase.valueOf(tag.getString("Phase"));
        } catch (Exception e) {
            currentPhase = BossPhase.IDLE;
        }
        this.entityData.set(DATA_ENRAGED, currentPhase == BossPhase.ENRAGED);
        // Re-apply the enrage attribute buffs when reloading a boss that was already enraged;
        // updatePhase() only buffs on the IDLE/COMBAT -> ENRAGED transition, which never fires on load.
        // applyEnrageBuffs() is idempotent (it no-ops if the modifier already exists).
        if (currentPhase == BossPhase.ENRAGED) {
            applyEnrageBuffs();
        }
        spellCooldown = tag.getInt("SpellCooldown");
        setEmerging(tag.getBoolean("Emerging"));
        statsApplied = false; // Will re-apply on next tick
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isEmerging()) return PlayState.STOP;
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.god_boss.walk"));
            }
            // Randomly play idle variants when out of combat
            if (this.getCurrentPhase() == BossPhase.IDLE && !idleVariantPlaying
                    && this.tickCount % 100 == 0 && this.random.nextFloat() < 0.3f) {
                idleVariantPlaying = true;
                String variant = this.random.nextBoolean()
                        ? "animation.god_boss.idle_look_around"
                        : "animation.god_boss.idle_flourish";
                return state.setAndContinue(RawAnimation.begin()
                        .thenPlay(variant)
                        .thenLoop("animation.god_boss.idle"));
            }
            idleVariantPlaying = false;
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.god_boss.idle"));
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.isEmerging()) return PlayState.STOP;
            if (this.isShielding()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.god_boss.shield_activate"));
            }
            if (this.isLeaping()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.god_boss.leap"));
            }
            if (this.isCasting()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.god_boss.attack_cast"));
            }
            if (this.swinging) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.god_boss.attack_melee"));
            }
            if (this.hurtTime > 0) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.god_boss.hit_react"));
            }
            return PlayState.STOP;
        }));

        // Spawn emergence animation controller
        controllers.add(new AnimationController<>(this, "spawn", 0, state -> {
            if (this.isEmerging()) {
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.god_boss.spawn_emerge"));
            }
            return PlayState.STOP;
        }));

        // Phase transition controller (highest priority for enrage transition)
        controllers.add(new AnimationController<>(this, "phase", 0, state -> {
            if (this.isEnraged() && !enrageAnimPlayed) {
                enrageAnimPlayed = true;
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.god_boss.enrage_transition"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- Spell Helpers ---

    public boolean canCastSpell() {
        return spellCooldown <= 0;
    }

    public void setSpellCooldown(int ticks) {
        this.spellCooldown = ticks;
    }

    public int getSpellCooldown() {
        return spellCooldown;
    }

    public BossPhase getCurrentPhase() {
        return currentPhase;
    }

    public boolean isEnraged() {
        return this.entityData.get(DATA_ENRAGED);
    }
}
