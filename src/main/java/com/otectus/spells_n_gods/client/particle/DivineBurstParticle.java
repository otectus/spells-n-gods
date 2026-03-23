package com.otectus.spells_n_gods.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A bright, expanding burst particle used for divine celebrations (tier-ups, bindings, offerings).
 * Larger and shorter-lived than DivineAuraParticle, with a rapid fade-out.
 */
@OnlyIn(Dist.CLIENT)
public class DivineBurstParticle extends TextureSheetParticle {

    private final float startSize;

    protected DivineBurstParticle(ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.lifetime = 10 + this.random.nextInt(8);
        this.gravity = -0.02F;
        this.startSize = 0.15F + this.random.nextFloat() * 0.1F;
        this.quadSize = this.startSize;
        this.xd = xSpeed + (this.random.nextDouble() - 0.5) * 0.04;
        this.yd = ySpeed + 0.02 + this.random.nextDouble() * 0.03;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5) * 0.04;
        this.alpha = 1.0F;
        // Bright white-gold color
        this.rCol = 1.0F;
        this.gCol = 0.9F + this.random.nextFloat() * 0.1F;
        this.bCol = 0.5F + this.random.nextFloat() * 0.3F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        // Full brightness (emissive)
        return 0xF000F0;
    }

    @Override
    public void tick() {
        super.tick();
        float progress = (float) this.age / this.lifetime;
        // Expand then shrink
        if (progress < 0.3f) {
            this.quadSize = this.startSize * (1.0f + progress * 3.0f);
        } else {
            this.quadSize = this.startSize * (1.9f - (progress - 0.3f) * 2.7f);
        }
        this.quadSize = Math.max(0.01f, this.quadSize);
        // Rapid fade
        this.alpha = 1.0F - progress * progress;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            DivineBurstParticle particle = new DivineBurstParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
