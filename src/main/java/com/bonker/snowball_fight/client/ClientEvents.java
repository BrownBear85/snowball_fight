package com.bonker.snowball_fight.client;

import com.bonker.snowball_fight.Config;
import com.bonker.snowball_fight.SnowballFight;
import com.bonker.snowball_fight.world.SnowballLauncherItem;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SnowballFight.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        ItemProperties.register(SnowballFight.SNOWBALL_LAUNCHER.get(),
                new ResourceLocation(SnowballFight.MODID, "loaded"),
                (stack, level, entity, seed) -> {
                    if (entity != null && entity.getType() != EntityType.PLAYER) return 0.5F;
                    int ammo = SnowballLauncherItem.getAmmo(stack);
                    if (ammo == 0) return 0;
                    if (ammo == 1) return 0.001F;
                    if (ammo == Config.getLauncherAmmoSize()) return 1;
                    return ammo / (float) Config.getLauncherAmmoSize();
                });
    }

    @SubscribeEvent
    public static void addRenderLayers(final EntityRenderersEvent.AddLayers event) {
        SnowGolemRenderer renderer = event.getRenderer(EntityType.SNOW_GOLEM);
        if (renderer != null) renderer.addLayer(new SnowGolemItemLayer(renderer, event.getContext().getItemInHandRenderer()));
    }
}
