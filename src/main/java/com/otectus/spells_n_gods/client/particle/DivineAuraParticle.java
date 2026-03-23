package com.otectus.spells_n_gods.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A softly glowing, slowly rising particle used for divine aura effects around god bosses.
 * Accepts school color tinting via the sprite sheet.
 */
@OnlyIn(Dist.CLIENT)
public class DivineAuraParticle extends TextureSheetParticle {

    protected DivineAuraParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.lifetime = 20 + this.random.nextInt(20);
        this.gravity = -0.01F;
        this.quadSize = 0.08F + this.random.nextFloat() * 0.04F;
        this.xd = xSpeed + (this.random.nextDouble() - 0.5) * 0.02;
        this.yd = ySpeed + 0.01 + this.random.nextDouble() * 0.02;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5) * 0.02;
        this.alpha = 0.8F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Fade out over lifetime
        this.alpha = Math.max(0, 1.0F - ((float) this.age / this.lifetime));
        // Slowly shrink
        this.quadSize *= 0.98F;
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
            DivineAuraParticle particle = new DivineAuraParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
