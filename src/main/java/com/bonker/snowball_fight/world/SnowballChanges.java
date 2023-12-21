package com.bonker.snowball_fight.world;

import com.bonker.snowball_fight.Config;
import com.bonker.snowball_fight.SnowballFight;
import com.bonker.snowball_fight.behavior.BehaviorChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SnowballFight.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SnowballChanges {
    private static final ParticleOptions ICE_PARTICLE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());

    @SubscribeEvent
    public static void onProjectileImpact(final ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof Snowball snowball) {
            Level level = snowball.level();

            // particles and sound
            if (!level.isClientSide) {
                Vec3 pos = event.getRayTraceResult().getLocation();
                if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY) pos = pos.add(0, 0.6, 0);

                SoundEvent sound;
                ParticleOptions particle;
                if (snowball.getItem().is(SnowballFight.ICY_SNOWBALL.get())) {
                    sound = SoundEvents.GLASS_BREAK;
                    particle = ICE_PARTICLE;
                } else {
                    sound = SoundEvents.SNOW_GOLEM_HURT;
                    particle = ParticleTypes.SNOWFLAKE;
                }

                level.playSound(null, pos.x(), pos.y(), pos.z(), sound, SoundSource.PLAYERS, 0.8F, 1.4F + level.random.nextFloat() * 0.3F);
                ((ServerLevel) level).sendParticles(particle, pos.x(), pos.y() + 0.4, pos.z(), 20, 0.4, 0.4, 0.4, 0);
            }

            switch (event.getRayTraceResult().getType()) {
                // hurt entity
                case ENTITY -> {
                    Entity entity = ((EntityHitResult) event.getRayTraceResult()).getEntity();

                    // icy snowballs do 50% more damage
                    float baseDamage = snowball.getItem().is(SnowballFight.ICY_SNOWBALL.get()) ?
                            Config.SERVER.icySnowballDamage.get().floatValue() :
                            Config.SERVER.snowballDamage.get().floatValue();
                    // faster snowballs do more damage
                    float damage = (float) snowball.getDeltaMovement().length() * baseDamage;

                    ((EntityHitResult) event.getRayTraceResult()).getEntity().hurt(
                            entity.damageSources().mobProjectile(snowball, snowball.getOwner() instanceof LivingEntity livingEntity ? livingEntity : null),
                            damage);

                    // slow mobs if enabled in config
                    if (Config.SERVER.snowballsSlowMobs.get() && entity instanceof LivingEntity livingEntity) {
                        livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    }
                }
                // increase snow layer when impacting the ground
                case BLOCK -> {
                     BlockHitResult hitResult = (BlockHitResult) event.getRayTraceResult();

                    // don't increase snow layer if the snowball hits the side of a block
                    if (hitResult.getDirection() != Direction.UP) return;

                    BlockPos pos = hitResult.getBlockPos();
                    BlockState state = level.getBlockState(pos);

                    // if snowball collides with a solid block under 1 layer snow,
                    // modify the snow layer instead of the solid block
                    if (notValidSnowLayer(state)) {
                        pos = pos.above();
                        state = level.getBlockState(pos);

                        // if it still isn't a snow layer
                        if (notValidSnowLayer(state)) {
                            if (state.canBeReplaced() && state.getFluidState().isEmpty() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
                                level.destroyBlock(pos, false);
                                // replace block with snow if possible
                                level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
                            }

                            // no snow layers to increase
                            return;
                        }
                    }

                    // increase snow layer height
                    if (state.getValue(SnowLayerBlock.LAYERS) < 8) {
                        level.setBlock(pos, state.cycle(SnowLayerBlock.LAYERS), 3);
                    }
                }
            }
        }
    }

    // is a snow layer block with less than 8 height
    private static boolean notValidSnowLayer(BlockState state) {
        return !state.is(Blocks.SNOW) || state.getValue(SnowLayerBlock.LAYERS) >= 8;
    }

    @SubscribeEvent
    public static void onBlockClicked(final PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();

        // click the top face
        if (event.getHitVec().getDirection() != Direction.UP ||
                // require an empty main hand
                event.getHand() != InteractionHand.MAIN_HAND || (!stack.isEmpty() && !stack.is(SnowballFight.SNOWBALL_LAUNCHER.get())) ||
                // when snowball is on cooldown, the above condition succeeds incorrectly
                event.getEntity().getCooldowns().isOnCooldown(stack.getItem())) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Level level = event.getLevel();

        // the offset to spawn an item at
        int offsetLayers;

        if (state.is(Blocks.SNOW)) {
            int height = state.getValue(SnowLayerBlock.LAYERS);
            if (height > 1) {
                // decrease snow height
                level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, --height), 3);
                offsetLayers = height;
            } else {
                // remove snow block
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                offsetLayers = 0;
            }
        } else if (state.is(Blocks.SNOW_BLOCK) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
            // replace with snow layers
            level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), 3);
            offsetLayers = 7;
        } else {
            return;
        }

        // cancelling the events with a SUCCESS result acts like
        // returning SUCCESS from the use method of SnowballItem
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        level.playSound(event.getEntity(), pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.8F + level.random.nextFloat() * 0.4F, 0.8F);

        Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + offsetLayers * 0.125, pos.getZ() + 0.5);
        if (!level.isClientSide) {
            if (stack.is(SnowballFight.SNOWBALL_LAUNCHER.get()) &&
                    SnowballLauncherItem.getAmmo(stack) < Config.getLauncherAmmoSize() &&
                    (SnowballLauncherItem.getAmmoType(stack) == Items.SNOWBALL || SnowballLauncherItem.getAmmo(stack) == 0)) {
                SnowballLauncherItem.setAmmo(stack, SnowballLauncherItem.getAmmo(stack) + 1);
                SnowballLauncherItem.setAmmoType(stack, Items.SNOWBALL);
            } else {
                EntityType.ITEM.spawn((ServerLevel) level, null, entity -> {
                    entity.setItem(new ItemStack(Items.SNOWBALL));
                    entity.setPickUpDelay(7);
                    // spawn at the surface of the snow
                    entity.setPos(spawnPos);
                    // random motion offsets
                    entity.setDeltaMovement(
                            level.random.nextFloat() * 0.1F - 0.05F,
                            0.3F + level.random.nextFloat() * 0.1F,
                            level.random.nextFloat() * 0.1F - 0.05F);
                }, pos, MobSpawnType.EVENT, false, false);
            }
        } else {
            ParticleOptions particle = ParticleTypes.ITEM_SNOWBALL;//new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW.defaultBlockState());
            for (int i = 0; i < 20; i++) {
                float angle = level.random.nextFloat() * Mth.TWO_PI;
                level.addParticle(particle, spawnPos.x() + 0.2F * Mth.cos(angle), spawnPos.y(), spawnPos.z() + 0.2F * Mth.sin(angle), 0, 0, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUsed(final PlayerInteractEvent.RightClickItem event) {
        // add 10 tick cooldown to snowballs and tweak throwing logic
        if (event.getItemStack().getItem() instanceof SnowballItem) {
            Level level = event.getEntity().level();
            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            // using 10 ticks for cooldown makes it easy to combo mobs
            if (!player.getAbilities().instabuild)
                player.getCooldowns().addCooldown(stack.getItem(), Config.SERVER.snowballCooldown.get());

            Vec3 angle = player.getLookAngle();

            if (!level.isClientSide) {
                Snowball entity = new Snowball(level, player);
                entity.setItem(stack);

                // the default shooting behavior makes vertical velocity influence the snowball's velocity.
                // using shoot() instead of shootFromRotation() removes this behavior to make
                // throwing snowballs while jumping or falling easier to aim
                entity.shoot(angle.x(), angle.y(), angle.z(), 1.4F, 1.0F);

                level.addFreshEntity(entity);
            }

            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget().isAlive() &&
                event.getItemStack().getItem() instanceof SnowballLauncherItem &&
                (BehaviorChanges.isSnowballFighter(event.getTarget()) || event.getTarget().getType() == EntityType.SNOW_GOLEM) &&
                !SnowballLauncherItem.isHolding((LivingEntity) event.getTarget())) {
            Mob mob = (Mob) event.getTarget();
            // drop current held item
            mob.spawnAtLocation(mob.getItemBySlot(EquipmentSlot.MAINHAND));
            // set snow golem's item from player's hand
            mob.setItemSlot(EquipmentSlot.MAINHAND, event.getItemStack());
            // make sure mob drops the player's item when it dies
            mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            // remove item from player's hand
            event.getEntity().setItemInHand(event.getHand(), ItemStack.EMPTY);
        }
    }
}
