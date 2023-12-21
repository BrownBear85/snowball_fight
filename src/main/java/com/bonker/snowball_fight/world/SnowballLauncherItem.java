package com.bonker.snowball_fight.world;

import com.bonker.snowball_fight.Config;
import com.bonker.snowball_fight.SnowballFight;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SnowballLauncherItem extends Item {
    private static final String AMMO_TAG = "Ammo";
    private static final String ITEM_TAG = "AmmoType";
    private static final int STANDARD_COLOR = 0xd6fff8;
    private static final int ICY_COLOR = 0x85adf8;
    private static final Style TOOLTIP_STYLE = Style.EMPTY.withColor(STANDARD_COLOR);

    public SnowballLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Config.getLauncherLoadTime() * Config.getLauncherAmmoSize() + 5;
    }

    // start loading or shoot
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // set ammo type tag initially
        if (!stack.hasTag() || stack.getOrCreateTag().getString(ITEM_TAG).equals("")) setAmmoType(stack, null);

        if (!stack.is(this)) return InteractionResultHolder.pass(stack);

        // shoot
        int ammo = getAmmo(stack);
        if (ammo > 0 && !(player.isCrouching() && ammo < Config.getLauncherAmmoSize())) {
            Vec3 angle = player.getLookAngle();
            Snowball entity = new Snowball(level, player);
            Item ammoItem = getAmmoType(stack);
            if (ammoItem != null && ammoItem != Items.SNOWBALL) entity.setItem(new ItemStack(ammoItem));
            entity.shoot(angle.x(), angle.y(), angle.z(), 2.4F, 0.8F);
            level.addFreshEntity(entity);

            if (level.isClientSide)
                level.playLocalSound(player.getX(), player.getY() + 1, player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.2F, 1.0F + level.random.nextFloat() * 0.4F, false);

            if (!player.getAbilities().instabuild)
                player.getCooldowns().addCooldown(this, Config.getLauncherCooldown());

            setAmmo(stack, ammo - 1);

            if (ammo == 1) setAmmoType(stack, null);
        } else {
            // start using
            SoundEvent sound;
            if (player.getAbilities().instabuild || !getFirstAmmoStack(player, getAmmoType(stack)).isEmpty()) {
                player.startUsingItem(hand);
                sound = SoundEvents.CROSSBOW_LOADING_START;
            } else {
                sound = SoundEvents.POWDER_SNOW_HIT;
            }
            if (level.isClientSide)
                level.playLocalSound(player.getX(), player.getY() + 1, player.getZ(), sound, SoundSource.PLAYERS, 0.5F, 1.0F, false);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!level.isClientSide && entity instanceof Player player) {
            // tick doesn't get called on last tick, so offset ticks by 1 tick to get everything to happen earlier
            ticksRemaining--;
            if (ticksRemaining < Config.getLauncherLoadTime() * Config.getLauncherAmmoSize() && ticksRemaining % Config.getLauncherLoadTime() == 0) {
                // load
                ItemStack ammoStack = getFirstAmmoStack(player, getAmmoType(stack));
                if (getAmmo(stack) < Config.getLauncherAmmoSize() && (player.getAbilities().instabuild || !ammoStack.isEmpty())) {

                    int ammo = getAmmo(stack) + 1;
                    setAmmo(stack, ammo);

                    if (ammo == 1) {
                        setAmmoType(stack, ammoStack.isEmpty() && player.getAbilities().instabuild ? Items.SNOWBALL : ammoStack.getItem());
                    }

                    if (!player.getAbilities().instabuild)
                        ammoStack.shrink(1);

                    level.playSound(null, player.getX(), player.getY() + 1, player.getZ(), SoundEvents.SNOW_HIT, SoundSource.PLAYERS, 1.0F, 0.2F + level.random.nextFloat() * 0.4F);
                }
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getAmmo(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getAmmo(stack) * 13F / Config.getLauncherAmmoSize());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (getAmmoType(stack) == SnowballFight.ICY_SNOWBALL.get()) return ICY_COLOR;
        return STANDARD_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Item ammoType = getAmmoType(stack);
        if (ammoType == null)
            tooltip.add(Component.translatable("item.snowball_fight.snowball_launcher.empty").withStyle(TOOLTIP_STYLE));
        else
            tooltip.add(Component.translatable("item.snowball_fight.snowball_launcher.ammo", ammoType.getName(ItemStack.EMPTY), getAmmo(stack), Config.getLauncherAmmoSize()).withStyle(TOOLTIP_STYLE));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    // don't automatically shoot after fully loading
    @Override
    public boolean useOnRelease(ItemStack stack) {
        return stack.is(this);
    }

    public static int getAmmo(ItemStack stack) {
        return stack.getOrCreateTag().getInt(AMMO_TAG);
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        if (stack.is(SnowballFight.SNOWBALL_LAUNCHER.get())) stack.getOrCreateTag().putInt(AMMO_TAG, ammo);
    }

    public static @Nullable Item getAmmoType(ItemStack stack) {
        String type = stack.getOrCreateTag().getString(ITEM_TAG);
        if (type.equals("empty")) return null;
        Item ammoItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(type));
        return ammoItem == Items.AIR ? null : ammoItem;
    }

    public static void setAmmoType(ItemStack stack, @Nullable Item type) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(type);
        stack.getOrCreateTag().putString(ITEM_TAG, key == ForgeRegistries.ITEMS.getDefaultKey() ? "empty" : String.valueOf(key));
    }

    public static ItemStack getFirstAmmoStack(Player player, @Nullable Item type) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack inventoryStack = player.getInventory().getItem(slot);
            if (!inventoryStack.isEmpty() &&
                    (type == null && inventoryStack.is(SnowballFight.SNOWBALL_LAUNCHER_AMMO)) ||
                    (type != null && inventoryStack.is(type))) {
                return inventoryStack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isHolding(LivingEntity entity) {
        return entity.isHolding(stack -> stack.getItem() instanceof SnowballLauncherItem);
    }
}
