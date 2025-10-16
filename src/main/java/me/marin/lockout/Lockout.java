package me.marin.lockout;

import lombok.Getter;
import lombok.Setter;
import me.marin.lockout.client.LockoutBoard;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.network.CompleteTaskPayload;
import me.marin.lockout.network.EndLockoutPayload;
import me.marin.lockout.network.LockoutGoalsTeamsPayload;
import me.marin.lockout.network.UpdateTimerPayload;
import me.marin.lockout.server.LockoutServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.util.tuples.Pair;

import java.util.*;

public class Lockout {

    private static final Logger logger = LogManager.getLogger("Lockout");
    public static final Random random = new Random();
    public static final int[] COLOR_ORDERS = new int[]{12, 9, 10, 14, 6, 13, 11, 5, 3, 2, 15, 4, 7, 1, 8, 0};

    public final Map<LockoutTeam, LinkedHashSet<EntityType<?>>> bredAnimalTypes = new HashMap<>();
    public final Map<LockoutTeam, LinkedHashSet<EntityType<?>>> killedHostileTypes = new HashMap<>();
    public final Map<LockoutTeam, LinkedHashSet<EntityType<?>>> killedRaidMobs = new HashMap<>();
    public final Map<LockoutTeam, Integer> killedUndeadMobs = new HashMap<>();
    public final Map<LockoutTeam, Integer> killedArthropods = new HashMap<>();
    public final Map<LockoutTeam, LinkedHashSet<Item>> foodTypesEaten = new HashMap<>();
    public final Map<LockoutTeam, LinkedHashSet<Identifier>> uniqueAdvancements = new HashMap<>();
    public final Map<LockoutTeam, Double> damageTaken = new HashMap<>();
    public final Map<LockoutTeam, Double> damageDealt = new HashMap<>();
    public final Map<LockoutTeam, Integer> deaths = new HashMap<>();
    public final Map<LockoutTeam, Integer> mobsKilled = new HashMap<>();

    // Tracks which teams have met the condition for each opponent goal (for 3+ team support)
    public final Map<Goal, Set<LockoutTeam>> opponentGoalProgress = new HashMap<>();

    public final Map<UUID, Long> pumpkinWearTime = new HashMap<>();
    public final Map<UUID, Integer> distanceSprinted = new HashMap<>();
    public final Map<UUID, Set<Item>> uniqueCrafts = new HashMap<>();
    public final Map<UUID, Integer> playerKills = new HashMap<>();
    public final Map<UUID, Integer> playerAdvancements = new HashMap<>();

    public UUID mostUniqueCraftsPlayer;
    public int mostUniqueCrafts;
    public UUID mostPlayerKillsPlayer;
    public int mostPlayerKills;
    public UUID mostAdvancementsPlayer;
    public int mostAdvancements;

    @Getter
    private final LockoutBoard board;
    @Getter
    private final List<? extends LockoutTeam> teams;
    private boolean hasStarted = false;
    private boolean isRunning = true;

    /**
     * Amount of *server* ticks the game has been running for.
     * Negative values mean that the game hasn't started yet (players are looking at the board).
     * This value is incremented by 1 every server tick.
     */
    @Setter
    @Getter
    private long ticks;

    public Lockout(LockoutBoard board, List<? extends LockoutTeam> teams) {
        this.board = board;
        this.teams = teams;
    }

    public static void log(String message) {
        logger.log(Level.INFO, message);
    }

    public static void error(Throwable t) {
        logger.error("Lockout error:\n", t);
    }

    public static boolean exists(Lockout lockout) {
        return lockout != null;
    }

    public static boolean isLockoutRunning(Lockout lockout) {
        return exists(lockout) && lockout.isRunning;
    }

    public String getModeName() {
        return teams.size() > 1 ? "Lockout" : "Blackout";
    }

    public boolean isSoloBlackout() {
        return teams.size() == 1 && teams.get(0).getPlayerNames().size() == 1;
    }

    public void tick() {
        ticks++;
    }

