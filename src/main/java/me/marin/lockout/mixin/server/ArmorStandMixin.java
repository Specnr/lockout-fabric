package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillArmorStandGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(ArmorStandEntity.class)
public class ArmorStandMixin {

    @Inject(method = "interactAt", at = @At("RETURN"))
    public void onInteractAt(PlayerEntity player, Vec3d hitPos, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.getEntityWorld().isClient()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        ArmorStandEntity armorStand = (ArmorStandEntity) (Object) this;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (!(goal instanceof FillArmorStandGoal fillArmorStandGoal)) continue;
            if (goal.isCompleted()) continue;

            // TODO: Do better
            var armor = new ArrayList<ItemStack>();
            armor.add(armorStand.getEquippedStack(EquipmentSlot.HEAD));
            armor.add(armorStand.getEquippedStack(EquipmentSlot.CHEST));
            armor.add(armorStand.getEquippedStack(EquipmentSlot.LEGS));
            armor.add(armorStand.getEquippedStack(EquipmentSlot.FEET));

            if (serverPlayer.interactionManager.getGameMode() != GameMode.SPECTATOR && cir.getReturnValue() == ActionResult.SUCCESS_SERVER) {
                for (ItemStack armorItem : armor) {
                    if (armorItem == null || armorItem.isEmpty()) return;
                }
                // Armor stand is now full
                lockout.completeGoal(fillArmorStandGoal, player);
                return;
            }
        }


    }

}
