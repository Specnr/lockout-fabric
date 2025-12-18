package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public interface MostStatGoal extends HasTooltipInfo {
    int getStat(LockoutTeam team);

    @Override
    default List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        Lockout lockout = LockoutServer.lockout;

        tooltip.add(" ");
        tooltip.add("Your Team's Best: " + getStat(team));
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    default List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();
        Lockout lockout = LockoutServer.lockout;

        tooltip.add(" ");
        for (LockoutTeam t : lockout.getTeams()) {
            tooltip.add(t.getColor() + t.getDisplayName() + Formatting.RESET + ": " + getStat(t));
        }
        tooltip.add(" ");

        return tooltip;
    }
}
