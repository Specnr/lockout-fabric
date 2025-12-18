package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.Deal400DamageGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import me.marin.lockout.lockout.goals.misc.BreakToolGoal;
import net.minecraft.component.DataComponentTypes;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "damage", at = @At("RETURN"))
    public void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!(source.getAttacker() instanceof PlayerEntity player) || !cir.getReturnValue()) return;
        if (player.getEntityWorld().isClient()) return;

        if (!lockout.isLockoutPlayer(player.getUuid())) return;
        LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
        lockout.damageDealt.putIfAbsent(team, 0d);
        lockout.damageDealt.merge(team, (double)amount, Double::sum);

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof Deal400DamageGoal deal400DamageGoal) {
                team.sendTooltipUpdate(deal400DamageGoal);
                if (lockout.damageDealt.get(team) >= 400) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

    @Inject(method = "sendEquipmentBreakStatus", at = @At("HEAD"))
    public void onEquipmentBreak(Item item, EquipmentSlot slot, CallbackInfo ci) {
        if (!((Object)this instanceof PlayerEntity player)) return;
        if (player.getEntityWorld().isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ItemStack stack = item.getDefaultStack();

        // Check if it's a tool (has TOOL component or is damageable)
        if (stack.contains(DataComponentTypes.TOOL) || stack.getMaxDamage() > 0) {
            for (Goal goal : lockout.getBoard().getGoals()) {
                if (goal == null) continue;
                if (goal.isCompleted()) continue;

                if (goal instanceof BreakToolGoal) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

}