    public void completeGoal(Goal goal, PlayerEntity player) {
        completeGoal(goal, player.getUuid());
    }
    public void completeGoal(Goal goal, UUID playerId) {
        if (goal.isCompleted()) return;
        if (!isLockoutPlayer(playerId)) return;
        if (!hasStarted()) return;

        LockoutTeamServer team = (LockoutTeamServer) getPlayerTeam(playerId);

        completeGoal(goal, team, team.getPlayerName(playerId) + " completed " + goal.getGoalName() + ".");
    }
    public void completeGoal(Goal goal, LockoutTeam team) {
        completeGoal(goal, team, team.getDisplayName() + " completed " + goal.getGoalName() + ".");
    }
    public void completeGoal(Goal goal, LockoutTeam team, String message) {
        if (goal.isCompleted()) return;
        if (!hasStarted()) return;

        team.addPoint();
        goal.setCompleted(true, team);

        for (LockoutTeam lockoutTeam : teams) {
            if (!(lockoutTeam instanceof LockoutTeamServer lockoutTeamServer)) continue;
            if (Objects.equals(lockoutTeamServer, team)) {
                lockoutTeamServer.sendMessage(Formatting.GREEN + message);
            } else {
                lockoutTeamServer.sendMessage(Formatting.RED + message);
            }
        }
        for (ServerPlayerEntity spectator : Utility.getSpectators(this, LockoutServer.server)) {
            spectator.sendMessage(Text.literal(message));
        }

        sendGoalCompletedPacket(goal, team);
        evaluateWinnerAndEndGame(team);
    }

    public void complete1v1Goal(Goal goal, PlayerEntity player, boolean isWinner, String message) {
        complete1v1Goal(goal, player.getUuid(), isWinner, message);
    }
    public void complete1v1Goal(Goal goal, UUID playerId, boolean isWinner, String message) {
        if (goal.isCompleted()) return;
        if (!isLockoutPlayer(playerId)) return;
        if (!hasStarted()) return;

        LockoutTeamServer team = (LockoutTeamServer) getPlayerTeam(playerId);

        complete1v1Goal(goal, team, isWinner, message);
    }
    public void complete1v1Goal(Goal goal, LockoutTeam team, boolean isWinner, String message) {
        if (goal.isCompleted()) return;
        if (!hasStarted()) return;

        LockoutTeamServer opponentTeam = (LockoutTeamServer) getOpponentTeam(team);

        LockoutTeamServer winnerTeam = isWinner ? (LockoutTeamServer) team : opponentTeam;
        LockoutTeamServer loserTeam  = isWinner ? opponentTeam : (LockoutTeamServer) team;

        goal.setCompleted(true, winnerTeam);
        winnerTeam.addPoint();

        winnerTeam.sendMessage(Formatting.GREEN + message);
        loserTeam.sendMessage(Formatting.RED + message);
        for (ServerPlayerEntity spectator : Utility.getSpectators(this, LockoutServer.server)) {
            spectator.sendMessage(Text.literal(message));
        }

        sendGoalCompletedPacket(goal, winnerTeam);
        evaluateWinnerAndEndGame(winnerTeam);
    }

