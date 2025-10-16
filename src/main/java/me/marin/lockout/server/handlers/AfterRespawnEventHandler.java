package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutConfig;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.server.LockoutServer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import static me.marin.lockout.server.LockoutServer.compassHandler;
import static me.marin.lockout.server.LockoutServer.lockout;

public class AfterRespawnEventHandler implements ServerPlayerEvents.AfterRespawn {

    @Override
    public void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (lockout.isSoloBlackout()) return;
        if (!lockout.isLockoutPlayer(newPlayer.getUuid())) return;
        if (alive) return; // end exit portal
        if (!LockoutConfig.getInstance().giveCompasses) return;

        int slot = compassHandler.compassSlots.getOrDefault(newPlayer.getUuid(), 0);
        if (slot == 40) {
            newPlayer.getInventory().setStack(40, compassHandler.newCompass());
        }
        if (slot >= 0 && slot <= 35) {
            newPlayer.getInventory().setStack(slot, compassHandler.newCompass());
        }
        
        // Re-apply waypoint color after respawn
        LockoutTeam playerTeam = lockout.getPlayerTeam(newPlayer.getUuid());
        if (playerTeam != null) {
            // Find player index within their team
            int playerIndex = playerTeam.getPlayerNames().indexOf(newPlayer.getName().getString());
            LockoutServer.updatePlayerWaypointColor(newPlayer, playerTeam.getColor(), playerIndex);
        }
    }
}
