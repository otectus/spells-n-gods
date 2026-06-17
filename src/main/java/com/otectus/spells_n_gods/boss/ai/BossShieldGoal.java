package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Periodically, the boss raises a shield/barrier, becoming temporarily damage resistant
 * and pushing nearby players away. Visual particles indicate the shield.
 * Only activates in COMBAT or ENRAGED phase.
 */
public class BossShieldGoal extends Goal {
    private final GodBossEntity boss;
    private int shieldDuration;
    private int cooldown = 0;
    /** Absorption granted by the current shield, removed when the shield drops. */
    private float grantedAbsorption = 0f;

    private static final double PUSH_STRENGTH = 1.5;
    private static final float SHIELD_ABSORPTION = 20.0f;

    public BossShieldGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private GodDefinition.BossDefinition bossDef() {
        GodDefinition god = boss.getGodDefinition();
        return god != null ? god.boss() : GodDefinition.BossDefinition.defaultBoss();
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (!boss.getCurrentPhase().isInCombat()) return false;
        if (boss.getTarget() == null) return false;

        // Activate when health is under the configured threshold
        float healthPct = boss.getHealth() / boss.getMaxHealth();
        if (healthPct > (float) bossDef().shieldActivationHealthPercent()) return false;

        return ThreadLocalRandom.current().nextFloat() < 0.30f;
    }

    @Override
    public void start() {
        shieldDuration = bossDef().shieldDurationTicks();
        boss.getNavigation().stop();
        boss.setShielding(true);

        // Apply temporary damage absorption while the shield is up (removed in stop()).
        grantedAbsorption = SHIELD_ABSORPTION;
        boss.setAbsorptionAmount(boss.getAbsorptionAmount() + grantedAbsorption);

        // Shield activation sound
        boss.level().playSound(null, boss.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.5F);

        // Push all nearby players away
        pushNearbyEntities();
    }

    @Override
    public boolean canContinueToUse() {
        return shieldDuration > 0;
    }

    @Override
    public void tick() {
        shieldDuration--;

        // School-colored particle ring around boss each tick
        if (boss.level() instanceof ServerLevel serverLevel && shieldDuration % 3 == 0) {
            GodDefinition god = boss.getGodDefinition();
            String school = god != null ? god.magicSchool() : "";
            Vector3f color = SchoolColors.getSchoolColor(school);
            DustParticleOptions dust = new DustParticleOptions(color, 1.0f);

            double radius = 2.0;
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12.0 + (boss.tickCount * 0.1);
                double px = boss.getX() + Math.cos(angle) * radius;
                double pz = boss.getZ() + Math.sin(angle) * radius;
                serverLevel.sendParticles(dust,
                        px, boss.getY() + 1.0, pz,
                        1, 0, 0.3, 0, 0.01);
            }
            // Also add enchant particles at lower density for the sparkle effect
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    boss.getX(), boss.getY() + 1.0, boss.getZ(),
                    4, radius * 0.3, 0.5, radius * 0.3, 0.1);
        }
    }

    private void pushNearbyEntities() {
        AABB aoe = boss.getBoundingBox().inflate(bossDef().shieldPushRadius());
        List<LivingEntity> nearby = boss.level().getEntitiesOfClass(
                LivingEntity.class, aoe,
                e -> e != boss && e.isAlive()
        );

        for (LivingEntity entity : nearby) {
            Vec3 push = entity.position().subtract(boss.position()).normalize().scale(PUSH_STRENGTH);
            entity.setDeltaMovement(entity.getDeltaMovement().add(push.x, 0.3, push.z));
            entity.hurtMarked = true;
        }

        // Shockwave particles and sound
        if (boss.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    boss.getX(), boss.getY() + 1.0, boss.getZ(),
                    1, 0, 0, 0, 0);
            boss.level().playSound(null, boss.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.5F, 1.5F);
        }
    }

    @Override
    public void stop() {
        GodDefinition.BossDefinition def = bossDef();
        cooldown = boss.getCurrentPhase().isEnraged() ? def.shieldEnragedCooldown() : def.shieldBaseCooldown();
        shieldDuration = 0;
        boss.setShielding(false);
        // Remove whatever absorption this shield granted so it doesn't accumulate over a long fight.
        if (grantedAbsorption > 0f) {
            boss.setAbsorptionAmount(Math.max(0f, boss.getAbsorptionAmount() - grantedAbsorption));
            grantedAbsorption = 0f;
        }
        // Trigger tactical reposition after shield expires
        if (boss.getRepositionGoal() != null) {
            boss.getRepositionGoal().triggerReposition();
        }
    }
}
