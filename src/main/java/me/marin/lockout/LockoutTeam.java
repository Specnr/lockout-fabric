package me.marin.lockout;

import lombok.Getter;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LockoutTeam {

    private final List<String> players;
    private final List<UUID> playerIds;
    @Getter
    private final Formatting color;
    @Getter
    private int points = 0;
    @Getter
    private boolean forfeited = false;

    public LockoutTeam(List<String> playerNames, Formatting formattingColor) {
        this(playerNames, new ArrayList<>(), formattingColor);
    }

    public LockoutTeam(List<String> playerNames, List<UUID> playerIds, Formatting formattingColor) {
        this.players = playerNames;
        this.playerIds = playerIds;
        this.color = formattingColor;
    }

    public List<String> getPlayerNames() {
        return players;
    }

    public List<UUID> getPlayerIds() {
        return playerIds;
    }

    public boolean containsPlayer(UUID uuid) {
        return playerIds.contains(uuid);
    }

    public String getDisplayName() {
        String name = players.size() == 1 ? players.get(0) : "Team " + formattingToString(color);
        return forfeited ? name + " (Forfeited)" : name;
    }

    public void setForfeited(boolean forfeited) {
        this.forfeited = forfeited;
    }

    public void addPoint() {
        this.points++;
    }
    public void takePoint() {
        this.points--;
    }

    public static String formattingToString(Formatting formatting) {
        return StringUtils.capitalize(formatting.asString().replace("_", " "));
    }

}
