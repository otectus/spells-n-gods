package com.otectus.spells_n_gods.client;

import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.network.DivineVfxPacket;
import com.otectus.spells_n_gods.registry.ModParticles;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * Client-side handler for divine gameplay VFX events.
 * Spawns particles and plays sounds in response to DivineVfxPacket.
 */
@OnlyIn(Dist.CLIENT)
public class DivineVfxHandler {

    public static void handleVfx(DivineVfxPacket.VfxType type, double x, double y, double z,
                                   String school, int intensity) {
        if (!SpellsNGodsConfig.COMMON.worshipVfxEnabled.get()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        switch (type) {
            case OFFERING_ACCEPTED -> spawnOfferingAccepted(level, x, y, z, school, intensity);
            case BINDING_COMPLETE -> spawnBindingComplete(level, x, y, z, school);
            case TIER_UP -> spawnTierUp(level, x, y, z, school, intensity);
            case APOSTASY_COMPLETE -> spawnApostasyComplete(level, x, y, z);
            case APOSTASY_TRIAL_START -> spawnApostasyTrialStart(level, x, y, z);
            case PRAYER_COMPLETE -> spawnPrayerComplete(level, x, y, z, school);
            case CURSE_LIFTED -> spawnCurseLifted(level, x, y, z);
        }
    }

    private static void spawnOfferingAccepted(ClientLevel level, double x, double y, double z,
                                               String school, int favorGained) {
        Vector3f color = SchoolColors.getSchoolColor(school);
        DustParticleOptions dust = new DustParticleOptions(color, 1.0f);
        ParticleOptions schoolParticle = SchoolColors.getSchoolParticle(school);

        // Upward spiral of school-colored dust
        int count = 12 + Math.min(favorGained, 8) * 2;
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2.0;
            double radius = 0.3 + (i / (double) count) * 0.5;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + 0.5 + (i / (double) count) * 1.5;
            level.addParticle(dust, px, py, pz, 0, 0.02, 0);
        }

        // School-themed particles rising from the altar
        for (int i = 0; i < 8; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.6;
            double oz = (level.random.nextDouble() - 0.5) * 0.6;
            level.addParticle(schoolParticle, x + ox, y + 0.8, z + oz,
                    0, 0.05 + level.random.nextDouble() * 0.05, 0);
        }

        // Burst particle at center
        level.addParticle(ModParticles.DIVINE_BURST.get(), x, y + 1.0, z, 0, 0.05, 0);

        level.playLocalSound(x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS,
                0.8f, 1.0f + level.random.nextFloat() * 0.2f, false);
    }

    private static void spawnBindingComplete(ClientLevel level, double x, double y, double z,
                                              String school) {
        Vector3f color = SchoolColors.getSchoolColor(school);
        DustParticleOptions dust = new DustParticleOptions(color, 1.5f);
        ParticleOptions schoolParticle = SchoolColors.getSchoolParticle(school);

        // Dramatic expanding ring
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.5 + ring * 0.8;
            int particlesInRing = 16 + ring * 8;
            for (int i = 0; i < particlesInRing; i++) {
                double angle = (i / (double) particlesInRing) * Math.PI * 2.0;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                double py = y + 0.5 + ring * 0.3;
                double vx = Math.cos(angle) * 0.03;
                double vz = Math.sin(angle) * 0.03;
                level.addParticle(dust, px, py, pz, vx, 0.02, vz);
            }
        }

        // Column of school particles
        for (int i = 0; i < 20; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.4;
            double oz = (level.random.nextDouble() - 0.5) * 0.4;
            level.addParticle(schoolParticle, x + ox, y + level.random.nextDouble() * 3.0, z + oz,
                    0, 0.01, 0);
        }

        // Burst particles
        for (int i = 0; i < 5; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.8;
            double oz = (level.random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ModParticles.DIVINE_BURST.get(), x + ox, y + 1.0 + level.random.nextDouble(), z + oz,
                    ox * 0.02, 0.03, oz * 0.02);
        }

        // Divine runes floating
        for (int i = 0; i < 6; i++) {
            double angle = (i / 6.0) * Math.PI * 2.0;
            double px = x + Math.cos(angle) * 1.2;
            double pz = z + Math.sin(angle) * 1.2;
            level.addParticle(ModParticles.DIVINE_RUNE.get(), px, y + 1.5, pz, 0, 0.01, 0);
        }

