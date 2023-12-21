package com.bonker.snowball_fight;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = SnowballFight.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static class Server {
        public final BooleanValue snowballsStackTo64;
        public final BooleanValue snowballsSlowMobs;
        public final BooleanValue raidersUseSnowballs;
        public final DoubleValue snowballDamage;
        public final DoubleValue icySnowballDamage;
        public final IntValue launcherAmmoSize;
        public final IntValue launcherLoadTime;
        public final IntValue launcherShotCooldown;
        public final IntValue snowballCooldown;
        public final EnumValue<MobConfigOptions> mobsThrowSnowballs;

        private enum MobConfigOptions {never, snowy_biomes, all_biomes}

        public Server(Builder builder) {
            builder.comment("Server-side config for Snowball Fight")
                    .push("server");

            snowballsStackTo64 = builder
                    .comment("When enabled, snowballs stack to 64 instead of 16.",
                            "Enabled by default.")
                    .translation("snowball_fight.configgui.snowballsStackTo64")
                    .define("snowballsStackTo64", true);

            snowballsSlowMobs = builder
                    .comment("When enabled, snowballs apply slowness to any entities that they hit.",
                            "Disabled by default.")
                    .translation("snowball_fight.configgui.snowballsSlowMobs")
                    .define("snowballsSlowMobs", false);

            raidersUseSnowballs = builder
                    .comment("When enabled, Pillagers and Vindicators have the ability to fight with snowballs.",
                            "Enabled by default.")
                    .translation("snowball_fight.configgui.raidersUseSnowballs")
                    .define("raidersUseSnowballs", true);

            snowballDamage = builder
                    .comment("The base damage of regular snowballs.",
                            "This value is multiplied by about 1.4 for throwing by hand and by about 2.4 when shooting from a Snowball Launcher.",
                            "Default is 2.2 damage.")
                    .translation("snowball_fight.configgui.snowballDamage")
                    .defineInRange("snowballDamage", 2.2, 0, 1000);

            icySnowballDamage = builder
                    .comment("The base damage of icy snowballs.",
                            "This value is multiplied by about 1.4 for throwing by hand and by about 2.4 when shooting from a Snowball Launcher.",
                            "Default is 3.3 damage.")
                    .translation("snowball_fight.configgui.icySnowballDamage")
                    .defineInRange("icySnowballDamage", 3.3, 0, 1000);

            launcherAmmoSize = builder
                    .comment("How many snowballs a Snowball Launcher can hold at a time.",
                            "Default amount is 32 snowballs.")
                    .translation("snowball_fight.configgui.launcherAmmoSize")
                    .defineInRange("launcherAmmoSize", 32, 1, 4096);

            launcherLoadTime = builder
                    .comment("The time in ticks to load 1 snowball into a Snowball Launcher.",
                            "Default time is 2 ticks.")
                    .translation("snowball_fight.configgui.launcherLoadTime")
                    .defineInRange("launcherLoadTime", 2, 1, 40);

            launcherShotCooldown = builder
                    .comment("The time in ticks of the cooldown after firing a snowball from a Snowball Launcher.",
                            "9 ticks is ideal for combos and is the default.")
                    .translation("snowball_fight.configgui.launcherShotCooldown")
                    .defineInRange("launcherShotCooldown", 9, 1, 1200);

            snowballCooldown = builder
                    .comment("The time in ticks of the cooldown after throwing a snowball.",
                            "Default cooldown is 10 ticks.")
                    .translation("snowball_fight.configgui.snowballCooldown")
                    .defineInRange("snowballCooldown", 10, 0, 1200);

            mobsThrowSnowballs = builder
                    .comment("If vanilla monster attacks should be replaced with snowball attacks.",
                            "Default value is snowy_biomes.")
                    .translation("snowball_fight.configgui.mobsThrowSnowballs")
                    .defineEnum("mobsThrowSnowballs", MobConfigOptions.snowy_biomes);

            builder.pop();
        }
    }

    public static final ForgeConfigSpec serverSpec;
    public static final Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static int getLauncherAmmoSize() {
        return SERVER.launcherAmmoSize.get();
    }

    public static int getLauncherLoadTime() {
        return SERVER.launcherLoadTime.get();
    }

    public static int getLauncherCooldown() {
        return SERVER.launcherShotCooldown.get();
    }

    public static boolean snowballAttacksInBiome(Level level, Entity entity) {
        return switch (SERVER.mobsThrowSnowballs.get()) {
            case never -> false;
            case snowy_biomes -> level.getBiome(entity.blockPosition()).get().getBaseTemperature() < 0.1;
            case all_biomes -> true;
        };
    }

    @SubscribeEvent
    public static void onConfigLoaded(final ModConfigEvent.Loading event) {
        setSnowballStackSize();
    }

    @SubscribeEvent
    public static void onConfigReloaded(final ModConfigEvent.Reloading event) {
        setSnowballStackSize();
    }

    @SubscribeEvent
    public static void onConfigUnloaded(final ModConfigEvent.Unloading event) {
        setSnowballStackSize();
    }

    // update snowball stack size
    private static void setSnowballStackSize() {
        boolean largeStacks = Config.SERVER.snowballsStackTo64.get();
        int currentSize = Items.SNOWBALL.getMaxStackSize(ItemStack.EMPTY);

        if (currentSize == (largeStacks ? 16 : 64)) {
            // set snowball stack size to 64 if it's currently set to 16
            ObfuscationReflectionHelper.setPrivateValue(Item.class, Items.SNOWBALL, largeStacks ? 64 : 16, "f_41370_"); // maxStackSize
        }
    }
}
