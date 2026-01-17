package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Boat2KmGoal extends Goal implements CustomTextureRenderer, HasTooltipInfo {

    private static final ItemStack ITEM_STACK = Items.OAK_BOAT.getDefaultStack();
    public Boat2KmGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Boat 2km";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawItem(ITEM_STACK, x, y);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer,  ITEM_STACK, x, y, "2km");
        return true;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        int maxDistance = 0;
        for (UUID playerId : ((LockoutTeamServer) team).getPlayerIds()) {
            maxDistance = Math.max(maxDistance, LockoutServer.lockout.distanceBoated.getOrDefault(playerId, 0));
        }

        tooltip.add(" ");
        tooltip.add("Distance: " + Math.min(2000, maxDistance / 100) + "/2000m");
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            int maxDistance = 0;
            for (UUID playerId : ((LockoutTeamServer) team).getPlayerIds()) {
                maxDistance = Math.max(maxDistance, LockoutServer.lockout.distanceBoated.getOrDefault(playerId, 0));
            }
            tooltip.add(team.getColor() + team.getDisplayName() + Formatting.RESET + ": " + Math.min(2000, maxDistance / 100) + "/2000m");
        }
        tooltip.add(" ");

        return tooltip;
    }
}