        level.playLocalSound(x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS,
                1.0f, 0.8f, false);
    }

    private static void spawnTierUp(ClientLevel level, double x, double y, double z,
                                     String school, int newTierLevel) {
        Vector3f color = SchoolColors.getSchoolColor(school);
        DustParticleOptions dust = new DustParticleOptions(color, 1.2f);
        ParticleOptions schoolParticle = SchoolColors.getSchoolParticle(school);

        // Expanding rings scaled by tier level
        int ringCount = 1 + newTierLevel;
        for (int ring = 0; ring < ringCount; ring++) {
            double radius = 0.8 + ring * 0.6;
            int particles = 12 + ring * 6;
            for (int i = 0; i < particles; i++) {
                double angle = (i / (double) particles) * Math.PI * 2.0;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                level.addParticle(dust, px, y + 0.1 + ring * 0.4, pz,
                        Math.cos(angle) * 0.02, 0.05, Math.sin(angle) * 0.02);
            }
        }

        // Upward burst of school particles
        int burstCount = 8 + newTierLevel * 4;
        for (int i = 0; i < burstCount; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.6;
            double oz = (level.random.nextDouble() - 0.5) * 0.6;
            level.addParticle(schoolParticle, x + ox, y + 0.5, z + oz,
                    ox * 0.05, 0.1 + level.random.nextDouble() * 0.1, oz * 0.05);
        }

        // Divine burst at player center
        for (int i = 0; i < 3; i++) {
            level.addParticle(ModParticles.DIVINE_BURST.get(), x, y + 1.0 + i * 0.5, z,
                    0, 0.02, 0);
        }

        // Floating runes
        for (int i = 0; i < newTierLevel + 2; i++) {
            double angle = (i / (double) (newTierLevel + 2)) * Math.PI * 2.0;
            level.addParticle(ModParticles.DIVINE_RUNE.get(),
                    x + Math.cos(angle) * 1.5, y + 1.0 + level.random.nextDouble() * 0.5,
                    z + Math.sin(angle) * 1.5, 0, 0.01, 0);
        }

        level.playLocalSound(x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                1.0f, 0.6f + newTierLevel * 0.1f, false);
    }

    private static void spawnApostasyComplete(ClientLevel level, double x, double y, double z) {
        // Dark swirling particles
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2.0;
            double radius = 0.5 + (i / 30.0) * 1.5;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + 0.5 + (i / 30.0) * 2.0;
            level.addParticle(ParticleTypes.SCULK_SOUL, px, py, pz,
                    -Math.cos(angle) * 0.02, -0.01, -Math.sin(angle) * 0.02);
        }

        // Soul particles imploding inward
        for (int i = 0; i < 16; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = 2.0 + level.random.nextDouble();
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.SOUL, px, y + 1.0 + level.random.nextDouble(), pz,
                    -Math.cos(angle) * 0.05, 0, -Math.sin(angle) * 0.05);
        }

        // Smoke burst
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x + (level.random.nextDouble() - 0.5) * 1.0,
                    y + 0.5 + level.random.nextDouble() * 0.5,
                    z + (level.random.nextDouble() - 0.5) * 1.0,
                    0, 0.03, 0);
        }

        level.playLocalSound(x, y, z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS,
                1.2f, 0.5f, false);
        level.playLocalSound(x, y, z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS,
                0.8f, 0.6f, false);
    }

    private static void spawnApostasyTrialStart(ClientLevel level, double x, double y, double z) {
        // Foreboding dark ring
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2.0;
            double radius = 1.5;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.SCULK_SOUL, px, y + 0.2, pz, 0, 0.02, 0);
        }

        // Dripping soul particles
        for (int i = 0; i < 8; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 1.0;
            double oz = (level.random.nextDouble() - 0.5) * 1.0;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    x + ox, y + 2.0 + level.random.nextDouble(), z + oz,
                    0, -0.03, 0);
        }

        level.playLocalSound(x, y, z, SoundEvents.WARDEN_NEARBY_CLOSER, SoundSource.PLAYERS,
                0.7f, 0.5f, false);
    }

    private static void spawnPrayerComplete(ClientLevel level, double x, double y, double z,
                                             String school) {
        Vector3f color = SchoolColors.getSchoolColor(school);
        DustParticleOptions dust = new DustParticleOptions(color, 1.0f);
        ParticleOptions schoolParticle = SchoolColors.getSchoolParticle(school);

        // Gentle upward spiral
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * Math.PI * 4.0;
            double radius = 0.4 + (i / 16.0) * 0.3;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + 0.5 + (i / 16.0) * 2.5;
            level.addParticle(dust, px, py, pz, 0, 0.01, 0);
        }

        // School particles at head level
        for (int i = 0; i < 6; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.5;
            double oz = (level.random.nextDouble() - 0.5) * 0.5;
            level.addParticle(schoolParticle, x + ox, y + 1.8, z + oz, 0, 0.02, 0);
        }

        // Single burst above head
        level.addParticle(ModParticles.DIVINE_BURST.get(), x, y + 2.2, z, 0, 0.02, 0);

        level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.6f, 1.2f, false);
    }

    private static void spawnCurseLifted(ClientLevel level, double x, double y, double z) {
        // Bright golden ascending particles - the inverse of dark apostasy VFX
        DustParticleOptions goldDust = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.2f), 1.5f);

        // Ascending spiral of golden light
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 6.0;
            double radius = 0.5 + (i / 24.0) * 0.4;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + (i / 24.0) * 3.0;
            level.addParticle(goldDust, px, py, pz, 0, 0.05, 0);
        }

        // Upward burst of light particles
        for (int i = 0; i < 15; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.6;
            double oz = (level.random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.END_ROD, x + ox, y + 1.0, z + oz,
                    ox * 0.05, 0.15 + level.random.nextDouble() * 0.1, oz * 0.05);
        }

        // Flash + bursts
        level.addParticle(ParticleTypes.FLASH, x, y + 1.5, z, 0, 0, 0);
        for (int i = 0; i < 3; i++) {
            level.addParticle(ModParticles.DIVINE_BURST.get(), x, y + 1.5 + i * 0.5, z, 0, 0.03, 0);
        }

        level.playLocalSound(x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.4f, false);
        level.playLocalSound(x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.0f, false);
    }
}
