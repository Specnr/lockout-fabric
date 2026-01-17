package me.marin.lockout;

import lombok.Getter;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.network.UpdateTooltipPayload;
import me.marin.lockout.server.LockoutServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class LockoutTeamServer extends LockoutTeam {

    private final Map<UUID, String> playerNameMap = new HashMap<>();
    @Getter
    private final MinecraftServer server;

    public LockoutTeamServer(List<String> playerNames, Formatting formattingColor, MinecraftServer server) {
        super(playerNames, new ArrayList<>(), formattingColor);
        this.server = server;

        PlayerManager manager = server.getPlayerManager();

        // All players from playerNames are online at this moment.
        for (String playerName : playerNames) {
            UUID uuid = manager.getPlayer(playerName).getUuid();
            this.getPlayerIds().add(uuid);
            this.playerNameMap.put(uuid, playerName);
        }
    }

    public String getPlayerName(UUID uuid) {
        return playerNameMap.get(uuid);
    }

    public void sendMessage(String message) {
        for (UUID uuid : getPlayerIds()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Text.literal(message));
            }
        }
    }

    public void sendTooltipUpdate(Goal goal) {
        sendTooltipUpdate(goal, true);
    }
    public void sendTooltipUpdate(Goal goal, boolean updateSpectators) {
        if (!(goal instanceof HasTooltipInfo tooltipGoal)) return;
        for (UUID playerId : getPlayerIds()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            var payload = new UpdateTooltipPayload(goal.getId(), String.join("\n", tooltipGoal.getTooltip(this, player)));
            if (player != null) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        if (updateSpectators) {
            this.sendTooltipPacketSpectators(goal);
        }
    }
    private void sendTooltipPacketSpectators(Goal goal) {
        if (!(goal instanceof HasTooltipInfo tooltipGoal)) return;
        var payload = new UpdateTooltipPayload(goal.getId(), String.join("\n", tooltipGoal.getSpectatorTooltip()));
        for (ServerPlayerEntity spectator : Utility.getSpectators(LockoutServer.lockout, server)) {
            ServerPlayNetworking.send(spectator, payload);
        }
    }

}
