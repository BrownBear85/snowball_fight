package com.bonker.snowball_fight.behavior;

import com.bonker.snowball_fight.world.SnowballLauncherItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.SnowballItem;

import java.util.EnumSet;

public class SnowballAttackGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private final int attackInterval;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public SnowballAttackGoal(Mob mob, double speedModifier, int attackInterval, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackInterval = attackInterval;
        this.attackRadiusSqr = attackRadius * attackRadius;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        return mob.getTarget() != null && mob.getTarget().isAlive() && !mob.getTarget().isInvulnerable() && isHoldingSnowball();
    }

    protected boolean isHoldingSnowball() {
        return mob.isHolding(stack -> stack.getItem() instanceof SnowballItem || stack.getItem() instanceof SnowballLauncherItem);
    }

    public boolean canContinueToUse() {
        return (canUse() || !mob.getNavigation().isDone()) && isHoldingSnowball();
    }

    public void start() {
        super.start();
        mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        mob.setAggressive(false);
        seeTime = 0;
        attackTime = -1;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            boolean launcher = SnowballLauncherItem.isHolding(mob);

            double distanceSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean hasLineOfSight = mob.getSensing().hasLineOfSight(target);
            boolean seenTarget = seeTime > 0;
            if (hasLineOfSight != seenTarget) {
                seeTime = 0;
            }

            if (hasLineOfSight) {
                ++seeTime;
            } else {
                --seeTime;
            }

            // has seen target for 1 second
            if (!(distanceSqr > attackRadiusSqr) && seeTime >= 20) {
                // tick strafing
                mob.getNavigation().stop();
                ++strafingTime;
            } else {
                // move towards target
                mob.getNavigation().moveTo(target, speedModifier);
                strafingTime = -1;
            }

            // if strafed for 1 second since
            if (strafingTime >= 20) {
                // 30% chance to switch rotation directions
                if (mob.getRandom().nextFloat() < 0.3D) {
                    strafingClockwise = !strafingClockwise;
                }
                // 30% chance to switch directions
                if (mob.getRandom().nextFloat() < 0.3D) {
                    strafingBackwards = !strafingBackwards;
                }
                // reset strafe time
                strafingTime = 0;
            }

            // if currently strafing
            if (strafingTime > -1) {
                if (distanceSqr > (attackRadiusSqr * 0.75F)) {
                    // don't strafe backwards if that would move out of target range
                    strafingBackwards = false;
                } else if (distanceSqr < (attackRadiusSqr * 0.25F)) {
                    // strafe backwards if too close to target
                    strafingBackwards = true;
                }

                mob.getMoveControl().strafe(strafingBackwards ? -0.5F : 0.5F, strafingClockwise ? 0.5F : -0.5F);

                if (mob.getControlledVehicle() instanceof Mob vehicle) {
                    vehicle.lookAt(target, 30.0F, 30.0F);
                }
                mob.lookAt(target, 30.0F, 30.0F);
            } else {
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (distanceSqr < attackRadiusSqr && --attackTime <= 0 && seeTime >= -60) {
                performSnowballAttack(target);
                attackTime = launcher ? attackInterval * 3 / 4 : attackInterval;
            }
        }
    }

    public void performSnowballAttack(LivingEntity target) {
        boolean launcher = mob.isHolding(stack -> stack.getItem() instanceof SnowballLauncherItem);

        if (!launcher) mob.swing(InteractionHand.MAIN_HAND);

        Snowball snowball = new Snowball(mob.level(), mob);
        double eyeY = target.getEyeY() - 1.1F;
        double dx = target.getX() - mob.getX();
        double dy = eyeY - snowball.getY();
        double dz = target.getZ() - mob.getZ();
        double scaledDistance = Math.sqrt(dx * dx + dz * dz) * (launcher ? 0.08F : 0.2);

        float velocity = switch (target.level().getDifficulty()) {
            default -> 1.4F;
            case NORMAL -> 1.6F;
            case HARD -> 1.8F;
        };

        if (launcher) velocity *= 1.3;

        snowball.shoot(dx, dy + scaledDistance, dz, velocity, launcher ? 3 : 10);

        mob.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (mob.getRandom().nextFloat() * 0.4F + 0.8F));

        mob.level().addFreshEntity(snowball);
    }
}