    /**
     * Completes an opponent goal when all other teams have met the condition.
     * Supports 2+ teams. For 2-team games, behaves the same as complete1v1Goal.
     *
     * @param goal The opponent goal to track
     * @param teamThatMetCondition The team that just met the goal condition
     * @param message The message to display when the goal is completed
     */
    public void completeMultiOpponentGoal(Goal goal, LockoutTeam teamThatMetCondition, String message) {
        if (goal.isCompleted()) return;
        if (!hasStarted()) return;

        // Track that this team has met the condition
        opponentGoalProgress.putIfAbsent(goal, new HashSet<>());
        opponentGoalProgress.get(goal).add(teamThatMetCondition);

        // Check if all other teams (excluding the potential winner) have met the condition
        int teamsSize = teams.size();
        int teamsThatMetCondition = opponentGoalProgress.get(goal).size();

        // If all opponents have met the condition, complete the goal for the remaining team(s)
        if (teamsThatMetCondition >= teamsSize - 1) {
            // Find the team(s) that haven't met the condition - they win
            List<LockoutTeamServer> winningTeams = new ArrayList<>();
            for (LockoutTeam team : teams) {
                if (!opponentGoalProgress.get(goal).contains(team)) {
                    winningTeams.add((LockoutTeamServer) team);
                }
            }

            // Should be exactly one winning team (or multiple in a tie scenario)
            if (winningTeams.isEmpty()) {
                // Edge case: all teams met condition simultaneously, shouldn't happen normally
                return;
            }

            // Complete the goal for the first winning team (in case of multiple, pick one)
            LockoutTeamServer winnerTeam = winningTeams.get(0);
            goal.setCompleted(true, winnerTeam);
            winnerTeam.addPoint();

            // Create a clear completion message showing the winner
            String completionMessage;
            if (teams.size() == 2) {
                // For 2 teams, use the trigger message as-is for backwards compatibility
                completionMessage = message;
            } else {
                // For 3+ teams, make it clear who won
                completionMessage = winnerTeam.getDisplayName() + " completed the goal! (" + message + ")";
            }

            // Send messages to all teams
            for (LockoutTeam lockoutTeam : teams) {
                if (!(lockoutTeam instanceof LockoutTeamServer lockoutTeamServer)) continue;
                if (winningTeams.contains(lockoutTeamServer)) {
                    lockoutTeamServer.sendMessage(Formatting.GREEN + completionMessage);
                } else {
                    lockoutTeamServer.sendMessage(Formatting.RED + completionMessage);
                }
            }
            for (ServerPlayerEntity spectator : Utility.getSpectators(this, LockoutServer.server)) {
                spectator.sendMessage(Text.literal(completionMessage));
            }

            sendGoalCompletedPacket(goal, winnerTeam);
            evaluateWinnerAndEndGame(winnerTeam);
        }
    }

    /**
     * Convenience method for completeMultiOpponentGoal that accepts a PlayerEntity.
     */
    public void completeMultiOpponentGoal(Goal goal, PlayerEntity player, String message) {
        completeMultiOpponentGoal(goal, getPlayerTeam(player.getUuid()), message);
    }

    /**
     * Convenience method for completeMultiOpponentGoal that accepts a UUID.
     */
    public void completeMultiOpponentGoal(Goal goal, UUID playerId, String message) {
        if (!isLockoutPlayer(playerId)) return;
        completeMultiOpponentGoal(goal, getPlayerTeam(playerId), message);
    }

    public void updateGoalCompletion(Goal goal, UUID playerId) {
        if (goal.isCompleted()) {
            clearGoalCompletion(goal, false);
        }
        completeGoal(goal, playerId);
    }

    public void clearGoalCompletion(Goal goal, boolean sendPacket) {
        if (!goal.isCompleted()) return;

        goal.getCompletedTeam().takePoint();
        goal.setCompleted(false, null);

        if (sendPacket) {
            var payload = new CompleteTaskPayload(goal.getId(), -1);
            for (ServerPlayerEntity serverPlayer : LockoutServer.server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(serverPlayer, payload);
            }
        }
    }

    private void sendGoalCompletedPacket(Goal goal, LockoutTeam team) {
        var payload = new CompleteTaskPayload(goal.getId(), teams.indexOf(team));
        for (ServerPlayerEntity serverPlayer : LockoutServer.server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(serverPlayer, payload);
        }
    }

