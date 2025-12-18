package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface OpponentGoal extends HasTooltipInfo {
    @Override
    default List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        if (((Goal) this).isCompleted()) return List.of();
        List<String> tooltip = new ArrayList<>();
        Lockout lockout = LockoutServer.lockout;
        Set<LockoutTeam> metCondition = lockout.opponentGoalProgress.getOrDefault(this, Set.of());

        List<? extends LockoutTeam> notMet = lockout.getTeams().stream()
                .filter(t -> !metCondition.contains(t))
                .map(t -> (LockoutTeam) t)
                .toList();

        if (!notMet.isEmpty()) {
            tooltip.add("Have not met the condition:");
            tooltip.addAll(HasTooltipInfo.commaSeparatedList(notMet.stream().map(t -> t.getColor() + t.getDisplayName() + Formatting.RESET).toList()));
        }

        return tooltip;
    }

    @Override
    default List<String> getSpectatorTooltip() {
        if (((Goal) this).isCompleted()) return List.of();
        List<String> tooltip = new ArrayList<>();
        Lockout lockout = LockoutServer.lockout;
        Set<LockoutTeam> metCondition = lockout.opponentGoalProgress.getOrDefault(this, Set.of());

        tooltip.add(" ");
        tooltip.add("Teams that HAVE NOT met the condition:");
        List<LockoutTeam> notMet = lockout.getTeams().stream()
                .filter(t -> !metCondition.contains(t))
                .map(t -> (LockoutTeam) t)
                .toList();

        if (notMet.isEmpty()) {
            tooltip.add(Formatting.GRAY + " " + Formatting.ITALIC + "None");
        } else {
            tooltip.addAll(HasTooltipInfo.commaSeparatedList(notMet.stream().map(t -> t.getColor() + t.getDisplayName() + Formatting.RESET).toList()));
        }

        tooltip.add("Teams that HAVE met the condition:");
        List<LockoutTeam> met = lockout.getTeams().stream()
                .filter(metCondition::contains)
                .map(t -> (LockoutTeam) t)
                .toList();
        if (met.isEmpty()) {
            tooltip.add(Formatting.GRAY + " " + Formatting.ITALIC + "None");
        } else {
            tooltip.addAll(HasTooltipInfo.commaSeparatedList(met.stream().map(t -> t.getColor() + t.getDisplayName() + Formatting.RESET).toList()));
        }
        tooltip.add(" ");

        return tooltip;
    }
}
