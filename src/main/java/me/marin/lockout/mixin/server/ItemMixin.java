package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "use", at = @At("HEAD"))
    public void onUseCompass(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.getEntityWorld().isClient()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ItemStack stack = player.getStackInHand(hand);

        if (stack == null || stack.isEmpty()) return;
        if (stack.getItem() != Items.COMPASS) return;

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null && customData.copyNbt().contains("PlayerTracker")) {
            LockoutServer.compassHandler.cycle(player);
        }
    }

}
