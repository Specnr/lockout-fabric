package me.marin.lockout.server;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.marin.lockout.*;
import me.marin.lockout.client.LockoutBoard;
import me.marin.lockout.generator.BoardGenerator;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.network.CustomBoardPayload;
import me.marin.lockout.network.LockoutVersionPayload;
import me.marin.lockout.network.StartLockoutPayload;
import me.marin.lockout.network.UpdateTooltipPayload;
import me.marin.lockout.server.handlers.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.AdvancementCommand;
import net.minecraft.server.command.LocateCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import oshi.util.tuples.Pair;

import java.util.*;

public class LockoutServer {

    public static final int LOCATE_SEARCH = 750;
    public static final Map<RegistryKey<Biome>, LocateData> BIOME_LOCATE_DATA = new HashMap<>();
    public static final Map<RegistryKey<Structure>, LocateData> STRUCTURE_LOCATE_DATA = new HashMap<>();
    public static final List<DyeColor> AVAILABLE_DYE_COLORS = new ArrayList<>();

    private static int lockoutStartTime = 60;
    private static int boardSize;

    public static Lockout lockout;
    public static MinecraftServer server;
    public static CompassItemHandler compassHandler;

    public static final Map<LockoutRunnable, Long> gameStartRunnables = new HashMap<>();

    private static LockoutBoard CUSTOM_BOARD = null;

    private static boolean isInitialized = false;

    public static Map<ServerPlayerEntity, Integer> waitingForVersionPacketPlayersMap = new HashMap<>();

    public static void initializeServer() {
        lockout = null;
        compassHandler = null;
        gameStartRunnables.clear();

        // Ideally, rejoining a world gets detected here, and this data doesn't get wiped
        BIOME_LOCATE_DATA.clear();
        STRUCTURE_LOCATE_DATA.clear();
        AVAILABLE_DYE_COLORS.clear();

        LockoutConfig.load(); // reload config every time the server starts
        boardSize = LockoutConfig.getInstance().boardSize;
        Lockout.log("Using default board size: " + boardSize);

        if (isInitialized) return;
        isInitialized = true;

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(new AllowChatMessageEventHandler());

        ServerPlayerEvents.AFTER_RESPAWN.register(new AfterRespawnEventHandler());

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(new AfterPlayerChangeWorldEventHandler());

        ServerPlayConnectionEvents.JOIN.register(new PlayerJoinEventHandler());

        ServerTickEvents.END_SERVER_TICK.register(new EndServerTickEventHandler());

        ServerLivingEntityEvents.AFTER_DEATH.register(new AfterDeathEventHandler());

        UseBlockCallback.EVENT.register(new UseBlockEventHandler());

        ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedEventHandler());

        ServerPlayConnectionEvents.DISCONNECT.register((handler, minecraftServer) -> {
            waitingForVersionPacketPlayersMap.remove(handler.getPlayer());
        });

        ServerPlayNetworking.registerGlobalReceiver(LockoutVersionPayload.ID, (payload, context) -> {
            // Client has Lockout mod, compare versions, then kick or initialize
            ServerPlayerEntity player = context.player();
            waitingForVersionPacketPlayersMap.remove(player);

            String version = payload.version();
            if (!version.equals(LockoutInitializer.MOD_VERSION.getFriendlyString())) {
                player.networkHandler.disconnect(Text.of("Wrong Lockout version: v" + version + ".\nServer is using Lockout v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + "."));
                return;
            }

            if (!Lockout.isLockoutRunning(lockout)) return;

            if (lockout.isLockoutPlayer(player.getUuid())) {
                LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal instanceof HasTooltipInfo hasTooltipInfo) {
                        ServerPlayNetworking.send(player, new UpdateTooltipPayload(goal.getId(), String.join("\n", hasTooltipInfo.getTooltip(team, player))));
                    }
                }
                player.changeGameMode(GameMode.SURVIVAL);
            } else {
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal instanceof HasTooltipInfo hasTooltipInfo) {
                        ServerPlayNetworking.send(player, new UpdateTooltipPayload(goal.getId(), String.join("\n", hasTooltipInfo.getSpectatorTooltip())));
                    }
                }
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You are spectating this match.").formatted(Formatting.GRAY, Formatting.ITALIC));
            }

