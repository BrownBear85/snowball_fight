package com.bonker.snowball_fight.behavior;

import com.bonker.snowball_fight.Config;
import com.bonker.snowball_fight.SnowballFight;
import com.google.common.collect.ImmutableMap;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mod.EventBusSubscriber(modid = SnowballFight.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BehaviorChanges {
    private static Map<EntityType<?>, Class<? extends Goal>> GOAL_MAP;
    private static Map<EntityType<?>, Set<Class<? extends Goal>>> EXTRA_GOALS_MAP;

    public static void init() {
        // define which entity types can fight with snowballs
        GOAL_MAP = ImmutableMap.<EntityType<?>, Class<? extends Goal>>builder()
                .put(EntityType.ZOMBIE, ZombieAttackGoal.class)
                .put(EntityType.ZOMBIE_VILLAGER, ZombieAttackGoal.class)
                .put(EntityType.HUSK, ZombieAttackGoal.class)
                .put(EntityType.SKELETON, RangedBowAttackGoal.class)
                .put(EntityType.STRAY, RangedBowAttackGoal.class)
                .put(EntityType.SPIDER, MeleeAttackGoal.class)
                .put(EntityType.ENDERMAN, MeleeAttackGoal.class)
                .put(EntityType.VINDICATOR, MeleeAttackGoal.class)
                .put(EntityType.PILLAGER, RangedCrossbowAttackGoal.class)
                .build();

        // define which goals interfere with snowball combat to be removed
        EXTRA_GOALS_MAP = ImmutableMap.<EntityType<?>, Set<Class<? extends Goal>>>builder()
                .put(EntityType.SPIDER, Set.of(LeapAtTargetGoal.class))
                .build();
    }

    public static boolean shouldReplaceAttackGoal(Mob mob) {
        return GOAL_MAP.containsKey(mob.getType()) && !(mob.getType().is(EntityTypeTags.RAIDERS) && !Config.SERVER.raidersUseSnowballs.get());
    }

    public static Optional<WrappedGoal> getGoalByClass(Mob mob, Class<? extends Goal> goalClass) {
        return mob.goalSelector.getAvailableGoals().stream()
                .filter(wrappedGoal -> goalClass.isInstance(wrappedGoal.getGoal()))
                .findFirst();
    }

    public static void replaceGoal(Mob mob, Class<? extends Goal> goalClass, Goal replaceWith) {
        // get old goal before it is removed
        Optional<WrappedGoal> oldGoal = getGoalByClass(mob, goalClass);
        // remove goal before new one is added in case the new goal extends the old goal
        mob.removeAllGoals(goalClass::isInstance);
        mob.goalSelector.addGoal(oldGoal
                        .map(WrappedGoal::getPriority)
                        .orElse(1),
                replaceWith);
    }

    public static void setAttackGoal(Mob mob, Goal replaceWith) {
        replaceGoal(mob, GOAL_MAP.get(mob.getType()), replaceWith);
    }

    public static boolean isSnowballFighter(Entity entity) {
        return entity.getTags().contains("snowball_fighter");
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && shouldReplaceAttackGoal(mob)) {
            // loaded from disk and has tag
            if ((event.loadedFromDisk() && isSnowballFighter(mob)) ||
                    // spawned for the first time
                    (!event.loadedFromDisk() &&
                            // on snowy block
                            (mob.getBlockStateOn().is(SnowballFight.SNOWBALL_FIGHTER_SPAWN_BLOCKS) ||
                                    // in snowy block
                                    mob.getFeetBlockState().is(SnowballFight.SNOWBALL_FIGHTER_SPAWN_BLOCKS) ||
                                    // in snowy biome
                                    Config.snowballAttacksInBiome(event.getLevel(), event.getEntity())))) {
                // replace attack goal with snowball attacks
                setAttackGoal(mob, new SnowballAttackGoal(mob, 1.0, 30, 19));

                // add tag
                mob.addTag("snowball_fighter");

                // prevent mob from picking up other items
                mob.setCanPickUpLoot(false);

                // set item if this entity is being spawned
                if (!event.loadedFromDisk()) {
                    Item item;

                    if (mob.getType().is(SnowballFight.ALWAYS_SPAWNS_WITH_LAUNCHER)) {
                        item = SnowballFight.SNOWBALL_LAUNCHER.get();
                    } else if (mob.getType().is(SnowballFight.ALWAYS_SPAWNS_WITH_ICY_SNOWBALL)) {
                        item = SnowballFight.ICY_SNOWBALL.get();
                    } else {
                        // special item chance depends on difficulty
                        float random = event.getLevel().random.nextFloat();
                        float chance = switch (event.getLevel().getDifficulty()) {
                            default -> 0.0F;
                            case NORMAL -> 0.1F;
                            case HARD -> 0.2F;
                        };

                        // small chance for launcher
                        if (random < chance * 0.15) item = SnowballFight.SNOWBALL_LAUNCHER.get();
                            // twice as likely for icy snowball
                        else if (random < chance) item = SnowballFight.ICY_SNOWBALL.get();
                            // default is snowball
                        else item = Items.SNOWBALL;
                    }

                    mob.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                }
            }

            // remove extra goals that mess with snowball combat
            if (EXTRA_GOALS_MAP.containsKey(mob.getType()))
                mob.removeAllGoals(goal -> EXTRA_GOALS_MAP.get(mob.getType()).contains(goal.getClass()));
        } else if (event.getEntity() instanceof SnowGolem snowGolem) {
            // replace snow golem goal so that they can use launchers
            replaceGoal(snowGolem, RangedAttackGoal.class, new SnowballAttackGoal(snowGolem, 0, 20, 20));
        }
    }
}
