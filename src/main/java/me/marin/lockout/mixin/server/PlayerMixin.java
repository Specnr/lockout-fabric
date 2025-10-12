package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.HaveShieldDisabledGoal;
import me.marin.lockout.lockout.goals.misc.Sprint1KmGoal;
import me.marin.lockout.lockout.goals.misc.Take200DamageGoal;
import me.marin.lockout.lockout.goals.opponent.*;
import me.marin.lockout.lockout.interfaces.IncrementStatGoal;
import me.marin.lockout.lockout.interfaces.ReachXPLevelGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin {

    @Inject(method = "collideWithEntity", at = @At("HEAD"))
    public void onCollide(Entity entity, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof OpponentHitBySnowballGoal) {
                if (entity instanceof SnowballEntity snowballEntity) {
                    if (snowballEntity.getOwner() instanceof PlayerEntity shooter && !Objects.equals(player, shooter)) {
                        lockout.complete1v1Goal(goal, shooter, true, shooter.getName().getString() + " hit " + player.getName().getString() + " with a Snowball.");
                    }
                }
            }
            if (goal instanceof OpponentHitByEggGoal) {
                if (entity instanceof EggEntity snowballEntity) {
                    if (snowballEntity.getOwner() instanceof PlayerEntity shooter && !Objects.equals(player, shooter)) {
                        lockout.complete1v1Goal(goal, shooter, true, shooter.getName().getString() + " hit " + player.getName().getString() + " with an Egg.");
                    }
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void onStartMatch(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (world.isClient()) return;
        if (!lockout.hasStarted()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    public void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!cir.getReturnValue()) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        if (!lockout.isLockoutPlayer(player.getUuid())) return;
        LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());

        lockout.damageTaken.putIfAbsent(team, 0d);
        lockout.damageTaken.merge(team, (double)amount, Double::sum);

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof Take200DamageGoal take200DamageGoal) {
                team.sendTooltipUpdate(take200DamageGoal);
                if (lockout.damageTaken.get(team) >= 200) {
                    lockout.completeGoal(goal, team);
                }
            }
            if (goal instanceof OpponentTakesFallDamageGoal) {
                if (source.isOf(DamageTypes.FALL)) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " took fall damage.");
                }
            }
            if (goal instanceof OpponentTakes100DamageGoal) {
                if (lockout.damageTaken.get(team) >= 100) {
                    lockout.complete1v1Goal(goal, team, false, team.getDisplayName() + " took 100 damage.");
                }
            }
        }
    }

    @Inject(method = "incrementStat(Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    public void onIncrementStat(Identifier stat, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof IncrementStatGoal incrementStatGoal && incrementStatGoal.getStats().contains(stat)) {
                lockout.completeGoal(goal, player);
            }
            if (goal instanceof OpponentEatsFoodGoal && stat.equals(Stats.EAT_CAKE_SLICE)) {
                lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " ate food.");
            }
        }
    }

    @Inject(method = "increaseStat(Lnet/minecraft/util/Identifier;I)V", at = @At("HEAD"))
    public void onIncreaseStat(Identifier stat, int amount, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;
            if (goal instanceof Sprint1KmGoal && stat.equals(Stats.SPRINT_ONE_CM)) {
                lockout.distanceSprinted.putIfAbsent(player.getUuid(), 0);
                lockout.distanceSprinted.merge(player.getUuid(), amount, Integer::sum);

                if (lockout.distanceSprinted.get(player.getUuid()) >= (100 * 1000)) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

    @Inject(method = "addExperienceLevels", at = @At("TAIL"))
    public void onExperienceLevelUp(int levels, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof ReachXPLevelGoal reachXPLevelGoal) {
                if (player.experienceLevel >= reachXPLevelGoal.getAmount()) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

    @Inject(method = "takeShieldHit", at = @At(value = "TAIL"))
    public void onTakeShieldHit(ServerWorld world, LivingEntity attacker, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        float f = attacker.getWeaponDisableBlockingForSeconds();

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;
            if (f <= 0.0F) continue;

            if (goal instanceof HaveShieldDisabledGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }


}