    private void evaluateWinnerAndEndGame(LockoutTeam team) {
        PlayerManager playerManager = LockoutServer.server.getPlayerManager();

        List<LockoutTeam> winners = new ArrayList<>();
        if (isWinner(team)) {
            playerManager.broadcast(Text.literal(team.getDisplayName() + " wins."), false);
            winners.add(team);
            setRunning(false);
        } else {
            if (getRemainingGoals() == 0 && teams.size() > 1) {
                int maxCompleted = teams.stream().max(Comparator.comparingInt(LockoutTeam::getPoints)).get().getPoints();
                List<? extends LockoutTeam> winnerTeams = teams.stream().filter(t -> t.getPoints() == maxCompleted).toList();
                winners.addAll(winnerTeams);
                playerManager.broadcast(Text.literal("It's a tie! " + getWinnerTeamsString(winnerTeams) + " win."), false);
                setRunning(false);
            }
        }

        if (!this.isRunning) {
            var payload = new EndLockoutPayload(winners.stream().mapToInt(winner -> teams.indexOf(winner)).toArray(), System.currentTimeMillis());
            for (ServerPlayerEntity serverPlayer : LockoutServer.server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(serverPlayer, payload);
            }
        }
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setStarted(boolean hasStarted) {
        this.hasStarted = hasStarted;
    }

    public boolean isLockoutPlayer(PlayerEntity player) {
        return isLockoutPlayer(player.getUuid());
    }
    public boolean isLockoutPlayer(UUID playerId) {
        for (LockoutTeam team : teams) {
            if (((LockoutTeamServer)team).getPlayers().contains(playerId)) {
                return true;
            }
        }
        return false;
    }

    public LockoutTeam getPlayerTeam(UUID playerId) {
        for (LockoutTeam team : teams) {
            if (((LockoutTeamServer)team).getPlayers().contains(playerId)) {
                return team;
            }
        }
        return null;
    }

    public LockoutTeam getOpponentTeam(UUID playerId) {
        for (LockoutTeam team : teams) {
            if (!((LockoutTeamServer)team).getPlayers().contains(playerId)) {
                return team;
            }
        }
        return null;
    }
    public LockoutTeam getOpponentTeam(LockoutTeam team) {
        for (LockoutTeam t : teams) {
            if (!Objects.equals(t, team)) {
                return t;
            }
        }
        return null;
    }

    public boolean isWinner(LockoutTeam team) {
        if (teams.size() == 1) {
            return getRemainingGoals() == 0;
        }
        for (LockoutTeam t : teams) {
            if (team == t) continue;
            if (t.getPoints() + getRemainingGoals() >= team.getPoints()) {
                return false;
            }
        }
        return true;
    }

    public int getRemainingGoals() {
        return (int) board.getGoals().stream().filter(goal -> !goal.isCompleted()).count();
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }



    private static String getWinnerTeamsString(List<? extends LockoutTeam> teams) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < teams.size(); i++) {
            if (i > 0) {
                if (i + 1 == teams.size()) {
                    sb.append(" and ");
                } else {
                    sb.append(", ");
                }
            }
            LockoutTeam team = teams.get(i);
            sb.append(team.getDisplayName());
        }
        return sb.toString();
    }

    public UpdateTimerPayload getUpdateTimerPacket() {
        return new UpdateTimerPayload(ticks);
    }

    public LockoutGoalsTeamsPayload getTeamsGoalsPacket() {
        return new LockoutGoalsTeamsPayload(teams.stream().map(team -> (LockoutTeam) team).toList(),
                board.getGoals().stream().map(goal -> new Pair<>(new Pair<>(goal.getId(), goal.getData()), teams.indexOf(goal.getCompletedTeam()))).toList(),
                isRunning);
    }


    public final Map<UUID, Integer> levels = new LinkedHashMap<>();
    private UUID mostLevelsPlayer;

    public void recalculateXPGoal(Goal goal) {
        List<UUID> largestLevelPlayers = new ArrayList<>();
        int largestLevel = 0;

        for (UUID uuid : levels.keySet()) {
            if (levels.get(uuid) == largestLevel) {
                largestLevelPlayers.add(uuid);
                continue;
            }
            if (levels.get(uuid) > largestLevel) {
                largestLevelPlayers.clear();
                largestLevelPlayers.add(uuid);
                largestLevel = levels.get(uuid);
            }
        }

        if (largestLevel == 0) {
            if (this.mostLevelsPlayer != null) {
                this.mostLevelsPlayer = null;
                clearGoalCompletion(goal, true);
            }
            return;
        }

        if (!largestLevelPlayers.contains(mostLevelsPlayer)) {
            this.mostLevelsPlayer = largestLevelPlayers.get(0);
            updateGoalCompletion(goal, largestLevelPlayers.get(0));
        }
    }

}
