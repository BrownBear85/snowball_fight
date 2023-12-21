package com.bonker.snowball_fight;

import com.bonker.snowball_fight.behavior.BehaviorChanges;
import com.bonker.snowball_fight.world.IceShardLootModifier;
import com.bonker.snowball_fight.world.SnowballLauncherItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SnowballFight.MODID)
public class SnowballFight {
    public static final String MODID = "snowball_fight";

    // items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> ICE_SHARD = ITEMS.register("ice_shard",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<SnowballItem> ICY_SNOWBALL = ITEMS.register("icy_snowball",
            () -> new SnowballItem(new Item.Properties()));

    public static final RegistryObject<SnowballLauncherItem> SNOWBALL_LAUNCHER = ITEMS.register("snowball_launcher",
            () -> new SnowballLauncherItem(new Item.Properties().stacksTo(1)));

    // other registries

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLM_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final RegistryObject<Codec<IceShardLootModifier>> ICE_SHARD_GLM = GLM_SERIALIZERS.register("ice_shards", () -> IceShardLootModifier.CODEC);

    // tags
    public static final TagKey<Item> SNOWBALL_LAUNCHER_AMMO = TagKey.create(Registries.ITEM, new ResourceLocation(MODID, "is_snowball_launcher_ammo"));

    public static final TagKey<Block> SNOWBALL_FIGHTER_SPAWN_BLOCKS = TagKey.create(Registries.BLOCK, new ResourceLocation(MODID, "snowball_fighter_spawn_blocks"));

    public static final TagKey<EntityType<?>> ALWAYS_SPAWNS_WITH_LAUNCHER = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(MODID, "always_spawns_with_launcher"));
    public static final TagKey<EntityType<?>> ALWAYS_SPAWNS_WITH_ICY_SNOWBALL = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(MODID, "always_spawns_with_icy_snowball"));

    public SnowballFight() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::buildTabs);

        ITEMS.register(modEventBus);
        GLM_SERIALIZERS.register(modEventBus);
    }

    // events

    private void commonSetup(final FMLCommonSetupEvent event) {
        BehaviorChanges.init();
    }

    private void buildTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(ICE_SHARD);
        }
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            event.accept(SNOWBALL_LAUNCHER);
            event.accept(ICY_SNOWBALL);
        }
    }
}
