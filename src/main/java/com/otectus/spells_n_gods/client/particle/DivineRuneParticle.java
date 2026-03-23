package com.otectus.spells_n_gods.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A rune symbol particle that floats and slowly rotates, used for spellcasting
 * and divine presence effects.
 */
@OnlyIn(Dist.CLIENT)
public class DivineRuneParticle extends TextureSheetParticle {

    private final float rotSpeed;

    protected DivineRuneParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.lifetime = 30 + this.random.nextInt(30);
        this.gravity = -0.005F;
        this.quadSize = 0.12F + this.random.nextFloat() * 0.06F;
        this.xd = xSpeed * 0.1;
        this.yd = ySpeed * 0.1 + 0.005;
        this.zd = zSpeed * 0.1;
        this.alpha = 0.9F;
        this.rotSpeed = (this.random.nextFloat() - 0.5F) * 0.05F;
        this.oRoll = this.roll = this.random.nextFloat() * (float) Math.PI * 2.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.rotSpeed;
        // Fade out
        float progress = (float) this.age / this.lifetime;
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
            DivineRuneParticle particle = new DivineRuneParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
