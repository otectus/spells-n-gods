package com.otectus.spells_n_gods.registry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.boss.GodBossEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SpellsNGodsMod.MODID);

    public static final RegistryObject<EntityType<GodBossEntity>> GOD_BOSS = ENTITIES.register("god_boss",
            () -> EntityType.Builder.<GodBossEntity>of(GodBossEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.925F) // 1.5x scale: 3 blocks tall, wider hitbox
                    .clientTrackingRange(64)
                    .fireImmune()
                    .build("god_boss"));
}