            ServerPlayNetworking.send(player, lockout.getTeamsGoalsPacket());
            ServerPlayNetworking.send(player, lockout.getUpdateTimerPacket());
            if (lockout.hasStarted()) {
                ServerPlayNetworking.send(player, StartLockoutPayload.INSTANCE);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(CustomBoardPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            if (!server.isSingleplayer()) {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("You do not have the permission for this command!").formatted(Formatting.RED));
                    return;
                }
            }

            boolean clearBoard = payload.boardOrClear().isEmpty();
            if (clearBoard) {
                CUSTOM_BOARD = null;
                player.sendMessage(Text.literal("Removed custom board."));
            } else {
                // validate board
                List<String> invalidGoals = new ArrayList<>();
                for (Pair<String, String> goal : payload.boardOrClear().get()) {
                    if (!GoalRegistry.INSTANCE.isGoalValid(goal.getA(), goal.getB())) {
                        invalidGoals.add(" - '" + goal.getA() + "'" + ("null".equals(goal.getB()) ? "" : (" with data: '" + goal.getB() + "'")));
                    }
                }
                if (!invalidGoals.isEmpty()) {
                    player.sendMessage(Text.literal("Invalid board. Could not create goals:\n" + String.join("\n", invalidGoals)));
                    return;
                }
                CUSTOM_BOARD = new LockoutBoard(payload.boardOrClear().get());
                player.sendMessage(Text.literal("Set custom board."));
            }
        });
    }

    public static LocateData locateBiome(MinecraftServer server, RegistryKey<Biome> biome) {
        if (BIOME_LOCATE_DATA.containsKey(biome)) return BIOME_LOCATE_DATA.get(biome);

        var spawnPoint = server.getOverworld().getSpawnPoint();
        var currentPos = spawnPoint.getPos();

        var pair = server.getOverworld().locateBiome(
                biomeRegistryEntry -> biomeRegistryEntry.matchesKey(biome),
                currentPos,
                LOCATE_SEARCH,
                32,
                64);

        LocateData data= new LocateData(false,0);
        if (pair != null) {
            int distance = MathHelper.floor(LocateCommand.getDistance(currentPos.getX(), currentPos.getZ(), pair.getFirst().getX(), pair.getFirst().getZ()));
            if (distance < LOCATE_SEARCH) {
                data = new LocateData(true, distance);
            }
        }
        BIOME_LOCATE_DATA.put(biome, data);

        return data;
    }

    public static LocateData locateStructure(MinecraftServer server, RegistryKey<Structure> structure) {
        if (STRUCTURE_LOCATE_DATA.containsKey(structure)) return STRUCTURE_LOCATE_DATA.get(structure);

        var spawnPoint = server.getOverworld().getSpawnPoint();
        var currentPos = spawnPoint.getPos();

        Registry<Structure> registry = server.getOverworld().getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        RegistryEntryList<Structure> structureList = RegistryEntryList.of(registry.getOrThrow(structure));

        var pair = server.getOverworld().getChunkManager().getChunkGenerator().locateStructure(
                server.getOverworld(),
                structureList,
                currentPos,
                LOCATE_SEARCH,
                false);

        LocateData data = new LocateData(false, 0);
        if (pair != null) {
            int distance = MathHelper.floor(LocateCommand.getDistance(currentPos.getX(), currentPos.getZ(), pair.getFirst().getX(), pair.getFirst().getZ()));
            if (distance < LOCATE_SEARCH) {
                data = new LocateData(true, distance);
            }
        }
        STRUCTURE_LOCATE_DATA.put(structure, data);

        return data;
    }

    public static int lockoutCommandLogic(CommandContext<ServerCommandSource> context) {
        List<LockoutTeamServer> teams = new ArrayList<>();

        int ret = parseArgumentsIntoTeams(teams, context, false);
        if (ret == 0) return 0;

        startLockout(teams);

        return 1;
    }

    public static int blackoutCommandLogic(CommandContext<ServerCommandSource> context) {
        List<LockoutTeamServer> teams = new ArrayList<>();

        int ret = parseArgumentsIntoTeams(teams, context, true);
        if (ret == 0) return 0;

        startLockout(teams);

        return 1;
    }

    private static void startLockout(List<LockoutTeamServer> teams) {
        // Clear old runnables
        gameStartRunnables.clear();

        PlayerManager playerManager = server.getPlayerManager();
        List<ServerPlayerEntity> allServerPlayers = playerManager.getPlayerList();
        List<UUID> allLockoutPlayers = teams.stream()
                .flatMap(team -> team.getPlayers().stream())
                .toList();
        List<UUID> allSpectatorPlayers = allServerPlayers.stream()
                .map(ServerPlayerEntity::getUuid)
                .filter(uuid -> !allLockoutPlayers.contains(uuid))
                .toList();

        for (ServerPlayerEntity serverPlayer : allServerPlayers) {
            serverPlayer.getInventory().clear();
            serverPlayer.setHealth(serverPlayer.getMaxHealth());
            serverPlayer.clearStatusEffects();
            serverPlayer.getHungerManager().setSaturationLevel(5);
            serverPlayer.getHungerManager().setFoodLevel(20);
            serverPlayer.getHungerManager().exhaustion = 0.0f;
            serverPlayer.setExperienceLevel(0);
            serverPlayer.setExperiencePoints(0);
            serverPlayer.setOnFire(false);

            // Clear all stats
            for (@SuppressWarnings("unchecked") StatType<Object> statType : new StatType[]{Stats.CRAFTED, Stats.MINED, Stats.USED, Stats.BROKEN, Stats.PICKED_UP, Stats.DROPPED, Stats.KILLED, Stats.KILLED_BY, Stats.CUSTOM}) {
                for (Identifier id : statType.getRegistry().getIds()) {
                    serverPlayer.resetStat(statType.getOrCreateStat(statType.getRegistry().get(id)));
                }
            }
            serverPlayer.getStatHandler().sendStats(serverPlayer);
            // Clear all advancements
            AdvancementCommand.Operation.REVOKE.processAll(serverPlayer, server.getAdvancementLoader().getAdvancements(), false);

            if (allLockoutPlayers.contains(serverPlayer.getUuid())) {
                serverPlayer.changeGameMode(GameMode.ADVENTURE);
            } else {
                serverPlayer.changeGameMode(GameMode.SPECTATOR);
                serverPlayer.sendMessage(Text.literal("You are spectating this match.").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        }

        ServerWorld world = server.getWorld(ServerWorld.OVERWORLD);

        // Generate & set board
        LockoutBoard lockoutBoard;
        if (CUSTOM_BOARD == null) {
            BoardGenerator boardGenerator = new BoardGenerator(GoalRegistry.INSTANCE.getRegisteredGoals(), teams, AVAILABLE_DYE_COLORS, BIOME_LOCATE_DATA, STRUCTURE_LOCATE_DATA);
            lockoutBoard = boardGenerator.generateBoard(boardSize);
        } else {
            // Reset custom board (TODO: do this somewhere else)
            for (Goal goal : CUSTOM_BOARD.getGoals()) {
                goal.setCompleted(false, null);
            }
            lockoutBoard = CUSTOM_BOARD;
        }

        lockout = new Lockout(lockoutBoard, teams);
        lockout.setTicks(-20L * lockoutStartTime); // see Lockout#ticks

        compassHandler = new CompassItemHandler(allLockoutPlayers, playerManager);

        List<Goal> tooltipGoals = new ArrayList<>(lockout.getBoard().getGoals()).stream().filter(g -> g instanceof HasTooltipInfo).toList();
        for (Goal goal : tooltipGoals) {
            // Update teams tooltip
            for (LockoutTeam team : lockout.getTeams()) {
                ((LockoutTeamServer) team).sendTooltipUpdate((Goal & HasTooltipInfo) goal, false);
            }
            // Update spectator tooltip
            if (!allSpectatorPlayers.isEmpty()) {
                var payload = new UpdateTooltipPayload(goal.getId(), String.join("\n", ((HasTooltipInfo) goal).getSpectatorTooltip()));
                for (UUID spectator : allSpectatorPlayers) {
                    ServerPlayNetworking.send(playerManager.getPlayer(spectator), payload);
                }
            }
        }

        for (ServerPlayerEntity player : allServerPlayers) {
            ServerPlayNetworking.send(player, lockout.getTeamsGoalsPacket());
            ServerPlayNetworking.send(player, lockout.getUpdateTimerPacket());

            if (!lockout.isSoloBlackout() && lockout.isLockoutPlayer(player.getUuid())) {
                player.giveItemStack(compassHandler.newCompass());
            }
        }

        world.setTimeOfDay(0);

        for (int i = 3; i >= 0; i--) {
            if (i > 0) {
                final int secs = i;
                ((LockoutRunnable) () -> {
                    playerManager.broadcast(Text.literal("Starting in " + secs + "..."), false);
                }).runTaskAfter(20L * (lockoutStartTime - i));
            } else {
                ((LockoutRunnable) () -> {
                    lockout.setStarted(true);

                    for (ServerPlayerEntity player : allServerPlayers) {
                        if (player == null) continue;
                        ServerPlayNetworking.send(player, StartLockoutPayload.INSTANCE);
                        if (allLockoutPlayers.contains(player.getUuid())) {
                            player.changeGameMode(GameMode.SURVIVAL);
                        }
                    }
                    server.getPlayerManager().broadcast(Text.literal(lockout.getModeName() + " has begun."), false);
                }).runTaskAfter(20L * lockoutStartTime);
            }
        }
    }

    private static int parseArgumentsIntoTeams(List<LockoutTeamServer> teams, CommandContext<ServerCommandSource> context, boolean isBlackout) {
        String argument = null;

        PlayerManager playerManager = server.getPlayerManager();

        try {
            argument = context.getArgument("player names", String.class);
            String[] players = argument.split(" +");
            if (isBlackout) {
                if (players.length == 0) {
                    context.getSource().sendError(Text.literal("Not enough players listed."));
                    return 0;
                }

                List<String> playerNames = new ArrayList<>();
                for (String player : players) {
                    if (playerManager.getPlayer(player) == null) {
                        context.getSource().sendError(Text.literal("Player " + player + " is invalid."));
                        return 0;
                    }
                    playerNames.add(playerManager.getPlayer(player).getName().getString());
                }
                teams.add(new LockoutTeamServer(playerNames, Formatting.byColorIndex(Lockout.COLOR_ORDERS[0]), server));

            } else {
                if (players.length < 2) {
                    context.getSource().sendError(Text.literal("Not enough players listed. Make sure you separate player names with spaces."));
                    return 0;
                }
                if (players.length > 16) {
                    context.getSource().sendError(Text.literal("Too many players listed."));
                    return 0;
                }

                for (int i = 0; i < players.length; i++) {
                    String player = players[i];
                    if (playerManager.getPlayer(player) == null) {
                        context.getSource().sendError(Text.literal("Player " + player + " is invalid."));
                        return 0;
                    }
                    teams.add(new LockoutTeamServer(List.of(playerManager.getPlayer(player).getName().getString()), Formatting.byColorIndex(Lockout.COLOR_ORDERS[i]), server));
                }
            }

        } catch (Exception ignored) {}

        if (argument == null) {
            try {
                ServerScoreboard scoreboard = server.getScoreboard();

                argument = context.getArgument(isBlackout ? "team name" : "team names", String.class);
                String[] teamNames = argument.split(" +");
                if (isBlackout) {
                    if (teamNames.length == 0) {
                        context.getSource().sendError(Text.literal("Not enough teams listed."));
                        return 0;
                    }
                    if (teamNames.length > 1) {
                        context.getSource().sendError(Text.literal("Only one team can play Blackout."));
                        return 0;
                    }
                } else {
                    if (teamNames.length < 2) {
                        context.getSource().sendError(Text.literal("Not enough teams listed. Make sure you separate team names with spaces."));
                        return 0;
                    }
                    if (teamNames.length > 16) {
                        context.getSource().sendError(Text.literal("Too many teams listed."));
                        return 0;
                    }
                }

                List<Team> scoreboardTeams = new ArrayList<>();
                for (String teamName : teamNames) {
                    Team team = scoreboard.getTeam(teamName);
                    if (team == null) {
                        context.getSource().sendError(Text.literal("Team " + teamName + " is invalid."));
                        return 0;
                    }
                    for (String player : team.getPlayerList()) {
                        if (playerManager.getPlayer(player) == null) {
                            context.getSource().sendError(Text.literal("Player " + player + " on team " + teamName + " is invalid. Remove them from the team and try again."));
                            return 0;
                        }
                    }
                    scoreboardTeams.add(team);
                }
                for (Team team : scoreboardTeams) {
                    if (team.getPlayerList().isEmpty()) {
                        context.getSource().sendError(Text.literal("Team " + team.getName() + " doesn't have any players."));
                        return 0;
                    }
                    Formatting teamColor = team.getColor();
                    if (teamColor.getColorValue() == null || teamHasColor(teams, teamColor)) {
                        // Select an available color.
                        boolean found = false;
                        for (int colorOrder : Lockout.COLOR_ORDERS) {
                            if (!teamHasColor(teams, Formatting.byColorIndex(colorOrder))) {
                                found = true;
                                team.setColor(Formatting.byColorIndex(colorOrder));
                                break;
                            }
                        }
                        if (!found) {
                            context.getSource().sendError(Text.literal("Could not find assignable color for team " + team.getName() + ". Try recreating teams."));
                            return 0;
                        }
                    }
                    List<String> actualPlayerNames = new ArrayList<>();
                    for (String playerName : team.getPlayerList()) {
                        actualPlayerNames.add(playerManager.getPlayer(playerName).getName().getString());
                    }
                    teams.add(new LockoutTeamServer(new ArrayList<>(actualPlayerNames), team.getColor(), server));
                }
            } catch (Exception ignored) {}
        }

        if (argument == null) {
            context.getSource().sendError(Text.literal("Illegal argument."));
            return 0;
        }
        return 1;
    }

    private static boolean teamHasColor(List<LockoutTeamServer> teams, Formatting color) {
        for (LockoutTeam lockoutTeam : teams) {
            if (lockoutTeam.getColor() == color) {
                return true;
            }
        }
        return false;
    }

    public static int setChat(CommandContext<ServerCommandSource> context, ChatManager.Type type) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This is a player-only command."));
            return 0;
        }

        ChatManager.Type curr = ChatManager.getChat(player);
        if (curr == type) {
            player.sendMessage(Text.of("You are already chatting in " + type.name() + "."));
        } else {
            player.sendMessage(Text.of("You are now chatting in " + type.name() + "."));
            ChatManager.setChat(player, type);
        }
        return 1;
    }

    public static int giveGoal(CommandContext<ServerCommandSource> context) {
        try {
            if (!Lockout.isLockoutRunning(lockout)) {
                context.getSource().sendError(Text.literal("There's no active lockout match."));
                return 0;
            }

            int idx = context.getArgument("goal number", Integer.class);

            Collection<PlayerConfigEntry> playerConfigs;
            try {
                playerConfigs = GameProfileArgumentType.getProfileArgument(context, "player name");
            } catch (CommandSyntaxException e) {
                context.getSource().sendError(Text.literal("Invalid target."));
                return 0;
            }

            if (playerConfigs.size() != 1) {
                context.getSource().sendError(Text.literal("Invalid number of targets."));
                return 0;
            }
            PlayerConfigEntry playerConfig = playerConfigs.stream().findFirst().get();
            if (!lockout.isLockoutPlayer(playerConfig.id())) {
                context.getSource().sendError(Text.literal("Player " + playerConfig.name() + " is not playing Lockout."));
                return 0;
            }

            if (idx > lockout.getBoard().getGoals().size()) {
                context.getSource().sendError(Text.literal("Goal number does not exist on the board."));
                return 0;
            }
            Goal goal = lockout.getBoard().getGoals().get(idx - 1);

            context.getSource().sendMessage(Text.of("Gave " + playerConfig.name() + " goal \"" + goal.getGoalName() + "\"."));
            lockout.updateGoalCompletion(goal, playerConfig.id());
            return 1;
        } catch (RuntimeException e) {
            Lockout.error(e);
            return 0;
        }
    }

    public static int setStartTime(CommandContext<ServerCommandSource> context) {
        int seconds = context.getArgument("seconds", Integer.class);

        lockoutStartTime = seconds;
        context.getSource().sendMessage(Text.of("Updated start time to " + seconds + "s."));
        return 1;
    }

    public static int setBoardSize(CommandContext<ServerCommandSource> context) {
        int size = context.getArgument("board size", Integer.class);

        boardSize = size;
        context.getSource().sendMessage(Text.of("Updated board size to " + size + "."));
        return 1;
    }

}