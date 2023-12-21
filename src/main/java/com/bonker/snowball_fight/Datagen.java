package com.bonker.snowball_fight;

import com.bonker.snowball_fight.world.IceShardLootModifier;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SnowballFight.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Datagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new GLMs(event.getGenerator().getPackOutput(), SnowballFight.MODID));
    }

    private static class GLMs extends GlobalLootModifierProvider {
        public GLMs(PackOutput output, String modid) {
            super(output, modid);
        }

        @Override
        protected void start() {
            add("ice_shards", new IceShardLootModifier(new LootItemCondition[] {
                    new LootItemBlockStatePropertyCondition.Builder(Blocks.ICE)
                            .or(new LootItemBlockStatePropertyCondition.Builder(Blocks.PACKED_ICE))
                            .or(new LootItemBlockStatePropertyCondition.Builder(Blocks.BLUE_ICE))
                            .build()}, SnowballFight.ICE_SHARD.get(), 6));
        }
    }
}
